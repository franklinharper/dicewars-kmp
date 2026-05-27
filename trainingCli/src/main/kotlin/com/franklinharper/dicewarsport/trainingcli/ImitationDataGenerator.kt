package com.franklinharper.dicewarsport.trainingcli

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.KotlinRandomSource
import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.neural.NeuralActionEncoder
import com.franklinharper.dicewarsport.ai.neural.NeuralStateEncoder
import com.franklinharper.dicewarsport.isLegalAttack
import com.franklinharper.dicewarsport.nextPlayer
import com.franklinharper.dicewarsport.resolveBattle
import com.franklinharper.dicewarsport.rollBattle
import com.franklinharper.dicewarsport.startSupply
import com.franklinharper.dicewarsport.supplyOneDie
import com.franklinharper.dicewarsport.tournament.RoundConfig
import com.franklinharper.dicewarsport.tournament.TournamentParticipant
import com.franklinharper.dicewarsport.tournament.deriveRoundSeed
import com.franklinharper.dicewarsport.tournament.rotatedLeft
import com.franklinharper.dicewarsport.tournament.scoreCompletedRound
import com.franklinharper.dicewarsport.tournament.scoreFailedRound
import java.io.File
import java.util.zip.GZIPOutputStream
import kotlin.random.Random

class ImitationDataGenerator(
    private val gameFactory: (playerCount: Int, random: RandomSource) -> DicewarsGame = { playerCount, random ->
        DicewarsGame.generate(playerCount, random)
    },
) {
    fun generate(
        participants: List<TournamentParticipant>,
        rounds: Int,
        seed: Int?,
        maxActionsPerRound: Int,
        outPath: File,
    ): ImitationDataResult {
        require(rounds > 0) { "rounds must be greater than zero" }
        require(participants.size >= 2) { "at least two participants are required" }
        outPath.parentFile?.mkdirs()
        val effectiveSeed = seed ?: Random.Default.nextInt()
        var recordsWritten = 0

        GZIPOutputStream(outPath.outputStream()).bufferedWriter().use { writer ->
            for (roundNumber in 1..rounds) {
                val seated = participants.rotatedLeft((roundNumber - 1) % participants.size)
                val roundSeed = deriveRoundSeed(effectiveSeed, roundNumber)
                val records = generateRoundRecords(
                    config = RoundConfig(
                        roundNumber = roundNumber,
                        participants = seated,
                        roundSeed = roundSeed,
                        maxActionsPerRound = maxActionsPerRound,
                    ),
                )
                for (record in records) {
                    writer.appendLine(record)
                    recordsWritten++
                }
            }
        }

        return ImitationDataResult(recordsWritten = recordsWritten, seed = effectiveSeed)
    }

    private fun generateRoundRecords(config: RoundConfig): List<String> {
        val random = KotlinRandomSource(Random(config.roundSeed))
        val strategies = config.participants.map { it.aiFactory(random) }
        var game = gameFactory(config.participants.size, random)
        var actionsTaken = 0
        var activePlayers = activePlayerSlots(game)
        var eliminationOrder = emptyList<String>()
        val pending = mutableListOf<PendingRecord>()

        while (activePlayers.size > 1 && actionsTaken < config.maxActionsPerRound) {
            val actor = game.currentPlayerId()
            val participantId = config.participants[actor].id
            val move = strategies[actor].chooseMove(game)
            val chosenActionIndex = NeuralActionEncoder.actionIndexFor(move)
            val legalMask = NeuralActionEncoder.legalActionMask(game, actor)
            val legalActionIndices = legalMask.indices.filter { legalMask[it] }

            for (perspectivePlayer in activePlayers) {
                val encoding = NeuralStateEncoder.encode(
                    game = game,
                    actorPlayer = actor,
                    perspectivePlayer = perspectivePlayer,
                )
                pending.add(
                    PendingRecord(
                        prefixJson = buildRecordPrefixJson(
                            config = config,
                            actionNumber = actionsTaken + 1,
                            actorPlayer = actor,
                            perspectivePlayer = perspectivePlayer,
                            botId = participantId,
                            chosenActionIndex = chosenActionIndex,
                            legalActionIndices = legalActionIndices,
                            policyWeight = if (perspectivePlayer == actor) 1.0f else 0.0f,
                            encoding = encoding,
                        ),
                        perspectiveParticipantId = config.participants[perspectivePlayer].id,
                    ),
                )
            }

            if (move != null && game.isLegalAttack(move.from, move.to, actor)) {
                val roll = rollBattle(
                    attackerDiceCount = game.areas[move.from].dice,
                    defenderDiceCount = game.areas[move.to].dice,
                    random = random,
                )
                game = game.resolveBattle(move.from, move.to, roll)
            } else {
                game = finishBotTurn(game, actor, random)
            }

            actionsTaken++
            val newActivePlayers = activePlayerSlots(game)
            val eliminated = activePlayers
                .filter { it !in newActivePlayers }
                .map { config.participants[it].id }
                .filter { it !in eliminationOrder }
            eliminationOrder = eliminationOrder + eliminated
            activePlayers = newActivePlayers
        }

        val participantIds = config.participants.map { it.id }
        val winner = activePlayers.singleOrNull()?.let { config.participants[it].id }
        val scores = if (winner != null) {
            val completeEliminationOrder = eliminationOrder + participantIds.filter {
                it != winner && it !in eliminationOrder
            }
            scoreCompletedRound(participantIds, winner, completeEliminationOrder)
        } else {
            scoreFailedRound(participantIds)
        }
        val maxScore = 2.0f * config.participants.size

        return pending.map { record ->
            val value = (scores[record.perspectiveParticipantId] ?: 0).toFloat() / maxScore
            record.prefixJson + ",\"value_target\":${value.toJsonNumber()}}"
        }
    }

    private fun buildRecordPrefixJson(
        config: RoundConfig,
        actionNumber: Int,
        actorPlayer: Int,
        perspectivePlayer: Int,
        botId: String,
        chosenActionIndex: Int,
        legalActionIndices: List<Int>,
        policyWeight: Float,
        encoding: com.franklinharper.dicewarsport.ai.neural.NeuralStateEncoding,
    ): String = buildString {
        append('{')
        append("\"schema_version\":1")
        append(",\"encoder_version\":${encoding.encoderVersion}")
        append(",\"action_space_version\":1")
        append(",\"round_seed\":${config.roundSeed}")
        append(",\"round_number\":${config.roundNumber}")
        append(",\"action_number\":$actionNumber")
        append(",\"actor_player\":$actorPlayer")
        append(",\"perspective_player\":$perspectivePlayer")
        append(",\"bot_id\":\"${botId.escapeJson()}\"")
        append(",\"chosen_action_index\":$chosenActionIndex")
        append(",\"legal_action_mask\":[${legalActionIndices.joinToString(",")}] ".trimEnd())
        append(",\"policy_weight\":${policyWeight.toJsonNumber()}")
        append(",\"state\":{")
        append("\"node_features\":${encoding.nodeFeatures.toJson()}")
        append(",\"adjacency\":${encoding.adjacency.toJson()}")
        append(",\"global_features\":${encoding.globalFeatures.toJson()}")
        append(",\"area_mask\":${encoding.areaMask.toJson()}")
        append(",\"player_mask\":${encoding.playerMask.toJson()}")
        append('}')
    }

    private fun finishBotTurn(game: DicewarsGame, player: Int, random: RandomSource): DicewarsGame {
        var next = game.startSupply(player)
        while (true) {
            val (suppliedGame, suppliedArea) = next.supplyOneDie(player, random)
            next = suppliedGame
            if (suppliedArea == null) break
        }
        return next.nextPlayer()
    }

    private fun activePlayerSlots(game: DicewarsGame): List<Int> =
        (0 until game.pmax).filter { player -> game.players[player].maxConnectedAreaCount > 0 }
}

data class ImitationDataResult(
    val recordsWritten: Int,
    val seed: Int,
)

private data class PendingRecord(
    val prefixJson: String,
    val perspectiveParticipantId: String,
)

private fun Array<FloatArray>.toJson(): String = joinToString(prefix = "[", postfix = "]") { it.toJson() }
private fun FloatArray.toJson(): String = joinToString(prefix = "[", postfix = "]") { it.toJsonNumber() }
private fun Array<BooleanArray>.toJson(): String = joinToString(prefix = "[", postfix = "]") { it.toJson() }
private fun BooleanArray.toJson(): String = joinToString(prefix = "[", postfix = "]") { if (it) "true" else "false" }
private fun Float.toJsonNumber(): String = if (isFinite()) toString() else error("non-finite float cannot be written as JSON")
private fun String.escapeJson(): String = buildString {
    for (ch in this@escapeJson) {
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(ch)
        }
    }
}
