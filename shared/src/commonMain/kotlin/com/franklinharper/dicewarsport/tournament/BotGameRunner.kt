package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.RandomSource

fun interface RoundRunner {
    fun runRound(config: RoundConfig): RoundResult
}

class BotGameRunner(
    private val gameFactory: (playerCount: Int, random: RandomSource) -> DicewarsGame = { playerCount, random ->
        DicewarsGame.generate(playerCount, random)
    },
) : RoundRunner {
    override fun runRound(config: RoundConfig): RoundResult {
        val stepper = BotRoundStepper(gameFactory)
        var state = stepper.initialState(
            RoundReplaySpec(
                roundNumber = config.roundNumber,
                roundSeed = config.roundSeed,
                participants = config.participants,
                maxActionsPerRound = config.maxActionsPerRound,
            ),
        )
        val actionLog = mutableListOf<RoundActionLogEntry>()

        while (!state.completed && !state.failed) {
            val step = stepper.step(state)
            state = step.state
            if (config.logActions) actionLog.add(step.actionLogEntry)
        }

        return stepper.resultFor(state, actionLog)
    }
}
