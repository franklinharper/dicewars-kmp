package com.franklinharper.dicewarsport

interface SoundPlayer {
    fun play(event: SoundEvent)
    fun release()
}

class NoOpSoundPlayer : SoundPlayer {
    override fun play(event: SoundEvent) {}
    override fun release() {}
}
