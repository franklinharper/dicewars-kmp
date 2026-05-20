package com.franklinharper.dicewarsport.ai.neural

/** Decodes platform ONNX Runtime outputs into the domain-level prediction type. */
object NeuralOnnxOutputDecoder {
    fun decode(outputs: Map<String, FloatArray>): NeuralPrediction {
        val policy = outputs["policy"]
            ?: throw IllegalArgumentException("ONNX output 'policy' is missing")
        require(policy.size == NeuralActionEncoder.ACTION_COUNT) {
            "ONNX output 'policy' must have ${NeuralActionEncoder.ACTION_COUNT} values, was ${policy.size}"
        }
        val value = outputs["value"]
            ?: throw IllegalArgumentException("ONNX output 'value' is missing")
        require(value.isNotEmpty()) { "ONNX output 'value' must contain at least one value" }

        return NeuralPrediction(
            policy = policy.copyOf(),
            value = value[0],
        )
    }
}
