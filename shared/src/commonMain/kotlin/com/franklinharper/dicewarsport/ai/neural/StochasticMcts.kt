package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.isLegalAttack
import com.franklinharper.dicewarsport.nextPlayer
import com.franklinharper.dicewarsport.resolveBattleForSimulation
import com.franklinharper.dicewarsport.setAreaTc
import com.franklinharper.dicewarsport.startSupply
import com.franklinharper.dicewarsport.supplyOneDie
import kotlin.math.sqrt

/**
 * AlphaZero-style stochastic Monte Carlo Tree Search for Dicewars.
 *
 * The tree contains two node types:
 * - [DecisionNode]: a player's turn, choosing an action (attack or end turn)
 * - [ChanceNode]: a deterministic intermediate after choosing an attack action,
 *   branching into win/loss outcomes with known probabilities
 *
 * Without a neural model, rollouts use random playout. With a model, leaf
 * evaluation uses the network's value head instead of rolling out.
 */
class StochasticMcts(
    private val random: RandomSource,
    private val model: NeuralModel? = null,
    private val explorationConstant: Double = 1.414,
) {
    /**
     * Runs MCTS from [rootGame] for [budget] simulations and returns visit counts
     * per legal action index. The caller picks the final action (e.g., most visited).
     */
    fun search(rootGame: DicewarsGame, budget: Int): Map<Int, Int> {
        val root = DecisionNode(rootGame, rootGame.currentPlayer())
        repeat(budget) {
            val leaf = select(root)
            val expanded = expand(leaf)
            val value = evaluate(expanded)
            backpropagate(expanded, value)
        }
        return root.visitCounts()
    }

    // -- Selection --

    private fun select(node: MctsNode): MctsNode {
        var current = node
        while (true) {
            when (current) {
                is DecisionNode -> {
                    if (!current.isFullyExpanded) break
                    current = current.bestChild(explorationConstant)
                }
                is ChanceNode -> {
                    current = current.sampleChild(random)
                }
            }
        }
        return current
    }

    // -- Expansion --

    private fun expand(node: MctsNode): MctsNode {
        when (node) {
            is ChanceNode -> return node.sampleChild(random)
            is DecisionNode -> if (node.isFullyExpanded) return node
        }
        return node.expandNext(random)
    }

    // -- Evaluation --

    private fun evaluate(node: MctsNode): Double {
        if (model != null) {
            val game = node.game
            val input = NeuralInput(
                state = NeuralStateEncoder.encode(game, game.currentPlayer(), game.currentPlayer()),
                legalActionMask = NeuralActionEncoder.legalActionMask(game, game.currentPlayer()),
                actorPlayer = game.currentPlayer(),
                perspectivePlayer = game.currentPlayer(),
            )
            return model.predict(input).value.toDouble()
        }
        return randomRollout(node.game)
    }

    private fun randomRollout(game: DicewarsGame): Double {
        var state = game
        var currentPlayer = state.currentPlayer()
        val rootPlayer = currentPlayer

        // Play out one full round (each surviving player gets one turn).
        // After that, evaluate position heuristically.
        for (turn in 0 until state.pmax * 3) {
            if (state.players[rootPlayer].maxConnectedAreaCount == 0) return 0.0
            val winner = state.findWinner()
            if (winner >= 0) return if (winner == rootPlayer) 1.0 else 0.0

            state = playRandomTurn(state)
            currentPlayer = state.currentPlayer()
        }
        // Heuristic: connected area ratio
        val areas = state.players[rootPlayer].maxConnectedAreaCount
        val total = (1 until DicewarsGame.AREA_MAX).count { state.areas[it].size > 0 }
        return if (total == 0) 0.0 else areas.toDouble() / total.toDouble()
    }

    private fun playRandomTurn(game: DicewarsGame): DicewarsGame {
        val player = game.currentPlayer()
        var state = game.startSupply(player)

        // Supply dice
        while (state.players[player].stock > 0) {
            val result = state.supplyOneDie(player, random)
            state = result.first
            if (result.second == null) break
        }

        // Make 0-3 random legal attacks
        repeat(3) {
            val legalAttacks = legalAttacks(state, player)
            if (legalAttacks.isEmpty()) return@repeat
            val attack = legalAttacks[random.nextInt(legalAttacks.size)]
            val winProb = BattleOutcomeProbabilities.winProbability(
                state.areas[attack.first].dice,
                state.areas[attack.second].dice,
            )
            val success = random.nextInt(1000) / 1000.0 < winProb
            state = state.resolveBattleForSimulation(attack.first, attack.second, success)
            state = state.setAreaTc(player)
            if (success) state = state.setAreaTc(state.areas[attack.second].owner)
        }

        return state.nextPlayer()
    }

    private fun legalAttacks(game: DicewarsGame, player: Int): List<Pair<Int, Int>> {
        val attacks = mutableListOf<Pair<Int, Int>>()
        for (from in 1 until DicewarsGame.AREA_MAX) {
            val area = game.areas[from]
            if (area.owner != player || area.dice <= 1) continue
            for (to in 1 until DicewarsGame.AREA_MAX) {
                if (game.isLegalAttack(from, to, player)) {
                    attacks.add(from to to)
                }
            }
        }
        return attacks
    }

    // -- Backpropagation --

    private fun backpropagate(node: MctsNode, value: Double) {
        var current: MctsNode? = node
        while (current != null) {
            current.visitCount++
            current.totalValue += perspectiveValue(current, value)
            current = current.parent
        }
    }

    /** Converts root-perspective value to this node's perspective. */
    private fun perspectiveValue(node: MctsNode, rootValue: Double): Double {
        val rootPlayer = rootPlayer(node)
        return when (node) {
            is ChanceNode -> rootValue
            is DecisionNode -> if (node.player == rootPlayer) rootValue else 1.0 - rootValue
        }
    }

    private fun rootPlayer(node: MctsNode): Int {
        var current = node
        while (current.parent != null) current = current.parent!!
        return (current as DecisionNode).player
    }
}

