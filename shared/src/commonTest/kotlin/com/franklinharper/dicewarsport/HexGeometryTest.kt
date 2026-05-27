package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals

class HexGeometryTest {

    @Test
    fun hexGridPositionsOffsetOddRowsByHalfCellWidth() {
        assertEquals(0f to 0f, HexGrid.getCellPosition(0, cellWidth = 30f, cellHeight = 20f))
        assertEquals(30f to 0f, HexGrid.getCellPosition(1, cellWidth = 30f, cellHeight = 20f))
        assertEquals(15f to 20f, HexGrid.getCellPosition(DicewarsGame.MAX_WIDTH, cellWidth = 30f, cellHeight = 20f))
    }

    @Test
    fun hexagonVerticesUsePointyTopShape() {
        val vertices = HexGeometry.getHexagonVertices(x = 10f, y = 20f, cellWidth = 30f, cellHeight = 18f)

        assertEquals(
            listOf(
                25f to 20f,
                40f to 26f,
                40f to 38f,
                25f to 44f,
                10f to 38f,
                10f to 26f,
            ),
            vertices,
        )
    }

    @Test
    fun hexEdgePointsMatchAdjacentVertices() {
        val vertices = HexGeometry.getHexagonVertices(x = 0f, y = 0f, cellWidth = 30f, cellHeight = 18f)

        for (direction in 0 until UiConstants.HEX_EDGE_COUNT) {
            val edge = HexGeometry.getHexEdgePoints(x = 0f, y = 0f, cellWidth = 30f, cellHeight = 18f, direction = direction)
            assertEquals(vertices[direction] to vertices[(direction + 1) % vertices.size], edge)
        }
        assertEquals(null, HexGeometry.getHexEdgePoints(0f, 0f, 30f, 18f, direction = 6))
    }

    @Test
    fun hexCenterUsesRenderedHexHeight() {
        assertEquals(15f to 12f, HexGeometry.getHexCenter(x = 0f, y = 0f, cellWidth = 30f, cellHeight = 18f))
    }
}
