package com.franklinharper.dicewarsport

object HexGrid {
    const val GRID_WIDTH: Int = DicewarsGame.XMAX
    const val GRID_HEIGHT: Int = DicewarsGame.YMAX
    const val TOTAL_CELLS: Int = GRID_WIDTH * GRID_HEIGHT

    fun cellIndex(x: Int, y: Int): Int = y * GRID_WIDTH + x
    fun cellX(cellIndex: Int): Int = cellIndex % GRID_WIDTH
    fun cellY(cellIndex: Int): Int = cellIndex / GRID_WIDTH

    fun getCellPosition(cellIndex: Int, cellWidth: Float, cellHeight: Float): Pair<Float, Float> {
        val x = cellX(cellIndex)
        val y = cellY(cellIndex)
        val posX = x * cellWidth + if (y % 2 == 1) cellWidth / 2 else 0f
        val posY = y * cellHeight
        return Pair(posX, posY)
    }
}
