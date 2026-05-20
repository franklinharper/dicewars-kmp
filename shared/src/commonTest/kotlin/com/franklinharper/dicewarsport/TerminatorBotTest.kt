package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.TerminatorBot
import kotlin.test.Test
import kotlin.test.assertTrue

class TerminatorBotTest {
    @Test
    fun returnsOnlyLegalMoveOrNullOnSimpleBoard() {
        val game = aiGame()

        val move = TerminatorBot().chooseMove(game)

        if (move != null) assertTrue(game.isLegalAttack(move.from, move.to), "Terminator returned illegal $move")
    }
}
