package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.MaxBot
import kotlin.test.Test
import kotlin.test.assertTrue

class MaxBotTest {
    @Test
    fun returnsOnlyLegalMoveOrNullOnSimpleBoard() {
        val game = aiGame()

        val move = MaxBot().chooseMove(game)

        if (move != null) assertTrue(game.isLegalAttack(move.from, move.to), "Max returned illegal $move")
    }
}
