package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.resolveBattleForSimulation
import com.franklinharper.dicewarsport.startSupply
import kotlin.math.max
import kotlin.math.min

/**
 * A tournament bot built around conservative expansion.
 *
 * Core ideas:
 * - exact dynamic-programming battle odds;
 * - graph search over owned components to value supply-producing connectivity;
 * - reserve feasibility after both battle outcomes;
 * - one-ply probabilistic board evaluation for every legal attack;
 * - leader pressure only when the attack is tactically sound.
 */
class TerminatorBot : AiStrategy {
    override val name: String = "Terminator"

    override fun chooseMove(game: DicewarsGame): Move? {
        val player = game.currentPlayer()
        val neighbors = game.precomputeNeighbors()
        val baseline = evaluate(game, player, neighbors)
        val leader = leaderOf(game, player)
        val myDice = game.players[player].diceCount

        var bestMove: Move? = null
        var bestScore = 0.0

        for (from in 1 until DicewarsGame.AREA_MAX) {
            val attacker = game.areas[from]
            if (attacker.size == 0 || attacker.owner != player || attacker.dice <= 1) continue
            for (to in neighbors[from]) {
                val defender = game.areas[to]
                if (defender.size == 0 || defender.owner == player) continue

                val p = WIN_PROB[attacker.dice][defender.dice]
                val reserveSafe = canFullyReplenishAfterEitherOutcome(game, player, from, to)
                if (!candidateAllowed(attacker.dice, defender.dice, reserveSafe)) continue

                val winGame = game.resolveBattleForSimulation(from, to, success = true)
                val loseGame = game.resolveBattleForSimulation(from, to, success = false)
                val winEval = evaluate(winGame, player, neighbors)
                val loseEval = evaluate(loseGame, player, neighbors)
                val expectedGain = p * winEval + (1.0 - p) * loseEval - baseline

                val connectGain = connectionGain(game, neighbors, player, to)
                val targetStats = game.players[defender.owner]
                var score = 0.0

                // Expected-utility ordering with reserve as a hard risk control.
                score += FULL_RESERVE_BONUS
                score += attacker.dice * 4.0
                score -= defender.dice * 1.1
                score += p * 0.45
                score += expectedGain * 0.08
                score += connectGain * 1.15
                if (defender.owner == leader) score += 0.65 + targetStats.areaCount * 0.03
                if (targetStats.areaCount <= 2) score += 0.25
                if (attacker.dice == DicewarsGame.MAX_DICE) score += 1.2
                if (myDice < strongestEnemyDice(game, player) && defender.owner != leader && p < 0.94) score -= 2.1

                if (score > bestScore) {
                    bestScore = score
                    bestMove = Move(from, to)
                }
            }
        }
        return bestMove
    }

    private fun candidateAllowed(
        attackerDice: Int,
        defenderDice: Int,
        reserveSafe: Boolean,
    ): Boolean {
        if (!reserveSafe) return false
        return attackerDice >= defenderDice || (attackerDice == DicewarsGame.MAX_DICE && defenderDice <= 6)
    }

    private fun evaluate(game: DicewarsGame, player: Int, neighbors: Array<IntArray>): Double {
        val me = game.players[player]
        if (me.areaCount <= 0) return -10_000.0

        var strongestEnemyAreas = 0
        var strongestEnemyConnected = 0
        var strongestEnemyDice = 0
        var livingEnemies = 0
        for (p in 0 until game.pmax) if (p != player) {
            val enemy = game.players[p]
            if (enemy.areaCount > 0) livingEnemies++
            strongestEnemyAreas = max(strongestEnemyAreas, enemy.areaCount)
            strongestEnemyConnected = max(strongestEnemyConnected, enemy.maxConnectedAreaCount)
            strongestEnemyDice = max(strongestEnemyDice, enemy.diceCount)
        }

        var score = 0.0
        score += me.areaCount * 7.25
        score += me.maxConnectedAreaCount * 7.35
        score += me.diceCount * 1.42
        score += min(me.stock, 24) * 0.48
        score -= strongestEnemyAreas * 2.85
        score -= strongestEnemyConnected * 2.05
        score -= strongestEnemyDice * 0.36
        score -= livingEnemies * 0.08

        for (id in 1 until DicewarsGame.AREA_MAX) {
            val area = game.areas[id]
            if (area.size == 0 || area.owner != player) continue
            var enemyEdges = 0
            var friendlyEdges = 0
            var maxEnemy = 0
            var weakEnemyNeighbors = 0
            for (n in neighbors[id]) {
                val other = game.areas[n]
                if (other.size == 0) continue
                if (other.owner == player) friendlyEdges++ else {
                    enemyEdges++
                    maxEnemy = max(maxEnemy, other.dice)
                    if (other.dice <= area.dice) weakEnemyNeighbors++
                }
            }
            if (enemyEdges == 0) {
                score += 1.25
            } else {
                score -= enemyEdges * 0.18
                score += weakEnemyNeighbors * 0.18
                if (area.dice == 1) score -= 1.15 + maxEnemy * 0.14
                if (area.dice >= maxEnemy) score += 0.34
                if (area.dice >= 6 && maxEnemy <= 3) score += 0.38
            }
            score += friendlyEdges * 0.09
        }
        return score
    }

