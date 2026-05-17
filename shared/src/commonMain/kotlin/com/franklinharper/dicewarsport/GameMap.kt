package com.franklinharper.dicewarsport

data class CellNeighbors(
    val directions: List<Int>,
)

data class Territory(
    val id: Int,
    val owner: Int,
    val armyCount: Int,
    val centerPos: Int,
    val size: Int,
    val adjacentTerritories: List<Int>,
)

data class GameMap(
    val gridWidth: Int,
    val gridHeight: Int,
    val maxTerritories: Int,
    val cells: List<Int>,
    val territories: List<Territory>,
    val cellNeighbors: List<CellNeighbors>,
) {
    val width: Int = gridWidth
    val height: Int = gridHeight
}
