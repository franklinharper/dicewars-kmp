package com.franklinharper.dicewarsport

data class PlayerStatsRecord(
    val playerId: String,
    val displayName: String,
    val wins: Int = 0,
    val gamesPlayed: Int = 0,
    val score: Int = 0,
) {
    val winRatioPercent: Int get() = if (gamesPlayed == 0) 0 else (wins * 100) / gamesPlayed
}

data class PlayerStatsHistory(
    val records: Map<String, PlayerStatsRecord> = emptyMap(),
) {
    fun sortedRecords(): List<PlayerStatsRecord> = records.values.sortedWith(
        compareByDescending<PlayerStatsRecord> { it.score }
            .thenByDescending { it.winRatioPercent }
            .thenBy { it.displayName }
    )

    fun recordGameResult(
        participants: List<PlayerStatsParticipant>,
        winnerSeatId: Int,
        eliminationOrderSeatIds: List<Int>,
    ): PlayerStatsHistory {
        val participantBySeat = participants.associateBy { it.seatId }
        val participantCount = participants.size
        val scoreBySeat = mutableMapOf<Int, Int>()
        eliminationOrderSeatIds
            .filter { it in participantBySeat && it != winnerSeatId }
            .distinct()
            .forEachIndexed { index, seatId -> scoreBySeat[seatId] = index }
        scoreBySeat[winnerSeatId] = participantCount * 2

        val updated = records.toMutableMap()
        participants.forEach { participant ->
            val current = updated[participant.playerId] ?: PlayerStatsRecord(
                playerId = participant.playerId,
                displayName = participant.displayName,
            )
            updated[participant.playerId] = current.copy(
                displayName = participant.displayName,
                wins = current.wins + if (participant.seatId == winnerSeatId) 1 else 0,
                gamesPlayed = current.gamesPlayed + 1,
                score = current.score + (scoreBySeat[participant.seatId] ?: 0),
            )
        }
        return copy(records = updated)
    }

    companion object {
        fun default(): PlayerStatsHistory = PlayerStatsHistory()
    }
}

data class PlayerStatsParticipant(
    val seatId: Int,
    val playerId: String,
    val displayName: String,
)

interface PlayerStatsStore {
    fun load(): PlayerStatsHistory
    fun save(history: PlayerStatsHistory)
    fun clear()
}

class InMemoryPlayerStatsStore(initialHistory: PlayerStatsHistory = PlayerStatsHistory.default()) : PlayerStatsStore {
    private var history = initialHistory
    override fun load(): PlayerStatsHistory = history
    override fun save(history: PlayerStatsHistory) { this.history = history }
    override fun clear() { history = PlayerStatsHistory.default() }
}
