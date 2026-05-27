package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.AreaData
import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.ai.AiStrategy
import com.franklinharper.dicewarsport.ai.Move
import com.franklinharper.dicewarsport.setAreaTc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BotRoundStepperTest {
    @Test
    fun stepperReturnsActionLogEntryForOneAction() {
        val stepper = BotRoundStepper(gameFactory = { _, _ -> stepperGame(attackerDice = 8, defenderDice = 1) })
        val state = stepper.initialState(
            RoundReplaySpec(
                roundNumber = 1,
                roundSeed = 123,
                participants = listOf(stepperParticipant("attacker", MoveAi(Move(1, 2))), stepperParticipant("defender", StepperNoMoveAi)),
                maxActionsPerRound = 10,
            ),
        )

        val result = stepper.step(state)

        assertEquals(RoundActionType.Attack, result.actionLogEntry.actionType)
        assertEquals(1, result.actionLogEntry.actionNumber)
        assertEquals("attacker", result.actionLogEntry.participantId)
        assertEquals(1, result.actionLogEntry.from)
        assertEquals(2, result.actionLogEntry.to)
        assertEquals(listOf("defender"), result.actionLogEntry.eliminatedParticipantIds)
    }

    @Test
    fun botGameRunnerCanCaptureActionLogForFailedRound() {
        val runner = BotGameRunner(gameFactory = { _, _ -> stepperGame(attackerDice = 2, defenderDice = 2) })

        val result = runner.runRound(
            RoundConfig(
                roundNumber = 1,
                participants = listOf(stepperParticipant("a", StepperNoMoveAi), stepperParticipant("b", StepperNoMoveAi)),
                roundSeed = 5,
                maxActionsPerRound = 2,
                logActions = true,
            ),
        )

        assertFalse(result.completed)
        assertEquals(3, result.actionLog.size, "two actions plus terminal failure entry")
        assertEquals(RoundActionType.RoundFailed, result.actionLog.last().actionType)
    }
}

private fun stepperParticipant(id: String, ai: AiStrategy): TournamentParticipant = TournamentParticipant(
    id = id,
    displayName = id,
    aiFactory = { ai },
)

private class MoveAi(private val move: Move) : AiStrategy {
    override fun chooseMove(game: DicewarsGame): Move = move
}

private object StepperNoMoveAi : AiStrategy {
    override fun chooseMove(game: DicewarsGame): Move? = null
}

private fun stepperGame(attackerDice: Int, defenderDice: Int): DicewarsGame {
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
