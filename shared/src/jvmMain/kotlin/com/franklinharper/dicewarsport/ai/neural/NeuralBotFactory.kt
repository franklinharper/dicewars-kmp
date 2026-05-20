package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.AiStrategy

actual object NeuralBotFactory {
    actual fun create(random: RandomSource, config: NeuralBotConfig): AiStrategy {
        val modelPath = System.getProperty("dicewars.neural.model")
            ?: error(
                "Neural bot requires a model. Set -Ddicewars.neural.model=<path/to/model.onnx> " +
                    "when launching the JVM.",
            )
        val model = OnnxNeuralModel(modelPath)
        return NeuralBot(model = model, config = config, random = random)
    }
}
