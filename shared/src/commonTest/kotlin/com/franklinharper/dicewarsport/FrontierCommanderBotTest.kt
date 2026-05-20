package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.FrontierCommanderBot
import kotlin.test.Test
import kotlin.test.assertTrue

class FrontierCommanderBotTest {
    @Test
    fun returnsOnlyLegalMoveOrNullOnSimpleBoard() {
        val game = aiGame()

        val move = FrontierCommanderBot().chooseMove(game)

        if (move != null) assertTrue(game.isLegalAttack(move.from, move.to), "Frontier Commander returned illegal $move")
    }
}
