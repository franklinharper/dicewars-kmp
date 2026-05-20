package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.TurtleBot
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TurtleBotTest {
    @Test
    fun returnsLegalMoveOnSimpleBoard() {
        val game = aiGame()

        val move = TurtleBot().chooseMove(game)

        assertNotNull(move)
        assertTrue(game.isLegalAttack(move.from, move.to), "Turtle returned illegal $move")
    }

    @Test
    fun skipsAttackFromVulnerableAreaWhenEstablishedAndNoStock() {
        val game = turtleSkipGame()

        assertNull(TurtleBot().chooseMove(game))
    }

    @Test
    fun returnsNullWhenNoValidMoves() {
        val game = aiGame().copy(
            areas = aiGame().areas.toMutableList().also {
                it[1] = it[1].copy(dice = 1)
                it[4] = it[4].copy(dice = 1)
            },
        )

        assertNull(TurtleBot().chooseMove(game))
    }
}
