package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerStatsHistoryTest {
    @Test
    fun recordGameResultUpdatesWinsGamesPlayedWinRatioAndScore() {
        val history = PlayerStatsHistory.default().recordGameResult(
            participants = listOf(
                PlayerStatsParticipant("human", "Human"),
                PlayerStatsParticipant("target-leader", "Rebel"),
                PlayerStatsParticipant("cautious", "Turtle"),
            ),
            winnerPlayerId = "human",
            eliminationOrderPlayerIds = listOf("cautious", "target-leader"),
        )

        val human = history.records.getValue("human")
        val rebel = history.records.getValue("target-leader")
        val turtle = history.records.getValue("cautious")

        assertEquals(1, human.wins)
        assertEquals(1, human.gamesPlayed)
        assertEquals(100, human.winRatioPercent)
        assertEquals(6, human.score)

        assertEquals(0, rebel.wins)
        assertEquals(1, rebel.gamesPlayed)
        assertEquals(0, rebel.winRatioPercent)
        assertEquals(1, rebel.score)

        assertEquals(0, turtle.wins)
        assertEquals(1, turtle.gamesPlayed)
        assertEquals(0, turtle.score)
    }
}
