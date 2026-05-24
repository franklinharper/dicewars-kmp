package com.franklinharper.dicewarsport

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SavedGameStateCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: SavedGameState): String = json.encodeToString(state)

    fun decode(text: String): SavedGameState? = runCatching {
        json.decodeFromString<SavedGameState>(text)
    }.getOrNull()
}
