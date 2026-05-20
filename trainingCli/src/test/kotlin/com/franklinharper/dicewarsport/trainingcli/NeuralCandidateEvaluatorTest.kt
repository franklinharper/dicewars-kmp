package com.franklinharper.dicewarsport.trainingcli

import com.franklinharper.dicewarsport.tournament.BotScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NeuralCandidateEvaluatorTest {
    @Test
    fun acceptsNeuralWhenItIsNotLastAndEliminatesLowestNonNeural() {
        val decision = NeuralCandidateEvaluator.evaluate(
            scores = listOf(
                score("terminator2", 100, 10),
                score("neural", 80, 5),
                score("max", 70, 4),
                score("bully", 50, 1),
            ),
        )

        assertTrue(decision.accepted)
        assertEquals("bully", decision.eliminatedBotId)
        assertEquals("neural did not finish last; eliminate lowest non-neural bot bully", decision.reason)
    }

    @Test
    fun rejectsNeuralWhenItIsLast() {
        val decision = NeuralCandidateEvaluator.evaluate(
            scores = listOf(
                score("terminator2", 100, 10),
                score("max", 70, 4),
                score("bully", 50, 1),
                score("neural", 40, 3),
            ),
        )

        assertFalse(decision.accepted)
        assertNull(decision.eliminatedBotId)
        assertEquals("neural finished last", decision.reason)
    }

    @Test
    fun acceptsWithoutTwoPercentMargin() {
        val decision = NeuralCandidateEvaluator.evaluate(
            scores = listOf(
                score("terminator2", 100, 10),
                score("max", 70, 4),
                score("neural", 51, 1),
                score("bully", 50, 9),
            ),
        )

        assertTrue(decision.accepted)
        assertEquals("bully", decision.eliminatedBotId)
    }

    @Test
    fun tieUsesTournamentRankingOrderProvidedByScoresList() {
        val decision = NeuralCandidateEvaluator.evaluate(
            scores = listOf(
                score("terminator2", 100, 10),
                score("neural", 50, 1),
                score("bully", 50, 9),
            ),
        )

        assertTrue(decision.accepted)
        assertEquals("bully", decision.eliminatedBotId)
    }

    @Test
    fun rejectsWhenNeuralIsMissing() {
        val decision = NeuralCandidateEvaluator.evaluate(
            scores = listOf(score("max", 70, 4), score("bully", 50, 1)),
        )

        assertFalse(decision.accepted)
        assertEquals("neural bot is missing from scores", decision.reason)
    }

    private fun score(id: String, score: Int, wins: Int): BotScore = BotScore(
        participantId = id,
        displayName = id,
        score = score,
        wins = wins,
    )
}
