package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.Terminator2Bot
import kotlin.test.Test
import kotlin.test.assertTrue

class Terminator2BotTest {
    @Test
    fun returnsOnlyLegalMoveOrNullOnSimpleBoard() {
        val game = aiGame()

        val move = Terminator2Bot().chooseMove(game)

        if (move != null) assertTrue(game.isLegalAttack(move.from, move.to), "Terminator 2 returned illegal $move")
    }
}
