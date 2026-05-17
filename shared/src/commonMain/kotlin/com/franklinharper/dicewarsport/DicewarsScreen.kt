package com.franklinharper.dicewarsport

enum class DicewarsScreen(val soundEvents: List<SoundEvent> = emptyList()) {
    Loading,
    Title,
    MapPreview,
    HumanTurn(soundEvents = listOf(SoundEvent.MY_TURN)),
    AiTurn,
    GameOver(soundEvents = listOf(SoundEvent.GAME_OVER)),
    Win(soundEvents = listOf(SoundEvent.WIN)),
    Stats,
    Debug,
}
