package com.franklinharper.dicewarsport.tournament

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class TournamentReportFormatterTest {
    @Test
    fun plainTextIncludesScoresAndFailedRounds() {
        val text = PlainTextTournamentReportFormatter.format(sampleTournamentResult())

        assertContains(text, "Dicewars Bot Tournament")
        assertContains(text, "Rounds requested: 2")
        assertContains(text, "Rounds completed: 1")
        assertContains(text, "Rounds failed: 1")
        assertContains(text, "Seed: 42")
        assertContains(text, "Bot A  4 pts  1 wins")
        assertContains(text, "Failed rounds:")
        assertContains(text, "Round 2: maxActionsPerRound=3 exceeded")
        assertContains(text, "Round seed: 222")
        assertContains(text, "Seats: b,a")
        assertContains(text, "Max actions: 3")
        assertContains(text, "./scripts/replay-round --round-seed 222 --seats b,a --max-actions 3 --last-steps 50")
        assertContains(text, "Action log entries: 1")
        assertContains(text, "ROUND_REPLAY_SPEC")
        assertContains(text, "roundSeed=222")
        assertContains(text, "seats=b,a")
        assertContains(text, "maxActions=3")
        assertContains(text, "lastSteps=50")
        assertContains(text, "END_ROUND_REPLAY_SPEC")
    }

    @Test
    fun csvIncludesStableHeaderScoreRowsAndRoundRows() {
        val csv = CsvTournamentReportFormatter.format(sampleTournamentResult())
        val lines = csv.trim().lines()

        assertTrue(lines.first().startsWith("section,tournament_seed,round_seed,seats,max_actions,action_log_entries,rank,bot_id,bot_name,score,wins,round,completed,winner,actions_taken,failure_reason"))
        assertContains(csv, "score,42,,,,,1,a,Bot A,4,1,,,,,")
        assertContains(csv, "score,42,,,,,2,b,Bot B,0,0,,,,,")
        assertContains(csv, "round,42,111,\"a,b\",3,0,,,,,,1,true,a,1,")
        assertContains(csv, "round,42,222,\"b,a\",3,1,,,,,,2,false,,3,maxActionsPerRound=3 exceeded")
    }
}

private fun sampleTournamentResult(): TournamentResult = TournamentResult(
    roundsRequested = 2,
    roundsCompleted = 1,
    roundsFailed = 1,
    seed = 42,
    botScores = listOf(
        BotScore(participantId = "a", displayName = "Bot A", score = 4, wins = 1),
        BotScore(participantId = "b", displayName = "Bot B", score = 0, wins = 0),
    ),
    roundResults = listOf(
        RoundResult(
            roundNumber = 1,
            roundSeed = 111,
            seatedParticipantIds = listOf("a", "b"),
            maxActionsPerRound = 3,
            completed = true,
            winnerParticipantId = "a",
            eliminationOrder = listOf("b"),
            scores = mapOf("a" to 4, "b" to 0),
            actionsTaken = 1,
        ),
        RoundResult(
            roundNumber = 2,
            roundSeed = 222,
            seatedParticipantIds = listOf("b", "a"),
            maxActionsPerRound = 3,
            completed = false,
            winnerParticipantId = null,
            eliminationOrder = emptyList(),
            scores = mapOf("a" to 0, "b" to 0),
            actionsTaken = 3,
            failureReason = "maxActionsPerRound=3 exceeded",
            actionLog = listOf(
                RoundActionLogEntry(
                    actionNumber = 1,
                    playerSlot = 0,
                    participantId = "b",
                    actionType = RoundActionType.EndTurn,
                ),
            ),
        ),
    ),
)
