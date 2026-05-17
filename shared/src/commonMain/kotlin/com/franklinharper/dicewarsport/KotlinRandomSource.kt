package com.franklinharper.dicewarsport

import kotlin.random.Random

class KotlinRandomSource(
    private val random: Random = Random.Default,
) : RandomSource {
    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        return random.nextInt(bound)
    }
}
