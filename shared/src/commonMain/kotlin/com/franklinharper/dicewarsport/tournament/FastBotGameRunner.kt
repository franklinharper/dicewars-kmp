package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.RandomSource

/**
 * Fast [RoundRunner] that uses [FastBotRoundStepper] with mutable game state.
 *
 * Produces identical tournament scores as [BotGameRunner] but avoids
 * intermediate immutable copies during round execution.
 */
class FastBotGameRunner(
    private val gameFactory: (playerCount: Int, random: RandomSource) -> DicewarsGame = { playerCount, random ->
        DicewarsGame.generate(playerCount, random)
    },
) : RoundRunner {
    private val stepper = FastBotRoundStepper(gameFactory)

    override fun runRound(config: RoundConfig): RoundResult {
        return stepper.runRound(
            spec = RoundReplaySpec(
                roundNumber = config.roundNumber,
                roundSeed = config.roundSeed,
                participants = config.participants,
                maxActionsPerRound = config.maxActionsPerRound,
            ),
            logActions = config.logActions,
        )
    }
}
