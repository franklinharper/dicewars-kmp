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
        factory = { random -> NeuralBotFactory.create(random, modelPathProperty = "dicewars.neural.model") },
        matches = { it is NeuralBot },
    )

    val neuralA = neuralVariant(id = "neural-a", displayName = "Neural A", modelPathProperty = "dicewars.neural.model.a")
    val neuralB = neuralVariant(id = "neural-b", displayName = "Neural B", modelPathProperty = "dicewars.neural.model.b")
    val neuralC = neuralVariant(id = "neural-c", displayName = "Neural C", modelPathProperty = "dicewars.neural.model.c")
    val neuralD = neuralVariant(id = "neural-d", displayName = "Neural D", modelPathProperty = "dicewars.neural.model.d")
    val neuralE = neuralVariant(id = "neural-e", displayName = "Neural E", modelPathProperty = "dicewars.neural.model.e")
    val neuralF = neuralVariant(id = "neural-f", displayName = "Neural F", modelPathProperty = "dicewars.neural.model.f")

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
        neuralA,
        neuralB,
        neuralC,
        neuralD,
        neuralE,
        neuralF,
    )

    val byId: Map<String, BuiltInBot> = all.associateBy { it.id }

    fun idFor(strategy: AiStrategy?): String =
        strategy?.let { ai -> all.firstOrNull { it.matches(ai) }?.id ?: ai.name } ?: "unknown-bot"

    private fun neuralVariant(id: String, displayName: String, modelPathProperty: String): BuiltInBot = BuiltInBot(
        id = id,
        displayName = displayName,
        factory = { random -> NeuralBotFactory.create(random, modelPathProperty = modelPathProperty) },
        // Variants share the same runtime strategy class; avoid ambiguous idFor matches.
        matches = { false },
    )
}
