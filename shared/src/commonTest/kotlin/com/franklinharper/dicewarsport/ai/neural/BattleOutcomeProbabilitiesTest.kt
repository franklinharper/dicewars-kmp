package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.DicewarsGame
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BattleOutcomeProbabilitiesTest {
    @Test
    fun oneDieVsOneDieUsesStrictAttackerGreaterThanDefenderRule() {
        val probability = BattleOutcomeProbabilities.winProbability(attackerDice = 1, defenderDice = 1)

        assertClose(15.0 / 36.0, probability)
    }

    @Test
    fun winAndLossProbabilitiesSumToOne() {
        for (attackerDice in 1..DicewarsGame.MAX_DICE) {
            for (defenderDice in 1..DicewarsGame.MAX_DICE) {
                val outcome = BattleOutcomeProbabilities.outcome(attackerDice, defenderDice)

                assertClose(1.0, outcome.win + outcome.loss)
            }
        }
    }

    @Test
    fun moreAttackerDiceImprovesProbabilityAgainstSameDefender() {
        val weak = BattleOutcomeProbabilities.winProbability(attackerDice = 2, defenderDice = 4)
        val strong = BattleOutcomeProbabilities.winProbability(attackerDice = 6, defenderDice = 4)

        assertTrue(strong > weak)
    }

    @Test
    fun moreDefenderDiceReducesProbabilityAgainstSameAttacker() {
        val weakDefender = BattleOutcomeProbabilities.winProbability(attackerDice = 5, defenderDice = 2)
        val strongDefender = BattleOutcomeProbabilities.winProbability(attackerDice = 5, defenderDice = 6)

        assertTrue(strongDefender < weakDefender)
    }

    @Test
    fun rejectsDiceCountsOutsideGameRange() {
        assertFailsWith<IllegalArgumentException> { BattleOutcomeProbabilities.winProbability(0, 1) }
        assertFailsWith<IllegalArgumentException> { BattleOutcomeProbabilities.winProbability(1, 0) }
        assertFailsWith<IllegalArgumentException> { BattleOutcomeProbabilities.winProbability(DicewarsGame.MAX_DICE + 1, 1) }
        assertFailsWith<IllegalArgumentException> { BattleOutcomeProbabilities.winProbability(1, DicewarsGame.MAX_DICE + 1) }
    }

    private fun assertClose(expected: Double, actual: Double) {
        assertTrue(abs(expected - actual) < 1.0e-12, "expected=$expected actual=$actual")
    }
}
