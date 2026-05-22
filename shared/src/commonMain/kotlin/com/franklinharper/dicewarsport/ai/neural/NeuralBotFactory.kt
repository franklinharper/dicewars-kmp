package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.AiStrategy

/**
 * Platform-specific factory for creating the neural bot.
 *
 * Each platform (JVM, Android, iOS) must provide an `actual` implementation
 * that loads the ONNX model from the appropriate location and constructs a
 * [NeuralBot]. If no model is available, the factory must throw an exception
 * explaining how to provide one — no silent fallback.
 */
expect object NeuralBotFactory {
    fun create(
        random: RandomSource,
        config: NeuralBotConfig = NeuralBotConfig.Default,
    ): AiStrategy
}
