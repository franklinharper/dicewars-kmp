package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DicewarsRulesEdgeCaseTest {

    @Test
    fun legalAttackRejectsOutOfRangeTerritoryIds() {
        val game = rulesGame()

        assertFalse(game.isLegalAttack(from = 0, to = 2, player = 0))
        assertFalse(game.isLegalAttack(from = DicewarsGame.AREA_MAX, to = 2, player = 0))
        assertFalse(game.isLegalAttack(from = 1, to = 0, player = 0))
        assertFalse(game.isLegalAttack(from = 1, to = DicewarsGame.AREA_MAX, player = 0))
    }

    @Test
    fun legalAttackAcceptsAsymmetricAdjacency() {
        val game = rulesGame(
            area1Adj = emptyList(),
            area2Adj = listOf(1),
        )

        assertEquals(true, game.isLegalAttack(from = 1, to = 2, player = 0))
    }

    @Test
    fun supplyOneDieDoesNothingWhenStockIsZero() {
        val game = rulesGame().copy(
            players = rulesGame().players.toMutableList().also { players ->
                players[0] = players[0].copy(stock = 0)
            },
        )

        val (result, suppliedArea) = game.supplyOneDie(player = 0, random = ZeroRandom())

        assertEquals(null, suppliedArea)
        assertEquals(game, result)
    }

    @Test
    fun supplyOneDieDoesNothingWhenAllOwnedAreasAreMaxed() {
        val game = rulesGame(sourceDice = DicewarsGame.MAX_DICE).copy(
            players = rulesGame().players.toMutableList().also { players ->
                players[0] = players[0].copy(stock = 3)
            },
        )

        val (result, suppliedArea) = game.supplyOneDie(player = 0, random = ZeroRandom())

        assertEquals(null, suppliedArea)
        assertEquals(game, result)
    }

    @Test
    fun setAreaTcCountsSeparatedConnectedComponents() {
        val game = DicewarsGame(
            maxPlayers = 2,
            areas = List(DicewarsGame.AREA_MAX) { i ->
                when (i) {
                    1 -> AreaData(size = 5, owner = 0, dice = 2, adjacentAreas = adj(2))
                    2 -> AreaData(size = 5, owner = 0, dice = 3, adjacentAreas = adj(1))
                    3 -> AreaData(size = 5, owner = 0, dice = 4, adjacentAreas = emptyAdj())
                    4 -> AreaData(size = 5, owner = 1, dice = 5, adjacentAreas = emptyAdj())
                    else -> AreaData()
                }
            },
        ).setAreaTc(0)

        assertEquals(3, game.players[0].areaCount)
        assertEquals(9, game.players[0].diceCount)
        assertEquals(2, game.players[0].maxConnectedAreaCount)
    }

    private fun rulesGame(
        sourceDice: Int = 3,
        targetDice: Int = 2,
        area1Adj: List<Int> = listOf(2),
        area2Adj: List<Int> = listOf(1),
    ): DicewarsGame {
        val game = DicewarsGame(
            maxPlayers = 2,
            turnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7),
            areas = List(DicewarsGame.AREA_MAX) { i ->
                when (i) {
                    1 -> AreaData(size = 5, owner = 0, dice = sourceDice, adjacentAreas = adj(*area1Adj.toIntArray()))
                    2 -> AreaData(size = 5, owner = 1, dice = targetDice, adjacentAreas = adj(*area2Adj.toIntArray()))
                    else -> AreaData()
                }
            },
        )
        return game.setAreaTc(0).setAreaTc(1)
    }

    private fun adj(vararg ids: Int): List<Int> = MutableList(DicewarsGame.AREA_MAX) { index ->
        if (index in ids) 1 else 0
    }

    private fun emptyAdj(): List<Int> = List(DicewarsGame.AREA_MAX) { 0 }

    private class ZeroRandom : RandomSource {
        override fun nextInt(bound: Int): Int {
            require(bound > 0)
            return 0
        }
    }
}
