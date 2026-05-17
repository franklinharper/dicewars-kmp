package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.RandomSource

data class Move(val from: Int, val to: Int)

interface AiStrategy {
    val name: String get() = this::class.simpleName ?: "Unknown"
    fun chooseMove(game: DicewarsGame): Move?
}

internal fun List<Move>.randomOrNull(random: RandomSource): Move? =
    if (isEmpty()) null else this[random.nextInt(size)]
