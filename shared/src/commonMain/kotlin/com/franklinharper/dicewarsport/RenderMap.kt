package com.franklinharper.dicewarsport

fun DicewarsGame.toRenderMap(): GameMap = GameMap(
    gridWidth = DicewarsGame.MAX_WIDTH,
    gridHeight = DicewarsGame.MAX_HEIGHT,
    maxTerritories = DicewarsGame.AREA_MAX,
    cells = cells,
    territories = (1 until DicewarsGame.AREA_MAX).map { areaNumber ->
        val area = areas[areaNumber]
        Territory(
            id = areaNumber,
            owner = area.owner,
            armyCount = area.dice,
            centerPos = area.centerPos,
            size = area.size,
            adjacentTerritories = area.adjacentAreas.mapIndexedNotNull { index, value ->
                if (index > 0 && value != 0 && areas[index].size > 0) index else null
            },
        )
    },
    cellNeighbors = cellNeighbors,
)
