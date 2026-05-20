package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.EmperorBot
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EmperorBotTest {
    @Test
    fun returnsLegalMoveOnSimpleBoard() {
        val game = aiGame()

        val move = EmperorBot(FixedAiRandom(0)).chooseMove(game)

        assertNotNull(move)
        assertTrue(game.isLegalAttack(move.from, move.to), "Emperor returned illegal $move")
    }
}
