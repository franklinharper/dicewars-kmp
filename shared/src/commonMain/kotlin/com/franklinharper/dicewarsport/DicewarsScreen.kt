package com.franklinharper.dicewarsport

import kotlinx.serialization.Serializable

@Serializable
enum class DicewarsScreen(val soundEvents: List<SoundEvent> = emptyList()) {
    Loading,
    Title,
    HumanTurn(soundEvents = listOf(SoundEvent.MY_TURN)),
    AiTurn,
    GameOver(soundEvents = listOf(SoundEvent.GAME_OVER)),
    Win(soundEvents = listOf(SoundEvent.WIN)),
    Stats,
    Debug,
    SelectBots,
}
