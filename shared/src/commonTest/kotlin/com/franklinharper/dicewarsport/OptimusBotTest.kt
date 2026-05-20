package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.OptimusBot
import kotlin.test.Test
import kotlin.test.assertTrue

class OptimusBotTest {
    @Test
    fun returnsOnlyLegalMoveOrNullOnSimpleBoard() {
        val game = aiGame()

        val move = OptimusBot().chooseMove(game)

        if (move != null) assertTrue(game.isLegalAttack(move.from, move.to), "Optimus returned illegal $move")
    }
}
