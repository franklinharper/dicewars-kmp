package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.KotlinRandomSource
import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.AiStrategy
import com.franklinharper.dicewarsport.isLegalAttack
import com.franklinharper.dicewarsport.nextPlayer
import com.franklinharper.dicewarsport.resolveBattle
import com.franklinharper.dicewarsport.rollBattle
import com.franklinharper.dicewarsport.startSupply
import com.franklinharper.dicewarsport.supplyOneDie
import kotlin.random.Random

data class RoundReplaySpec(
    val roundNumber: Int = 1,
    val roundSeed: Int,
    val participants: List<TournamentParticipant>,
    val maxActionsPerRound: Int,
) {
    val seatedParticipantIds: List<String> = participants.map { it.id }
}

data class BotRoundState(
    val spec: RoundReplaySpec,
    val game: DicewarsGame,
    val random: RandomSource,
    val strategies: List<AiStrategy>,
    val actionsTaken: Int,
    val activePlayers: List<Int>,
    val eliminationOrder: List<String> = emptyList(),
    val completed: Boolean = false,
    val winnerParticipantId: String? = null,
    val failed: Boolean = false,
    val failureReason: String? = null,
)

data class BotRoundStepResult(
    val state: BotRoundState,
    val actionLogEntry: RoundActionLogEntry,
)

class BotRoundStepper(
    private val gameFactory: (playerCount: Int, random: RandomSource) -> DicewarsGame = { playerCount, random ->
        DicewarsGame.generate(playerCount, random)
    },
) {
    fun initialState(spec: RoundReplaySpec): BotRoundState {
        val random = KotlinRandomSource(Random(spec.roundSeed))
        val strategies = spec.participants.map { it.aiFactory(random) }
        val game = gameFactory(spec.participants.size, random)
        val activePlayers = activePlayerSlots(game)
        val winner = activePlayers.singleOrNull()?.let { spec.participants[it].id }
        return BotRoundState(
            spec = spec,
            game = game,
            random = random,
            strategies = strategies,
            actionsTaken = 0,
            activePlayers = activePlayers,
            completed = winner != null,
            winnerParticipantId = winner,
        )
    }

    fun step(state: BotRoundState): BotRoundStepResult {
        if (state.completed) {
            return BotRoundStepResult(state, state.terminalEntry(RoundActionType.RoundWon))
        }
        if (state.failed || state.actionsTaken >= state.spec.maxActionsPerRound) {
            val reason = state.failureReason ?: "maxActionsPerRound=${state.spec.maxActionsPerRound} exceeded"
            val failedState = state.copy(failed = true, failureReason = reason)
            return BotRoundStepResult(failedState, failedState.terminalEntry(RoundActionType.RoundFailed))
        }

        val player = state.game.currentPlayer()
        val participantId = state.spec.participants[player].id
        val strategy = state.strategies.getOrNull(player)
        val move = strategy?.chooseMove(state.game)
        var game = state.game
        val actionType: RoundActionType
        var suppliedAreas = emptyList<Int>()
        var battleRoll: com.franklinharper.dicewarsport.BattleRoll? = null

        if (move != null && game.isLegalAttack(move.from, move.to, player)) {
            actionType = RoundActionType.Attack
            battleRoll = rollBattle(
                attackerDiceCount = game.areas[move.from].dice,
                defenderDiceCount = game.areas[move.to].dice,
                random = state.random,
            )
            game = game.resolveBattle(move.from, move.to, battleRoll)
        } else {
            actionType = if (move == null) RoundActionType.EndTurn else RoundActionType.IllegalMove
            val turnResult = game.finishBotTurnWithSuppliedAreas(player, state.random)
            game = turnResult.first
            suppliedAreas = turnResult.second
        }

        val newActivePlayers = activePlayerSlots(game)
        val eliminatedParticipantIds = state.activePlayers
            .filter { it !in newActivePlayers }
            .map { state.spec.participants[it].id }
            .filter { it !in state.eliminationOrder }
        val eliminationOrder = state.eliminationOrder + eliminatedParticipantIds
        val winner = newActivePlayers.singleOrNull()?.let { state.spec.participants[it].id }
        val newState = state.copy(
            game = game,
            actionsTaken = state.actionsTaken + 1,
            activePlayers = newActivePlayers,
            eliminationOrder = eliminationOrder,
            completed = winner != null,
            winnerParticipantId = winner,
        )

        return BotRoundStepResult(
            state = newState,
            actionLogEntry = RoundActionLogEntry(
                actionNumber = newState.actionsTaken,
                playerSlot = player,
                participantId = participantId,
                actionType = actionType,
                from = move?.from,
                to = move?.to,
                battleRoll = battleRoll,
                suppliedAreas = suppliedAreas,
                eliminatedParticipantIds = eliminatedParticipantIds,
            ),
        )
    }

    fun resultFor(state: BotRoundState, actionLog: List<RoundActionLogEntry> = emptyList()): RoundResult {
        val participantIds = state.spec.seatedParticipantIds
        if (state.completed && state.winnerParticipantId != null) {
            val completeEliminationOrder = state.eliminationOrder + participantIds.filter {
                it != state.winnerParticipantId && it !in state.eliminationOrder
            }
            return RoundResult(
                roundNumber = state.spec.roundNumber,
                roundSeed = state.spec.roundSeed,
                seatedParticipantIds = participantIds,
                maxActionsPerRound = state.spec.maxActionsPerRound,
                completed = true,
                winnerParticipantId = state.winnerParticipantId,
                eliminationOrder = completeEliminationOrder,
                scores = scoreCompletedRound(participantIds, state.winnerParticipantId, completeEliminationOrder),
                actionsTaken = state.actionsTaken,
                actionLog = actionLog,
            )
        }

        return RoundResult(
            roundNumber = state.spec.roundNumber,
            roundSeed = state.spec.roundSeed,
            seatedParticipantIds = participantIds,
            maxActionsPerRound = state.spec.maxActionsPerRound,
            completed = false,
            winnerParticipantId = null,
            eliminationOrder = state.eliminationOrder,
            scores = scoreFailedRound(participantIds),
            actionsTaken = state.actionsTaken,
            failureReason = state.failureReason ?: "maxActionsPerRound=${state.spec.maxActionsPerRound} exceeded",
            actionLog = actionLog,
        )
    }

    private fun BotRoundState.terminalEntry(type: RoundActionType): RoundActionLogEntry = RoundActionLogEntry(
        actionNumber = actionsTaken + 1,
        playerSlot = game.currentPlayer(),
        participantId = spec.participants.getOrNull(game.currentPlayer())?.id ?: "",
        actionType = type,
        eliminatedParticipantIds = emptyList(),
    )
}

private fun activePlayerSlots(game: DicewarsGame): List<Int> =
    (0 until game.pmax).filter { player -> game.players[player].maxConnectedAreaCount > 0 }

private fun DicewarsGame.finishBotTurnWithSuppliedAreas(player: Int, random: RandomSource): Pair<DicewarsGame, List<Int>> {
    var game = startSupply(player)
    val suppliedAreas = mutableListOf<Int>()
    while (true) {
        val (newGame, suppliedArea) = game.supplyOneDie(player, random)
        game = newGame
        if (suppliedArea == null) break
        suppliedAreas.add(suppliedArea)
    }
    return game.nextPlayer() to suppliedAreas.toList()
}
