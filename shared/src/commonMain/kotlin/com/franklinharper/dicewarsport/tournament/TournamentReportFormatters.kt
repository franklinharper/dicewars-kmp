package com.franklinharper.dicewarsport.tournament

interface TournamentReportFormatter {
    fun format(result: TournamentResult): String
}

object PlainTextTournamentReportFormatter : TournamentReportFormatter {
    override fun format(result: TournamentResult): String = buildString {
        appendLine("Dicewars Bot Tournament")
        appendLine("Rounds requested: ${result.roundsRequested}")
        appendLine("Rounds completed: ${result.roundsCompleted}")
        appendLine("Rounds failed: ${result.roundsFailed}")
        appendLine("Seed: ${result.seed?.toString() ?: "random"}")
        appendLine()
        appendLine("Scores:")
        result.botScores.forEachIndexed { index, score ->
            appendLine("${index + 1}. ${score.displayName}  ${score.score} pts  ${score.wins} wins")
        }

        val failedRounds = result.roundResults.filter { !it.completed }
        if (failedRounds.isNotEmpty()) {
            appendLine()
            appendLine("Failed rounds:")
            for (round in failedRounds) {
                appendLine("- Round ${round.roundNumber}: ${round.failureReason ?: "failed"} after ${round.actionsTaken} actions")
                appendLine("  Tournament seed: ${result.seed?.toString() ?: "random"}")
                appendLine("  Round seed: ${round.roundSeed}")
                appendLine("  Seats: ${round.seatedParticipantIds.joinToString(",")}")
                appendLine("  Max actions: ${round.maxActionsPerRound}")
                if (round.actionLog.isNotEmpty()) {
                    appendLine("  Action log entries: ${round.actionLog.size}")
                }
                val replaySpec = RoundReplaySpecText(
                    roundSeed = round.roundSeed,
                    seatIds = round.seatedParticipantIds,
                    maxActions = round.maxActionsPerRound,
                    lastSteps = 50,
                )
                appendLine("  Repro spec:")
                RoundReplaySpecParser.format(replaySpec).lines().forEach { line -> appendLine("  $line") }
                appendLine("  Repro:")
                appendLine("    ./scripts/replay-round --round-seed ${round.roundSeed} --seats ${round.seatedParticipantIds.joinToString(",")} --max-actions ${round.maxActionsPerRound} --last-steps 50")
            }
        }
    }.trimEnd() + "\n"
}

object CsvTournamentReportFormatter : TournamentReportFormatter {
    override fun format(result: TournamentResult): String = buildString {
        appendLine("section,tournament_seed,round_seed,seats,max_actions,action_log_entries,rank,bot_id,bot_name,score,wins,round,completed,winner,actions_taken,failure_reason")
        result.botScores.forEachIndexed { index, score ->
            appendCsvRow(
                "score",
                result.seed?.toString() ?: "",
                "",
                "",
                "",
                "",
                (index + 1).toString(),
                score.participantId,
                score.displayName,
                score.score.toString(),
                score.wins.toString(),
                "",
                "",
                "",
                "",
                "",
            )
        }
        for (round in result.roundResults) {
            appendCsvRow(
                "round",
                result.seed?.toString() ?: "",
                round.roundSeed.toString(),
                round.seatedParticipantIds.joinToString(","),
                round.maxActionsPerRound.toString(),
                round.actionLog.size.toString(),
                "",
                "",
                "",
                "",
                "",
                round.roundNumber.toString(),
                round.completed.toString(),
                round.winnerParticipantId ?: "",
                round.actionsTaken.toString(),
                round.failureReason ?: "",
            )
        }
    }
}

private fun StringBuilder.appendCsvRow(vararg values: String) {
    appendLine(values.joinToString(",") { it.csvEscaped() })
}

private fun String.csvEscaped(): String {
    val needsQuotes = any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    if (!needsQuotes) return this
    return "\"${replace("\"", "\"\"")}\""
}
