package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.AreaData
import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.ai.AiStrategy
import com.franklinharper.dicewarsport.ai.Move
import com.franklinharper.dicewarsport.setAreaTc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BotGameRunnerTest {
    @Test
    fun completedRoundRecordsWinnerAndEliminations() {
        val runner = BotGameRunner(gameFactory = { _, _ -> twoPlayerGame(attackerDice = 8, defenderDice = 1) })
        val result = runner.runRound(
            RoundConfig(
                roundNumber = 1,
                participants = listOf(
                    tournamentParticipant("bot0", AttackAi(Move(1, 2))),
                    tournamentParticipant("bot1", NoMoveAiForRound),
                ),
                roundSeed = 5,
                maxActionsPerRound = 10,
            ),
        )

        assertTrue(result.completed)
        assertEquals("bot0", result.winnerParticipantId)
        assertEquals(listOf("bot1"), result.eliminationOrder)
        assertEquals(4, result.scores["bot0"])
        assertEquals(0, result.scores["bot1"])
        assertEquals(1, result.actionsTaken)
    }

    @Test
    fun maxActionsExceededProducesFailedRoundWithNoPoints() {
        val runner = BotGameRunner(gameFactory = { _, _ -> twoPlayerGame(attackerDice = 2, defenderDice = 2) })
        val result = runner.runRound(
            RoundConfig(
                roundNumber = 7,
                participants = listOf(
                    tournamentParticipant("bot0", NoMoveAiForRound),
                    tournamentParticipant("bot1", NoMoveAiForRound),
                ),
                roundSeed = 0,
                maxActionsPerRound = 3,
            ),
        )

        assertFalse(result.completed)
        assertEquals(null, result.winnerParticipantId)
        assertEquals(mapOf("bot0" to 0, "bot1" to 0), result.scores)
        assertEquals(3, result.actionsTaken)
        assertEquals("maxActionsPerRound=3 exceeded", result.failureReason)
    }

    @Test
    fun illegalAiMoveDoesNotCrashRunnerAndIsTreatedAsEndTurn() {
        val runner = BotGameRunner(gameFactory = { _, _ -> twoPlayerGame(attackerDice = 2, defenderDice = 2) })
        val result = runner.runRound(
            RoundConfig(
                roundNumber = 2,
                participants = listOf(
                    tournamentParticipant("bot0", AttackAi(Move(1, 99))),
                    tournamentParticipant("bot1", NoMoveAiForRound),
                ),
                roundSeed = 0,
                maxActionsPerRound = 2,
            ),
        )

        assertFalse(result.completed)
        assertEquals(2, result.actionsTaken)
    }
}

private fun tournamentParticipant(id: String, ai: AiStrategy): TournamentParticipant = TournamentParticipant(
    id = id,
    displayName = id,
    aiFactory = { ai },
)

private class AttackAi(private val move: Move) : AiStrategy {
    override fun chooseMove(game: DicewarsGame): Move = move
}

private object NoMoveAiForRound : AiStrategy {
    override fun chooseMove(game: DicewarsGame): Move? = null
}

private fun twoPlayerGame(attackerDice: Int, defenderDice: Int): DicewarsGame {
    val adj1 = List(DicewarsGame.AREA_MAX) { if (it == 2) 1 else 0 }
    val adj2 = List(DicewarsGame.AREA_MAX) { if (it == 1) 1 else 0 }
    val game = DicewarsGame(
        maxPlayers = 2,
        turnOrder = listOf(0, 1),
        turnIndex = 0,
        areas = List(DicewarsGame.AREA_MAX) { areaId ->
            when (areaId) {
                1 -> AreaData(size = 6, owner = 0, dice = attackerDice, adjacentAreas = adj1)
                2 -> AreaData(size = 6, owner = 1, dice = defenderDice, adjacentAreas = adj2)
                else -> AreaData()
            }
        },
    )
    return game.setAreaTc(0).setAreaTc(1)
}

