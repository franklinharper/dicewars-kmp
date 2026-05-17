package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.BattleRoll
import kotlin.test.Test
import kotlin.test.assertEquals

class RoundActionDebugFormatterTest {
    @Test
    fun formatsNormalActionAsStep() {
        val text = RoundActionDebugFormatter.format(
            RoundActionLogEntry(
                actionNumber = 12,
                playerSlot = 0,
                participantId = "target-leader",
                actionType = RoundActionType.Attack,
                from = 3,
                to = 4,
                battleRoll = BattleRoll(
                    attackerDice = listOf(6),
                    defenderDice = listOf(1),
                    attackerTotal = 6,
                    defenderTotal = 1,
                    success = true,
                ),
                eliminatedParticipantIds = listOf("cautious"),
            ),
        )

        assertEquals("Step 12: target-leader attacks 3 -> 4, success, eliminated: cautious", text)
    }

    @Test
    fun formatsEndActionAsEnd() {
        val text = RoundActionDebugFormatter.format(
            RoundActionLogEntry(
                actionNumber = 101,
                playerSlot = 0,
                participantId = "target-leader",
                actionType = RoundActionType.RoundFailed,
            ),
        )

        assertEquals("End: round failed, eliminated: none", text)
    }
}
