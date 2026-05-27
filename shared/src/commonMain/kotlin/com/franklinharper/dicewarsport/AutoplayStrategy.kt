package com.franklinharper.dicewarsport

fun chooseAutoplayMove(game: DicewarsGame, player: Int): Pair<Int, Int>? {
    var bestMove: Pair<Int, Int>? = null
    var bestScore: Pair<Int, Int>? = null
    for (from in game.areas.indices) {
        val attacker = game.areas[from]
        if (attacker.size <= 0 || attacker.owner != player || attacker.dice <= 1) continue
        for (to in attacker.adjacentAreas.indices) {
            if (attacker.adjacentAreas[to] == 0) continue
            val defender = game.areas.getOrNull(to) ?: continue
            if (
                defender.size > 0 &&
                defender.owner != player &&
                attacker.dice >= defender.dice &&
                game.isLegalAttack(from, to, player) &&
                game.hasEnoughReserveAfterEitherOutcome(from, to, player)
            ) {
                val score = attacker.dice to -defender.dice
                if (bestScore == null || score.first > bestScore.first || (score.first == bestScore.first && score.second > bestScore.second)) {
                    bestScore = score
                    bestMove = from to to
                }
            }
        }
    }
    return bestMove
}

private fun DicewarsGame.hasEnoughReserveAfterEitherOutcome(from: Int, to: Int, player: Int): Boolean =
    canReplenishAllOwnedTerritories(resolveBattleForSimulation(from, to, win = true), player) &&
        canReplenishAllOwnedTerritories(resolveBattleForSimulation(from, to, win = false), player)

private fun canReplenishAllOwnedTerritories(gameAfterAttack: DicewarsGame, player: Int): Boolean {
    val suppliedGame = gameAfterAttack.startSupply(player)
    val diceNeeded = suppliedGame.areas.sumOf { area ->
        if (area.size > 0 && area.owner == player) DicewarsGame.MAX_DICE - area.dice else 0
    }
    return suppliedGame.players[player].stock >= diceNeeded
}
