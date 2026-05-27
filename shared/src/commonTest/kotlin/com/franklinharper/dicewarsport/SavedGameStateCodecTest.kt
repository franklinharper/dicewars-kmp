package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedGameStateCodecTest {
    @Test
    fun roundTripsSavedGameState() {
        val game = DicewarsGame(
            maxPlayers = 2,
            user = 0,
            cells = List(DicewarsGame.MAX_WIDTH * DicewarsGame.MAX_HEIGHT) { if (it % 3 == 0) 1 else 2 },
            areas = List(DicewarsGame.AREA_MAX) { areaId ->
                when (areaId) {
                    1 -> AreaData(
                        size = 4,
                        centerPos = 10,
                        owner = 0,
                        dice = 3,
                        left = 1,
                        right = 4,
                        top = 2,
                        bottom = 5,
                        centerX = 12,
                        centerY = 14,
                        minDistance = 2,
                        lineCells = listOf(1, 2, 3),
                        lineDirections = listOf(0, 1, 2),
                        adjacentAreas = List(DicewarsGame.AREA_MAX) { if (it == 2) 1 else 0 },
                    )
                    2 -> AreaData(size = 5, centerPos = 20, owner = 1, dice = 6)
                    else -> AreaData()
                }
            },
            players = List(8) { player ->
                Player(
                    areaCount = player,
                    maxConnectedAreaCount = if (player < 2) 1 else 0,
                    diceCount = player + 3,
                    diceRank = player % 3,
                    stock = player + 1,
                )
            },
            turnOrder = listOf(1, 0, 2, 3, 4, 5, 6, 7),
            turnIndex = 0,
            history = listOf(HistoryData(from = 1, to = 2, result = 1)),
        )
        val saved = SavedGameState(
            screen = DicewarsScreen.AiTurn,
            game = game,
            selectedFrom = 1,
            selectedTo = null,
            spectateMode = false,
            selectedPlayerCount = 2,
            soundEnabled = false,
            debugMode = true,
            playerNames = mapOf(0 to "Human", 1 to "Bully"),
            playerIds = mapOf(0 to "human", 1 to "bully"),
            eliminatedPlayerIds = listOf("human"),
            eliminatedPlayerSeats = listOf(0),
            gameStatsRecorded = true,
            humanAutoplayEnabled = false,
            resolvingAfterHumanEliminated = true,
            selectedDebugBotIds = setOf("bully", "max"),
        )

        val encoded = SavedGameStateCodec.encode(saved)
        val decoded = SavedGameStateCodec.decode(encoded)

        assertEquals(saved, decoded)
    }

    @Test
    fun returnsNullForInvalidSavedState() {
        assertNull(SavedGameStateCodec.decode("not a saved state"))
    }

    @Test
    fun savedStateFromUiStateNormalizesTransientScreens() {
        val saved = SavedGameState.fromUiState(
            GameUiState(screen = DicewarsScreen.Debug, game = DicewarsGame()),
        )

        assertEquals(DicewarsScreen.Title, saved?.screen)
    }

    @Test
    fun restoredAiTurnCanContinue() {
        val reducer = GameReducer(FixedRandom())
        val game = DicewarsGame.generate(2, FixedRandom())
        val aiTurnIndex = game.turnOrder.indexOf(1).takeIf { it >= 0 } ?: 1
        val restored = reducer.restore(
            SavedGameState(
                screen = DicewarsScreen.AiTurn,
                game = game.copy(turnIndex = aiTurnIndex),
                selectedFrom = null,
                selectedTo = null,
                spectateMode = false,
                selectedPlayerCount = 2,
                soundEnabled = true,
                debugMode = false,
                playerNames = mapOf(0 to "Human", 1 to "Bully"),
                playerIds = mapOf(0 to "human", 1 to "bully"),
                eliminatedPlayerIds = emptyList(),
                eliminatedPlayerSeats = emptyList(),
                gameStatsRecorded = false,
                humanAutoplayEnabled = false,
                resolvingAfterHumanEliminated = false,
                selectedDebugBotIds = emptySet(),
            ),
            PlayerStatsHistory.default(),
        )

        val result = reducer.reduce(restored, GameAction.AiStep)

        assertTrue(result.state.game.history.isNotEmpty() || result.state.game.turnIndex != restored.game.turnIndex)
    }

    private class FixedRandom : RandomSource {
        override fun nextInt(bound: Int): Int {
            require(bound > 0)
            return 0
        }
    }
}
