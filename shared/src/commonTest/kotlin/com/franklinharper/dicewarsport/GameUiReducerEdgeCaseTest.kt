package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals

class GameUiReducerEdgeCaseTest {

    @Test
    fun invalidFirstHumanClickDoesNotSelectTerritory() {
        val result = reducer().reduce(turnState(DicewarsScreen.HumanTurn), GameAction.TerritoryClicked(2))

        assertEquals(null, result.state.selectedFrom)
        assertEquals(emptyList(), result.soundEvents)
    }

    @Test
    fun clickingSelectedTerritoryDeselectsIt() {
        val selected = turnState(DicewarsScreen.HumanTurn).copy(selectedFrom = 1)

        val result = reducer().reduce(selected, GameAction.TerritoryClicked(1))

        assertEquals(null, result.state.selectedFrom)
        assertEquals(null, result.state.selectedTo)
    }

    @Test
    fun illegalSecondHumanClickKeepsSelectionAndDoesNotAttack() {
        val selected = turnState(DicewarsScreen.HumanTurn).copy(selectedFrom = 1)

        val result = reducer().reduce(selected, GameAction.TerritoryClicked(3))

        assertEquals(1, result.state.selectedFrom)
        assertEquals(1, result.state.game.areas[3].owner)
        assertEquals(emptyList(), result.soundEvents)
    }

    @Test
    fun actionsFromWrongScreensAreIgnored() {
        val state = turnState(DicewarsScreen.Title)

        val humanClick = reducer().reduce(state, GameAction.TerritoryClicked(1))
        val aiStep = reducer().reduce(state, GameAction.AiStep)

        assertEquals(state, humanClick.state)
        assertEquals(state, aiStep.state)
    }

    @Test
    fun toggleSoundFlipsSoundEnabled() {
        val on = initialState()
        val off = reducer().reduce(on, GameAction.ToggleSound)
        val backOn = reducer().reduce(off.state, GameAction.ToggleSound)

        assertEquals(false, off.state.soundEnabled)
        assertEquals(true, backOn.state.soundEnabled)
    }

    @Test
    fun loadingFinishedReadsPersistedDebugPreference() {
        val result = reducer(debugEnabled = true).reduce(initialState(screen = DicewarsScreen.Loading), GameAction.LoadingFinished)

        assertEquals(DicewarsScreen.Title, result.state.screen)
        assertEquals(true, result.state.debugMode)
    }

    @Test
    fun titleTapWindowResetRequiresConsecutiveTapsWithinWindow() {
        val staleTapState = initialState(screen = DicewarsScreen.Title).copy(
            titleTapCount = 4,
            titleTapTimestamp = 1L,
        )

        val result = reducer().reduce(staleTapState, GameAction.TitleTapped)

        assertEquals(false, result.state.debugMode)
        assertEquals(1, result.state.titleTapCount)
    }

    private fun reducer(
        debugEnabled: Boolean = false,
    ): GameReducer = GameReducer(
        random = ZeroRandom(),
        debugPreferences = MemoryDebugPreferences(debugEnabled),
    )

    private fun initialState(screen: DicewarsScreen = DicewarsScreen.Title): GameUiState = GameUiState(
        screen = screen,
        game = testGame(),
    )

    private fun turnState(screen: DicewarsScreen, currentPlayer: Int = 0): GameUiState = GameUiState(
        screen = screen,
        game = testGame().copy(turnIndex = currentPlayer),
    )

    private fun testGame(): DicewarsGame {
        val game = DicewarsGame(
            maxPlayers = 2,
            user = 0,
            turnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7),
            areas = List(DicewarsGame.AREA_MAX) { i ->
                when (i) {
                    1 -> AreaData(size = 5, owner = 0, dice = 4, adjacentAreas = adj(2))
                    2 -> AreaData(size = 5, owner = 1, dice = 2, adjacentAreas = adj(1, 3))
                    3 -> AreaData(size = 5, owner = 1, dice = 2, adjacentAreas = adj(2))
                    else -> AreaData()
                }
            },
        )
        return game.setAreaTc(0).setAreaTc(1)
    }

    private fun adj(vararg ids: Int): List<Int> = MutableList(DicewarsGame.AREA_MAX) { index ->
        if (index in ids) 1 else 0
    }

    private class ZeroRandom : RandomSource {
        override fun nextInt(bound: Int): Int {
            require(bound > 0)
            return 0
        }
    }

    private class SequenceRandom : RandomSource {
        private var next = 0
        override fun nextInt(bound: Int): Int {
            require(bound > 0)
            val value = next % bound
            next++
            return value
        }
    }

    private class MemoryDebugPreferences(initial: Boolean) : DebugPreferences {
        private var enabled = initial
        override fun isDebugMode(): Boolean = enabled
        override fun setDebugMode(enabled: Boolean) {
            this.enabled = enabled
        }
        override fun selectedBotIds(): Set<String> = emptySet()
        override fun setSelectedBotIds(ids: Set<String>) {}
    }
}
