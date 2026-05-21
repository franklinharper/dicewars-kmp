package com.franklinharper.dicewarsport.ai.neural

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

class OnnxNeuralModel(
    modelBytes: ByteArray,
) : NeuralModel, AutoCloseable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(modelBytes)

    override fun predict(input: NeuralInput): NeuralPrediction {
        val onnxInput = NeuralOnnxInputEncoder.encode(input)
        val feeds = mutableMapOf<String, OnnxTensor>()
        for (tensor in onnxInput.tensors) {
            feeds[tensor.name] = createFloatTensor(tensor.shape, tensor.data)
        }

        val results = session.run(feeds)
        try {
            @Suppress("UNCHECKED_CAST")
            val policyTensor = results.get("policy").get() as? OnnxTensor
                ?: error("ONNX model did not produce a 'policy' output")
            @Suppress("UNCHECKED_CAST")
            val valueTensor = results.get("value").get() as? OnnxTensor
                ?: error("ONNX model did not produce a 'value' output")

            val policyBuffer = policyTensor.floatBuffer
            val policyArray = FloatArray(policyBuffer.remaining()) { policyBuffer.get() }

            val valueBuffer = valueTensor.floatBuffer
            val value = if (valueBuffer.remaining() > 0) valueBuffer.get() else 0.0f

            return NeuralOnnxOutputDecoder.decode(
                outputs = mapOf("policy" to policyArray, "value" to floatArrayOf(value)),
            )
        } finally {
            results.close()
            feeds.values.forEach { it.close() }
        }
    }

    private fun createFloatTensor(shape: List<Int>, data: FloatArray): OnnxTensor {
        val buffer = FloatBuffer.wrap(data)
        val longShape = LongArray(shape.size) { shape[it].toLong() }
        return OnnxTensor.createTensor(env, buffer, longShape)
    }

    override fun close() {
        session.close()
    }
}
