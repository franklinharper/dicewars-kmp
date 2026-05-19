package com.franklinharper.dicewarsport.tournament

import kotlin.random.Random

class TournamentRunner(
    private val roundRunner: RoundRunner = BotGameRunner(),
) {
    fun run(config: TournamentConfig): TournamentResult {
        val startedAt = kotlin.time.TimeSource.Monotonic.markNow()
        val effectiveSeed = config.seed ?: Random.Default.nextInt()
        val roundResults = mutableListOf<RoundResult>()

        for (roundNumber in 1..config.rounds) {
            val seatedParticipants = config.participants.rotatedLeft((roundNumber - 1) % config.participants.size)
            val roundConfig = RoundConfig(
                roundNumber = roundNumber,
                participants = seatedParticipants,
                roundSeed = deriveRoundSeed(effectiveSeed, roundNumber),
                maxActionsPerRound = config.maxActionsPerRound,
                logActions = config.logAllRounds,
            )
            val initialResult = roundRunner.runRound(roundConfig)
            val finalResult = if (config.logFailedRounds && !config.logAllRounds && !initialResult.completed) {
                roundRunner.runRound(roundConfig.copy(logActions = true))
            } else {
                initialResult
            }
            roundResults.add(finalResult)
        }

        return TournamentResult(
            roundsRequested = config.rounds,
            roundsCompleted = roundResults.count { it.completed },
            roundsFailed = roundResults.count { !it.completed },
            seed = effectiveSeed,
            parallelism = 1,
            durationMillis = startedAt.elapsedNow().inWholeMilliseconds,
            botScores = aggregateBotScores(config.participants, roundResults),
            roundResults = roundResults.toList(),
        )
    }
}

fun <T> List<T>.rotatedLeft(count: Int): List<T> {
    if (isEmpty()) return emptyList()
    val normalized = count.mod(size)
    return drop(normalized) + take(normalized)
}
