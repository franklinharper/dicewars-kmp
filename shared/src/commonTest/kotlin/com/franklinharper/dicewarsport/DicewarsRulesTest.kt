package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DicewarsRulesTest {

    @Test
    fun legalAttackRequiresOwnedSourceWithMoreThanOneDieEnemyTargetAndAdjacency() {
        val game = rulesGame()

        assertTrue(game.isLegalAttack(from = 1, to = 2, player = 0))
        assertFalse(game.isLegalAttack(from = 2, to = 1, player = 0), "attacker must own source")

        val noDice = game.withArea(1) { it.copy(dice = 1) }
        assertFalse(noDice.isLegalAttack(from = 1, to = 2, player = 0), "source dice must be > 1")

        val sameOwner = game.withArea(2) { it.copy(owner = 0) }
        assertFalse(sameOwner.isLegalAttack(from = 1, to = 2, player = 0), "target must be enemy")

        val notAdjacent = game
            .withArea(1) { it.copy(adjacentAreas = it.adjacentAreas.toMutableList().also { adj -> adj[2] = 0 }) }
            .withArea(2) { it.copy(adjacentAreas = it.adjacentAreas.toMutableList().also { adj -> adj[1] = 0 }) }
        assertFalse(notAdjacent.isLegalAttack(from = 1, to = 2, player = 0), "target must be adjacent")
    }

    @Test
    fun battleRollContainsDiceTotalsAndSuccess() {
        val roll = rollBattle(attackerDiceCount = 2, defenderDiceCount = 2, random = FixedRandomSource(5, 0, 2, 1))

        assertEquals(listOf(6, 1), roll.attackerDice)
        assertEquals(listOf(3, 2), roll.defenderDice)
        assertEquals(7, roll.attackerTotal)
        assertEquals(5, roll.defenderTotal)
        assertTrue(roll.success)
    }

    @Test
    fun attackerWinsOnlyWhenAttackerTotalIsGreaterThanDefenderTotal() {
        assertTrue(rollBattle(1, 1, FixedRandomSource(1, 0)).success)
        assertFalse(rollBattle(1, 1, FixedRandomSource(0, 0)).success)
        assertFalse(rollBattle(1, 1, FixedRandomSource(0, 1)).success)
    }

    @Test
    fun attackerLossSetsSourceDiceToOneAndRecordsHistory() {
        val game = rulesGame(sourceDice = 4, targetDice = 5)
        val roll = BattleRoll(listOf(1), listOf(6), attackerTotal = 1, defenderTotal = 6, success = false)

        val result = game.resolveBattle(from = 1, to = 2, roll = roll)

        assertEquals(1, result.areas[1].dice)
        assertEquals(5, result.areas[2].dice)
        assertEquals(1, result.areas[2].owner)
        assertEquals(HistoryData(from = 1, to = 2, result = 0), result.history.single())
    }

    @Test
    fun attackerWinTransfersOwnerAndDiceAndUpdatesConnectedAreaCounts() {
        val game = rulesGame(sourceDice = 4, targetDice = 2)
        val roll = BattleRoll(listOf(6), listOf(1), attackerTotal = 6, defenderTotal = 1, success = true)

        val result = game.resolveBattle(from = 1, to = 2, roll = roll)

        assertEquals(1, result.areas[1].dice)
        assertEquals(3, result.areas[2].dice)
        assertEquals(0, result.areas[2].owner)
        assertEquals(2, result.players[0].maxConnectedAreaCount)
        assertEquals(0, result.players[1].maxConnectedAreaCount)
        assertEquals(HistoryData(from = 1, to = 2, result = 1), result.history.single())
    }

    @Test
    fun simulationBattleUsesSameResolutionButDoesNotAppendHistory() {
        val game = rulesGame(sourceDice = 4, targetDice = 2).copy(
            history = listOf(HistoryData(from = 9, to = 10, result = 1)),
        )
        val roll = BattleRoll(listOf(6), listOf(1), attackerTotal = 6, defenderTotal = 1, success = true)

        val real = game.resolveBattle(from = 1, to = 2, roll = roll)
        val simulated = game.resolveBattleForSimulation(from = 1, to = 2, win = true)

        assertEquals(real.areas, simulated.areas)
        assertEquals(real.players, simulated.players)
        assertEquals(game.history, simulated.history)
        assertEquals(game.history + HistoryData(from = 1, to = 2, result = 1), real.history)
    }

    @Test
    fun supplyIsCappedAtStockMaxAndOnlyAffectsOwnedAreasBelowEightDice() {
        val game = rulesGame(sourceDice = 7, targetDice = 7).copy(
            areas = rulesGame(sourceDice = 7, targetDice = 7).areas.toMutableList().also { areas ->
                areas[3] = AreaData(size = 5, owner = 0, dice = 8)
            },
            players = rulesGame().players.toMutableList().also { players ->
                players[0] = players[0].copy(stock = DicewarsGame.STOCK_MAX - 1)
            },
        )

        val supplied = game.startSupply(player = 0)
        assertEquals(DicewarsGame.STOCK_MAX, supplied.players[0].stock)

        val (afterSupply, areaNumber) = supplied.supplyOneDie(player = 0, random = FixedRandomSource(0))

        assertEquals(1, areaNumber)
        assertEquals(8, afterSupply.areas[1].dice)
        assertEquals(8, afterSupply.areas[3].dice)
        assertEquals(7, afterSupply.areas[2].dice, "enemy area is not supplied")
        assertEquals(DicewarsGame.STOCK_MAX - 1, afterSupply.players[0].stock)
        assertEquals(HistoryData(from = 1, to = 0, result = 0), afterSupply.history.single())
    }

    @Test
    fun nextPlayerSkipsEliminatedPlayers() {
        val game = DicewarsGame(
            maxPlayers = 3,
            turnOrder = listOf(0, 1, 2),
            turnIndex = 0,
            players = List(8) { i ->
                when (i) {
                    1 -> Player(maxConnectedAreaCount = 0)
                    2 -> Player(maxConnectedAreaCount = 1)
                    else -> Player(maxConnectedAreaCount = 1)
                }
            },
        )

        val result = game.nextPlayer()
        assertEquals(2, result.currentPlayerId())
    }
}

private fun rulesGame(sourceDice: Int = 3, targetDice: Int = 2): DicewarsGame {
    val adj1 = List(DicewarsGame.AREA_MAX) { if (it == 2) 1 else 0 }
    val adj2 = List(DicewarsGame.AREA_MAX) { if (it == 1) 1 else 0 }
    val game = DicewarsGame(
        maxPlayers = 2,
        turnOrder = listOf(0, 1),
        turnIndex = 0,
        areas = List(DicewarsGame.AREA_MAX) { i ->
            when (i) {
                1 -> AreaData(size = 5, owner = 0, dice = sourceDice, adjacentAreas = adj1)
                2 -> AreaData(size = 5, owner = 1, dice = targetDice, adjacentAreas = adj2)
                else -> AreaData()
            }
        },
    )
    return game.setAreaTc(0).setAreaTc(1)
}

private fun DicewarsGame.withArea(index: Int, transform: (AreaData) -> AreaData): DicewarsGame {
    val newAreas = areas.toMutableList()
    newAreas[index] = transform(newAreas[index])
    return copy(areas = newAreas.toList())
}

private class FixedRandomSource(private vararg val values: Int) : RandomSource {
    private var index = 0

    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        val value = values.getOrElse(index) { 0 } % bound
        index++
        return value
    }
}
