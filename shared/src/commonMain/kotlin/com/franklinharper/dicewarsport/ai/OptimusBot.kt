package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.resolveBattleForSimulation
import com.franklinharper.dicewarsport.startSupply
import kotlin.math.max

/**
 * Tournament bot built from scratch.
 *
 * Uses exact dice-combat probabilities plus one-ply expectimax.  The evaluator
 * favors connected territory growth (future supply), total dice, attacking the
 * leader, and safe frontier shape while penalizing exposed 1-die remnants.
 */
class OptimusBot : AiStrategy {
    override val name: String = "Optimus"

    override fun chooseMove(game: DicewarsGame): Move? {
        val player = game.currentPlayer()
        val neighbors = game.precomputeNeighbors()
        val baseline = evaluate(game, player, neighbors)
        var bestMove: Move? = null
        var bestScore = -0.35 // pass only if every attack is clearly poor

        for (from in 1 until DicewarsGame.AREA_MAX) {
            val attacker = game.areas[from]
            if (attacker.size == 0 || attacker.owner != player || attacker.dice <= 1) continue
            for (to in neighbors[from]) {
                val defender = game.areas[to]
                if (defender.size == 0 || defender.owner == player) continue

                val p = WIN_PROB[attacker.dice][defender.dice]
                val leader = leaderOf(game, player)
                if (p < minProbability(attacker.dice, defender.dice, game.players[player].areaCount)) continue
                if (leader >= 0 && defender.owner != leader && game.players[leader].areaCount > game.players[player].areaCount + 1 && p < 0.72) continue

                val winGame = game.resolveBattleForSimulation(from, to, success = true)
                val loseGame = game.resolveBattleForSimulation(from, to, success = false)
                val winEval = evaluate(winGame, player, neighbors)
                val loseEval = evaluate(loseGame, player, neighbors)
                val reserveSafe = canReplenishAllOwnedTerritories(winGame, player) && canReplenishAllOwnedTerritories(loseGame, player)
                var score = p * winEval + (1.0 - p) * loseEval - baseline

                // Tournament-tuned ordering: reserve-safe big stacks are gold.
                val targetOwner = defender.owner
                score += attacker.dice * 0.95
                score -= defender.dice * 0.42
                if (reserveSafe) score += 3.0 else score -= 1.7
                if (targetOwner == leader) score += 2.1
                if (attacker.dice == DicewarsGame.MAX_DICE) score += 0.75
                if (defender.dice <= 2 && attacker.dice >= 5) score += 0.35
                if (wouldConnectFriendlyGroups(game, neighbors, player, to)) score += 1.35

                if (score > bestScore) {
                    bestScore = score
                    bestMove = Move(from, to)
                }
            }
        }
        return bestMove
    }

    private fun minProbability(attackerDice: Int, defenderDice: Int, myAreas: Int): Double = when {
        attackerDice >= 7 && defenderDice <= 6 -> 0.28
        myAreas <= 3 -> 0.50
        attackerDice > defenderDice -> 0.36
        attackerDice == DicewarsGame.MAX_DICE -> 0.32
        else -> 0.55
    }

    private fun evaluate(game: DicewarsGame, player: Int, neighbors: Array<IntArray>): Double {
        val me = game.players[player]
        var score = 0.0
        score += me.areaCount * 9.0
        score += me.maxConnectedAreaCount * 6.5
        score += me.diceCount * 0.95
        score += me.stock * 0.28

        var strongestEnemyAreas = 0
        var strongestEnemyDice = 0
        for (p in 0 until game.pmax) if (p != player) {
            strongestEnemyAreas = max(strongestEnemyAreas, game.players[p].areaCount)
            strongestEnemyDice = max(strongestEnemyDice, game.players[p].diceCount)
        }
        score -= strongestEnemyAreas * 4.0
        score -= strongestEnemyDice * 0.45

        for (id in 1 until DicewarsGame.AREA_MAX) {
            val area = game.areas[id]
            if (area.size == 0 || area.owner != player) continue
            var enemyNeighbors = 0
            var maxEnemyDice = 0
            var friendlyNeighbors = 0
            for (n in neighbors[id]) {
                val other = game.areas[n]
                if (other.size == 0) continue
                if (other.owner == player) friendlyNeighbors++ else {
                    enemyNeighbors++
                    maxEnemyDice = max(maxEnemyDice, other.dice)
                }
            }
            if (enemyNeighbors == 0) score += 1.4
            else {
                score -= enemyNeighbors * 0.22
                if (area.dice <= 1) score -= 1.25 + maxEnemyDice * 0.18
                if (area.dice >= maxEnemyDice) score += 0.45
            }
            score += friendlyNeighbors * 0.10
        }
        return score
    }

    private fun leaderOf(game: DicewarsGame, player: Int): Int {
        var best = -1
        var bestValue = Int.MIN_VALUE
        for (p in 0 until game.pmax) if (p != player) {
            val value = game.players[p].areaCount * 10 + game.players[p].diceCount + game.players[p].maxConnectedAreaCount * 4
            if (value > bestValue) { bestValue = value; best = p }
        }
        return best
    }

    private fun wouldConnectFriendlyGroups(game: DicewarsGame, neighbors: Array<IntArray>, player: Int, captured: Int): Boolean {
        var friendly = 0
        for (n in neighbors[captured]) if (game.areas[n].owner == player) friendly++
        return friendly >= 2
    }

    private fun canReplenishAllOwnedTerritories(gameAfterAttack: DicewarsGame, player: Int): Boolean {
        val suppliedGame = gameAfterAttack.startSupply(player)
        var diceNeeded = 0
        for (area in suppliedGame.areas) {
            if (area.size > 0 && area.owner == player) diceNeeded += DicewarsGame.MAX_DICE - area.dice
        }
        return suppliedGame.players[player].stock >= diceNeeded
    }

    private companion object {
        val WIN_PROB: Array<DoubleArray> = Array(DicewarsGame.MAX_DICE + 1) { a ->
            DoubleArray(DicewarsGame.MAX_DICE + 1) { d -> probability(a, d) }
        }

        private fun probability(a: Int, d: Int): Double {
            if (a <= 0 || d <= 0) return 0.0
            val ad = distribution(a)
            val dd = distribution(d)
            var wins = 0L
            var total = 0L
            for (at in ad.indices) for (dt in dd.indices) {
                val ways = ad[at] * dd[dt]
                if (ways == 0L) continue
                total += ways
                if (at > dt) wins += ways
            }
            return wins.toDouble() / total.toDouble()
        }

        private fun distribution(dice: Int): LongArray {
            var dist = LongArray(dice * 6 + 1)
            dist[0] = 1
            repeat(dice) {
                val next = LongArray(dice * 6 + 1)
                for (sum in dist.indices) if (dist[sum] != 0L) {
                    for (face in 1..6) next[sum + face] += dist[sum]
                }
                dist = next
            }
            return dist
        }
    }
}
