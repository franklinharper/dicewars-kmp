package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.presentation.components.computeTerritoryLabelPositionsForTest
import com.franklinharper.dicewarsport.presentation.components.findDicewarsTerritoryAtPositionForTest
import com.franklinharper.dicewarsport.presentation.components.visibleDiceCountLabelsForTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MapRendererLogicTest {

    @Test
    fun visibleDiceLabelsIncludeOnlyActiveTerritories() {
        val labels = visibleDiceCountLabelsForTest(rendererMap(), showTerritoryIds = false)
        val debugLabels = visibleDiceCountLabelsForTest(rendererMap(), showTerritoryIds = true)

        assertEquals(listOf("4", "2"), labels.map { it.text })
        assertEquals(listOf("4 (1)", "2 (2)"), debugLabels.map { it.text })
    }

    @Test
    fun territoryHitTestingMapsClickPositionToJsAreaId() {
        val map = rendererMap()
        val cellWidth = 30f
        val cellHeight = 20f

        assertEquals(1, findDicewarsTerritoryAtPositionForTest(15f, 10f, cellWidth, cellHeight, map))
        assertEquals(2, findDicewarsTerritoryAtPositionForTest(45f, 10f, cellWidth, cellHeight, map))
        assertEquals(null, findDicewarsTerritoryAtPositionForTest(75f, 10f, cellWidth, cellHeight, map))
    }

    @Test
    fun labelPositionForSingleCellTerritoryUsesTerritoryCenterCell() {
        val positions = computeTerritoryLabelPositionsForTest(rendererMap(), cellWidth = 30f, cellHeight = 18f)

        assertEquals(15f, positions[0].x)
        assertEquals(12f, positions[0].y)
        assertEquals(45f, positions[1].x)
        assertEquals(12f, positions[1].y)
    }

    private fun rendererMap(): GameMap {
        val cells = MutableList(DicewarsGame.MAX_WIDTH * DicewarsGame.MAX_HEIGHT) { 0 }
        cells[0] = 1
        cells[1] = 2
        val territories = (1 until DicewarsGame.AREA_MAX).map { areaId ->
            when (areaId) {
                1 -> Territory(id = 1, owner = 0, armyCount = 4, centerPos = 0, size = 1, adjacentTerritories = listOf(2))
                2 -> Territory(id = 2, owner = 1, armyCount = 2, centerPos = 1, size = 1, adjacentTerritories = listOf(1))
                else -> Territory(id = areaId, owner = -1, armyCount = 0, centerPos = 0, size = 0, adjacentTerritories = emptyList())
            }
        }
        return GameMap(
            gridWidth = DicewarsGame.MAX_WIDTH,
            gridHeight = DicewarsGame.MAX_HEIGHT,
            maxTerritories = DicewarsGame.AREA_MAX,
            cells = cells,
            territories = territories,
            cellNeighbors = List(cells.size) { cellIndex ->
                CellNeighbors(List(UiConstants.HEX_EDGE_COUNT) { direction -> DicewarsGame.nextCell(cellIndex, direction) })
            },
        )
    }
}
