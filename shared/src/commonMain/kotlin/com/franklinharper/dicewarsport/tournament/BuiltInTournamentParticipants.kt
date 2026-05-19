package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.ai.AlwaysAttackWhenStrongerBot
import com.franklinharper.dicewarsport.ai.CautiousBot
import com.franklinharper.dicewarsport.ai.FrontierCommanderBot
import com.franklinharper.dicewarsport.ai.MaxBot
import com.franklinharper.dicewarsport.ai.StrategicBot
import com.franklinharper.dicewarsport.ai.TargetTheLeader

object BuiltInTournamentParticipants {
    val attackWhenStronger = TournamentParticipant(
        id = "attack-when-stronger",
        displayName = "Berzerker",
        aiFactory = { random -> AlwaysAttackWhenStrongerBot(random) },
    )

    val targetLeader = TournamentParticipant(
        id = "target-leader",
        displayName = "Rebel",
        aiFactory = { random -> TargetTheLeader(random) },
    )

    val cautious = TournamentParticipant(
        id = "cautious",
        displayName = "Turtle",
        aiFactory = { CautiousBot() },
    )

    val strategic = TournamentParticipant(
        id = "strategic",
        displayName = "Emperor",
        aiFactory = { random -> StrategicBot(random) },
    )

    val frontierCommander = TournamentParticipant(
        id = "frontier-commander",
        displayName = "Frontier Commander",
        aiFactory = { FrontierCommanderBot() },
    )

    val max = TournamentParticipant(
        id = "max",
        displayName = "Max",
        aiFactory = { MaxBot() },
    )

    val all: List<TournamentParticipant> = listOf(targetLeader, cautious, attackWhenStronger, strategic, frontierCommander, max)

    val byId: Map<String, TournamentParticipant> = all.associateBy { it.id }
}
