package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.ai.Move
import com.franklinharper.dicewarsport.isLegalAttack

/**
 * Fixed action encoding for neural policy outputs.
 *
 * Attack actions occupy one slot per possible (from, to) area pair:
 *
 *   index = from * AREA_MAX + to
 *
 * End turn is the final action:
 *
 *   index = AREA_MAX * AREA_MAX
 */
object NeuralActionEncoder {
    const val AREA_COUNT: Int = DicewarsGame.AREA_MAX
    const val END_TURN_INDEX: Int = AREA_COUNT * AREA_COUNT
    const val ACTION_COUNT: Int = END_TURN_INDEX + 1

    fun actionIndexFor(move: Move?): Int {
        if (move == null) return END_TURN_INDEX
        require(move.from in 0 until AREA_COUNT) { "move.from must be in 0 until $AREA_COUNT: ${move.from}" }
        require(move.to in 0 until AREA_COUNT) { "move.to must be in 0 until $AREA_COUNT: ${move.to}" }
        return move.from * AREA_COUNT + move.to
    }

    fun moveForActionIndex(index: Int): Move? {
        require(index in 0 until ACTION_COUNT) { "action index must be in 0 until $ACTION_COUNT: $index" }
        if (index == END_TURN_INDEX) return null
        return Move(from = index / AREA_COUNT, to = index % AREA_COUNT)
    }

    fun legalActionMask(game: DicewarsGame, actorPlayer: Int = game.currentPlayer()): BooleanArray {
        require(actorPlayer in 0 until game.pmax) { "actorPlayer must be in 0 until game.pmax: $actorPlayer" }
        val mask = BooleanArray(ACTION_COUNT)
        for (from in 0 until AREA_COUNT) {
            for (to in 0 until AREA_COUNT) {
                if (game.isLegalAttack(from, to, actorPlayer)) {
                    mask[actionIndexFor(Move(from, to))] = true
                }
            }
        }
        mask[END_TURN_INDEX] = true
        return mask
    }
}
