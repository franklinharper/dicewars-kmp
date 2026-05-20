package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.ai.Move
import com.franklinharper.dicewarsport.aiGame
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NeuralBattleChanceExpanderTest {
    @Test
    fun expandsLegalAttackIntoWinAndLossOutcomes() {
        val game = aiGame()
        val move = Move(1, 2)
        val outcomes = NeuralBattleChanceExpander.expand(game, move, actorPlayer = 0)

        assertEquals(2, outcomes.size)
        assertEquals(BattleChanceOutcomeType.Win, outcomes[0].type)
        assertEquals(BattleChanceOutcomeType.Loss, outcomes[1].type)
        assertClose(1.0, outcomes.sumOf { it.probability })
    }

    @Test
    fun winOutcomeCapturesTargetAndMovesDice() {
        val game = aiGame()
        val outcomes = NeuralBattleChanceExpander.expand(game, Move(1, 2), actorPlayer = 0)
        val win = outcomes.single { it.type == BattleChanceOutcomeType.Win }.game

        assertEquals(0, win.areas[2].owner)
        assertEquals(4, win.areas[2].dice)
        assertEquals(1, win.areas[1].dice)
    }

    @Test
    fun lossOutcomeLeavesTargetOwnerAndReducesSourceToOneDie() {
        val game = aiGame()
        val outcomes = NeuralBattleChanceExpander.expand(game, Move(1, 2), actorPlayer = 0)
        val loss = outcomes.single { it.type == BattleChanceOutcomeType.Loss }.game

        assertEquals(1, loss.areas[2].owner)
        assertEquals(3, loss.areas[2].dice)
        assertEquals(1, loss.areas[1].dice)
    }

    @Test
    fun usesExactBattleProbabilityForSourceAndTargetDice() {
        val game = aiGame()
        val outcomes = NeuralBattleChanceExpander.expand(game, Move(1, 2), actorPlayer = 0)
        val win = outcomes.single { it.type == BattleChanceOutcomeType.Win }

        assertClose(BattleOutcomeProbabilities.winProbability(5, 3), win.probability)
    }

    @Test
    fun rejectsIllegalAttack() {
        assertFailsWith<IllegalArgumentException> {
            NeuralBattleChanceExpander.expand(aiGame(), Move(2, 1), actorPlayer = 0)
        }
    }

    private fun assertClose(expected: Double, actual: Double) {
        assertTrue(abs(expected - actual) < 1.0e-12, "expected=$expected actual=$actual")
    }
}
