package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.aiGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NeuralStateEncoderTest {
    @Test
    fun encodedStateHasStableShapes() {
        val encoding = NeuralStateEncoder.encode(
            game = aiGame(),
            actorPlayer = 0,
            perspectivePlayer = 1,
        )

        assertEquals(DicewarsGame.AREA_MAX, encoding.nodeFeatures.size)
        assertTrue(encoding.nodeFeatures.all { it.size == NeuralStateEncoder.NODE_FEATURE_COUNT })
        assertEquals(DicewarsGame.AREA_MAX, encoding.adjacency.size)
        assertTrue(encoding.adjacency.all { it.size == DicewarsGame.AREA_MAX })
        assertEquals(DicewarsGame.AREA_MAX, encoding.areaMask.size)
        assertEquals(8, encoding.playerMask.size)
        assertEquals(NeuralStateEncoder.GLOBAL_FEATURE_COUNT, encoding.globalFeatures.size)
    }

    @Test
    fun areaMaskIdentifiesRealAreas() {
        val encoding = NeuralStateEncoder.encode(aiGame(), actorPlayer = 0, perspectivePlayer = 0)

        assertFalse(encoding.areaMask[0])
        assertTrue(encoding.areaMask[1])
        assertTrue(encoding.areaMask[2])
        assertTrue(encoding.areaMask[3])
        assertTrue(encoding.areaMask[4])
        assertFalse(encoding.areaMask[5])
    }

    @Test
    fun playerMaskIdentifiesPlayersInGame() {
        val encoding = NeuralStateEncoder.encode(aiGame(), actorPlayer = 0, perspectivePlayer = 0)

        assertTrue(encoding.playerMask[0])
        assertTrue(encoding.playerMask[1])
        assertFalse(encoding.playerMask[2])
        assertFalse(encoding.playerMask[7])
    }

    @Test
    fun adjacencyMatrixUsesAreaAdjacency() {
        val encoding = NeuralStateEncoder.encode(aiGame(), actorPlayer = 0, perspectivePlayer = 0)

        assertTrue(encoding.adjacency[1][2])
        assertTrue(encoding.adjacency[1][3])
        assertTrue(encoding.adjacency[2][1])
        assertTrue(encoding.adjacency[2][4])
        assertFalse(encoding.adjacency[1][4])
        assertFalse(encoding.adjacency[0][1])
    }

    @Test
    fun nodeFeaturesEncodeActorAndPerspectiveOwnership() {
        val encoding = NeuralStateEncoder.encode(aiGame(), actorPlayer = 0, perspectivePlayer = 1)

        assertEquals(1.0f, encoding.nodeFeatures[1][NeuralStateEncoder.NODE_IS_ACTOR_OWNED])
        assertEquals(0.0f, encoding.nodeFeatures[2][NeuralStateEncoder.NODE_IS_ACTOR_OWNED])
        assertEquals(0.0f, encoding.nodeFeatures[1][NeuralStateEncoder.NODE_IS_PERSPECTIVE_OWNED])
        assertEquals(1.0f, encoding.nodeFeatures[2][NeuralStateEncoder.NODE_IS_PERSPECTIVE_OWNED])
        assertEquals(1.0f, encoding.nodeFeatures[3][NeuralStateEncoder.NODE_IS_ENEMY_OWNED])
    }

    @Test
    fun nodeFeaturesNormalizeDiceAndAreaSize() {
        val encoding = NeuralStateEncoder.encode(aiGame(), actorPlayer = 0, perspectivePlayer = 0)

        assertEquals(5.0f / DicewarsGame.MAX_DICE, encoding.nodeFeatures[1][NeuralStateEncoder.NODE_DICE_FRACTION])
        assertEquals(5.0f / (DicewarsGame.MAX_WIDTH * DicewarsGame.MAX_HEIGHT), encoding.nodeFeatures[1][NeuralStateEncoder.NODE_SIZE_FRACTION])
        assertEquals(0.0f, encoding.nodeFeatures[0][NeuralStateEncoder.NODE_DICE_FRACTION])
    }

    @Test
    fun globalFeaturesEncodeActorPerspectiveAndCurrentPlayer() {
        val encoding = NeuralStateEncoder.encode(aiGame(), actorPlayer = 0, perspectivePlayer = 1)

        assertEquals(0.0f, encoding.globalFeatures[NeuralStateEncoder.GLOBAL_ACTOR_PLAYER_FRACTION])
        assertEquals(1.0f / 7.0f, encoding.globalFeatures[NeuralStateEncoder.GLOBAL_PERSPECTIVE_PLAYER_FRACTION])
        assertEquals(0.0f, encoding.globalFeatures[NeuralStateEncoder.GLOBAL_CURRENT_PLAYER_FRACTION])
        assertEquals(2.0f / 8.0f, encoding.globalFeatures[NeuralStateEncoder.GLOBAL_PLAYER_COUNT_FRACTION])
    }
}
