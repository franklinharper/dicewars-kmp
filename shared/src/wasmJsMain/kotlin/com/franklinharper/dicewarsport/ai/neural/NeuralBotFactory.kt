package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.AiStrategy

actual object NeuralBotFactory {
    actual fun create(
        random: RandomSource,
        config: NeuralBotConfig,
        modelPathProperty: String,
    ): AiStrategy {
        error("Neural bot is not yet supported on wasmJs.")
    }
}
