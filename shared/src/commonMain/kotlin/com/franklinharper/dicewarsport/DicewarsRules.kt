package com.franklinharper.dicewarsport

data class BattleRoll(
    val attackerDice: List<Int>,
    val defenderDice: List<Int>,
    val attackerTotal: Int,
    val defenderTotal: Int,
    val success: Boolean,
)

fun DicewarsGame.isLegalAttack(from: Int, to: Int, player: Int = currentPlayer()): Boolean {
    if (from !in 1 until DicewarsGame.AREA_MAX) return false
    if (to !in 1 until DicewarsGame.AREA_MAX) return false

    val source = areas[from]
    val target = areas[to]
    if (source.size == 0 || target.size == 0) return false
    if (source.owner != player) return false
    if (source.dice <= 1) return false
    if (target.owner == player) return false
    if (source.adjacentAreas[to] == 0 && target.adjacentAreas[from] == 0) return false

    return true
}

fun rollBattle(
    attackerDiceCount: Int,
    defenderDiceCount: Int,
    random: RandomSource,
): BattleRoll {
    val attackerDice = List(attackerDiceCount) { random.nextInt(6) + 1 }
    val defenderDice = List(defenderDiceCount) { random.nextInt(6) + 1 }
    val attackerTotal = attackerDice.sum()
    val defenderTotal = defenderDice.sum()
    return BattleRoll(
        attackerDice = attackerDice,
        defenderDice = defenderDice,
        attackerTotal = attackerTotal,
        defenderTotal = defenderTotal,
        success = attackerTotal > defenderTotal,
    )
}

fun DicewarsGame.resolveBattle(from: Int, to: Int, roll: BattleRoll): DicewarsGame =
    resolveBattleOutcome(
        from = from,
        to = to,
        success = roll.success,
        historyEntry = HistoryData(from = from, to = to, result = if (roll.success) 1 else 0),
    )

/**
 * Resolves a battle outcome for AI/search simulation without appending history.
 *
 * This uses the same territory and player-stat update path as [resolveBattle],
 * but intentionally skips the history append because search trees may evaluate
 * thousands of hypothetical outcomes that should not be replay-visible actions.
 */
fun DicewarsGame.resolveBattleForSimulation(from: Int, to: Int, success: Boolean): DicewarsGame =
    resolveBattleOutcome(from = from, to = to, success = success, historyEntry = null)

private fun DicewarsGame.resolveBattleOutcome(
    from: Int,
    to: Int,
    success: Boolean,
    historyEntry: HistoryData?,
): DicewarsGame {
    val attackerOwner = areas[from].owner
    val defenderOwner = areas[to].owner
    val newAreas = areas.toMutableList()

    if (success) {
        newAreas[to] = newAreas[to].copy(dice = newAreas[from].dice - 1, owner = attackerOwner)
        newAreas[from] = newAreas[from].copy(dice = 1)
    } else {
        newAreas[from] = newAreas[from].copy(dice = 1)
    }

    var game = copy(
        areas = newAreas.toList(),
        history = if (historyEntry == null) history else history + historyEntry,
    )
    game = game.setAreaTc(attackerOwner)
    if (success) game = game.setAreaTc(defenderOwner)
    return game
}

fun DicewarsGame.startSupply(player: Int = currentPlayer()): DicewarsGame {
    var game = setAreaTc(player)
    val playerData = game.players[player]
    val newStock = (playerData.stock + playerData.maxConnectedAreaCount)
        .coerceAtMost(DicewarsGame.STOCK_MAX)
    val newPlayers = game.players.toMutableList()
    newPlayers[player] = playerData.copy(stock = newStock)
    return game.copy(players = newPlayers.toList())
}

fun DicewarsGame.supplyOneDie(player: Int, random: RandomSource): Pair<DicewarsGame, Int?> {
    val candidates = mutableListOf<Int>()
    for (areaNumber in 1 until DicewarsGame.AREA_MAX) {
        val area = areas[areaNumber]
        if (area.size == 0) continue
        if (area.owner != player) continue
        if (area.dice >= DicewarsGame.MAX_DICE) continue
        candidates.add(areaNumber)
    }

    if (candidates.isEmpty() || players[player].stock <= 0) return this to null

    val areaNumber = candidates[random.nextInt(candidates.size)]
    val newPlayers = players.toMutableList()
    newPlayers[player] = newPlayers[player].copy(stock = newPlayers[player].stock - 1)
    val newAreas = areas.toMutableList()
    newAreas[areaNumber] = newAreas[areaNumber].copy(dice = newAreas[areaNumber].dice + 1)
    return copy(
        areas = newAreas.toList(),
        players = newPlayers.toList(),
        history = history + HistoryData(from = areaNumber, to = 0, result = 0),
    ) to areaNumber
}

fun DicewarsGame.nextPlayer(): DicewarsGame {
    var newTurnIndex = turnIndex
    for (i in 0 until pmax) {
        newTurnIndex++
        if (newTurnIndex >= pmax) newTurnIndex = 0
        val player = turnOrder[newTurnIndex]
        if (players[player].maxConnectedAreaCount > 0) break
    }
    return copy(turnIndex = newTurnIndex)
}

fun DicewarsGame.setAreaTc(player: Int): DicewarsGame {
    val parent = IntArray(DicewarsGame.AREA_MAX) { it }
    val rank = IntArray(DicewarsGame.AREA_MAX) { 0 }

    fun find(x: Int): Int {
        var root = x
        while (parent[root] != root) root = parent[root]

        var current = x
        while (parent[current] != root) {
            val next = parent[current]
            parent[current] = root
            current = next
        }
        return root
    }

    fun union(a: Int, b: Int) {
        val rootA = find(a)
        val rootB = find(b)
        if (rootA == rootB) return

        when {
            rank[rootA] < rank[rootB] -> parent[rootA] = rootB
            rank[rootA] > rank[rootB] -> parent[rootB] = rootA
            else -> {
                parent[rootB] = rootA
                rank[rootA]++
            }
        }
    }

    val neighbors = precomputeNeighbors()
    for (areaId in 1 until DicewarsGame.AREA_MAX) {
        val area = areas[areaId]
        if (area.size == 0 || area.owner != player) continue

        for (neighborId in neighbors[areaId]) {
            val neighbor = areas[neighborId]
            if (neighbor.size == 0 || neighbor.owner != player) continue
            union(areaId, neighborId)
        }
    }

    val connectedAreaCounts = IntArray(DicewarsGame.AREA_MAX)
    var areaCount = 0
    var diceCount = 0
    for (areaId in 1 until DicewarsGame.AREA_MAX) {
        val area = areas[areaId]
        if (area.size == 0 || area.owner != player) continue
        connectedAreaCounts[find(areaId)]++
        areaCount++
        diceCount += area.dice
    }

    var maxConnected = 0
    for (count in connectedAreaCounts) {
        if (count > maxConnected) maxConnected = count
    }

    val newPlayers = players.toMutableList()
    newPlayers[player] = newPlayers[player].copy(
        areaCount = areaCount,
        diceCount = diceCount,
        maxConnectedAreaCount = maxConnected,
    )
    return copy(players = newPlayers.toList())
}
