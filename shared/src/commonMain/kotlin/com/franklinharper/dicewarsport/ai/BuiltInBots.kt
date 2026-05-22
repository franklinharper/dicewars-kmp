package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.ai.neural.NeuralBot
import com.franklinharper.dicewarsport.ai.neural.NeuralBotFactory

data class BuiltInBot(
    val id: String,
    val displayName: String,
    val factory: (RandomSource) -> AiStrategy,
    val matches: (AiStrategy) -> Boolean,
)

object BuiltInBots {
    val rebel = BuiltInBot(
        id = "rebel",
        displayName = "Rebel",
        factory = { random -> RebelBot(random) },
        matches = { it is RebelBot },
    )

    val turtle = BuiltInBot(
        id = "turtle",
        displayName = "Turtle",
        factory = { TurtleBot() },
        matches = { it is TurtleBot },
    )

    val bully = BuiltInBot(
        id = "bully",
        displayName = "Bully",
        factory = { random -> BullyBot(random) },
        matches = { it is BullyBot },
    )

    val emperor = BuiltInBot(
        id = "emperor",
        displayName = "Emperor",
        factory = { random -> EmperorBot(random) },
        matches = { it is EmperorBot },
    )

    val frontierCommander = BuiltInBot(
        id = "frontier-commander",
        displayName = "Frontier Commander",
        factory = { FrontierCommanderBot() },
        matches = { it is FrontierCommanderBot },
    )

    val max = BuiltInBot(
        id = "max",
        displayName = "Max",
        factory = { MaxBot() },
        matches = { it is MaxBot },
    )

    val optimus = BuiltInBot(
        id = "optimus",
        displayName = "Optimus",
        factory = { OptimusBot() },
        matches = { it is OptimusBot },
    )

    val terminator = BuiltInBot(
        id = "terminator",
        displayName = "Terminator",
        factory = { TerminatorBot() },
        matches = { it is TerminatorBot },
    )

    val terminator2 = BuiltInBot(
        id = "terminator2",
        displayName = "Terminator 2",
        factory = { Terminator2Bot() },
        matches = { it is Terminator2Bot },
    )

    val neural = BuiltInBot(
        id = "neural",
        displayName = "Neural",
        factory = { random -> NeuralBotFactory.create(random) },
        matches = { it is NeuralBot },
    )

    val all: List<BuiltInBot> = listOf(
        rebel,
        turtle,
        bully,
        emperor,
        frontierCommander,
        max,
        optimus,
        terminator,
        terminator2,
        neural,
    )

    val byId: Map<String, BuiltInBot> = all.associateBy { it.id }

    fun idFor(strategy: AiStrategy?): String =
        strategy?.let { ai -> all.firstOrNull { it.matches(ai) }?.id ?: ai.name } ?: "unknown-bot"
}
