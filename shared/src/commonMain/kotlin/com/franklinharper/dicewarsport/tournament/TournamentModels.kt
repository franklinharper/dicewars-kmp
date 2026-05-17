package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.AiStrategy

data class TournamentConfig(
    val participants: List<TournamentParticipant>,
    val rounds: Int,
    val seed: Int? = null,
    val maxActionsPerRound: Int = 100_000,
    val logFailedRounds: Boolean = false,
    val logAllRounds: Boolean = false,
) {
    init {
        require(participants.size >= 2) { "Tournament requires at least two participants" }
        require(participants.size <= 8) { "Tournament supports at most eight participants" }
        require(rounds > 0) { "Tournament rounds must be greater than zero" }
        require(maxActionsPerRound > 0) { "maxActionsPerRound must be greater than zero" }
        require(participants.map { it.id }.distinct().size == participants.size) { "Participant IDs must be unique" }
    }
}

data class TournamentParticipant(
    val id: String,
    val displayName: String,
    val aiFactory: (RandomSource) -> AiStrategy,
)

data class RoundConfig(
    val roundNumber: Int,
    val participants: List<TournamentParticipant>,
    val roundSeed: Int,
    val maxActionsPerRound: Int,
    val logActions: Boolean = false,
)

data class TournamentResult(
    val roundsRequested: Int,
    val roundsCompleted: Int,
    val roundsFailed: Int,
    val seed: Int?,
    val botScores: List<BotScore>,
    val roundResults: List<RoundResult>,
)

data class BotScore(
    val participantId: String,
    val displayName: String,
    val score: Int,
    val wins: Int,
)

data class RoundResult(
    val roundNumber: Int,
    val roundSeed: Int = 0,
    val seatedParticipantIds: List<String> = emptyList(),
    val maxActionsPerRound: Int = 0,
    val completed: Boolean,
    val winnerParticipantId: String?,
    val eliminationOrder: List<String>,
    val scores: Map<String, Int>,
    val actionsTaken: Int,
    val failureReason: String? = null,
    val actionLog: List<RoundActionLogEntry> = emptyList(),
)

data class RoundActionLogEntry(
    val actionNumber: Int,
    val playerSlot: Int,
    val participantId: String,
    val actionType: RoundActionType,
    val from: Int? = null,
    val to: Int? = null,
    val battleRoll: com.franklinharper.dicewarsport.BattleRoll? = null,
    val suppliedAreas: List<Int> = emptyList(),
    val eliminatedParticipantIds: List<String> = emptyList(),
)

enum class RoundActionType {
    Attack,
    IllegalMove,
    EndTurn,
    RoundFailed,
    RoundWon,
}
