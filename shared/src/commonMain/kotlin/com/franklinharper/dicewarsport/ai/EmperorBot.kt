package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.resolveBattleForSimulation

/**
 * A bot using 3-ply expectiminimax search.
 *
 * Candidate attacks are filtered using CautiousBot's proven safety rules.
 * The evaluation function uses a MaxN heuristic: 2×my_strength − sum(all).
 */
class EmperorBot(private val random: RandomSource) : AiStrategy {
    override val name = "Emperor"

    override fun chooseMove(game: DicewarsGame): Move? {
        val player = game.currentPlayer()
        val neighbors = game.precomputeNeighbors()
        val candidates = filteredMoves(game, player, neighbors)
        if (candidates.isEmpty()) return null

        val currentEval = evaluate(game, player)
        var bestMove: Move? = null
        var bestEv = currentEval

        for (move in candidates) {
            val winProb = winProbability(game.areas[move.from].dice, game.areas[move.to].dice)
            if (winProb < 0.15) continue

            val gameAfterWin = game.resolveBattleForSimulation(move.from, move.to, true)
            val gameAfterLoss = game.resolveBattleForSimulation(move.from, move.to, false)

            val ev = winProb * search(gameAfterWin, player, SEARCH_DEPTH - 1) +
                     (1.0 - winProb) * search(gameAfterLoss, player, SEARCH_DEPTH - 1)

            if (ev > bestEv) {
                bestEv = ev
                bestMove = move
            }
        }

        return bestMove
    }

    private fun search(game: DicewarsGame, player: Int, depth: Int): Double {
        if (depth <= 0) return evaluate(game, player)
        if (game.players[player].maxConnectedAreaCount == 0) return EVAL_ELIMINATED

        val neighbors = game.precomputeNeighbors()
        val candidates = filteredMoves(game, player, neighbors)
        if (candidates.isEmpty()) return evaluate(game, player)

        val currentEval = evaluate(game, player)
        var bestEv = currentEval

        for (move in candidates) {
            val winProb = winProbability(game.areas[move.from].dice, game.areas[move.to].dice)
            if (winProb < 0.15) continue

            val gameAfterWin = game.resolveBattleForSimulation(move.from, move.to, true)
            val gameAfterLoss = game.resolveBattleForSimulation(move.from, move.to, false)

            val ev = winProb * search(gameAfterWin, player, depth - 1) +
                     (1.0 - winProb) * search(gameAfterLoss, player, depth - 1)

            if (ev > bestEv) bestEv = ev
        }

        return bestEv
    }

    private fun filteredMoves(game: DicewarsGame, player: Int, neighbors: Array<IntArray>): List<Move> {
        val stock = game.players[player].stock
        val established = game.players[player].maxConnectedAreaCount > 4
        val areas = game.areas
        val moves = mutableListOf<Move>()

        for (from in 1 until DicewarsGame.AREA_MAX) {
            val attacker = areas[from]
            if (attacker.size == 0 || attacker.owner != player || attacker.dice <= 1) continue

            var fromVulnHi = 0; var fromVulnHi2 = 0
            if (established && stock == 0) {
                for (n in neighbors[from]) {
                    val na = areas[n]
                    if (na.owner != player) {
                        if (na.dice > fromVulnHi) { fromVulnHi2 = fromVulnHi; fromVulnHi = na.dice }
                        else if (na.dice > fromVulnHi2) fromVulnHi2 = na.dice
                    }
                }
            }

            for (to in neighbors[from]) {
                val defender = areas[to]
                if (defender.size == 0 || defender.owner == player) continue
                if (attacker.dice <= 1) continue

                // CautiousBot filter: must have more dice (max-dice exception)
                if (defender.dice >= attacker.dice && attacker.dice != DicewarsGame.MAX_DICE) continue

                // CautiousBot filter: let a stronger friendly neighbor attack instead
                var hiFriendly = 0
                for (n in neighbors[to]) {
                    val na = areas[n]
                    if (na.size > 0 && na.owner == player && na.dice > hiFriendly) hiFriendly = na.dice
                }
                if (hiFriendly > attacker.dice) continue

                // CautiousBot filter: don't attack from vulnerable positions when established
                if (established && stock == 0 && fromVulnHi2 > 2) continue

                moves.add(Move(from, to))
            }
        }
        return moves
    }

    // --- Position evaluation ---

    private fun evaluate(game: DicewarsGame, player: Int): Double {
        val pd = game.players[player]
        if (pd.maxConnectedAreaCount == 0) return EVAL_ELIMINATED

        val myStrength = playerStrength(game, player)
        var totalStrength = myStrength
        for (p in 0 until game.pmax) {
            if (p == player || game.players[p].maxConnectedAreaCount == 0) continue
            totalStrength += playerStrength(game, p)
        }

        return 2.0 * myStrength - totalStrength
    }

    private fun playerStrength(game: DicewarsGame, player: Int): Double {
        val pd = game.players[player]
        var s = pd.maxConnectedAreaCount.toDouble() * W_SUPPLY
        s += pd.diceCount.toDouble() * W_DICE
        s += pd.areaCount.toDouble() * W_TERRITORY
        s += pd.stock.toDouble() * W_STOCK
        if (pd.areaCount > 0) {
            s += (pd.diceCount.toDouble() / pd.areaCount) * W_CONCENTRATION
        }
        return s
    }

    // --- Exact win probability ---

    companion object {
        private const val SEARCH_DEPTH = 3
        private const val EVAL_ELIMINATED = -100_000.0

        private const val W_SUPPLY = 100.0
        private const val W_DICE = 10.0
        private const val W_TERRITORY = 5.0
        private const val W_STOCK = 8.0
        private const val W_CONCENTRATION = 12.0

        private val WIN_PROBABILITY: Array<DoubleArray> = computeWinProbabilities()

        private fun computeWinProbabilities(): Array<DoubleArray> {
            val table = Array(9) { DoubleArray(9) { 0.0 } }
            fun sumDist(n: Int): Map<Int, Double> {
                var dist = mapOf(0 to 1.0)
                repeat(n) {
                    val next = mutableMapOf<Int, Double>()
                    for ((s, p) in dist) for (f in 1..6) next[s + f] = (next[s + f] ?: 0.0) + p / 6.0
                    dist = next
                }
                return dist
            }
            for (a in 1..8) {
                val ad = sumDist(a)
                for (d in 1..8) {
                    val dd = sumDist(d)
                    var wp = 0.0
                    for ((aS, aP) in ad) for ((dS, dP) in dd) if (aS > dS) wp += aP * dP
                    table[a][d] = wp
                }
            }
            return table
        }

        fun winProbability(attackerDice: Int, defenderDice: Int): Double {
            if (attackerDice !in 1..8 || defenderDice !in 1..8) return 0.0
            return WIN_PROBABILITY[attackerDice][defenderDice]
        }
    }
}
