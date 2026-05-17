package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.resolveBattleForSimulation

/**
 * A tournament-focused bot that treats every possible attack as a small expected-value problem.
 *
 * The bot does not assume that "more attacks" is always better: it estimates the chance of winning
 * each battle exactly, evaluates the likely board after success and failure, and attacks only when
 * the risk-adjusted position improves enough to justify spending a dice stack.
 */
class FrontierCommanderBot : AiStrategy {
    override val name: String = "Frontier Commander"

    override fun chooseMove(game: DicewarsGame): Move? {
        val player = game.currentPlayer()
        if (game.players.getOrNull(player)?.areaCount == 0) return null

        val neighbors = game.precomputeNeighbors()
        val currentValue = evaluate(game, player, neighbors)
        var bestMove: Move? = null
        var bestScore = Double.NEGATIVE_INFINITY
        var bestProbability = 0.0

        for (from in 1 until DicewarsGame.AREA_MAX) {
            val source = game.areas[from]
            if (source.size == 0 || source.owner != player || source.dice <= 1) continue

            for (to in neighbors[from]) {
                val target = game.areas[to]
                if (target.size == 0 || target.owner == player) continue

                val winProbability = WIN_PROBABILITY[source.dice][target.dice]
                val successGame = game.resolveBattleForSimulation(from, to, success = true)
                val failureGame = game.resolveBattleForSimulation(from, to, success = false)

                val successValue = evaluate(successGame, player, neighbors)
                val failureValue = evaluate(failureGame, player, neighbors)
                val expectedGain = winProbability * successValue + (1.0 - winProbability) * failureValue - currentValue
                val tacticalScore = tacticalAdjustment(
                    game = game,
                    player = player,
                    from = from,
                    to = to,
                    winProbability = winProbability,
                    neighbors = neighbors,
                )
                val score = expectedGain + tacticalScore

                if (score > bestScore || (score == bestScore && tieBreak(from, to, source.dice, target.dice) > tieBreak(bestMove))) {
                    bestScore = score
                    bestMove = Move(from, to)
                    bestProbability = winProbability
                }
            }
        }

        val activeEnemies = (0 until game.pmax).count { it != player && game.players[it].areaCount > 0 }
        val attackThreshold = when {
            game.players[player].areaCount <= 2 -> 8.0
            activeEnemies <= 2 -> if (bestProbability >= 0.80) -2.0 else 8.0
            activeEnemies == 3 -> when {
                bestProbability >= 0.93 -> 50.0
                bestProbability >= 0.84 -> 90.0
                else -> 140.0
            }
            else -> when {
                bestProbability >= 0.93 -> 140.0
                bestProbability >= 0.84 -> 210.0
                else -> 300.0
            }
        }
        return if (bestMove != null && bestScore >= attackThreshold) bestMove else null
    }

    private fun tacticalAdjustment(
        game: DicewarsGame,
        player: Int,
        from: Int,
        to: Int,
        winProbability: Double,
        neighbors: Array<IntArray>,
    ): Double {
        val source = game.areas[from]
        val target = game.areas[to]
        val targetOwner = target.owner
        val targetPlayer = game.players[targetOwner]
        val ownPlayer = game.players[player]

        var score = 0.0

        // Prefer attacks that hurt the current front-runner; avoid wasting good stacks on dying players
        // unless it removes them from the board.
        val strongestEnemyAreas = (0 until game.pmax)
            .filter { it != player }
            .maxOfOrNull { game.players[it].areaCount } ?: 0
        if (targetPlayer.areaCount == strongestEnemyAreas && strongestEnemyAreas >= ownPlayer.areaCount) {
            score += 5.0 * winProbability
        }
        if (targetPlayer.areaCount == 1) {
            score += 14.0 * winProbability
        }

        // High-dice targets are valuable when the odds are good, but expensive to bounce off.
        score += target.dice * (winProbability - 0.45) * 3.0

        // Capturing a tile that borders another friendly tile often merges or thickens a component.
        val friendlyContactsBeyondSource = neighbors[to].count { neighbor ->
            neighbor != from && game.areas[neighbor].size != 0 && game.areas[neighbor].owner == player
        }
        score += friendlyContactsBeyondSource * 7.5 * winProbability

        // Do not empty a stack that is holding off several enemy stacks unless the battle is excellent.
        val pressureOnSource = enemyPressure(game, player, from, neighbors)
        score -= pressureOnSource * (1.0 - winProbability) * 7.0

        // Landing a large stack in a dangerous place is acceptable; landing a small one is not.
        val landingDice = source.dice - 1
        val pressureOnTarget = enemyPressure(game, player, to, neighbors)
        if (landingDice <= pressureOnTarget) {
            score -= (pressureOnTarget - landingDice + 1) * 5.2 * winProbability
        } else {
            score += (landingDice - pressureOnTarget) * 0.6 * winProbability
        }

        // Strong stacks should keep rolling over weak neighbors; marginal attacks should wait for supply.
        score += (source.dice - target.dice) * 1.1
        if (source.dice <= 2 && targetPlayer.areaCount > 1) score -= 22.0
        if (source.dice == 3 && targetPlayer.areaCount > 1) score -= 7.0
        if (source.dice >= 7 && target.dice <= 3) score += 4.0
        if (winProbability < 0.70) score -= 12.0

        return score
    }

