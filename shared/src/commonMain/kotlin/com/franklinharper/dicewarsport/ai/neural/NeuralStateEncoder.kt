package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.DicewarsGame

/**
 * Fixed-shape neural state encoder.
 *
 * This encoder intentionally starts small and explicit. It provides the stable
 * tensor contract needed by training data generation and inference while leaving
 * room to add features through versioned encoder changes later.
 */
object NeuralStateEncoder {
    const val NODE_IS_REAL_AREA: Int = 0
    const val NODE_IS_ACTOR_OWNED: Int = 1
    const val NODE_IS_PERSPECTIVE_OWNED: Int = 2
    const val NODE_IS_ENEMY_OWNED: Int = 3
    const val NODE_DICE_FRACTION: Int = 4
    const val NODE_SIZE_FRACTION: Int = 5
    const val NODE_FEATURE_COUNT: Int = 6

    const val GLOBAL_ACTOR_PLAYER_FRACTION: Int = 0
    const val GLOBAL_PERSPECTIVE_PLAYER_FRACTION: Int = 1
    const val GLOBAL_CURRENT_PLAYER_FRACTION: Int = 2
    const val GLOBAL_PLAYER_COUNT_FRACTION: Int = 3
    const val GLOBAL_FEATURE_COUNT: Int = 4

    const val ENCODER_VERSION: Int = 1

    fun encode(
        game: DicewarsGame,
        actorPlayer: Int = game.currentPlayerId(),
        perspectivePlayer: Int = actorPlayer,
    ): NeuralStateEncoding {
        require(actorPlayer in 0 until game.maxPlayers) { "actorPlayer must be in 0 until game.pmax: $actorPlayer" }
        require(perspectivePlayer in 0 until game.maxPlayers) {
            "perspectivePlayer must be in 0 until game.pmax: $perspectivePlayer"
        }

        val nodeFeatures = Array(DicewarsGame.AREA_MAX) { FloatArray(NODE_FEATURE_COUNT) }
        val adjacency = Array(DicewarsGame.AREA_MAX) { BooleanArray(DicewarsGame.AREA_MAX) }
        val areaMask = BooleanArray(DicewarsGame.AREA_MAX)
        val playerMask = BooleanArray(8)

        for (player in 0 until game.maxPlayers) {
            playerMask[player] = true
        }

        for (areaId in 0 until DicewarsGame.AREA_MAX) {
            val area = game.areas[areaId]
            val isReal = areaId != 0 && area.size > 0
            areaMask[areaId] = isReal
            if (!isReal) continue

            nodeFeatures[areaId][NODE_IS_REAL_AREA] = 1.0f
            nodeFeatures[areaId][NODE_IS_ACTOR_OWNED] = if (area.owner == actorPlayer) 1.0f else 0.0f
            nodeFeatures[areaId][NODE_IS_PERSPECTIVE_OWNED] = if (area.owner == perspectivePlayer) 1.0f else 0.0f
            nodeFeatures[areaId][NODE_IS_ENEMY_OWNED] = if (area.owner in 0 until game.maxPlayers && area.owner != actorPlayer) 1.0f else 0.0f
            nodeFeatures[areaId][NODE_DICE_FRACTION] = area.dice.toFloat() / DicewarsGame.MAX_DICE.toFloat()
            nodeFeatures[areaId][NODE_SIZE_FRACTION] = area.size.toFloat() / (DicewarsGame.MAX_WIDTH * DicewarsGame.MAX_HEIGHT).toFloat()

            val adjacentAreas = area.adjacentAreas
            for (neighborId in 0 until minOf(adjacentAreas.size, DicewarsGame.AREA_MAX)) {
                if (neighborId != 0 && adjacentAreas[neighborId] != 0) {
                    adjacency[areaId][neighborId] = true
                }
            }
        }

        val globalFeatures = FloatArray(GLOBAL_FEATURE_COUNT)
        globalFeatures[GLOBAL_ACTOR_PLAYER_FRACTION] = actorPlayer.toFloat() / 7.0f
        globalFeatures[GLOBAL_PERSPECTIVE_PLAYER_FRACTION] = perspectivePlayer.toFloat() / 7.0f
        globalFeatures[GLOBAL_CURRENT_PLAYER_FRACTION] = game.currentPlayerId().toFloat() / 7.0f
        globalFeatures[GLOBAL_PLAYER_COUNT_FRACTION] = game.maxPlayers.toFloat() / 8.0f

        return NeuralStateEncoding(
            encoderVersion = ENCODER_VERSION,
            nodeFeatures = nodeFeatures,
            adjacency = adjacency,
            globalFeatures = globalFeatures,
            areaMask = areaMask,
            playerMask = playerMask,
            actorPlayer = actorPlayer,
            perspectivePlayer = perspectivePlayer,
        )
    }
}

data class NeuralStateEncoding(
    val encoderVersion: Int,
    val nodeFeatures: Array<FloatArray>,
    val adjacency: Array<BooleanArray>,
    val globalFeatures: FloatArray,
    val areaMask: BooleanArray,
    val playerMask: BooleanArray,
    val actorPlayer: Int,
    val perspectivePlayer: Int,
)
