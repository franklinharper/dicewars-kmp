package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.MaxBot
import com.franklinharper.dicewarsport.ai.Move
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameUiReducerTest {

    @Test
    fun loadingTransitionsToTitle() {
        val result = reducer().reduce(initialUiState(), GameAction.LoadingFinished)

        assertEquals(DicewarsScreen.Title, result.state.screen)
    }

    @Test
    fun titleTransitionsDirectlyToGameScreen() {
        val result = reducer().reduce(initialUiState(screen = DicewarsScreen.Title), GameAction.StartPressed)

        assertTrue(result.state.screen == DicewarsScreen.HumanTurn || result.state.screen == DicewarsScreen.AiTurn)
    }

    @Test
    fun selectedPlayerCountIsUsedWhenGameStarts() {
        val reducer = reducer()
        val selected = reducer.reduce(initialUiState(screen = DicewarsScreen.Title), GameAction.SelectPlayerCount(4))
        val started = reducer.reduce(selected.state, GameAction.StartPressed)

        assertEquals(4, selected.state.selectedPlayerCount)
        assertEquals(4, started.state.game.pmax)
        assertTrue(started.state.screen == DicewarsScreen.HumanTurn || started.state.screen == DicewarsScreen.AiTurn)
        assertEquals(listOf(SoundEvent.BUTTON), selected.soundEvents)
    }

    @Test
    fun startAssignsNamesToHumanAndAiPlayers() {
        val reducer = reducer()
        val selected = reducer.reduce(initialUiState(screen = DicewarsScreen.Title), GameAction.SelectPlayerCount(4))
        val started = reducer.reduce(selected.state, GameAction.StartPressed)

        assertEquals("Human", started.state.playerNames[0])
        assertEquals("Rebel", started.state.playerNames[1])
        assertEquals("Rebel", started.state.playerNames[2])
        assertEquals("Rebel", started.state.playerNames[3])
    }

    @Test
    fun humanTurnResolvesAttackAndStaysOnTurnAfterTwoLegalClicks() {
        val reducer = reducer()
        val selected = reducer.reduce(turnState(DicewarsScreen.HumanTurn), GameAction.TerritoryClicked(1))
        val next = reducer.reduce(selected.state, GameAction.TerritoryClicked(2))

        assertEquals(DicewarsScreen.HumanTurn, next.state.screen)
        assertEquals(null, next.state.selectedFrom)
        assertEquals(null, next.state.selectedTo)
        assertEquals(0, next.state.game.areas[2].owner)
        assertEquals(3, next.state.game.areas[2].dice)
        assertEquals(1, next.state.game.areas[1].dice)
        assertEquals(2, next.state.game.players[0].maxConnectedAreaCount)
        assertEquals(1, next.state.game.players[1].maxConnectedAreaCount)
        assertEquals(listOf(SoundEvent.CLICK), selected.soundEvents)
        assertTrue(next.soundEvents.contains(SoundEvent.DICE))
        assertTrue(next.soundEvents.contains(SoundEvent.SUCCESS))
    }

    @Test
    fun humanTurnReceivesReinforcementsAndAdvancesToNextPlayerOnEndTurn() {
        val result = reducer().reduce(turnState(DicewarsScreen.HumanTurn), GameAction.EndTurn)

        assertEquals(DicewarsScreen.AiTurn, result.state.screen)
        assertEquals(1, result.state.game.currentPlayer())
        assertEquals(5, result.state.game.areas[1].dice)
    }

    @Test
    fun aiTurnFinishesTurnImmediatelyWhenBotHasNoMove() {
        val noMoveGame = uiGame(
            areas = mapOf(
                1 to AreaData(size = 5, owner = 0, dice = 8, adjacentAreas = adj(2)),
                2 to AreaData(size = 5, owner = 1, dice = 1, adjacentAreas = adj(1)),
            ),
            turnIndex = 1,
        )
        val finishedReducer = reducerWithAssignedBots()
        val finished = finishedReducer.reduce(GameUiState(screen = DicewarsScreen.AiTurn, game = noMoveGame), GameAction.AiStep)

        assertEquals(DicewarsScreen.HumanTurn, finished.state.screen)
        assertEquals(0, finished.state.game.currentPlayer())
    }

    @Test
    fun immediateAttackResolutionTransitionsToWinOrGameOver() {
        val gameOverGame = uiGame(areas = mapOf(
            1 to AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = adj(2)),
            2 to AreaData(size = 5, owner = 1, dice = 8, adjacentAreas = adj(1)),
        ), turnIndex = 1)
        val reducer = reducerWithAssignedBots()
        val gameOver = reducer.reduce(
            GameUiState(screen = DicewarsScreen.AiTurn, game = gameOverGame),
            GameAction.AiStep,
        )
        assertEquals(DicewarsScreen.GameOver, gameOver.state.screen)
        assertTrue(gameOver.soundEvents.contains(SoundEvent.GAME_OVER))

        val winGame = uiGame(areas = mapOf(
            1 to AreaData(size = 5, owner = 0, dice = 4, adjacentAreas = adj(2)),
            2 to AreaData(size = 5, owner = 1, dice = 2, adjacentAreas = adj(1, 3)),
            3 to AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = adj(2)),
        ))
        val win = reducer().reduce(
            reducer().reduce(GameUiState(screen = DicewarsScreen.HumanTurn, game = winGame), GameAction.TerritoryClicked(1)).state,
            GameAction.TerritoryClicked(2),
        )
        assertEquals(DicewarsScreen.Win, win.state.screen)
        assertTrue(win.soundEvents.contains(SoundEvent.WIN))
    }

    @Test
    fun finishMatchAfterHumanEliminatedRecordsActualWinnerWithoutSounds() {
        val game = uiGame(
            areas = mapOf(
                1 to AreaData(size = 0, owner = 0, dice = 0, adjacentAreas = adj(2)),
                2 to AreaData(size = 5, owner = 1, dice = 8, adjacentAreas = adj(1, 3)),
                3 to AreaData(size = 5, owner = 2, dice = 7, adjacentAreas = adj(2)),
            ),
            playerCount = 3,
            turnIndex = 1,
        )
        val state = GameUiState(
            screen = DicewarsScreen.AiTurn,
            game = game,
            resolvingAfterHumanEliminated = true,
            eliminatedPlayerSeats = listOf(0),
            playerIds = mapOf(0 to "human", 1 to "rebel", 2 to "turtle"),
            playerNames = mapOf(0 to "Human", 1 to "Rebel", 2 to "Turtle"),
        )

        val reducer = reducer()
        reducer.reduce(initialUiState(screen = DicewarsScreen.Title), GameAction.StartPressed)

        val result = reducer.reduce(state, GameAction.FinishMatchAfterHumanEliminated)

        assertEquals(DicewarsScreen.GameOver, result.state.screen)
        assertEquals(false, result.state.resolvingAfterHumanEliminated)
        assertTrue(result.state.gameStatsRecorded)
        assertEquals(emptyList(), result.soundEvents)
        assertEquals(1, result.state.playerStatsHistory.records.getValue("rebel").wins)
    }

    @Test
    fun humanAutoplayTogglesAndResetsForNewGame() {
        val reducer = reducer()
        val enabled = reducer.reduce(turnState(DicewarsScreen.HumanTurn), GameAction.ToggleHumanAutoplay)
        assertTrue(enabled.state.humanAutoplayEnabled)

        val started = reducer.reduce(enabled.state.copy(screen = DicewarsScreen.Title), GameAction.StartPressed)
        assertEquals(false, started.state.humanAutoplayEnabled)
    }

    @Test
    fun humanAutoplayAttacksOnlyWhenReserveCanReplenishAllOwnedTerritoriesAfterEitherOutcome() {
        val game = uiGame(
            areas = mapOf(
                1 to AreaData(size = 5, owner = 0, dice = 8, adjacentAreas = adj(2)),
                2 to AreaData(size = 5, owner = 1, dice = 7, adjacentAreas = adj(1)),
            ),
        ).withStock(player = 0, stock = 6)
        val state = GameUiState(screen = DicewarsScreen.HumanTurn, game = game, humanAutoplayEnabled = true)

        val result = reducer().reduce(state, GameAction.HumanAutoplayStep)

        assertEquals(0, result.state.game.areas[2].owner)
        assertEquals(7, result.state.game.areas[2].dice)
    }

    @Test
    fun autoplayStrategyChoosesMostFavorableAvailableAttack() {
        val game = uiGame(
            areas = mapOf(
                1 to AreaData(size = 5, owner = 0, dice = 5, adjacentAreas = adj(2)),
                2 to AreaData(size = 5, owner = 1, dice = 5, adjacentAreas = adj(1)),
                3 to AreaData(size = 5, owner = 0, dice = 8, adjacentAreas = adj(4)),
                4 to AreaData(size = 5, owner = 1, dice = 2, adjacentAreas = adj(3)),
            ),
        ).withStock(player = 0, stock = 64)
        val state = GameUiState(screen = DicewarsScreen.HumanTurn, game = game, humanAutoplayEnabled = true)

        val result = reducer().reduce(state, GameAction.HumanAutoplayStep)

        assertEquals(1, result.state.game.areas[2].owner)
        assertEquals(0, result.state.game.areas[4].owner)
    }

    @Test
    fun maxBotUsesAutoplayStrategy() {
        val game = uiGame(
            areas = mapOf(
                1 to AreaData(size = 5, owner = 0, dice = 5, adjacentAreas = adj(2)),
                2 to AreaData(size = 5, owner = 1, dice = 5, adjacentAreas = adj(1)),
                3 to AreaData(size = 5, owner = 0, dice = 8, adjacentAreas = adj(4)),
                4 to AreaData(size = 5, owner = 1, dice = 2, adjacentAreas = adj(3)),
            ),
        ).withStock(player = 0, stock = 64)

        val move = MaxBot().chooseMove(game)

        assertEquals(Move(3, 4), move)
    }

    @Test
    fun humanAutoplayDoesNotAttackWhenReserveCannotReplenishWinOutcome() {
        val game = uiGame(
            areas = mapOf(
                1 to AreaData(size = 5, owner = 0, dice = 8, adjacentAreas = adj(2)),
                2 to AreaData(size = 5, owner = 1, dice = 8, adjacentAreas = adj(1)),
            ),
        ).withStock(player = 0, stock = 5)
        val state = GameUiState(screen = DicewarsScreen.HumanTurn, game = game, humanAutoplayEnabled = true)

        val result = reducer().reduce(state, GameAction.HumanAutoplayStep)

        assertEquals(1, result.state.game.areas[2].owner)
        assertEquals(DicewarsScreen.AiTurn, result.state.screen)
    }

    @Test
    fun humanAutoplayDoesNotAttackWhenDefenderHasMoreDice() {
        val game = uiGame(
            areas = mapOf(
                1 to AreaData(size = 5, owner = 0, dice = 4, adjacentAreas = adj(2)),
                2 to AreaData(size = 5, owner = 1, dice = 5, adjacentAreas = adj(1)),
            ),
        ).withStock(player = 0, stock = 64)
        val state = GameUiState(screen = DicewarsScreen.HumanTurn, game = game, humanAutoplayEnabled = true)

        val result = reducer().reduce(state, GameAction.HumanAutoplayStep)

        assertEquals(1, result.state.game.areas[2].owner)
        assertEquals(DicewarsScreen.AiTurn, result.state.screen)
    }

    @Test
    fun winAndGameOverReturnToTitle() {
        val fromWin = reducer().reduce(initialUiState(screen = DicewarsScreen.Win), GameAction.BackToTitle)
        val fromGameOver = reducer().reduce(initialUiState(screen = DicewarsScreen.GameOver), GameAction.BackToTitle)

        assertEquals(DicewarsScreen.Title, fromWin.state.screen)
        assertEquals(DicewarsScreen.Title, fromGameOver.state.screen)
    }

    @Test
    fun spectateModeStartsDirectlyOnAiTurn() {
        val result = reducer().reduce(initialUiState(screen = DicewarsScreen.Title), GameAction.StartSpectate)

        assertTrue(result.state.spectateMode)
        assertEquals(DicewarsScreen.AiTurn, result.state.screen)
    }

    @Test
    fun statsBackReturnsToPreviousScreen() {
        val reducer = reducer()
        val fromTitle = reducer.reduce(initialUiState(screen = DicewarsScreen.Title), GameAction.ShowStats).state
        assertEquals(DicewarsScreen.Title, reducer.reduce(fromTitle, GameAction.BackFromStats).state.screen)

        val fromWin = reducer.reduce(initialUiState(screen = DicewarsScreen.Win), GameAction.ShowStats).state
        assertEquals(DicewarsScreen.Win, reducer.reduce(fromWin, GameAction.BackFromStats).state.screen)
    }

    @Test
    fun debugModeCanBeToggledViaTitleTaps() {
        val reducer = reducer()
        var state = initialUiState(screen = DicewarsScreen.Title)
        // Tap 4 times - not enough
        repeat(4) { state = reducer.reduce(state, GameAction.TitleTapped).state }
        assertEquals(false, state.debugMode)
        // 5th tap enables debug mode
        state = reducer.reduce(state, GameAction.TitleTapped).state
        assertEquals(true, state.debugMode)
    }

    @Test
    fun debugModeCanBeDisabled() {
        val reducer = reducer()
        var state = initialUiState(screen = DicewarsScreen.Title).copy(debugMode = true)
        state = reducer.reduce(state, GameAction.DisableDebugMode).state
        assertEquals(false, state.debugMode)
        assertEquals(DicewarsScreen.Title, state.screen)
    }

    @Test
    fun showDebugScreenNavigatesToWinScreen() {
        val result = reducer().reduce(
            initialUiState(screen = DicewarsScreen.Debug),
            GameAction.ShowDebugScreen(DicewarsScreen.Win),
        )
        assertEquals(DicewarsScreen.Win, result.state.screen)
        assertEquals(listOf(SoundEvent.WIN), result.soundEvents)
    }

    @Test
    fun showDebugScreenGeneratesGameForHumanTurn() {
        val result = reducer().reduce(
            initialUiState(screen = DicewarsScreen.Debug),
            GameAction.ShowDebugScreen(DicewarsScreen.HumanTurn),
        )
        assertEquals(DicewarsScreen.HumanTurn, result.state.screen)
        // Game should have been generated (non-default areas exist)
        assertTrue(result.state.game.areas.any { it.size > 0 })
    }
}

