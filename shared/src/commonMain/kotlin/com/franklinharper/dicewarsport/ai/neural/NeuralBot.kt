package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.AiStrategy
import com.franklinharper.dicewarsport.ai.Move

/**
 * Neural bot with configurable search depth.
 *
 * With [NeuralRuntimeStrength.PolicyOnly] it picks the highest-scored legal
 * action directly from the network's policy head. With simulations > 0 it
 * runs [StochasticMcts] using the network for both leaf evaluation and
 * (eventually) prior guidance, selecting the most-visited action.
 */
class NeuralBot(
    private val model: NeuralModel,
    private val config: NeuralBotConfig = NeuralBotConfig.Default,
    private val random: RandomSource,
) : AiStrategy {
    override val name: String = "Neural"

    override fun chooseMove(game: DicewarsGame): Move? {
        val simulations = config.runtimeStrength.simulations
        return if (simulations > 0) {
            chooseByMcts(game, simulations)
        } else {
            chooseByPolicy(game)
        }
    }

    private fun chooseByPolicy(game: DicewarsGame): Move? {
        val actor = game.currentPlayerId()
        val legalMask = NeuralActionEncoder.legalActionMask(game, actor)
        val prediction = predict(game, actor)

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

    private fun chooseByMcts(game: DicewarsGame, simulations: Int): Move? {
        val mcts = StochasticMcts(random = random, model = model)
        val visits = mcts.search(game, budget = simulations)
        val bestIndex = visits.maxByOrNull { it.value }?.key
            ?: NeuralActionEncoder.END_TURN_INDEX
        return NeuralActionEncoder.moveForActionIndex(bestIndex)
    }

    private fun predict(game: DicewarsGame, actor: Int): NeuralPrediction {
        val input = NeuralInput(
            state = NeuralStateEncoder.encode(
                game = game,
                actorPlayer = actor,
                perspectivePlayer = actor,
            ),
            legalActionMask = NeuralActionEncoder.legalActionMask(game, actor),
            actorPlayer = actor,
            perspectivePlayer = actor,
        )
        val prediction = model.predict(input)
        require(prediction.policy.size == NeuralActionEncoder.ACTION_COUNT) {
            "Neural model policy size must be ${NeuralActionEncoder.ACTION_COUNT}, was ${prediction.policy.size}"
        }
        return prediction
    }
}
