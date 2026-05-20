package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.ai.BuiltInBots

object BuiltInTournamentParticipants {
    val all: List<TournamentParticipant> = BuiltInBots.all.map { bot ->
        TournamentParticipant(
            id = bot.id,
            displayName = bot.displayName,
            aiFactory = bot.factory,
        )
    }

    val byId: Map<String, TournamentParticipant> = all.associateBy { it.id }
}
