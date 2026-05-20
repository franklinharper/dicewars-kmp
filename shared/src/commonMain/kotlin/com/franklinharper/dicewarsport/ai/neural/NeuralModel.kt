package com.franklinharper.dicewarsport.ai.neural

interface NeuralModel {
    fun predict(input: NeuralInput): NeuralPrediction
}

data class NeuralInput(
    val state: NeuralStateEncoding,
    val legalActionMask: BooleanArray,
    val actorPlayer: Int,
    val perspectivePlayer: Int,
)

data class NeuralPrediction(
    val policy: FloatArray,
    val value: Float,
    val playerValues: FloatArray? = null,
)
