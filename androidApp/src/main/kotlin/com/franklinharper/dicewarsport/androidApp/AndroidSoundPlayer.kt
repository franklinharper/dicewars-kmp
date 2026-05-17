package com.franklinharper.dicewarsport.androidApp

import android.content.Context
import android.media.SoundPool
import com.franklinharper.dicewarsport.SoundEvent
import com.franklinharper.dicewarsport.SoundPlayer

class AndroidSoundPlayer(context: Context) : SoundPlayer {

    private val soundPool = SoundPool.Builder().setMaxStreams(4).build()
    private val soundIds: Map<SoundEvent, Int>

    init {
        soundIds = mapOf(
            SoundEvent.BUTTON to load(context, R.raw.snd_button),
            SoundEvent.CLICK to load(context, R.raw.snd_click),
            SoundEvent.DICE to load(context, R.raw.snd_dice),
            SoundEvent.SUCCESS to load(context, R.raw.snd_success),
            SoundEvent.FAIL to load(context, R.raw.snd_fail),
            SoundEvent.MY_TURN to load(context, R.raw.snd_myturn),
            SoundEvent.GAME_OVER to load(context, R.raw.snd_over),
            SoundEvent.WIN to load(context, R.raw.snd_clear),
        )
    }

    private fun load(context: Context, resId: Int): Int {
        return soundPool.load(context, resId, 1)
    }

    override fun play(event: SoundEvent) {
        val soundId = soundIds[event] ?: return
        soundPool.play(soundId, 0.5f, 0.5f, 1, 0, 1f)
    }

    override fun release() {
        soundPool.release()
    }
}
