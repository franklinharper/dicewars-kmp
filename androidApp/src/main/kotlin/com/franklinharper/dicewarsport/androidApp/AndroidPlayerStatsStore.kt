package com.franklinharper.dicewarsport.androidApp

import android.content.Context
import com.franklinharper.dicewarsport.PlayerStatsHistory
import com.franklinharper.dicewarsport.PlayerStatsRecord
import com.franklinharper.dicewarsport.PlayerStatsStore

class AndroidPlayerStatsStore(context: Context) : PlayerStatsStore {
    private val prefs = context.getSharedPreferences("dicewars_player_stats", Context.MODE_PRIVATE)

    override fun load(): PlayerStatsHistory {
        val json = prefs.getString(KEY, null) ?: return PlayerStatsHistory.default()
        return runCatching { parse(json) }.getOrElse { PlayerStatsHistory.default() }
    }

    override fun save(history: PlayerStatsHistory) {
        prefs.edit().putString(KEY, encode(history)).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private fun encode(history: PlayerStatsHistory): String {
        val records = history.records.values.joinToString(",") { record ->
            "{" +
                "\"playerId\":\"${escape(record.playerId)}\"," +
                "\"displayName\":\"${escape(record.displayName)}\"," +
                "\"wins\":${record.wins}," +
                "\"gamesPlayed\":${record.gamesPlayed}," +
                "\"score\":${record.score}" +
                "}"
        }
        return "{\"version\":1,\"records\":[${records}]}"
    }

    private fun parse(json: String): PlayerStatsHistory {
        val recordRegex = Regex("\\{\\\"playerId\\\":\\\"(.*?)\\\",\\\"displayName\\\":\\\"(.*?)\\\",\\\"wins\\\":(\\d+),\\\"gamesPlayed\\\":(\\d+),\\\"score\\\":(\\d+)\\}")
        val records = recordRegex.findAll(json).map { match ->
            val record = PlayerStatsRecord(
                playerId = unescape(match.groupValues[1]),
                displayName = unescape(match.groupValues[2]),
                wins = match.groupValues[3].toInt(),
                gamesPlayed = match.groupValues[4].toInt(),
                score = match.groupValues[5].toInt(),
            )
            record.playerId to record
        }.toMap()
        return PlayerStatsHistory(records)
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

    private fun unescape(value: String): String = value
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")

    private companion object {
        const val KEY = "dicewars_player_stats_v1"
    }
}
