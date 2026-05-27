package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.RandomSource

class BullyBot(private val random: RandomSource) : AiStrategy {
    override val name = "Bully"

    override fun chooseMove(game: DicewarsGame): Move? {
        val player = game.currentPlayerId()
        val neighbors = game.neighborIds()
        val moves = mutableListOf<Move>()
        val stalemateBreakers = mutableListOf<Move>()
        val canSpendMaxStack = allOwnedTerritoriesAreMaxed(game, player) &&
            game.players[player].stock + game.players[player].maxConnectedAreaCount >= DicewarsGame.MAX_DICE

        for (from in 1 until DicewarsGame.AREA_MAX) {
            val attacker = game.areas[from]
            if (attacker.size == 0) continue
            if (attacker.owner != player) continue
            if (attacker.dice <= 1) continue

            for (to in neighbors[from]) {
                val defender = game.areas[to]
                if (defender.size == 0) continue
                if (defender.owner == player) continue
                if (defender.dice < attacker.dice) {
                    moves.add(Move(from, to))
                } else if (canSpendMaxStack) {
                    stalemateBreakers.add(Move(from, to))
                }
            }
        }

        return moves.randomOrNull(random) ?: stalemateBreakers.randomOrNull(random)
    }

    private fun allOwnedTerritoriesAreMaxed(game: DicewarsGame, player: Int): Boolean {
        var owned = 0
        for (area in game.areas) {
            if (area.size == 0 || area.owner != player) continue
            owned++
            if (area.dice < DicewarsGame.MAX_DICE) return false
        }
        return owned > 0
    }
}
