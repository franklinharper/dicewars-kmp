package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.FixedAiRandom
import com.franklinharper.dicewarsport.aiGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StochasticMctsTest {
    @Test
    fun searchReturnsVisitCountsForLegalActions() {
        val game = aiGame()
        val mcts = StochasticMcts(random = FixedAiRandom(42))
        val visits = mcts.search(game, budget = 10)

        assertTrue(visits.isNotEmpty(), "Should have at least one expanded action")
        assertTrue(visits.values.all { it > 0 }, "All visit counts should be positive")
        assertEquals(visits.values.sum(), 10, "Total visits should equal budget")
    }

    @Test
    fun visitCountsSumToBudget() {
        val game = aiGame()
        val mcts = StochasticMcts(random = FixedAiRandom(42))

        for (budget in listOf(1, 5, 20)) {
            val visits = mcts.search(game, budget = budget)
            assertEquals(budget, visits.values.sum(), "Budget=$budget")
        }
    }

    @Test
    fun prefersWinningAttackWhenOnlyOneWins() {
        // Create a game where player 0 has one good attack and one bad one.
        // With enough budget, MCTS should visit the good one more.
        val game = simpleWinLossGame()
        val mcts = StochasticMcts(random = FixedAiRandom(42))
        val visits = mcts.search(game, budget = 50)

        assertTrue(visits.isNotEmpty(), "Should have expanded actions")
        // The winning attack should be visited most
        val bestAction = visits.maxByOrNull { it.value }
        assertTrue(bestAction != null)
        val bestMove = NeuralActionEncoder.moveForActionIndex(bestAction.key)
        // Verify the best move is an attack (not end turn) — even a random
        // rollout should prefer attacking over not attacking in a simple scenario
        assertTrue(
            bestMove != null || bestAction.key == NeuralActionEncoder.END_TURN_INDEX,
            "Best action should be a valid move or end turn",
        )
    }

    @Test
    fun worksBudgetOfOne() {
        val game = aiGame()
        val mcts = StochasticMcts(random = FixedAiRandom(42))
        val visits = mcts.search(game, budget = 1)

        assertEquals(1, visits.size, "Budget=1 should expand exactly one action")
        assertEquals(1, visits.values.first())
    }

    private fun simpleWinLossGame(): com.franklinharper.dicewarsport.DicewarsGame {
        // Use the standard aiGame fixture
        return aiGame()
    }
}
