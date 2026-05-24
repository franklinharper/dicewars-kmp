package com.franklinharper.dicewarsport

import kotlinx.serialization.Serializable

interface GameStateStore {
    fun load(): SavedGameState?
    fun save(state: SavedGameState)
    fun clear()
}

class NoOpGameStateStore : GameStateStore {
    override fun load(): SavedGameState? = null
    override fun save(state: SavedGameState) {}
    override fun clear() {}
}

@Serializable
data class SavedGameState(
    val screen: DicewarsScreen,
    val game: DicewarsGame,
    val selectedFrom: Int?,
    val selectedTo: Int?,
    val spectateMode: Boolean,
    val selectedPlayerCount: Int,
    val soundEnabled: Boolean,
    val debugMode: Boolean,
    val playerNames: Map<Int, String>,
    val playerIds: Map<Int, String>,
    val eliminatedPlayerIds: List<String>,
    val eliminatedPlayerSeats: List<Int>,
    val gameStatsRecorded: Boolean,
    val humanAutoplayEnabled: Boolean,
    val resolvingAfterHumanEliminated: Boolean,
    val selectedDebugBotIds: Set<String>,
) {
    companion object {
        fun fromUiState(state: GameUiState): SavedGameState? {
            if (state.screen == DicewarsScreen.Loading) return null
            return SavedGameState(
                screen = state.screen.normalizedForSave(),
                game = state.game,
                selectedFrom = state.selectedFrom,
                selectedTo = state.selectedTo,
                spectateMode = state.spectateMode,
                selectedPlayerCount = state.selectedPlayerCount,
                soundEnabled = state.soundEnabled,
                debugMode = state.debugMode,
                playerNames = state.playerNames,
                playerIds = state.playerIds,
                eliminatedPlayerIds = state.eliminatedPlayerIds,
                eliminatedPlayerSeats = state.eliminatedPlayerSeats,
                gameStatsRecorded = state.gameStatsRecorded,
                humanAutoplayEnabled = state.humanAutoplayEnabled,
                resolvingAfterHumanEliminated = state.resolvingAfterHumanEliminated,
                selectedDebugBotIds = state.selectedDebugBotIds,
            )
        }

        private fun DicewarsScreen.normalizedForSave(): DicewarsScreen = when (this) {
            DicewarsScreen.Loading,
            DicewarsScreen.Debug,
            DicewarsScreen.SelectBots,
            DicewarsScreen.Stats -> DicewarsScreen.Title
            else -> this
        }
    }
}
