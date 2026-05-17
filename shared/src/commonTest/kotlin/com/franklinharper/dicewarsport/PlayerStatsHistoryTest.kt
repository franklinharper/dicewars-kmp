package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerStatsHistoryTest {
    @Test
    fun recordGameResultUpdatesWinsGamesPlayedWinRatioAndScore() {
        val history = PlayerStatsHistory.default().recordGameResult(
            participants = listOf(
                PlayerStatsParticipant(0, "human", "Human"),
                PlayerStatsParticipant(1, "target-leader", "Rebel"),
                PlayerStatsParticipant(2, "cautious", "Turtle"),
            ),
            winnerSeatId = 0,
            eliminationOrderSeatIds = listOf(2, 1),
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

    @Test
    fun duplicateStrategiesAreScoredPerSeatAndAggregatedIntoOneRecord() {
        val history = PlayerStatsHistory.default().recordGameResult(
            participants = listOf(
                PlayerStatsParticipant(0, "human", "Human"),
                PlayerStatsParticipant(1, "cautious", "Turtle"),
                PlayerStatsParticipant(2, "cautious", "Turtle"),
                PlayerStatsParticipant(3, "attack-when-stronger", "Berzerker"),
                PlayerStatsParticipant(4, "attack-when-stronger", "Berzerker"),
                PlayerStatsParticipant(5, "strategic", "Emperor"),
                PlayerStatsParticipant(6, "target-leader", "Rebel"),
            ),
            winnerSeatId = 2,
            eliminationOrderSeatIds = listOf(3, 1, 0, 6, 4, 5),
        )

        val turtle = history.records.getValue("cautious")
        val berzerker = history.records.getValue("attack-when-stronger")

        assertEquals(1, turtle.wins)
        assertEquals(2, turtle.gamesPlayed)
        assertEquals(50, turtle.winRatioPercent)
        assertEquals(15, turtle.score)

        assertEquals(0, berzerker.wins)
        assertEquals(2, berzerker.gamesPlayed)
        assertEquals(4, berzerker.score)
    }
}
