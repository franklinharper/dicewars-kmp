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
        appendLine("Parallel processes: ${result.parallelism}")
        appendLine("Duration: ${formatDuration(result.durationMillis)}")
        appendLine()
        appendLine("Scores:")
        if (result.botScores.isNotEmpty()) {
            val rankWidth = result.botScores.size.toString().length
            val nameWidth = result.botScores.maxOf { it.displayName.length }
            val scoreWidth = result.botScores.maxOf { it.score.withThousandsSeparators().length }
            val winsWidth = result.botScores.maxOf { it.wins.withThousandsSeparators().length }
            result.botScores.forEachIndexed { index, score ->
                appendLine(
                    "${(index + 1).toString().padStart(rankWidth)}. " +
                        score.displayName.padEnd(nameWidth) +
                        "  ${score.score.withThousandsSeparators().padStart(scoreWidth)} pts" +
                        "  ${score.wins.withThousandsSeparators().padStart(winsWidth)} wins",
                )
            }
        }

        val failedRounds = result.roundResults.filter { !it.completed }
        if (result.debug && failedRounds.isNotEmpty()) {
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
        appendLine("section,tournament_seed,parallel_processes,duration_ms,round_seed,seats,max_actions,action_log_entries,rank,bot_id,bot_name,score,wins,round,completed,winner,actions_taken,failure_reason")
        result.botScores.forEachIndexed { index, score ->
            appendCsvRow(
                "score",
                result.seed?.toString() ?: "",
                result.parallelism.toString(),
                result.durationMillis.toString(),
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
                result.parallelism.toString(),
                result.durationMillis.toString(),
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

private fun Int.withThousandsSeparators(): String = toLong().withThousandsSeparators()

private fun Long.withThousandsSeparators(): String {
    val sign = if (this < 0) "-" else ""
    val digits = kotlin.math.abs(this).toString()
    return sign + digits.reversed().chunked(3).joinToString(",").reversed()
}

private fun formatDuration(durationMillis: Long): String {
    val minutes = durationMillis / 60_000
    val seconds = (durationMillis % 60_000) / 1_000
    val millis = durationMillis % 1_000
    return when {
        minutes > 0 -> "${minutes}m ${seconds}s ${millis}ms"
        seconds > 0 -> "${seconds}s ${millis}ms"
        else -> "${millis}ms"
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
