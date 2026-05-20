package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.ai.AiStrategy
import com.franklinharper.dicewarsport.ai.Move

/**
 * Policy-only neural bot.
 *
 * This first implementation deliberately does no MCTS. It validates the model
 * output length, masks illegal actions, and picks the highest-scored legal
 * action. MCTS can be layered on top of the same [NeuralModel] abstraction.
 */
class NeuralBot(
    private val model: NeuralModel,
) : AiStrategy {
    override val name: String = "Neural"

    override fun chooseMove(game: DicewarsGame): Move? {
        val actor = game.currentPlayer()
        val perspective = actor
        val legalMask = NeuralActionEncoder.legalActionMask(game, actor)
        val input = NeuralInput(
            state = NeuralStateEncoder.encode(
                game = game,
                actorPlayer = actor,
                perspectivePlayer = perspective,
            ),
            legalActionMask = legalMask,
            actorPlayer = actor,
            perspectivePlayer = perspective,
        )
        val prediction = model.predict(input)
        require(prediction.policy.size == NeuralActionEncoder.ACTION_COUNT) {
            "Neural model policy size must be ${NeuralActionEncoder.ACTION_COUNT}, was ${prediction.policy.size}"
        }

        var bestIndex = NeuralActionEncoder.END_TURN_INDEX
        var bestScore = Float.NEGATIVE_INFINITY
        for (index in prediction.policy.indices) {
            if (!legalMask[index]) continue
            val score = prediction.policy[index]
            if (score > bestScore) {
                bestScore = score
                bestIndex = index
            }
        }
        return NeuralActionEncoder.moveForActionIndex(bestIndex)
    }
}
