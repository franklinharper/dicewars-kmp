package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.ai.BullyBot
import com.franklinharper.dicewarsport.ai.TurtleBot
import com.franklinharper.dicewarsport.ai.FrontierCommanderBot
import com.franklinharper.dicewarsport.ai.MaxBot
import com.franklinharper.dicewarsport.ai.OptimusBot
import com.franklinharper.dicewarsport.ai.EmperorBot
import com.franklinharper.dicewarsport.ai.RebelBot
import com.franklinharper.dicewarsport.ai.Terminator2Bot
import com.franklinharper.dicewarsport.ai.TerminatorBot

object BuiltInTournamentParticipants {
    val attackWhenStronger = TournamentParticipant(
        id = "bully",
        displayName = "Bully",
        aiFactory = { random -> BullyBot(random) },
    )

    val targetLeader = TournamentParticipant(
        id = "rebel",
        displayName = "Rebel",
        aiFactory = { random -> RebelBot(random) },
    )

    val turtle = TournamentParticipant(
        id = "turtle",
        displayName = "Turtle",
        aiFactory = { TurtleBot() },
    )

    val emperor = TournamentParticipant(
        id = "emperor",
        displayName = "Emperor",
        aiFactory = { random -> EmperorBot(random) },
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

    val terminator2 = TournamentParticipant(
        id = "terminator2",
        displayName = "Terminator 2",
        aiFactory = { Terminator2Bot() },
    )

    val all: List<TournamentParticipant> = listOf(targetLeader, turtle, attackWhenStronger, emperor, frontierCommander, max, optimus, terminator, terminator2)

    val byId: Map<String, TournamentParticipant> = all.associateBy { it.id }
}