// -- Tree node types --

sealed class MctsNode {
    abstract val game: DicewarsGame
    abstract val parent: MctsNode?
    abstract var visitCount: Int
    abstract var totalValue: Double
}

class DecisionNode(
    override val game: DicewarsGame,
    val player: Int,
    override val parent: MctsNode? = null,
) : MctsNode() {
    override var visitCount: Int = 0
    override var totalValue: Double = 0.0

    /** Legal action indices yet to be expanded. */
    private val unexpanded: MutableList<Int> = NeuralActionEncoder.legalActionIndices(game, player).toMutableList()

    /** Action index -> ChanceNode or DecisionNode (for end turn). */
    private val children: MutableMap<Int, MctsNode> = mutableMapOf()

    val isFullyExpanded: Boolean get() = unexpanded.isEmpty()

    fun expandNext(random: RandomSource): MctsNode {
        if (unexpanded.isEmpty()) return this
        val actionIndex = unexpanded.removeAt(random.nextInt(unexpanded.size))
        val child = if (actionIndex == NeuralActionEncoder.END_TURN_INDEX) {
            expandEndTurn()
        } else {
            expandAttack(actionIndex)
        }
        children[actionIndex] = child
        return child
    }

    private fun expandEndTurn(): MctsNode {
        var nextGame = game.startSupply(player)
        // Supply dice deterministically for the current player
        // (rollout will handle randomness)
        nextGame = nextGame.nextPlayer()
        return DecisionNode(nextGame, nextGame.currentPlayer(), parent = this)
    }

    private fun expandAttack(actionIndex: Int): MctsNode {
        val move = NeuralActionEncoder.moveForActionIndex(actionIndex)!!
        return ChanceNode(game, player, move.from, move.to, parent = this)
    }

    fun bestChild(c: Double): MctsNode {
        var bestNode: MctsNode? = null
        var bestUcb = Double.NEGATIVE_INFINITY
        for ((_, child) in children) {
            val ucb = ucb1(child, c)
            if (ucb > bestUcb) {
                bestUcb = ucb
                bestNode = child
            }
        }
        return bestNode ?: error("no children")
    }

    private fun ucb1(child: MctsNode, c: Double): Double {
        if (child.visitCount == 0) return Double.POSITIVE_INFINITY
        val exploitation = child.totalValue / child.visitCount
        val exploration = c * sqrt(ln(visitCount.toDouble()) / child.visitCount.toDouble())
        return exploitation + exploration
    }

    fun visitCounts(): Map<Int, Int> = children.mapValues { it.value.visitCount }
}

class ChanceNode(
    override val game: DicewarsGame,
    val player: Int,
    val from: Int,
    val to: Int,
    override val parent: MctsNode? = null,
) : MctsNode() {
    override var visitCount: Int = 0
    override var totalValue: Double = 0.0

    private val outcome: BattleOutcomeProbability =
        BattleOutcomeProbabilities.outcome(game.areas[from].dice, game.areas[to].dice)

    private var winChild: DecisionNode? = null
    private var lossChild: DecisionNode? = null

    val isExpanded: Boolean get() = winChild != null && lossChild != null

    /** Returns a child sampled by outcome probability. */
    fun sampleChild(random: RandomSource): DecisionNode {
        if (winChild == null || lossChild == null) expandOutcomes()
        return if (random.nextInt(1000) / 1000.0 < outcome.win) winChild!! else lossChild!!
    }

    private fun expandOutcomes() {
        val winGame = game.resolveBattleForSimulation(from, to, success = true)
        val loseGame = game.resolveBattleForSimulation(from, to, success = false)
        // Same player continues after both win and loss; turn only ends on explicit end-turn
        winChild = DecisionNode(winGame, player, parent = this)
        lossChild = DecisionNode(loseGame, player, parent = this)
    }

    override fun toString(): String = "ChanceNode(from=$from, to=$to, visits=$visitCount)"
}

// Utility
private fun ln(x: Double): Double = kotlin.math.ln(x)

private fun DicewarsGame.findWinner(): Int {
    var lastOwner = -1
    for (areaId in 1 until DicewarsGame.AREA_MAX) {
        if (areas[areaId].size == 0) continue
        val owner = areas[areaId].owner
        if (lastOwner == -1) { lastOwner = owner; continue }
        if (owner != lastOwner) return -1
    }
    return lastOwner
}
