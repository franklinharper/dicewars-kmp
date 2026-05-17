package com.franklinharper.dicewarsport.tournament

fun deriveRoundSeed(tournamentSeed: Int, roundNumber: Int): Int {
    require(roundNumber > 0) { "roundNumber must be greater than zero" }
    var value = tournamentSeed.toLong() xor (roundNumber.toLong() * 0x9E3779B1L)
    value = (value xor (value ushr 16)) * 0x45D9F3BL
    value = (value xor (value ushr 16)) * 0x45D9F3BL
    value = value xor (value ushr 16)
    return value.toInt()
}
