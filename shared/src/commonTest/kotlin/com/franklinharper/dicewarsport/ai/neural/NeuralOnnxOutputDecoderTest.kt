package com.franklinharper.dicewarsport.ai.neural

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NeuralOnnxOutputDecoderTest {
    @Test
    fun decodesPolicyAndScalarValue() {
        val policy = FloatArray(NeuralActionEncoder.ACTION_COUNT) { index -> index.toFloat() }
        val prediction = NeuralOnnxOutputDecoder.decode(
            outputs = mapOf(
                "policy" to policy,
                "value" to floatArrayOf(0.75f),
            ),
        )

        assertEquals(NeuralActionEncoder.ACTION_COUNT, prediction.policy.size)
        assertEquals(12.0f, prediction.policy[12])
        assertEquals(0.75f, prediction.value)
    }

    @Test
    fun rejectsMissingPolicyOutput() {
        assertFailsWith<IllegalArgumentException> {
            NeuralOnnxOutputDecoder.decode(mapOf("value" to floatArrayOf(0.0f)))
        }
    }

    @Test
    fun rejectsMissingValueOutput() {
        assertFailsWith<IllegalArgumentException> {
            NeuralOnnxOutputDecoder.decode(mapOf("policy" to FloatArray(NeuralActionEncoder.ACTION_COUNT)))
        }
    }

    @Test
    fun rejectsWrongPolicySize() {
        assertFailsWith<IllegalArgumentException> {
            NeuralOnnxOutputDecoder.decode(
                mapOf(
                    "policy" to FloatArray(NeuralActionEncoder.ACTION_COUNT - 1),
                    "value" to floatArrayOf(0.0f),
                ),
            )
        }
    }

    @Test
    fun rejectsEmptyValueOutput() {
        assertFailsWith<IllegalArgumentException> {
            NeuralOnnxOutputDecoder.decode(
                mapOf(
                    "policy" to FloatArray(NeuralActionEncoder.ACTION_COUNT),
                    "value" to FloatArray(0),
                ),
            )
        }
    }
}