    private fun evaluate(game: DicewarsGame, player: Int, neighbors: Array<IntArray>): Double {
        val snapshots = computeSnapshots(game, neighbors)
        val own = snapshots[player]
        if (own.areas == 0) return -1_000_000.0
        val activeEnemies = (0 until game.pmax).filter { it != player && snapshots[it].areas > 0 }
        if (activeEnemies.isEmpty()) return 1_000_000.0

        val largestEnemyAreas = activeEnemies.maxOf { snapshots[it].areas }
        val largestEnemyDice = activeEnemies.maxOf { snapshots[it].dice }
        val largestEnemyComponent = activeEnemies.maxOf { snapshots[it].largestComponent }
        val leaderPressure = activeEnemies.sumOf { enemy ->
            val enemySnapshot = snapshots[enemy]
            if (enemySnapshot.areas >= own.areas) enemySnapshot.areas - own.areas + 1 else 0
        }

        var value = 0.0
        value += own.areas * 78.0
        value += own.largestComponent * 148.0
        value += own.dice * 12.0
        value += own.stock * 3.0
        value += (own.areas - largestEnemyAreas) * 34.0
        value += (own.largestComponent - largestEnemyComponent) * 48.0
        value += (own.dice - largestEnemyDice) * 4.0
        value -= leaderPressure * 9.0

        value += safeGrowthPotential(game, player, neighbors) * 4.5
        value -= exposedSingleDiceAreas(game, player, neighbors) * 25.0
        value -= overextendedFrontier(game, player, neighbors) * 4.0

        // Survival still matters in scored tournaments, but only as a path to winning.
        value += activeEnemies.count { snapshots[it].areas == 1 } * 16.0
        value -= activeEnemies.count { snapshots[it].areas > own.areas && snapshots[it].largestComponent > own.largestComponent } * 22.0

        return value
    }

    private fun computeSnapshots(game: DicewarsGame, neighbors: Array<IntArray>): Array<PlayerSnapshot> {
        val result = Array(game.pmax) { PlayerSnapshot() }
        val visited = BooleanArray(DicewarsGame.AREA_MAX)

        for (player in 0 until game.pmax) {
            var areaCount = 0
            var diceCount = 0
            var largestComponent = 0
            visited.fill(false)

            for (areaId in 1 until DicewarsGame.AREA_MAX) {
                val area = game.areas[areaId]
                if (area.size == 0 || area.owner != player) continue
                areaCount++
                diceCount += area.dice

                if (!visited[areaId]) {
                    var componentSize = 0
                    val stack = IntArray(DicewarsGame.AREA_MAX)
                    var stackSize = 0
                    stack[stackSize++] = areaId
                    visited[areaId] = true

                    while (stackSize > 0) {
                        val current = stack[--stackSize]
                        componentSize++
                        for (neighbor in neighbors[current]) {
                            val neighborArea = game.areas[neighbor]
                            if (neighborArea.size == 0 || neighborArea.owner != player || visited[neighbor]) continue
                            visited[neighbor] = true
                            stack[stackSize++] = neighbor
                        }
                    }
                    if (componentSize > largestComponent) largestComponent = componentSize
                }
            }

            result[player] = PlayerSnapshot(
                areas = areaCount,
                dice = diceCount,
                largestComponent = largestComponent,
                stock = game.players.getOrNull(player)?.stock ?: 0,
            )
        }

        return result
    }

