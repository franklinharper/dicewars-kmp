package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.AiStrategy

actual object NeuralBotFactory {
    actual fun create(
        random: RandomSource,
        config: NeuralBotConfig,
        modelPathProperty: String,
    ): AiStrategy {
        val modelPath = System.getProperty(modelPathProperty)
            ?: error(
                "Neural bot requires a model. Set -D$modelPathProperty=<path/to/model.onnx> " +
                    "when launching the JVM.",
            )
        val model = OnnxNeuralModel(modelPath)
        return NeuralBot(model = model, config = config, random = random)
    }
}
