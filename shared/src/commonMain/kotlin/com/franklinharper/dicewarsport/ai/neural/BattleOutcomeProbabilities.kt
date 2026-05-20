package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.DicewarsGame

object BattleOutcomeProbabilities {
    private val outcomes: Array<Array<BattleOutcomeProbability?>> = Array(DicewarsGame.MAX_DICE + 1) { attacker ->
        Array(DicewarsGame.MAX_DICE + 1) { defender ->
            if (attacker == 0 || defender == 0) null else compute(attacker, defender)
        }
    }

    fun outcome(attackerDice: Int, defenderDice: Int): BattleOutcomeProbability {
        validateDice(attackerDice, "attackerDice")
        validateDice(defenderDice, "defenderDice")
        return outcomes[attackerDice][defenderDice]!!
    }

    fun winProbability(attackerDice: Int, defenderDice: Int): Double =
        outcome(attackerDice, defenderDice).win

    private fun validateDice(value: Int, name: String) {
        require(value in 1..DicewarsGame.MAX_DICE) { "$name must be in 1..${DicewarsGame.MAX_DICE}: $value" }
    }

    private fun compute(attackerDice: Int, defenderDice: Int): BattleOutcomeProbability {
        val attackerDistribution = diceSumDistribution(attackerDice)
        val defenderDistribution = diceSumDistribution(defenderDice)
        var winningWays = 0L
        var totalWays = 0L

        for (attackerTotal in attackerDistribution.indices) {
            val attackerWays = attackerDistribution[attackerTotal]
            if (attackerWays == 0L) continue
            for (defenderTotal in defenderDistribution.indices) {
                val defenderWays = defenderDistribution[defenderTotal]
                if (defenderWays == 0L) continue
                val ways = attackerWays * defenderWays
                totalWays += ways
                if (attackerTotal > defenderTotal) winningWays += ways
            }
        }

        val win = winningWays.toDouble() / totalWays.toDouble()
        return BattleOutcomeProbability(win = win, loss = 1.0 - win)
    }

    private fun diceSumDistribution(dice: Int): LongArray {
        var distribution = LongArray(dice * 6 + 1)
        distribution[0] = 1L
        repeat(dice) {
            val next = LongArray(dice * 6 + 1)
            for (sum in distribution.indices) {
                val ways = distribution[sum]
                if (ways == 0L) continue
                for (face in 1..6) {
                    next[sum + face] += ways
                }
            }
            distribution = next
        }
        return distribution
    }
}

data class BattleOutcomeProbability(
    val win: Double,
    val loss: Double,
)
