package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.AlwaysAttackWhenStrongerBot
import com.franklinharper.dicewarsport.ai.CautiousBot
import com.franklinharper.dicewarsport.ai.MaxBot
import com.franklinharper.dicewarsport.ai.StrategicBot
import com.franklinharper.dicewarsport.ai.TargetTheLeader
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DicewarsAiTest {

    @Test
    fun aiStrategiesNeverReturnIllegalMoves() {
        val strategies = listOf(
            AlwaysAttackWhenStrongerBot(FixedAiRandom(0)),
            TargetTheLeader(FixedAiRandom(0)),
            CautiousBot(),
            StrategicBot(FixedAiRandom(0)),
        )
        for (strategy in strategies) {
            val game = aiGame()
            val move = strategy.chooseMove(game)
            assertNotNull(move, strategy::class.simpleName ?: "strategy")
            assertTrue(game.isLegalAttack(move.from, move.to), "${strategy::class.simpleName} returned illegal $move")
        }
    }

    @Test
    fun precomputeNeighborsReturnsCompactNeighborLists() {
        val game = DicewarsGame(
            areas = List(DicewarsGame.AREA_MAX) { i ->
                when (i) {
                    1 -> AreaData(size = 5, owner = 0, dice = 3, adjacentAreas = adj(2, 4))
                    2 -> AreaData(size = 5, owner = 1, dice = 2, adjacentAreas = adj(1))
                    4 -> AreaData(size = 5, owner = 1, dice = 2, adjacentAreas = adj(1, 7))
                    7 -> AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = adj(4))
                    else -> AreaData()
                }
            },
        )

        val neighbors = game.precomputeNeighbors()

        assertContentEquals(intArrayOf(2, 4), neighbors[1])
        assertContentEquals(intArrayOf(1), neighbors[2])
        assertContentEquals(intArrayOf(1, 7), neighbors[4])
        assertContentEquals(intArrayOf(), neighbors[3])
    }

    @Test
    fun alwaysAttackWhenStrongerBotAttacksOnlyWeakerAdjacentEnemyAreas() {
        val game = aiGame().copy(
            areas = aiGame().areas.toMutableList().also {
                it[2] = it[2].copy(dice = it[1].dice)
            },
        )

        val move = AlwaysAttackWhenStrongerBot(FixedAiRandom(0)).chooseMove(game)

        assertNotNull(move)
        assertEquals(3, move.to)
        assertTrue(game.areas[move.to].dice < game.areas[move.from].dice)
    }

    @Test
    fun emperorBotReturnsLegalMoveOnSimpleBoard() {
        val game = aiGame()

        val move = StrategicBot(FixedAiRandom(0)).chooseMove(game)

        assertNotNull(move)
        assertTrue(game.isLegalAttack(move.from, move.to), "StrategicBot returned illegal $move")
    }

    @Test
    fun turtleBotSkipsAttackFromVulnerableAreaWhenEstablishedAndNoStock() {
        val game = turtleSkipGame()

        assertNull(CautiousBot().chooseMove(game))
    }

    @Test
    fun noValidMovesReturnsNull() {
        val game = aiGame().copy(
            areas = aiGame().areas.toMutableList().also {
                it[1] = it[1].copy(dice = 1)
                it[4] = it[4].copy(dice = 1)
            },
        )

        assertNull(AlwaysAttackWhenStrongerBot(FixedAiRandom(0)).chooseMove(game))
        assertNull(TargetTheLeader(FixedAiRandom(0)).chooseMove(game))
        assertNull(CautiousBot().chooseMove(game))
    }
}

private fun adj(vararg ids: Int): List<Int> {
    val list = MutableList(DicewarsGame.AREA_MAX) { 0 }
    for (id in ids) if (id in list.indices) list[id] = 1
    return list
}

private fun aiGame(): DicewarsGame {
    val game = DicewarsGame(
        pmax = 2,
        turnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7),
        turnIndex = 0,
        areas = List(DicewarsGame.AREA_MAX) { i ->
            when (i) {
                1 -> AreaData(size = 5, owner = 0, dice = 5, adjacentAreas = adj(2, 3))
                2 -> AreaData(size = 5, owner = 1, dice = 3, adjacentAreas = adj(1, 4))
                3 -> AreaData(size = 5, owner = 1, dice = 4, adjacentAreas = adj(1))
                4 -> AreaData(size = 5, owner = 0, dice = 2, adjacentAreas = adj(2))
                else -> AreaData()
            }
        },
    )
    return game.setAreaTc(0).setAreaTc(1)
}

private fun turtleSkipGame(): DicewarsGame {
    val game = DicewarsGame(
        pmax = 2,
        turnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7),
        turnIndex = 0,
        areas = List(DicewarsGame.AREA_MAX) { i ->
            when (i) {
                1 -> AreaData(size = 5, owner = 0, dice = 5, adjacentAreas = adj(2, 6, 7))
                2 -> AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = adj(1, 3))
                3 -> AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = adj(2, 4))
                4 -> AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = adj(3, 5))
                5 -> AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = adj(4))
                6 -> AreaData(size = 5, owner = 1, dice = 3, adjacentAreas = adj(1))
                7 -> AreaData(size = 5, owner = 1, dice = 4, adjacentAreas = adj(1))
                else -> AreaData()
            }
        },
    )
    return game.setAreaTc(0).setAreaTc(1)
}

private class FixedAiRandom(private val value: Int) : RandomSource {
    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        return value % bound
    }
}
