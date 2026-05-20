package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.ai.Move
import com.franklinharper.dicewarsport.isLegalAttack
import com.franklinharper.dicewarsport.resolveBattleForSimulation

object NeuralBattleChanceExpander {
    fun expand(
        game: DicewarsGame,
        move: Move,
        actorPlayer: Int = game.currentPlayer(),
    ): List<BattleChanceOutcome> {
        require(game.isLegalAttack(move.from, move.to, actorPlayer)) {
            "Cannot expand illegal attack $move for player $actorPlayer"
        }
        val source = game.areas[move.from]
        val target = game.areas[move.to]
        val outcome = BattleOutcomeProbabilities.outcome(source.dice, target.dice)
        return listOf(
            BattleChanceOutcome(
                type = BattleChanceOutcomeType.Win,
                probability = outcome.win,
                game = game.resolveBattleForSimulation(move.from, move.to, success = true),
            ),
            BattleChanceOutcome(
                type = BattleChanceOutcomeType.Loss,
                probability = outcome.loss,
                game = game.resolveBattleForSimulation(move.from, move.to, success = false),
            ),
        )
    }
}

data class BattleChanceOutcome(
    val type: BattleChanceOutcomeType,
    val probability: Double,
    val game: DicewarsGame,
)

enum class BattleChanceOutcomeType {
    Win,
    Loss,
}
