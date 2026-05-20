package com.franklinharper.dicewarsport.ai.neural

/**
 * Pure Kotlin ONNX input encoder.
 *
 * Platform-specific ONNX Runtime adapters can use these tensor names, shapes,
 * and flattened FloatArray values without depending on game-domain classes.
 */
object NeuralOnnxInputEncoder {
    fun encode(input: NeuralInput): NeuralOnnxInput {
        val state = input.state
        return NeuralOnnxInput(
            tensors = listOf(
                NeuralOnnxTensor(
                    name = "node_features",
                    shape = listOf(1, 32, NeuralStateEncoder.NODE_FEATURE_COUNT),
                    data = flattenNodeFeatures(state.nodeFeatures),
                ),
                NeuralOnnxTensor(
                    name = "adjacency",
                    shape = listOf(1, 32, 32),
                    data = flattenBooleans(state.adjacency),
                ),
                NeuralOnnxTensor(
                    name = "global_features",
                    shape = listOf(1, NeuralStateEncoder.GLOBAL_FEATURE_COUNT),
                    data = state.globalFeatures.copyOf(),
                ),
                NeuralOnnxTensor(
                    name = "area_mask",
                    shape = listOf(1, 32),
                    data = state.areaMask.toFloatArray(),
                ),
                NeuralOnnxTensor(
                    name = "player_mask",
                    shape = listOf(1, 8),
                    data = state.playerMask.toFloatArray(),
                ),
            ),
        )
    }

    private fun flattenNodeFeatures(nodeFeatures: Array<FloatArray>): FloatArray {
        val result = FloatArray(32 * NeuralStateEncoder.NODE_FEATURE_COUNT)
        var index = 0
        for (areaId in 0 until 32) {
            val features = nodeFeatures[areaId]
            for (feature in 0 until NeuralStateEncoder.NODE_FEATURE_COUNT) {
                result[index++] = features[feature]
            }
        }
        return result
    }

    private fun flattenBooleans(values: Array<BooleanArray>): FloatArray {
        val result = FloatArray(32 * 32)
        var index = 0
        for (row in 0 until 32) {
            for (col in 0 until 32) {
                result[index++] = if (values[row][col]) 1.0f else 0.0f
            }
        }
        return result
    }

    private fun BooleanArray.toFloatArray(): FloatArray = FloatArray(size) { index ->
        if (this[index]) 1.0f else 0.0f
    }
}

data class NeuralOnnxInput(
    val tensors: List<NeuralOnnxTensor>,
) {
    fun tensor(name: String): NeuralOnnxTensor = tensors.firstOrNull { it.name == name }
        ?: throw IllegalArgumentException("Unknown ONNX tensor '$name'")
}

data class NeuralOnnxTensor(
    val name: String,
    val shape: List<Int>,
    val data: FloatArray,
)
