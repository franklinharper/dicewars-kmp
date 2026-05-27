package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.RandomSource

class RebelBot(private val random: RandomSource) : AiStrategy {
    override val name = "Rebel"

    override fun chooseMove(game: DicewarsGame): Move? {
        val areaCounts = MutableList(8) { 0 }
        val diceCounts = MutableList(8) { 0 }
        for (areaNumber in 1 until DicewarsGame.AREA_MAX) {
            val area = game.areas[areaNumber]
            if (area.size == 0) continue
            val owner = area.owner
            if (owner !in 0 until 8) continue
            areaCounts[owner]++
            diceCounts[owner] += area.dice
        }

        val rankedPlayers = (0 until 8).sortedByDescending { diceCounts[it] }
        val diceRanks = IntArray(8)
        rankedPlayers.forEachIndexed { rank, player -> diceRanks[player] = rank }

        val totalDice = diceCounts.sum()
        var topPlayer = -1
        for (player in 0 until 8) {
            if (diceCounts[player] > totalDice * 2 / 5) topPlayer = player
        }

        val currentPlayer = game.currentPlayerId()
        val neighbors = game.neighborIds()
        val moves = mutableListOf<Move>()

        for (from in 1 until DicewarsGame.AREA_MAX) {
            val attacker = game.areas[from]
            if (attacker.size == 0) continue
            if (attacker.owner != currentPlayer) continue
            if (attacker.dice <= 1) continue

            for (to in neighbors[from]) {
                val defender = game.areas[to]
                if (defender.size == 0) continue
                if (defender.owner == currentPlayer) continue
                if (topPlayer >= 0 && attacker.owner != topPlayer && defender.owner != topPlayer) continue
                if (defender.dice > attacker.dice) continue
                if (defender.dice == attacker.dice) {
                    val enemy = defender.owner
                    var shouldAttack = false
                    if (diceRanks[currentPlayer] == 0) shouldAttack = true
                    if (diceRanks[enemy] == 0) shouldAttack = true
                    if (random.nextInt(10) > 1) shouldAttack = true
                    if (!shouldAttack) continue
                }
                moves.add(Move(from, to))
            }
        }

        return moves.randomOrNull(random)
    }
}
