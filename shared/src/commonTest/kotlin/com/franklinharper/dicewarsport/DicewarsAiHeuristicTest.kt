package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.RebelBot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DicewarsAiHeuristicTest {

    @Test
    fun targetTheLeaderTargetsDominantPlayerWhenOneExists() {
        val game = aiGame(
            areas = mapOf(
                1 to AreaData(size = 5, owner = 0, dice = 6, adjacentAreas = adj(2, 3)),
                2 to AreaData(size = 5, owner = 1, dice = 3, adjacentAreas = adj(1)),
                3 to AreaData(size = 5, owner = 2, dice = 3, adjacentAreas = adj(1)),
                4 to AreaData(size = 5, owner = 1, dice = 8, adjacentAreas = emptyAdj()),
            ),
        )

        val move = RebelBot(FixedRandom(0)).chooseMove(game)

        assertNotNull(move)
        assertEquals(2, move.to, "player 1 has more than 40% of total dice and should be targeted")
    }

    @Test
    fun targetTheLeaderSkipsEqualDiceAttackWhenNeitherSideIsTopRankedAndRandomRollIsLow() {
        val game = aiGame(
            areas = mapOf(
                1 to AreaData(size = 5, owner = 0, dice = 3, adjacentAreas = adj(2)),
                2 to AreaData(size = 5, owner = 1, dice = 3, adjacentAreas = adj(1)),
                3 to AreaData(size = 5, owner = 2, dice = 4, adjacentAreas = emptyAdj()),
                4 to AreaData(size = 5, owner = 3, dice = 2, adjacentAreas = emptyAdj()),
            ),
            pmax = 4,
        )

        assertNull(RebelBot(FixedRandom(0)).chooseMove(game))
    }

    @Test
    fun targetTheLeaderAllowsEqualDiceAttackWhenRandomRollIsHigh() {
        val game = aiGame(
            areas = mapOf(
                1 to AreaData(size = 5, owner = 0, dice = 3, adjacentAreas = adj(2)),
                2 to AreaData(size = 5, owner = 1, dice = 3, adjacentAreas = adj(1)),
                3 to AreaData(size = 5, owner = 2, dice = 4, adjacentAreas = emptyAdj()),
                4 to AreaData(size = 5, owner = 3, dice = 2, adjacentAreas = emptyAdj()),
            ),
            pmax = 4,
        )

        val move = RebelBot(FixedRandom(2, 0)).chooseMove(game)

        assertEquals(1, move?.from)
        assertEquals(2, move?.to)
    }

    private fun aiGame(areas: Map<Int, AreaData>, pmax: Int = 3): DicewarsGame {
        var game = DicewarsGame(
            maxPlayers = pmax,
            turnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7),
            turnIndex = 0,
            areas = List(DicewarsGame.AREA_MAX) { i -> areas[i] ?: AreaData() },
        )
        for (player in 0 until pmax) game = game.setAreaTc(player)
        return game
    }

    private fun adj(vararg ids: Int): List<Int> = MutableList(DicewarsGame.AREA_MAX) { index ->
        if (index in ids) 1 else 0
    }

    private fun emptyAdj(): List<Int> = List(DicewarsGame.AREA_MAX) { 0 }

    private class FixedRandom(private vararg val values: Int) : RandomSource {
        private var index = 0
        override fun nextInt(bound: Int): Int {
            require(bound > 0)
            val value = values.getOrElse(index) { 0 } % bound
            index++
            return value
        }
    }
}
