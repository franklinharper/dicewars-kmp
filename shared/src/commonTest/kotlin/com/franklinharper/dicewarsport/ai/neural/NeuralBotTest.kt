package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.AreaData
import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.ai.Move
import com.franklinharper.dicewarsport.aiGame
import com.franklinharper.dicewarsport.aiAdj
import com.franklinharper.dicewarsport.setAreaTc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NeuralBotTest {
    @Test
    fun choosesHighestScoredLegalAttack() {
        val model = FakeNeuralModel().withPolicyScores(
            NeuralActionEncoder.actionIndexFor(Move(1, 2)) to 0.2f,
            NeuralActionEncoder.actionIndexFor(Move(1, 3)) to 0.9f,
            NeuralActionEncoder.END_TURN_INDEX to 0.1f,
        )
        val bot = NeuralBot(model = model)

        assertEquals(Move(1, 3), bot.chooseMove(aiGame()))
    }

    @Test
    fun ignoresHighestScoredIllegalAttack() {
        val model = FakeNeuralModel().withPolicyScores(
            NeuralActionEncoder.actionIndexFor(Move(2, 1)) to 1.0f, // illegal: enemy source
            NeuralActionEncoder.actionIndexFor(Move(1, 2)) to 0.4f,
            NeuralActionEncoder.END_TURN_INDEX to 0.1f,
        )
        val bot = NeuralBot(model = model)

        assertEquals(Move(1, 2), bot.chooseMove(aiGame()))
    }

    @Test
    fun choosesEndTurnWhenEndTurnIsBestLegalAction() {
        val model = FakeNeuralModel().withPolicyScores(
            NeuralActionEncoder.actionIndexFor(Move(1, 2)) to 0.4f,
            NeuralActionEncoder.END_TURN_INDEX to 0.8f,
        )
        val bot = NeuralBot(model = model)

        assertNull(bot.chooseMove(aiGame()))
    }

    @Test
    fun returnsEndTurnWhenThereAreNoLegalAttacks() {
        val model = FakeNeuralModel().withPolicyScores(
            NeuralActionEncoder.END_TURN_INDEX to 0.0f,
        )
        val bot = NeuralBot(model = model)

        assertNull(bot.chooseMove(noLegalAttackGame()))
    }

    @Test
    fun passesActorAndPerspectiveAsCurrentPlayerForPolicyOnlyMode() {
        val model = CapturingNeuralModel()
        val bot = NeuralBot(model = model)
        val game = aiGame()

        bot.chooseMove(game)

        val input = model.lastInput!!
        assertEquals(game.currentPlayer(), input.actorPlayer)
        assertEquals(game.currentPlayer(), input.perspectivePlayer)
    }

    private fun noLegalAttackGame(): DicewarsGame {
        val game = DicewarsGame(
            pmax = 2,
            turnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7),
            turnIndex = 0,
            areas = List(DicewarsGame.AREA_MAX) { i ->
                when (i) {
                    1 -> AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = aiAdj(2))
                    2 -> AreaData(size = 5, owner = 1, dice = 3, adjacentAreas = aiAdj(1))
                    else -> AreaData()
                }
            },
        )
        return game.setAreaTc(0).setAreaTc(1)
    }
}

private class FakeNeuralModel : NeuralModel {
    private val scores = FloatArray(NeuralActionEncoder.ACTION_COUNT) { 0.0f }

    fun withPolicyScores(vararg entries: Pair<Int, Float>): FakeNeuralModel {
        for ((index, score) in entries) scores[index] = score
        return this
    }

    override fun predict(input: NeuralInput): NeuralPrediction = NeuralPrediction(
        policy = scores.copyOf(),
        value = 0.0f,
    )
}

private class CapturingNeuralModel : NeuralModel {
    var lastInput: NeuralInput? = null

    override fun predict(input: NeuralInput): NeuralPrediction {
        lastInput = input
        return NeuralPrediction(
            policy = FloatArray(NeuralActionEncoder.ACTION_COUNT) { 0.0f },
            value = 0.0f,
        )
    }
}
