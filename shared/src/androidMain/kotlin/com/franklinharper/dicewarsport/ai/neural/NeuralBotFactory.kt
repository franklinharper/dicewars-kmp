package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.AiStrategy
import java.io.File
import java.io.IOException

actual object NeuralBotFactory {
    actual fun create(
        random: RandomSource,
        config: NeuralBotConfig,
        modelPathProperty: String,
    ): AiStrategy {
        val modelPath = System.getProperty(modelPathProperty) ?: defaultModelAssetFor(modelPathProperty)
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

    private fun defaultModelAssetFor(modelPathProperty: String): String = when (modelPathProperty) {
        "dicewars.neural.model" -> "models/neural-v0.onnx"
        "dicewars.neural.model.a" -> "models/neuralbot/g001/neural-a.onnx"
        "dicewars.neural.model.b" -> "models/neuralbot/g001/neural-b.onnx"
        "dicewars.neural.model.c" -> "models/neuralbot/g001/neural-c.onnx"
        "dicewars.neural.model.d" -> "models/neuralbot/g001/neural-d.onnx"
        "dicewars.neural.model.e" -> "models/neuralbot/g001/neural-e.onnx"
        "dicewars.neural.model.f" -> "models/neuralbot/g001/neural-f.onnx"
        else -> error(
            "No default Android neural model is mapped for property '$modelPathProperty'. " +
                "Set -D$modelPathProperty=<asset-path-or-file-path>.",
        )
    }
}
