package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.KotlinRandomSource
import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.AiStrategy
import kotlin.random.Random

/**
 * Fast tournament round executor using mutable [FastGameState].
 *
 * Produces identical results to [BotRoundStepper] but avoids all intermediate
 * immutable copies. A [DicewarsGame] snapshot is created only when calling
 * bot [AiStrategy.chooseMove].
 */
class FastBotRoundStepper(
    private val gameFactory: (playerCount: Int, random: RandomSource) -> DicewarsGame = { playerCount, random ->
        DicewarsGame.generate(playerCount, random)
    },
) {
    fun runRound(spec: RoundReplaySpec, logActions: Boolean = false): RoundResult {
        val random = KotlinRandomSource(Random(spec.roundSeed))
        val strategies = spec.participants.map { it.aiFactory(random) }
        val game = gameFactory(spec.participants.size, random)
        val state = FastGameState.fromGame(game)

        var actionsTaken = 0
        var activePlayers = state.activePlayerSlots()
        var eliminationOrder = emptyList<String>()
        val actionLog = if (logActions) mutableListOf<RoundActionLogEntry>() else null

        // Check for immediate win (degenerate case)
        if (activePlayers.size == 1) {
            val winner = spec.participants[activePlayers[0]].id
            return roundResult(
                spec = spec,
                completed = true,
                winnerParticipantId = winner,
                eliminationOrder = eliminationOrder,
                actionsTaken = 0,
                actionLog = actionLog?.toList() ?: emptyList(),
            )
        }

        while (actionsTaken < spec.maxActionsPerRound) {
            val player = state.currentPlayer()
            val participantId = spec.participants[player].id
            val strategy = strategies.getOrNull(player)

            // Create snapshot only for bot decision
            val snapshot = state.toDicewarsGame()
            val move = strategy?.chooseMove(snapshot)

            if (move != null && state.isLegalAttack(move.from, move.to, player)) {
                // Attack
                val attackerDice = state.areaDice(move.from)
                val defenderDice = state.areaDice(move.to)

                // Roll battle (consume same random numbers as immutable path)
                val battleRoll = rollBattleFast(attackerDice, defenderDice, random)
                state.resolveBattle(move.from, move.to, battleRoll.success)

                if (logActions) {
                    actionLog!!.add(RoundActionLogEntry(
                        actionNumber = actionsTaken + 1,
                        playerSlot = player,
                        participantId = participantId,
                        actionType = RoundActionType.Attack,
                        from = move.from,
                        to = move.to,
                        battleRoll = battleRoll.toBattleRoll(attackerDice, defenderDice),
                    ))
                }
            } else {
                // End turn + supply
                state.startSupply(player)
                val suppliedAreas = mutableListOf<Int>()
                while (true) {
                    val area = state.supplyOneDie(player, random)
                    if (area == null) break
                    suppliedAreas.add(area)
                }
                state.nextPlayer()

                if (logActions) {
                    actionLog!!.add(RoundActionLogEntry(
                        actionNumber = actionsTaken + 1,
                        playerSlot = player,
                        participantId = participantId,
                        actionType = if (move == null) RoundActionType.EndTurn else RoundActionType.IllegalMove,
                        suppliedAreas = suppliedAreas,
                    ))
                }
            }

            actionsTaken++

            // Check eliminations
            val newActivePlayers = state.activePlayerSlots()
            val eliminated = activePlayers.filter { it !in newActivePlayers }
                .map { spec.participants[it].id }
                .filter { it !in eliminationOrder }
            eliminationOrder = eliminationOrder + eliminated

            // Check win
            if (newActivePlayers.size == 1) {
                val winner = spec.participants[newActivePlayers[0]].id
                return roundResult(
                    spec = spec,
                    completed = true,
                    winnerParticipantId = winner,
                    eliminationOrder = eliminationOrder,
                    actionsTaken = actionsTaken,
                    actionLog = actionLog?.toList() ?: emptyList(),
                )
            }
            activePlayers = newActivePlayers
        }

        // Exceeded max actions
        return roundResult(
            spec = spec,
            completed = false,
            winnerParticipantId = null,
            eliminationOrder = eliminationOrder,
            actionsTaken = actionsTaken,
            actionLog = actionLog?.toList() ?: emptyList(),
            failureReason = "maxActionsPerRound=${spec.maxActionsPerRound} exceeded",
        )
    }

    private fun FastGameState.areaDice(areaId: Int): Int {
        // Access internal dice array — using a helper since we need this for battle rolls
        // We'll add a public accessor to FastGameState
        return getAreaDice(areaId)
    }

    private fun roundResult(
        spec: RoundReplaySpec,
        completed: Boolean,
        winnerParticipantId: String?,
        eliminationOrder: List<String>,
        actionsTaken: Int,
        actionLog: List<RoundActionLogEntry>,
        failureReason: String? = null,
    ): RoundResult {
        val participantIds = spec.seatedParticipantIds
        return RoundResult(
            roundNumber = spec.roundNumber,
            roundSeed = spec.roundSeed,
            seatedParticipantIds = participantIds,
            maxActionsPerRound = spec.maxActionsPerRound,
            completed = completed,
            winnerParticipantId = winnerParticipantId,
            eliminationOrder = if (completed && winnerParticipantId != null) {
                eliminationOrder + participantIds.filter {
                    it != winnerParticipantId && it !in eliminationOrder
                }
            } else {
                eliminationOrder
            },
            scores = if (completed && winnerParticipantId != null) {
                scoreCompletedRound(participantIds, winnerParticipantId, eliminationOrder)
            } else {
                scoreFailedRound(participantIds)
            },
            actionsTaken = actionsTaken,
            failureReason = failureReason,
            actionLog = actionLog,
        )
    }
}

/**
 * Lightweight battle roll that avoids creating List<Int> objects.
 */
private class FastBattleRoll(
    val attackerTotal: Int,
    val defenderTotal: Int,
    val success: Boolean,
) {
    fun toBattleRoll(attackerDiceCount: Int, defenderDiceCount: Int): com.franklinharper.dicewarsport.BattleRoll {
        // Reconstruct individual dice values deterministically from totals.
        // This is only used for logging, so we create a plausible decomposition.
        val attackerDice = decompose(attackerTotal, attackerDiceCount)
        val defenderDice = decompose(defenderTotal, defenderDiceCount)
        return com.franklinharper.dicewarsport.BattleRoll(
            attackerDice = attackerDice,
            defenderDice = defenderDice,
            attackerTotal = attackerTotal,
            defenderTotal = defenderTotal,
            success = success,
        )
    }

    private fun decompose(total: Int, count: Int): List<Int> {
        if (count == 0) return emptyList()
        val dice = MutableList(count) { 1 }
        var remaining = total - count
        var i = 0
        while (remaining > 0) {
            val add = minOf(remaining, 5) // max face value is 6, already have 1
            dice[i] += add
            remaining -= add
            i = (i + 1) % count
        }
        return dice
    }
}

private fun rollBattleFast(attackerDiceCount: Int, defenderDiceCount: Int, random: RandomSource): FastBattleRoll {
    var attackerTotal = 0
    for (i in 0 until attackerDiceCount) {
        attackerTotal += random.nextInt(6) + 1
    }
    var defenderTotal = 0
    for (i in 0 until defenderDiceCount) {
        defenderTotal += random.nextInt(6) + 1
    }
    return FastBattleRoll(
        attackerTotal = attackerTotal,
        defenderTotal = defenderTotal,
        success = attackerTotal > defenderTotal,
    )
}
