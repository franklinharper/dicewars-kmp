package com.franklinharper.dicewarsport.androidApp

import android.content.Context
import com.franklinharper.dicewarsport.GameStateStore
import com.franklinharper.dicewarsport.SavedGameState
import com.franklinharper.dicewarsport.SavedGameStateCodec

class AndroidGameStateStore(context: Context) : GameStateStore {
    private val prefs = context.getSharedPreferences("dicewars_game_state", Context.MODE_PRIVATE)

    override fun load(): SavedGameState? {
        val encoded = prefs.getString(KEY, null) ?: return null
        return SavedGameStateCodec.decode(encoded)
    }

    override fun save(state: SavedGameState) {
        prefs.edit().putString(KEY, SavedGameStateCodec.encode(state)).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "saved_game_state"
    }
}
