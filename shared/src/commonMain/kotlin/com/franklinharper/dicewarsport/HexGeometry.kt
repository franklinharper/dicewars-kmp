package com.franklinharper.dicewarsport

object HexGeometry {
    fun getHexagonVertices(
        x: Float,
        y: Float,
        cellWidth: Float,
        cellHeight: Float,
    ): List<Pair<Float, Float>> {
        val halfWidth = cellWidth / 2f
        val hexHeight = cellHeight * 4f / 3f
        val quarterHexHeight = hexHeight / 4f

        return listOf(
            Pair(x + halfWidth, y),
            Pair(x + cellWidth, y + quarterHexHeight),
            Pair(x + cellWidth, y + hexHeight - quarterHexHeight),
            Pair(x + halfWidth, y + hexHeight),
            Pair(x, y + hexHeight - quarterHexHeight),
            Pair(x, y + quarterHexHeight),
        )
    }

    fun getHexEdgePoints(
        x: Float,
        y: Float,
        cellWidth: Float,
        cellHeight: Float,
        direction: Int,
    ): Pair<Pair<Float, Float>, Pair<Float, Float>>? {
        val halfWidth = cellWidth / 2f
        val hexHeight = cellHeight * 4f / 3f
        val quarterHexHeight = hexHeight / 4f

        return when (direction) {
            0 -> Pair(Pair(x + halfWidth, y), Pair(x + cellWidth, y + quarterHexHeight))
            1 -> Pair(Pair(x + cellWidth, y + quarterHexHeight), Pair(x + cellWidth, y + hexHeight - quarterHexHeight))
            2 -> Pair(Pair(x + cellWidth, y + hexHeight - quarterHexHeight), Pair(x + halfWidth, y + hexHeight))
            3 -> Pair(Pair(x + halfWidth, y + hexHeight), Pair(x, y + hexHeight - quarterHexHeight))
            4 -> Pair(Pair(x, y + hexHeight - quarterHexHeight), Pair(x, y + quarterHexHeight))
            5 -> Pair(Pair(x, y + quarterHexHeight), Pair(x + halfWidth, y))
            else -> null
        }
    }

    fun getHexCenter(x: Float, y: Float, cellWidth: Float, cellHeight: Float): Pair<Float, Float> {
        val hexHeight = cellHeight * 4f / 3f
        return Pair(x + cellWidth / 2f, y + hexHeight / 2f)
    }
}
