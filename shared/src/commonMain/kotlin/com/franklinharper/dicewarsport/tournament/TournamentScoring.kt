package com.franklinharper.dicewarsport.tournament

fun scoreCompletedRound(
    participantIds: List<String>,
    winnerParticipantId: String,
    eliminationOrder: List<String>,
): Map<String, Int> {
    require(participantIds.size >= 2) { "Round scoring requires at least two participants" }
    require(winnerParticipantId in participantIds) { "Winner must be a participant" }

    val scores = participantIds.associateWith { 0 }.toMutableMap()
    val seen = mutableSetOf<String>()

    eliminationOrder.forEachIndexed { index, participantId ->
        if (participantId == winnerParticipantId) return@forEachIndexed
        if (participantId !in participantIds) return@forEachIndexed
        if (!seen.add(participantId)) return@forEachIndexed
        scores[participantId] = index
    }

    scores[winnerParticipantId] = 2 * participantIds.size
    return scores.toMap()
}

fun scoreFailedRound(participantIds: List<String>): Map<String, Int> =
    participantIds.associateWith { 0 }

fun aggregateBotScores(
    participants: List<TournamentParticipant>,
    roundResults: List<RoundResult>,
): List<BotScore> {
    val scores = participants.associate { it.id to 0 }.toMutableMap()
    val wins = participants.associate { it.id to 0 }.toMutableMap()

    for (round in roundResults) {
        for ((participantId, score) in round.scores) {
            scores[participantId] = (scores[participantId] ?: 0) + score
        }
        val winner = round.winnerParticipantId
        if (round.completed && winner != null) {
            wins[winner] = (wins[winner] ?: 0) + 1
        }
    }

    val names = participants.associate { it.id to it.displayName }
    return participants.map { participant ->
        BotScore(
            participantId = participant.id,
            displayName = names.getValue(participant.id),
            score = scores.getValue(participant.id),
            wins = wins.getValue(participant.id),
        )
    }.sortedWith(
        compareByDescending<BotScore> { it.score }
            .thenByDescending { it.wins }
            .thenBy { it.displayName }
            .thenBy { it.participantId },
    )
}
