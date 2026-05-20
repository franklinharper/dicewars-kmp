package com.franklinharper.dicewarsport.trainingcli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TournamentReportParserTest {
    @Test
    fun parsesBotScoresFromPlainTextTournamentReport() {
        val scores = TournamentReportParser.parseBotScores(report())

        assertEquals(listOf("terminator2", "neural", "max", "bully"), scores.map { it.participantId })
        assertEquals("Terminator 2", scores[0].displayName)
        assertEquals(52531, scores[0].score)
        assertEquals(2503, scores[0].wins)
        assertEquals("Neural", scores[1].displayName)
        assertEquals(46410, scores[1].score)
        assertEquals(731, scores[1].wins)
    }

    @Test
    fun parserNormalizesDisplayNamesToKnownBotIds() {
        val scores = TournamentReportParser.parseBotScores(report())

        assertEquals("terminator2", scores[0].participantId)
        assertEquals("neural", scores[1].participantId)
    }

    @Test
    fun rejectsReportWithoutScoresSection() {
        assertFailsWith<IllegalArgumentException> {
            TournamentReportParser.parseBotScores("Dicewars Bot Tournament\nRounds requested: 1")
        }
    }

    @Test
    fun rejectsUnknownDisplayName() {
        val badReport = report().replace("Neural", "Mystery Bot")

        assertFailsWith<IllegalArgumentException> {
            TournamentReportParser.parseBotScores(badReport)
        }
    }

    private fun report(): String = """
        Dicewars Bot Tournament
        Rounds requested: 10000
        Rounds completed: 10000
        Rounds failed: 0
        Seed: 123
        Parallel processes: 8
        Duration: 38s 269ms

        Scores:
        1. Terminator 2        52,531 pts  2,503 wins
        2. Neural              46,410 pts    731 wins
        3. Max                 44,641 pts  1,714 wins
        4. Bully               30,661 pts    718 wins
    """.trimIndent()
}
