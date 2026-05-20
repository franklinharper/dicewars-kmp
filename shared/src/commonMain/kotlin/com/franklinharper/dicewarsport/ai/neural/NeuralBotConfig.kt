package com.franklinharper.dicewarsport.ai.neural

data class NeuralBotConfig(
    val runtimeStrength: NeuralRuntimeStrength = NeuralRuntimeStrength.Simulations64,
) {
    companion object {
        val Default: NeuralBotConfig = NeuralBotConfig()
    }
}

enum class NeuralRuntimeStrength(
    val persistedValue: String,
    val simulations: Int,
) {
    PolicyOnly("policy-only", 0),
    Simulations64("64", 64),
    Simulations256("256", 256),
    Simulations1024("1024", 1024);

    companion object {
        fun fromPersistedValue(value: String): NeuralRuntimeStrength = entries.firstOrNull { it.persistedValue == value }
            ?: throw IllegalArgumentException("Unknown neural runtime strength '$value'")
    }
}
