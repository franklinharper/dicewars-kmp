package com.franklinharper.dicewarsport.tournament

object RoundActionDebugFormatter {
    fun format(entry: RoundActionLogEntry): String {
        val eliminated = entry.eliminatedParticipantIds.joinToString(",").ifBlank { "none" }
        return when (entry.actionType) {
            RoundActionType.Attack -> "Step ${entry.actionNumber}: ${entry.participantId} attacks ${entry.from} -> ${entry.to}, ${if (entry.battleRoll?.success == true) "success" else "fail"}, eliminated: $eliminated"
            RoundActionType.IllegalMove -> "Step ${entry.actionNumber}: ${entry.participantId} illegal move ${entry.from} -> ${entry.to}; ends turn, eliminated: $eliminated"
            RoundActionType.EndTurn -> "Step ${entry.actionNumber}: ${entry.participantId} ends turn, supplied: ${entry.suppliedAreas.joinToString(",").ifBlank { "none" }}, eliminated: $eliminated"
            RoundActionType.RoundFailed -> "End: round failed, eliminated: $eliminated"
            RoundActionType.RoundWon -> "End: round won by ${entry.participantId}, eliminated: $eliminated"
        }
    }
}
