package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertContentEquals

class DicewarsAiTest {
    @Test
    fun precomputeNeighborsReturnsCompactNeighborLists() {
        val game = DicewarsGame(
            areas = List(DicewarsGame.AREA_MAX) { i ->
                when (i) {
                    1 -> AreaData(size = 5, owner = 0, dice = 3, adjacentAreas = aiAdj(2, 4))
                    2 -> AreaData(size = 5, owner = 1, dice = 2, adjacentAreas = aiAdj(1))
                    4 -> AreaData(size = 5, owner = 1, dice = 2, adjacentAreas = aiAdj(1, 7))
                    7 -> AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = aiAdj(4))
                    else -> AreaData()
                }
            },
        )

        val neighbors = game.neighborIds()

        assertContentEquals(intArrayOf(2, 4), neighbors[1])
        assertContentEquals(intArrayOf(1), neighbors[2])
        assertContentEquals(intArrayOf(1, 7), neighbors[4])
        assertContentEquals(intArrayOf(), neighbors[3])
    }
}
