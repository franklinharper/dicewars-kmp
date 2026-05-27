package com.franklinharper.dicewarsport

import kotlinx.serialization.Serializable

@Serializable
data class DicewarsGame(
    val maxPlayers: Int = 7,
    val user: Int = 0,
    val cells: List<Int> = List(MAX_WIDTH * MAX_HEIGHT) { 0 },
    val cellNeighbors: List<CellNeighbors> = List(MAX_WIDTH * MAX_HEIGHT) { cellIndex ->
        CellNeighbors(List(6) { direction -> nextCell(cellIndex, direction) })
    },
    val areas: List<AreaData> = List(AREA_MAX) { AreaData() },
    val players: List<Player> = List(8) { Player() },
    val turnOrder: List<Int> = List(8) { it },
    val turnIndex: Int = 0,
    val history: List<HistoryData> = emptyList(),
) {
    companion object {
        const val MAX_WIDTH: Int = 28
        const val MAX_HEIGHT: Int = 32
        const val AREA_MAX: Int = 32
        const val STOCK_MAX: Int = 64
        const val MAX_DICE: Int = 8
        const val AVERAGE_DICE_PLACEMENT: Int = 3

        fun nextCell(position: Int, direction: Int): Int {
            val originX = position % MAX_WIDTH
            val originY = position / MAX_WIDTH
            val rowParity = originY % 2
            val deltaX: Int
            val deltaY: Int
            when (direction) {
                0 -> { deltaX = rowParity; deltaY = -1 }
                1 -> { deltaX = 1; deltaY = 0 }
                2 -> { deltaX = rowParity; deltaY = 1 }
                3 -> { deltaX = rowParity - 1; deltaY = 1 }
                4 -> { deltaX = -1; deltaY = 0 }
                5 -> { deltaX = rowParity - 1; deltaY = -1 }
                else -> return -1
            }
            val x = originX + deltaX
            val y = originY + deltaY
            if (x < 0 || y < 0 || x >= MAX_WIDTH || y >= MAX_HEIGHT) return -1
            return y * MAX_WIDTH + x
        }

        fun generate(pmax: Int, random: RandomSource, user: Int = 0): DicewarsGame =
            DicewarsMapGenerator.generate(pmax = pmax, random = random, user = user)
    }

    fun currentPlayerId(): Int = turnOrder[turnIndex]

    /**
     * Returns compact neighbor lists for each area.
     *
     * Area adjacency is stored as a dense AREA_MAX-sized marker list because it
     * mirrors the original game's data model. Most game logic only needs the
     * actual adjacent area IDs, so this helper converts the dense markers into
     * small IntArrays. Bots can compute this once per move decision and then
     * iterate only real neighbors instead of scanning every possible area.
     *
     * Results are cached; the cache is lost when [copy] creates a new instance
     * (acceptable since adjacency never changes and recomputation is cheap).
     */
    private var cachedNeighbors: Array<IntArray>? = null

    fun neighborIds(): Array<IntArray> {
        cachedNeighbors?.let { return it }
        val result = Array(AREA_MAX) { IntArray(0) }
        for (areaId in 1 until AREA_MAX) {
            val adjacentAreas = areas[areaId].adjacentAreas
            var count = 0
            for (neighborId in 1 until AREA_MAX) {
                if (adjacentAreas[neighborId] != 0) count++
            }
            if (count == 0) continue

            val neighbors = IntArray(count)
            var index = 0
            for (neighborId in 1 until AREA_MAX) {
                if (adjacentAreas[neighborId] != 0) neighbors[index++] = neighborId
            }
            result[areaId] = neighbors
        }
        cachedNeighbors = result
        return result
    }
}
