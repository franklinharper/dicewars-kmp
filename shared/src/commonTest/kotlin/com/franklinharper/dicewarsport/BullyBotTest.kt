package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.BullyBot
import com.franklinharper.dicewarsport.ai.Move
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BullyBotTest {
    @Test
    fun returnsLegalMoveOnSimpleBoard() {
        val game = aiGame()

        val move = BullyBot(FixedAiRandom(0)).chooseMove(game)

        assertNotNull(move)
        assertTrue(game.isLegalAttack(move.from, move.to), "Bully returned illegal $move")
    }

    @Test
    fun attacksOnlyWeakerAdjacentEnemyAreasWhenFavorableMovesExist() {
        val game = aiGame().copy(
            areas = aiGame().areas.toMutableList().also {
                it[2] = it[2].copy(dice = it[1].dice)
            },
        )

        val move = BullyBot(FixedAiRandom(0)).chooseMove(game)

        assertNotNull(move)
        assertEquals(3, move.to)
        assertTrue(game.areas[move.to].dice < game.areas[move.from].dice)
    }

    @Test
    fun breaksMaxDiceStalemateWhenReinforcementsCanReplaceStack() {
        val base = DicewarsGame(
            pmax = 2,
            turnOrder = listOf(0, 1, 2, 3, 4, 5, 6, 7),
            turnIndex = 0,
            areas = List(DicewarsGame.AREA_MAX) { i ->
                when (i) {
                    1 -> AreaData(size = 5, owner = 0, dice = 8, adjacentAreas = aiAdj(2))
                    2 -> AreaData(size = 5, owner = 1, dice = 8, adjacentAreas = aiAdj(1))
                    else -> AreaData()
                }
            },
        ).setAreaTc(0).setAreaTc(1)
        val players = base.players.toMutableList()
        players[0] = players[0].copy(stock = 7) // stock + connected supply = 8
        val game = base.copy(players = players)

        val move = BullyBot(FixedAiRandom(0)).chooseMove(game)

        assertEquals(Move(1, 2), move)
    }

    @Test
    fun returnsNullWhenNoValidMoves() {
        val game = aiGame().copy(
            areas = aiGame().areas.toMutableList().also {
                it[1] = it[1].copy(dice = 1)
                it[4] = it[4].copy(dice = 1)
            },
        )

        assertNull(BullyBot(FixedAiRandom(0)).chooseMove(game))
    }
}
