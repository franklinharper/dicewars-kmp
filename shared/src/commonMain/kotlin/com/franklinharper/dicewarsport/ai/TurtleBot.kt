package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.isLegalAttack

class TurtleBot : AiStrategy {
    override val name = "Turtle"

    override fun chooseMove(game: DicewarsGame): Move? {
        val currentPlayer = game.currentPlayerId()
        val neighbors = game.neighborIds()
        val areaInfo = List(DicewarsGame.AREA_MAX) { areaId -> analyzeArea(game, areaId, neighbors) }
        var bestMove: Move? = null

        for (defenderId in 1 until DicewarsGame.AREA_MAX) {
            val defender = game.areas[defenderId]
            if (defender.size == 0) continue
            if (defender.owner == currentPlayer) continue

            for (attackerId in 1 until DicewarsGame.AREA_MAX) {
                val attacker = game.areas[attackerId]
                if (attacker.size == 0) continue
                if (attacker.owner != currentPlayer) continue
                if (!neighbors[attackerId].contains(defenderId) && !neighbors[defenderId].contains(attackerId)) continue
                if (!game.isLegalAttack(attackerId, defenderId, currentPlayer)) continue

                if (defender.dice >= attacker.dice && attacker.dice != DicewarsGame.MAX_DICE) continue
                if (areaInfo[defenderId].highestFriendlyNeighborDice > attacker.dice) continue
                if (game.players[currentPlayer].maxConnectedAreaCount > 4 &&
                    areaInfo[attackerId].secondHighestUnfriendlyNeighborDice > 2 &&
                    game.players[currentPlayer].stock == 0
                ) continue

                val previous = bestMove
                if (previous == null) {
                    bestMove = Move(attackerId, defenderId)
                } else {
                    val previousInfo = areaInfo[previous.from]
                    val challengerInfo = areaInfo[attackerId]
                    if (previousInfo.unfriendlyNeighbors == 1) {
                        if (challengerInfo.unfriendlyNeighbors == 1) {
                            if (attacker.dice < game.areas[previous.from].dice) continue
                            if (attacker.dice == game.areas[previous.from].dice &&
                                challengerInfo.numNeighbors < previousInfo.numNeighbors
                            ) continue
                        } else {
                            continue
                        }
                    }
                    bestMove = Move(attackerId, defenderId)
                }
            }
        }

        return bestMove
    }

    private fun analyzeArea(game: DicewarsGame, areaId: Int, neighbors: Array<IntArray>): AreaInfo {
        var friendlyNeighbors = 0
        var unfriendlyNeighbors = 0
        var highestFriendlyNeighborDice = 0
        var highestUnfriendlyNeighborDice = 0
        var secondHighestUnfriendlyNeighborDice = 0
        val area = game.areas[areaId]

        for (neighborId in neighbors[areaId]) {
            val neighbor = game.areas[neighborId]
            if (neighbor.size == 0) continue
            val dice = neighbor.dice
            if (area.owner == neighbor.owner) {
                friendlyNeighbors++
                if (highestFriendlyNeighborDice < dice) highestFriendlyNeighborDice = dice
            } else {
                unfriendlyNeighbors++
                if (highestUnfriendlyNeighborDice < dice) {
                    secondHighestUnfriendlyNeighborDice = highestUnfriendlyNeighborDice
                    highestUnfriendlyNeighborDice = dice
                } else if (secondHighestUnfriendlyNeighborDice < dice) {
                    secondHighestUnfriendlyNeighborDice = dice
                }
            }
        }

        return AreaInfo(
            friendlyNeighbors = friendlyNeighbors,
            unfriendlyNeighbors = unfriendlyNeighbors,
            highestFriendlyNeighborDice = highestFriendlyNeighborDice,
            highestUnfriendlyNeighborDice = highestUnfriendlyNeighborDice,
            secondHighestUnfriendlyNeighborDice = secondHighestUnfriendlyNeighborDice,
            numNeighbors = friendlyNeighbors + unfriendlyNeighbors,
        )
    }
}

private data class AreaInfo(
    val friendlyNeighbors: Int,
    val unfriendlyNeighbors: Int,
    val highestFriendlyNeighborDice: Int,
    val highestUnfriendlyNeighborDice: Int,
    val secondHighestUnfriendlyNeighborDice: Int,
    val numNeighbors: Int,
)