    private fun canFullyReplenishAfterEitherOutcome(game: DicewarsGame, player: Int, from: Int, to: Int): Boolean {
        val win = game.resolveBattleForSimulation(from, to, success = true)
        val lose = game.resolveBattleForSimulation(from, to, success = false)
        return replenishmentCoverage(win, player) >= 1.0 && replenishmentCoverage(lose, player) >= 1.0
    }

    private fun replenishmentCoverage(gameAfterAttack: DicewarsGame, player: Int): Double {
        val suppliedGame = gameAfterAttack.startSupply(player)
        var diceNeeded = 0
        for (area in suppliedGame.areas) {
            if (area.size > 0 && area.owner == player) diceNeeded += DicewarsGame.MAX_DICE - area.dice
        }
        if (diceNeeded <= 0) return 1.0
        return suppliedGame.players[player].stock.toDouble() / diceNeeded.toDouble()
    }

    private fun connectionGain(game: DicewarsGame, neighbors: Array<IntArray>, player: Int, captured: Int): Int {
        var friendly = 0
        for (n in neighbors[captured]) if (game.areas[n].size > 0 && game.areas[n].owner == player) friendly++
        return max(0, friendly - 1)
    }

    private fun strongestEnemyDice(game: DicewarsGame, player: Int): Int {
        var best = 0
        for (p in 0 until game.pmax) if (p != player) best = max(best, game.players[p].diceCount)
        return best
    }

    private fun leaderOf(game: DicewarsGame, player: Int): Int {
        var best = -1
        var bestValue = Int.MIN_VALUE
        for (p in 0 until game.pmax) if (p != player) {
            val pd = game.players[p]
            if (pd.areaCount <= 0) continue
            val value = pd.areaCount * 12 + pd.maxConnectedAreaCount * 7 + pd.diceCount
            if (value > bestValue) {
                bestValue = value
                best = p
            }
        }
        return best
    }

    private companion object {
        const val FULL_RESERVE_BONUS = 4.2

        val WIN_PROB: Array<DoubleArray> = Array(DicewarsGame.MAX_DICE + 1) { a ->
            DoubleArray(DicewarsGame.MAX_DICE + 1) { d -> probability(a, d) }
        }

        private fun probability(attackerDice: Int, defenderDice: Int): Double {
            if (attackerDice <= 0 || defenderDice <= 0) return 0.0
            val a = distribution(attackerDice)
            val d = distribution(defenderDice)
            var wins = 0L
            var total = 0L
            for (at in a.indices) {
                val aw = a[at]
                if (aw == 0L) continue
                for (dt in d.indices) {
                    val dw = d[dt]
                    if (dw == 0L) continue
                    val ways = aw * dw
                    total += ways
                    if (at > dt) wins += ways
                }
            }
            return wins.toDouble() / total.toDouble()
        }

        private fun distribution(dice: Int): LongArray {
            var dist = LongArray(dice * 6 + 1)
            dist[0] = 1L
            repeat(dice) {
                val next = LongArray(dice * 6 + 1)
                for (sum in dist.indices) {
                    val ways = dist[sum]
                    if (ways == 0L) continue
                    for (face in 1..6) next[sum + face] += ways
                }
                dist = next
            }
            return dist
        }
    }
}
