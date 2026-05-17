package com.franklinharper.dicewarsport

data class GameUiState(
    val screen: DicewarsScreen,
    val game: DicewarsGame,
    val selectedFrom: Int? = null,
    val selectedTo: Int? = null,
    val spectateMode: Boolean = false,
    val selectedPlayerCount: Int = game.pmax,
    val soundEnabled: Boolean = true,
    val debugMode: Boolean = false,
    val titleTapCount: Int = 0,
    val titleTapTimestamp: Long = 0L,
    val playerNames: Map<Int, String> = emptyMap(),
    val playerIds: Map<Int, String> = emptyMap(),
    val playerStatsHistory: PlayerStatsHistory = PlayerStatsHistory.default(),
    val eliminatedPlayerIds: List<String> = emptyList(),
    val eliminatedPlayerSeats: List<Int> = emptyList(),
    val gameStatsRecorded: Boolean = false,
    val confirmResetStats: Boolean = false,
)
