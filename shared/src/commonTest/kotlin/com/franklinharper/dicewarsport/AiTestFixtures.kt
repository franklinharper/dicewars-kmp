package com.franklinharper.dicewarsport

fun aiAdj(vararg ids: Int): List<Int> {
    val list = MutableList(DicewarsGame.AREA_MAX) { 0 }
    for (id in ids) if (id in list.indices) list[id] = 1
    return list
}

fun aiGame(): DicewarsGame {
    val game = DicewarsGame(
        maxPlayers = 2,
        turnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7),
        turnIndex = 0,
        areas = List(DicewarsGame.AREA_MAX) { i ->
            when (i) {
                1 -> AreaData(size = 5, owner = 0, dice = 5, adjacentAreas = aiAdj(2, 3))
                2 -> AreaData(size = 5, owner = 1, dice = 3, adjacentAreas = aiAdj(1, 4))
                3 -> AreaData(size = 5, owner = 1, dice = 4, adjacentAreas = aiAdj(1))
                4 -> AreaData(size = 5, owner = 0, dice = 2, adjacentAreas = aiAdj(2))
                else -> AreaData()
            }
        },
    )
    return game.setAreaTc(0).setAreaTc(1)
}

fun turtleSkipGame(): DicewarsGame {
    val game = DicewarsGame(
        maxPlayers = 2,
        turnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7),
        turnIndex = 0,
        areas = List(DicewarsGame.AREA_MAX) { i ->
            when (i) {
                1 -> AreaData(size = 5, owner = 0, dice = 5, adjacentAreas = aiAdj(2, 6, 7))
                2 -> AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = aiAdj(1, 3))
                3 -> AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = aiAdj(2, 4))
                4 -> AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = aiAdj(3, 5))
                5 -> AreaData(size = 5, owner = 0, dice = 1, adjacentAreas = aiAdj(4))
                6 -> AreaData(size = 5, owner = 1, dice = 3, adjacentAreas = aiAdj(1))
                7 -> AreaData(size = 5, owner = 1, dice = 4, adjacentAreas = aiAdj(1))
                else -> AreaData()
            }
        },
    )
    return game.setAreaTc(0).setAreaTc(1)
}

class FixedAiRandom(private val value: Int) : RandomSource {
    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        return value % bound
    }
}
