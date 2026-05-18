package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DicewarsMapGenerationTest {

    @Test
    fun generatedMapHasValidActiveTerritories() {
        val game = DicewarsGame.generate(7, SequenceRandomSource())
        val map = game.toRenderMap()

        val activeTerritories = map.territories.filter { it.size > 0 }
        assertTrue(activeTerritories.isNotEmpty())
        activeTerritories.forEach { territory ->
            assertTrue(territory.size > 0)
            assertTrue(territory.armyCount in 1..DicewarsGame.MAX_DICE)
            assertTrue(territory.owner in 0 until game.pmax)
        }
    }

    @Test
    fun generatedMapContainsOpenSeaCells() {
        val map = DicewarsGame.generate(7, SequenceRandomSource()).toRenderMap()

        assertTrue(map.cells.any { it == 0 }, "generated maps should contain unoccupied sea/open cells")
    }

    @Test
    fun generatedMapGivesEveryPlayerTheSameInitialArmyTotal() {
        for (playerCount in 2..8) {
            val game = DicewarsGame.generate(playerCount, SequenceRandomSource())

            val armyTotals = (0 until playerCount).map { player ->
                game.areas
                    .filter { it.size > 0 && it.owner == player }
                    .sumOf { it.dice }
            }

            assertTrue(
                armyTotals.distinct().size == 1,
                "playerCount=$playerCount should have equal initial armies, got $armyTotals",
            )
        }
    }

    @Test
    fun adjacencyIsSymmetric() {
        val game = DicewarsGame.generate(7, SequenceRandomSource())
        val map = game.toRenderMap()

        map.territories.forEachIndexed { index, territory ->
            val areaId = index + 1
            territory.adjacentTerritories.forEach { adjacentAreaId ->
                assertTrue(adjacentAreaId in 1 until DicewarsGame.AREA_MAX)
                val adjacent = map.territories[adjacentAreaId - 1]
                assertTrue(
                    areaId in adjacent.adjacentTerritories,
                    "area $areaId should be in area $adjacentAreaId adjacency list",
                )
            }
        }
    }

    @Test
    fun everyActiveCellReferencesAnActiveTerritoryUsingJsAreaIds() {
        val game = DicewarsGame.generate(7, SequenceRandomSource())
        val map = game.toRenderMap()

        map.cells.forEach { areaId ->
            if (areaId > 0) {
                assertTrue(areaId in 1 until DicewarsGame.AREA_MAX)
                assertTrue(map.territories[areaId - 1].size > 0)
            }
        }
    }

    @Test
    fun adapterMapsGameFieldsToRendererModelConsistently() {
        val game = DicewarsGame.generate(7, SequenceRandomSource())
        val map = game.toRenderMap()

        assertEquals(DicewarsGame.XMAX, map.width)
        assertEquals(DicewarsGame.YMAX, map.height)
        assertEquals(game.cells, map.cells)
        assertEquals(game.cellNeighbors[29].directions, map.cellNeighbors[29].directions)

        for (areaId in 1 until DicewarsGame.AREA_MAX) {
            val area = game.areas[areaId]
            val territory = map.territories[areaId - 1]
            assertEquals(area.owner, territory.owner)
            assertEquals(area.dice, territory.armyCount)
            assertEquals(area.centerPos, territory.centerPos)
            assertEquals(area.size, territory.size)
            assertEquals(
                area.adjacentAreas.mapIndexedNotNull { index, value ->
                    if (index > 0 && value != 0) index else null
                },
                territory.adjacentTerritories,
            )
        }
    }
}

private class SequenceRandomSource : RandomSource {
    private var next = 0

    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        val value = next % bound
        next++
        return value
    }
}
