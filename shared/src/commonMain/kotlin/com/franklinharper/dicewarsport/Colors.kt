package com.franklinharper.dicewarsport

import androidx.compose.ui.graphics.Color

object GameColors {
    val Player0 = Color(0xFFB37FFE)
    val Player1 = Color(0xFF4CAF50)
    val Player2 = Color(0xFFF44336)
    val Player3 = Color(0xFFFFEB3B)
    val Player4 = Color(0xFFFF9800)
    val Player5 = Color(0xFF2196F3)
    val Player6 = Color(0xFF00BCD4)
    val Player7 = Color(0xFFE91E63)

    val TerritoryBorder = Color(0xFF222244)
    val TerritoryText = Color.Black
    val DebugCellOutline = Color(0xFFCCCCCC)
    val HighlightAttack = Color(0xFFFF0000)
    val HighlightDefend = Color(0xFFFFFF00)
    val UnknownPlayer = Color(0xFF808080)

    fun getPlayerColor(playerId: Int): Color = when (playerId) {
        0 -> Player0
        1 -> Player1
        2 -> Player2
        3 -> Player3
        4 -> Player4
        5 -> Player5
        6 -> Player6
        7 -> Player7
        else -> UnknownPlayer
    }
}
