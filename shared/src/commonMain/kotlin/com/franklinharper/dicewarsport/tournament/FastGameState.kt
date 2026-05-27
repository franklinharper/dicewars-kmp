package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.AreaData
import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.Player
import com.franklinharper.dicewarsport.RandomSource

/**
 * Mutable, allocation-light game state for fast tournament execution.
 *
 * Uses primitive arrays instead of immutable data-class lists.
 * Area adjacency is precomputed once at construction time.
 * No history tracking.
 *
 * Can produce a [DicewarsGame] snapshot for bot consumption via [toDicewarsGame].
 */
class FastGameState private constructor(
    val pmax: Int,
    private val turnOrder: IntArray,
    private var turnIndex: Int,
    // Fixed after construction — original area data for snapshot reconstruction
    private val baseCells: List<Int>,
    private val baseCellNeighbors: List<com.franklinharper.dicewarsport.CellNeighbors>,
    private val baseAreas: List<AreaData>,
    // Cached immutable list for snapshots (adjacency never changes)
    private val cachedTurnOrderList: List<Int>,
    // Precomputed compact neighbor lists (adjacency never changes)
    private val neighbors: Array<IntArray>,
    // Mutable game state
    private val areaSize: IntArray,
    private val areaOwner: IntArray,
    private val areaDice: IntArray,
    private val playerAreaCount: IntArray,
    private val playerMaxConnected: IntArray,
    private val playerDiceCount: IntArray,
    private val playerStock: IntArray,
    private val playerDiceRank: IntArray,
) {
    companion object {
        private const val AREA_MAX = DicewarsGame.AREA_MAX
        private const val STOCK_MAX = DicewarsGame.STOCK_MAX
        private const val MAX_DICE = DicewarsGame.MAX_DICE
        private const val PLAYER_MAX = 8

        fun fromGame(game: DicewarsGame): FastGameState {
            val neighbors = game.neighborIds()

            return FastGameState(
                pmax = game.maxPlayers,
                turnOrder = game.turnOrder.toIntArray(),
                turnIndex = game.turnIndex,
                baseCells = game.cells,
                baseCellNeighbors = game.cellNeighbors,
                baseAreas = game.areas,
                cachedTurnOrderList = game.turnOrder,
                neighbors = neighbors,
                areaSize = IntArray(AREA_MAX) { game.areas[it].size },
                areaOwner = IntArray(AREA_MAX) { game.areas[it].owner },
                areaDice = IntArray(AREA_MAX) { game.areas[it].dice },
                playerAreaCount = IntArray(PLAYER_MAX) { game.players[it].areaCount },
                playerMaxConnected = IntArray(PLAYER_MAX) { game.players[it].maxConnectedAreaCount },
                playerDiceCount = IntArray(PLAYER_MAX) { game.players[it].diceCount },
                playerStock = IntArray(PLAYER_MAX) { game.players[it].stock },
                playerDiceRank = IntArray(PLAYER_MAX) { game.players[it].diceRank },
            )
        }
    }

    /**
     * Produce an immutable [DicewarsGame] snapshot for bot consumption.
     *
     * This is the only allocation-heavy operation, called once per bot decision.
     * Reuses [baseAreas] for all immutable fields; only owner and dice are updated.
     */
    fun toDicewarsGame(): DicewarsGame {
        val areas = List(AREA_MAX) { i ->
            val base = baseAreas[i]
            if (areaSize[i] == 0) base
            else base.copy(owner = areaOwner[i], dice = areaDice[i])
        }
        val players = List(PLAYER_MAX) { i ->
            Player(
                areaCount = playerAreaCount[i],
                maxConnectedAreaCount = playerMaxConnected[i],
                diceCount = playerDiceCount[i],
                stock = playerStock[i],
                diceRank = playerDiceRank[i],
            )
        }
        return DicewarsGame(
            maxPlayers = pmax,
            cells = baseCells,
            cellNeighbors = baseCellNeighbors,
            areas = areas,
            players = players,
            turnOrder = cachedTurnOrderList,
            turnIndex = turnIndex,
            history = emptyList(),
        )
    }

    fun currentPlayer(): Int = turnOrder[turnIndex]

    fun isLegalAttack(from: Int, to: Int, player: Int = currentPlayer()): Boolean {
        if (from !in 1 until AREA_MAX) return false
        if (to !in 1 until AREA_MAX) return false
        if (areaSize[from] == 0 || areaSize[to] == 0) return false
        if (areaOwner[from] != player) return false
        if (areaDice[from] <= 1) return false
        if (areaOwner[to] == player) return false
        val nbrs = neighbors[from]
        for (n in nbrs) {
            if (n == to) return true
        }
        return false
    }

    fun resolveBattle(from: Int, to: Int, success: Boolean) {
        val attackerOwner = areaOwner[from]
        val defenderOwner = areaOwner[to]

        if (success) {
            areaOwner[to] = attackerOwner
            areaDice[to] = areaDice[from] - 1
            areaDice[from] = 1
        } else {
            areaDice[from] = 1
        }

        recalculatePlayerStats(attackerOwner)
        if (success && defenderOwner != attackerOwner) {
            recalculatePlayerStats(defenderOwner)
        }
    }

    fun startSupply(player: Int) {
        recalculatePlayerStats(player)
        playerStock[player] = (playerStock[player] + playerMaxConnected[player]).coerceAtMost(STOCK_MAX)
    }

    /**
     * Supply one die to a random eligible territory.
     * Returns the area number supplied, or null if no die could be placed.
     */
    fun supplyOneDie(player: Int, random: RandomSource): Int? {
        // Count candidates first
        var count = 0
        for (i in 1 until AREA_MAX) {
            if (areaSize[i] == 0 || areaOwner[i] != player || areaDice[i] >= MAX_DICE) continue
            count++
        }
        if (count == 0 || playerStock[player] <= 0) return null

        // Pick a random candidate
        var target = random.nextInt(count)
        for (i in 1 until AREA_MAX) {
            if (areaSize[i] == 0 || areaOwner[i] != player || areaDice[i] >= MAX_DICE) continue
            if (target == 0) {
                playerStock[player]--
                areaDice[i]++
                return i
            }
            target--
        }
        return null // unreachable
    }

    fun nextPlayer() {
        for (i in 0 until pmax) {
            turnIndex = (turnIndex + 1) % pmax
            if (playerMaxConnected[turnOrder[turnIndex]] > 0) break
        }
    }

    fun activePlayerSlots(): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until pmax) {
            if (playerMaxConnected[i] > 0) result.add(i)
        }
        return result
    }

    /** Public accessor for dice count on an area. Used for battle rolls. */
    fun getAreaDice(areaId: Int): Int = areaDice[areaId]

    private fun recalculatePlayerStats(player: Int) {
        val parent = IntArray(AREA_MAX) { it }
        val rank = IntArray(AREA_MAX) { 0 }

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
                else -> { parent[rootB] = rootA; rank[rootA]++ }
            }
        }

        for (areaId in 1 until AREA_MAX) {
            if (areaSize[areaId] == 0 || areaOwner[areaId] != player) continue
            for (neighborId in neighbors[areaId]) {
                if (areaSize[neighborId] == 0 || areaOwner[neighborId] != player) continue
                union(areaId, neighborId)
            }
        }

        val connectedCounts = IntArray(AREA_MAX)
        var areaCount = 0
        var diceCount = 0
        for (areaId in 1 until AREA_MAX) {
            if (areaSize[areaId] == 0 || areaOwner[areaId] != player) continue
            connectedCounts[find(areaId)]++
            areaCount++
            diceCount += areaDice[areaId]
        }

        var maxConnected = 0
        for (c in connectedCounts) {
            if (c > maxConnected) maxConnected = c
        }

        playerAreaCount[player] = areaCount
        playerDiceCount[player] = diceCount
        playerMaxConnected[player] = maxConnected
    }
}
