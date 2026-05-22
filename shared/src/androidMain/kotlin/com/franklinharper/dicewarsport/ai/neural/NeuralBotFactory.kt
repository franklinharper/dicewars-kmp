package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.AiStrategy
import java.io.File
import java.io.IOException

actual object NeuralBotFactory {
    actual fun create(
        random: RandomSource,
        config: NeuralBotConfig,
    ): AiStrategy {
        val modelPath = System.getProperty("dicewars.neural.model") ?: "models/neural-v0.onnx"
        val modelBytes = loadModelBytes(modelPath)
        val model = OnnxNeuralModel(modelBytes)
        return NeuralBot(model = model, config = config, random = random)
    }

    private fun loadModelBytes(pathOrAsset: String): ByteArray {
        val context = AndroidNeuralRuntime.requireContext()

        val normalized = pathOrAsset.removePrefix("assets:/").removePrefix("asset:/")
        if (!normalized.startsWith("/") && !File(normalized).isAbsolute) {
            try {
                context.assets.open(normalized).use { return it.readBytes() }
            } catch (_: IOException) {
                // Fall through to file path attempt for explicit paths.
            }
        }

        val file = File(pathOrAsset)
        if (file.isFile) return file.readBytes()

        error(
            "Unable to load neural model '$pathOrAsset'. " +
                "Expected an Android asset (e.g. models/neural-v0.onnx) or a readable file path.",
        )
    }
}
