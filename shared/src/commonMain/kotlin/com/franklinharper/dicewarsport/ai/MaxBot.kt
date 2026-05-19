package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.chooseAutoplayMove

class MaxBot : AiStrategy {
    override val name: String = "Max"

    override fun chooseMove(game: DicewarsGame): Move? {
        val move = chooseAutoplayMove(game, game.currentPlayer()) ?: return null
        return Move(move.first, move.second)
    }
}
