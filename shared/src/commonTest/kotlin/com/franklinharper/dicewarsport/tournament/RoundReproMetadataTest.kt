package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.ai.AiStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RoundReproMetadataTest {
    @Test
    fun roundSeedsAreDeterministicAndIndependentPerRound() {
        val seed = 42

        assertEquals(deriveRoundSeed(seed, 1), deriveRoundSeed(seed, 1))
        assertNotEquals(deriveRoundSeed(seed, 1), deriveRoundSeed(seed, 2))
    }

    @Test
    fun roundResultContainsReproMetadata() {
        val runner = TournamentRunner(
            roundRunner = RoundRunner { config ->
                RoundResult(
                    roundNumber = config.roundNumber,
                    roundSeed = config.roundSeed,
                    seatedParticipantIds = config.participants.map { it.id },
                    maxActionsPerRound = config.maxActionsPerRound,
                    completed = false,
                    winnerParticipantId = null,
                    eliminationOrder = emptyList(),
                    scores = scoreFailedRound(config.participants.map { it.id }),
                    actionsTaken = config.maxActionsPerRound,
                    failureReason = "max actions",
                )
            },
        )

        val result = runner.run(
            TournamentConfig(
                participants = listOf(reproParticipant("a"), reproParticipant("b"), reproParticipant("c")),
                rounds = 2,
                seed = 99,
                maxActionsPerRound = 123,
            ),
        )

        val first = result.roundResults[0]
        val second = result.roundResults[1]
        assertEquals(deriveRoundSeed(99, 1), first.roundSeed)
        assertEquals(deriveRoundSeed(99, 2), second.roundSeed)
        assertEquals(listOf("a", "b", "c"), first.seatedParticipantIds)
        assertEquals(listOf("b", "c", "a"), second.seatedParticipantIds)
        assertEquals(123, first.maxActionsPerRound)
    }
}

private fun reproParticipant(id: String): TournamentParticipant = TournamentParticipant(
    id = id,
    displayName = id.uppercase(),
    aiFactory = { ReproNoMoveAi },
)

private object ReproNoMoveAi : AiStrategy {
    override fun chooseMove(game: DicewarsGame) = null
}
