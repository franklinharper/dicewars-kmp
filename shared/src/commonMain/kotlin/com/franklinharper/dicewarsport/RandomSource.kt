package com.franklinharper.dicewarsport

interface RandomSource {
    fun nextInt(bound: Int): Int
}
