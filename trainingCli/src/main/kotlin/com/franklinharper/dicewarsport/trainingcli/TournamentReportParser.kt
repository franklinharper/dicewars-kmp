package com.franklinharper.dicewarsport.trainingcli

import com.franklinharper.dicewarsport.tournament.BotScore
import com.franklinharper.dicewarsport.tournament.BuiltInTournamentParticipants

object TournamentReportParser {
    private val displayNameToId: Map<String, String> =
        BuiltInTournamentParticipants.all.associate { it.displayName to it.id } + ("Neural" to "neural")

    fun parseBotScores(report: String): List<BotScore> {
        val lines = report.lineSequence().map { it.trimEnd() }.toList()
        val scoresIndex = lines.indexOfFirst { it.trim() == "Scores:" }
        require(scoresIndex >= 0) { "Report does not contain a Scores section" }

        val scores = mutableListOf<BotScore>()
        val scoreLine = Regex("^\\s*\\d+\\.\\s+(.+?)\\s+([0-9,]+)\\s+pts\\s+([0-9,]+)\\s+wins\\s*$")
        for (line in lines.drop(scoresIndex + 1)) {
            if (line.isBlank()) continue
            val match = scoreLine.matchEntire(line) ?: break
            val displayName = match.groupValues[1].trim()
            val participantId = displayNameToId[displayName]
                ?: throw IllegalArgumentException("Unknown bot display name '$displayName'")
            scores.add(
                BotScore(
                    participantId = participantId,
                    displayName = displayName,
                    score = match.groupValues[2].replace(",", "").toInt(),
                    wins = match.groupValues[3].replace(",", "").toInt(),
                ),
            )
        }
        require(scores.isNotEmpty()) { "Scores section does not contain bot score rows" }
        return scores
    }
}