private fun adj(vararg ids: Int): List<Int> {
    val list = MutableList(DicewarsGame.AREA_MAX) { 0 }
    for (id in ids) if (id in list.indices) list[id] = 1
    return list
}

private fun reducer(): GameReducer = GameReducer(
    random = UiFixedRandom(),
    debugPreferences = object : DebugPreferences {
        private var mode = false
        override fun isDebugMode(): Boolean = mode
        override fun setDebugMode(enabled: Boolean) { mode = enabled }
    },
)

private fun reducerWithAssignedBots(): GameReducer = reducer().also { reducer ->
    reducer.reduce(initialUiState(screen = DicewarsScreen.Title), GameAction.StartPressed)
}

private fun initialUiState(screen: DicewarsScreen = DicewarsScreen.Loading): GameUiState = GameUiState(
    screen = screen,
    game = uiGame(),
)

private fun turnState(screen: DicewarsScreen, currentPlayer: Int = 0): GameUiState {
    val game = uiGame().copy(turnIndex = currentPlayer)
    return GameUiState(screen = screen, game = game)
}

private fun uiGame(
    areas: Map<Int, AreaData> = mapOf(
        1 to AreaData(size = 5, owner = 0, dice = 4, adjacentAreas = adj(2)),
        2 to AreaData(size = 5, owner = 1, dice = 2, adjacentAreas = adj(1, 3)),
        3 to AreaData(size = 5, owner = 1, dice = 1, adjacentAreas = adj(2)),
    ),
    turnIndex: Int = 0,
    playerCount: Int = 2,
): DicewarsGame {
    val game = DicewarsGame(
        pmax = playerCount,
        user = 0,
        turnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7),
        turnIndex = turnIndex,
        areas = List(DicewarsGame.AREA_MAX) { i -> areas[i] ?: AreaData() },
    )
    return (0 until playerCount).fold(game) { current, player -> current.setAreaTc(player) }
}

private fun DicewarsGame.withStock(player: Int, stock: Int): DicewarsGame {
    val newPlayers = players.toMutableList()
    newPlayers[player] = newPlayers[player].copy(stock = stock)
    return copy(players = newPlayers)
}

private class UiFixedRandom : RandomSource {
    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        return 0
    }
}
