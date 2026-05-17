package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.AiStrategy
import kotlin.test.Test
import kotlin.test.assertEquals

class TournamentScoringTest {
    @Test
    fun twoPlayerWinnerGetsFourPoints() {
        val scores = scoreCompletedRound(
            participantIds = listOf("a", "b"),
            winnerParticipantId = "b",
            eliminationOrder = listOf("a"),
        )

        assertEquals(0, scores["a"])
        assertEquals(4, scores["b"])
    }

    @Test
    fun threePlayerEliminationOrderScoresZeroOneAndWinnerSix() {
        val scores = scoreCompletedRound(
            participantIds = listOf("a", "b", "c"),
            winnerParticipantId = "c",
            eliminationOrder = listOf("a", "b"),
        )

        assertEquals(0, scores["a"])
        assertEquals(1, scores["b"])
        assertEquals(6, scores["c"])
    }

    @Test
    fun failedRoundAwardsNoPoints() {
        assertEquals(
            mapOf("a" to 0, "b" to 0, "c" to 0),
            scoreFailedRound(listOf("a", "b", "c")),
        )
    }

    @Test
    fun aggregateScoresAcrossRounds() {
        val participants = listOf(participant("a"), participant("b"), participant("c"))
        val rounds = listOf(
            RoundResult(
                roundNumber = 1,
                completed = true,
                winnerParticipantId = "c",
                eliminationOrder = listOf("a", "b"),
                scores = mapOf("a" to 0, "b" to 1, "c" to 6),
                actionsTaken = 10,
            ),
            RoundResult(
                roundNumber = 2,
                completed = false,
                winnerParticipantId = null,
                eliminationOrder = emptyList(),
                scores = mapOf("a" to 0, "b" to 0, "c" to 0),
                actionsTaken = 100,
                failureReason = "max actions",
            ),
            RoundResult(
                roundNumber = 3,
                completed = true,
                winnerParticipantId = "b",
                eliminationOrder = listOf("c", "a"),
                scores = mapOf("a" to 1, "b" to 6, "c" to 0),
                actionsTaken = 12,
            ),
        )

        val aggregate = aggregateBotScores(participants, rounds)

        assertEquals(listOf("b", "c", "a"), aggregate.map { it.participantId })
        assertEquals(7, aggregate.single { it.participantId == "b" }.score)
        assertEquals(6, aggregate.single { it.participantId == "c" }.score)
        assertEquals(1, aggregate.single { it.participantId == "a" }.score)
        assertEquals(1, aggregate.single { it.participantId == "b" }.wins)
        assertEquals(1, aggregate.single { it.participantId == "c" }.wins)
    }
}

private fun participant(id: String): TournamentParticipant = TournamentParticipant(
    id = id,
    displayName = id.uppercase(),
    aiFactory = { NoMoveAi },
)

private object NoMoveAi : AiStrategy {
    override fun chooseMove(game: com.franklinharper.dicewarsport.DicewarsGame) = null
}
