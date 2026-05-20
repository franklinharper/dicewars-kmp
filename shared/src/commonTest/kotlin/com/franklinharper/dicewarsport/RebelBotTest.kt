package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.RebelBot
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RebelBotTest {
    @Test
    fun returnsLegalMoveOnSimpleBoard() {
        val game = aiGame()

        val move = RebelBot(FixedAiRandom(0)).chooseMove(game)

        assertNotNull(move)
        assertTrue(game.isLegalAttack(move.from, move.to), "Rebel returned illegal $move")
    }

    @Test
    fun returnsNullWhenNoValidMoves() {
        val game = aiGame().copy(
            areas = aiGame().areas.toMutableList().also {
                it[1] = it[1].copy(dice = 1)
                it[4] = it[4].copy(dice = 1)
            },
        )

        assertNull(RebelBot(FixedAiRandom(0)).chooseMove(game))
    }
}
