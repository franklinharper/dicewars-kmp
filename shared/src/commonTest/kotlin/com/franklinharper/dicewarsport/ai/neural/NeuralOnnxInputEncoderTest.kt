package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.aiGame
import kotlin.test.Test
import kotlin.test.assertEquals

class NeuralOnnxInputEncoderTest {
    @Test
    fun encodesInputTensorNamesAndShapes() {
        val input = NeuralOnnxInputEncoder.encode(
            NeuralInput(
                state = NeuralStateEncoder.encode(aiGame(), actorPlayer = 0, perspectivePlayer = 0),
                legalActionMask = NeuralActionEncoder.legalActionMask(aiGame(), actorPlayer = 0),
                actorPlayer = 0,
                perspectivePlayer = 0,
            ),
        )

        assertEquals(listOf("node_features", "adjacency", "global_features", "area_mask", "player_mask"), input.tensors.map { it.name })
        assertEquals(listOf(1, 32, NeuralStateEncoder.NODE_FEATURE_COUNT), input.tensor("node_features").shape)
        assertEquals(listOf(1, 32, 32), input.tensor("adjacency").shape)
        assertEquals(listOf(1, NeuralStateEncoder.GLOBAL_FEATURE_COUNT), input.tensor("global_features").shape)
        assertEquals(listOf(1, 32), input.tensor("area_mask").shape)
        assertEquals(listOf(1, 8), input.tensor("player_mask").shape)
    }

    @Test
    fun flattensNodeFeaturesInRowMajorOrder() {
        val state = NeuralStateEncoder.encode(aiGame(), actorPlayer = 0, perspectivePlayer = 0)
        val input = NeuralOnnxInputEncoder.encode(
            NeuralInput(
                state = state,
                legalActionMask = NeuralActionEncoder.legalActionMask(aiGame(), actorPlayer = 0),
                actorPlayer = 0,
                perspectivePlayer = 0,
            ),
        )

        val nodeFeatures = input.tensor("node_features").data
        val area1Offset = 1 * NeuralStateEncoder.NODE_FEATURE_COUNT
        assertEquals(state.nodeFeatures[1][NeuralStateEncoder.NODE_IS_REAL_AREA], nodeFeatures[area1Offset + NeuralStateEncoder.NODE_IS_REAL_AREA])
        assertEquals(state.nodeFeatures[1][NeuralStateEncoder.NODE_DICE_FRACTION], nodeFeatures[area1Offset + NeuralStateEncoder.NODE_DICE_FRACTION])
    }

    @Test
    fun encodesBooleanMasksAsFloatZerosAndOnes() {
        val state = NeuralStateEncoder.encode(aiGame(), actorPlayer = 0, perspectivePlayer = 0)
        val input = NeuralOnnxInputEncoder.encode(
            NeuralInput(
                state = state,
                legalActionMask = NeuralActionEncoder.legalActionMask(aiGame(), actorPlayer = 0),
                actorPlayer = 0,
                perspectivePlayer = 0,
            ),
        )

        assertEquals(0.0f, input.tensor("area_mask").data[0])
        assertEquals(1.0f, input.tensor("area_mask").data[1])
        assertEquals(1.0f, input.tensor("player_mask").data[0])
        assertEquals(0.0f, input.tensor("player_mask").data[2])
    }
}