    private fun safeGrowthPotential(game: DicewarsGame, player: Int, neighbors: Array<IntArray>): Int {
        var potential = 0
        for (from in 1 until DicewarsGame.AREA_MAX) {
            val source = game.areas[from]
            if (source.size == 0 || source.owner != player || source.dice <= 1) continue
            for (to in neighbors[from]) {
                val target = game.areas[to]
                if (target.size == 0 || target.owner == player) continue
                if (source.dice > target.dice) potential += source.dice - target.dice
            }
        }
        return potential
    }

    private fun exposedSingleDiceAreas(game: DicewarsGame, player: Int, neighbors: Array<IntArray>): Int {
        var exposed = 0
        for (areaId in 1 until DicewarsGame.AREA_MAX) {
            val area = game.areas[areaId]
            if (area.size == 0 || area.owner != player || area.dice > 1) continue
            if (neighbors[areaId].any { neighbor ->
                    val neighborArea = game.areas[neighbor]
                    neighborArea.size != 0 && neighborArea.owner != player && neighborArea.dice >= 3
                }
            ) {
                exposed++
            }
        }
        return exposed
    }

    private fun overextendedFrontier(game: DicewarsGame, player: Int, neighbors: Array<IntArray>): Int {
        var frontier = 0
        for (areaId in 1 until DicewarsGame.AREA_MAX) {
            val area = game.areas[areaId]
            if (area.size == 0 || area.owner != player) continue
            for (neighbor in neighbors[areaId]) {
                val neighborArea = game.areas[neighbor]
                if (neighborArea.size != 0 && neighborArea.owner != player && neighborArea.dice >= area.dice) {
                    frontier++
                }
            }
        }
        return frontier
    }

    private fun enemyPressure(game: DicewarsGame, player: Int, areaId: Int, neighbors: Array<IntArray>): Int {
        var pressure = 0
        for (neighbor in neighbors[areaId]) {
            val neighborArea = game.areas[neighbor]
            if (neighborArea.size == 0 || neighborArea.owner == player) continue
            if (neighborArea.dice > pressure) pressure = neighborArea.dice
        }
        return pressure
    }

    private fun isEndgame(game: DicewarsGame, player: Int): Boolean =
        (0 until game.pmax).count { it != player && game.players[it].areaCount > 0 } <= 2

    private fun tieBreak(move: Move?): Int = move?.let { tieBreak(it.from, it.to, 0, 0) } ?: Int.MIN_VALUE

    private fun tieBreak(from: Int, to: Int, sourceDice: Int, targetDice: Int): Int =
        sourceDice * 10_000 - targetDice * 1_000 - from * 32 - to

    private data class PlayerSnapshot(
        val areas: Int = 0,
        val dice: Int = 0,
        val largestComponent: Int = 0,
        val stock: Int = 0,
    )

    private companion object {
        val WIN_PROBABILITY: Array<DoubleArray> = buildWinProbabilityTable()

        private fun buildWinProbabilityTable(): Array<DoubleArray> {
            val table = Array(DicewarsGame.MAX_DICE + 1) { DoubleArray(DicewarsGame.MAX_DICE + 1) }
            val distributions = Array(DicewarsGame.MAX_DICE + 1) { diceSumDistribution(it) }
            for (attackerDice in 1..DicewarsGame.MAX_DICE) {
                for (defenderDice in 1..DicewarsGame.MAX_DICE) {
                    val attackerDistribution = distributions[attackerDice]
                    val defenderDistribution = distributions[defenderDice]
                    var wins = 0L
                    var total = 0L
                    for (attackerTotal in attackerDistribution.indices) {
                        val attackerWays = attackerDistribution[attackerTotal]
                        if (attackerWays == 0) continue
                        for (defenderTotal in defenderDistribution.indices) {
                            val defenderWays = defenderDistribution[defenderTotal]
                            if (defenderWays == 0) continue
                            val ways = attackerWays.toLong() * defenderWays.toLong()
                            total += ways
                            if (attackerTotal > defenderTotal) wins += ways
                        }
                    }
                    table[attackerDice][defenderDice] = wins.toDouble() / total.toDouble()
                }
            }
            return table
        }

        private fun diceSumDistribution(diceCount: Int): IntArray {
            if (diceCount <= 0) return intArrayOf(1)
            var distribution = IntArray(1) { 1 }
            repeat(diceCount) {
                val next = IntArray(distribution.size + 6)
                for (sum in distribution.indices) {
                    val ways = distribution[sum]
                    if (ways == 0) continue
                    for (roll in 1..6) {
                        next[sum + roll] += ways
                    }
                }
                distribution = next
            }
            return distribution
        }
    }
}
