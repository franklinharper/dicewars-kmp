package com.franklinharper.dicewarsport

import kotlinx.serialization.Serializable

@Serializable
data class AreaData(
    val size: Int = 0,
    val centerPos: Int = 0,
    val owner: Int = 0,
    val dice: Int = 0,
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
    val centerX: Int = 0,
    val centerY: Int = 0,
    val minDistance: Int = 0,
    val lineCells: List<Int> = emptyList(),
    val lineDirections: List<Int> = emptyList(),
    val adjacentAreas: List<Int> = List(DicewarsGame.AREA_MAX) { 0 },
)

@Serializable
data class PlayerData(
    val areaCount: Int = 0,
    val maxConnectedAreaCount: Int = 0,
    val diceCount: Int = 0,
    val diceRank: Int = 0,
    val stock: Int = 0,
)

@Serializable
data class HistoryData(
    val from: Int = 0,
    val to: Int = 0,
    val result: Int = 0,
)
