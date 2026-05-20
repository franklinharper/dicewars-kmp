package com.franklinharper.dicewarsport.ai.neural

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NeuralBotConfigTest {
    @Test
    fun runtimeStrengthMapsToSimulationCounts() {
        assertEquals(0, NeuralRuntimeStrength.PolicyOnly.simulations)
        assertEquals(64, NeuralRuntimeStrength.Simulations64.simulations)
        assertEquals(256, NeuralRuntimeStrength.Simulations256.simulations)
        assertEquals(1024, NeuralRuntimeStrength.Simulations1024.simulations)
    }

    @Test
    fun defaultConfigUsesAndroidDefaultStrength() {
        assertEquals(NeuralRuntimeStrength.Simulations64, NeuralBotConfig.Default.runtimeStrength)
    }

    @Test
    fun parsesPersistedRuntimeStrengthValues() {
        assertEquals(NeuralRuntimeStrength.PolicyOnly, NeuralRuntimeStrength.fromPersistedValue("policy-only"))
        assertEquals(NeuralRuntimeStrength.Simulations64, NeuralRuntimeStrength.fromPersistedValue("64"))
        assertEquals(NeuralRuntimeStrength.Simulations256, NeuralRuntimeStrength.fromPersistedValue("256"))
        assertEquals(NeuralRuntimeStrength.Simulations1024, NeuralRuntimeStrength.fromPersistedValue("1024"))
    }

    @Test
    fun rejectsUnknownPersistedRuntimeStrengthValue() {
        assertFailsWith<IllegalArgumentException> {
            NeuralRuntimeStrength.fromPersistedValue("999")
        }
    }
}
