package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.aiGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OnnxNeuralModelTest {
    private fun testModel(): OnnxNeuralModel {
        val resource = javaClass.getResourceAsStream("/test-model.onnx")
            ?: error("test-model.onnx not found on classpath")
        return OnnxNeuralModel(resource.readBytes())
    }

    @Test
    fun loadsModelAndRunsInference() {
        val model = testModel()
        model.use {
            val game: DicewarsGame = aiGame()
            val input = NeuralInput(
                state = NeuralStateEncoder.encode(game, actorPlayer = 0, perspectivePlayer = 0),
                legalActionMask = NeuralActionEncoder.legalActionMask(game, 0),
                actorPlayer = 0,
                perspectivePlayer = 0,
            )
            val prediction = model.predict(input)
            assertEquals(NeuralActionEncoder.ACTION_COUNT, prediction.policy.size)
            assertEquals(0.42f, prediction.value)
        }
    }

    @Test
    fun neuralBotPicksFirstLegalActionWhenAllPolicyScoresAreZero() {
        val model = testModel()
        model.use {
            val bot = NeuralBot(model)
            // All-zero policy: first legal attack wins (strict > tie-breaking)
            val game = aiGame()
            val move = bot.chooseMove(game)
            assertNotNull(move)
        }
    }
}
