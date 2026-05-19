package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.ai.AlwaysAttackWhenStrongerBot
import com.franklinharper.dicewarsport.ai.CautiousBot
import com.franklinharper.dicewarsport.ai.FrontierCommanderBot
import com.franklinharper.dicewarsport.ai.MaxBot
import com.franklinharper.dicewarsport.ai.OptimusBot
import com.franklinharper.dicewarsport.ai.StrategicBot
import com.franklinharper.dicewarsport.ai.TargetTheLeader
import com.franklinharper.dicewarsport.ai.TerminatorBot

object BuiltInTournamentParticipants {
    val attackWhenStronger = TournamentParticipant(
        id = "bully",
        displayName = "Bully",
        aiFactory = { random -> AlwaysAttackWhenStrongerBot(random) },
    )

    val targetLeader = TournamentParticipant(
        id = "rebel",
        displayName = "Rebel",
        aiFactory = { random -> TargetTheLeader(random) },
    )

    val turtle = TournamentParticipant(
        id = "turtle",
        displayName = "Turtle",
        aiFactory = { CautiousBot() },
    )

    val emperor = TournamentParticipant(
        id = "emperor",
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

    val optimus = TournamentParticipant(
        id = "optimus",
        displayName = "Optimus",
        aiFactory = { OptimusBot() },
    )

    val terminator = TournamentParticipant(
        id = "terminator",
        displayName = "Terminator",
        aiFactory = { TerminatorBot() },
    )

    val all: List<TournamentParticipant> = listOf(targetLeader, turtle, attackWhenStronger, emperor, frontierCommander, max, optimus, terminator)

    val byId: Map<String, TournamentParticipant> = all.associateBy { it.id }
}
