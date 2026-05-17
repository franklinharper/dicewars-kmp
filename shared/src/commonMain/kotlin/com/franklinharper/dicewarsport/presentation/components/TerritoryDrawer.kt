package com.franklinharper.dicewarsport.presentation.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.franklinharper.dicewarsport.GameColors
import com.franklinharper.dicewarsport.GameMap
import com.franklinharper.dicewarsport.HexGeometry
import com.franklinharper.dicewarsport.UiConstants
import kotlin.math.max

object TerritoryDrawer {
    fun DrawScope.drawTerritoryFills(
        map: GameMap,
        cellWidth: Float,
        cellHeight: Float,
        getCellPosition: (Int) -> Pair<Float, Float>,
    ) {
        for (i in map.cells.indices) {
            val territoryId = map.cells[i]
            if (territoryId > 0 && territoryId <= map.territories.size) {
                val territory = map.territories[territoryId - 1]
                val (cellX, cellY) = getCellPosition(i)
                drawPath(
                    path = buildHexagonPath(cellX, cellY, cellWidth, cellHeight),
                    color = GameColors.getPlayerColor(territory.owner),
                    style = Fill,
                )
            }
        }
    }

    fun DrawScope.drawCellOutlines(
        map: GameMap,
        cellWidth: Float,
        cellHeight: Float,
        getCellPosition: (Int) -> Pair<Float, Float>,
    ) {
        val strokeWidth = max(0.5f, cellWidth / UiConstants.ORIGINAL_CELL_WIDTH)
        for (i in map.cells.indices) {
            val (cellX, cellY) = getCellPosition(i)
            for (dir in 0 until UiConstants.HEX_EDGE_COUNT) {
                drawHexEdge(cellX, cellY, cellWidth, cellHeight, dir, GameColors.DebugCellOutline, strokeWidth)
            }
        }
    }

    fun DrawScope.drawTerritoryBorders(
        map: GameMap,
        cellWidth: Float,
        cellHeight: Float,
        getCellPosition: (Int) -> Pair<Float, Float>,
    ) {
        for (i in map.cells.indices) {
            val territoryId = map.cells[i]
            if (territoryId == 0) continue
            val neighbors = map.cellNeighbors[i].directions
            for (dir in neighbors.indices) {
                val neighborCell = neighbors[dir]
                val neighborTerritoryId = if (neighborCell != -1) map.cells[neighborCell] else -1
                if (neighborTerritoryId != territoryId) {
                    val (cellX, cellY) = getCellPosition(i)
                    drawHexEdge(
                        cellX,
                        cellY,
                        cellWidth,
                        cellHeight,
                        dir,
                        GameColors.TerritoryBorder,
                        max(1f, 3f * (cellWidth / UiConstants.ORIGINAL_CELL_WIDTH)),
                    )
                }
            }
        }
    }

    fun DrawScope.drawHighlightedTerritories(
        map: GameMap,
        highlightedTerritories: Set<Int>,
        attackFromTerritory: Int?,
        cellWidth: Float,
        cellHeight: Float,
        getCellPosition: (Int) -> Pair<Float, Float>,
    ) {
        for (territoryIndex in highlightedTerritories) {
            val territory = map.territories.getOrNull(territoryIndex) ?: continue
            if (territory.size == 0) continue
            val highlightColor = if (territoryIndex == attackFromTerritory) GameColors.HighlightAttack else GameColors.HighlightDefend
            for (cellIndex in map.cells.indices) {
                if (map.cells[cellIndex] == territoryIndex + 1) {
                    val (cellX, cellY) = getCellPosition(cellIndex)
                    val neighbors = map.cellNeighbors[cellIndex].directions
                    for (dir in neighbors.indices) {
                        val neighborCell = neighbors[dir]
                        val neighborTerritoryId = if (neighborCell != -1) map.cells[neighborCell] else -1
                        if (neighborTerritoryId != territoryIndex + 1) {
                            drawHexEdge(
                                cellX,
                                cellY,
                                cellWidth,
                                cellHeight,
                                dir,
                                highlightColor,
                                max(4f, 6f * (cellWidth / UiConstants.ORIGINAL_CELL_WIDTH)),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun buildHexagonPath(x: Float, y: Float, cellWidth: Float, cellHeight: Float): Path {
        val path = Path()
        val overlap = 0.6f
        val vertices = HexGeometry.getHexagonVertices(x, y, cellWidth, cellHeight)
        path.moveTo(vertices[0].first, vertices[0].second - overlap)
        path.lineTo(vertices[1].first + overlap, vertices[1].second)
        path.lineTo(vertices[2].first + overlap, vertices[2].second)
        path.lineTo(vertices[3].first, vertices[3].second + overlap)
        path.lineTo(vertices[4].first - overlap, vertices[4].second)
        path.lineTo(vertices[5].first - overlap, vertices[5].second)
        path.close()
        return path
    }

    private fun DrawScope.drawHexEdge(
        x: Float,
        y: Float,
        cellWidth: Float,
        cellHeight: Float,
        direction: Int,
        color: Color,
        strokeWidth: Float,
    ) {
        val edgePoints = HexGeometry.getHexEdgePoints(x, y, cellWidth, cellHeight, direction) ?: return
        val (start, end) = edgePoints
        drawLine(
            color = color,
            start = Offset(start.first, start.second),
            end = Offset(end.first, end.second),
            strokeWidth = strokeWidth,
        )
    }
}
