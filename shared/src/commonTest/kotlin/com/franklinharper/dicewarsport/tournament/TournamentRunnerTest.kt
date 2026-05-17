package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.KotlinRandomSource
import com.franklinharper.dicewarsport.ai.AiStrategy
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class TournamentRunnerTest {
    @Test
    fun attemptsConfiguredNumberOfRoundsAndCountsFailures() {
        val runner = TournamentRunner(
            roundRunner = RoundRunner { config ->
                if (config.roundNumber == 2) failedRound(config) else completedRound(config, winnerIndex = 0)
            },
        )

        val result = runner.run(
            TournamentConfig(
                participants = listOf(testParticipant("a"), testParticipant("b")),
                rounds = 3,
                seed = 1,
            ),
        )

        assertEquals(3, result.roundsRequested)
        assertEquals(2, result.roundsCompleted)
        assertEquals(1, result.roundsFailed)
        assertEquals(3, result.roundResults.size)
    }

    @Test
    fun rotatesParticipantSeatingEachRound() {
        val observedSeatOrders = mutableListOf<List<String>>()
        val runner = TournamentRunner(
            roundRunner = RoundRunner { config ->
                observedSeatOrders.add(config.participants.map { it.id })
                completedRound(config, winnerIndex = 0)
            },
        )

        runner.run(
            TournamentConfig(
                participants = listOf(testParticipant("a"), testParticipant("b"), testParticipant("c")),
                rounds = 4,
                seed = 1,
            ),
        )

        assertEquals(
            listOf(
                listOf("a", "b", "c"),
                listOf("b", "c", "a"),
                listOf("c", "a", "b"),
                listOf("a", "b", "c"),
            ),
            observedSeatOrders,
        )
    }

    @Test
    fun sameSeedProducesSameResultAndDifferentSeedCanDiffer() {
        val randomRoundRunner = RoundRunner { config ->
            val random = KotlinRandomSource(Random(config.roundSeed))
            val winnerIndex = random.nextInt(config.participants.size)
            completedRound(config, winnerIndex = winnerIndex, actionsTaken = random.nextInt(1000))
        }
        val participants = listOf(testParticipant("a"), testParticipant("b"), testParticipant("c"))

        val first = TournamentRunner(randomRoundRunner).run(TournamentConfig(participants, rounds = 5, seed = 42))
        val second = TournamentRunner(randomRoundRunner).run(TournamentConfig(participants, rounds = 5, seed = 42))
        val third = TournamentRunner(randomRoundRunner).run(TournamentConfig(participants, rounds = 5, seed = 43))

        assertEquals(first, second)
        assertNotEquals(first.roundResults, third.roundResults)
    }

    @Test
    fun logFailedRoundsRequestsLogsOnlyForFailedRoundsAfterProbeRun() {
        val observedLogFlags = mutableListOf<Boolean>()
        val runner = TournamentRunner(
            roundRunner = RoundRunner { config ->
                observedLogFlags.add(config.logActions)
                if (config.roundNumber == 1) completedRound(config, winnerIndex = 0) else failedRound(config)
            },
        )

        runner.run(
            TournamentConfig(
                participants = listOf(testParticipant("a"), testParticipant("b")),
                rounds = 2,
                seed = 1,
                logFailedRounds = true,
            ),
        )

        assertEquals(listOf(false, false, true), observedLogFlags)
    }

    @Test
    fun logAllRoundsRequestsLogsForEveryRound() {
        val observedLogFlags = mutableListOf<Boolean>()
        val runner = TournamentRunner(
            roundRunner = RoundRunner { config ->
                observedLogFlags.add(config.logActions)
                completedRound(config, winnerIndex = 0)
            },
        )

        runner.run(
            TournamentConfig(
                participants = listOf(testParticipant("a"), testParticipant("b")),
                rounds = 2,
                seed = 1,
                logAllRounds = true,
            ),
        )

        assertEquals(listOf(true, true), observedLogFlags)
    }

    @Test
    fun omittedSeedIsGeneratedAndReportedForReproducibility() {
        val runner = TournamentRunner(roundRunner = RoundRunner { config -> completedRound(config, winnerIndex = 0) })

        val result = runner.run(
            TournamentConfig(
                participants = listOf(testParticipant("a"), testParticipant("b")),
                rounds = 1,
            ),
        )

        assertNotNull(result.seed)
    }

    @Test
    fun builtInParticipantsAreAvailableById() {
        assertEquals(
            BuiltInTournamentParticipants.all.map { it.id }.toSet(),
            BuiltInTournamentParticipants.byId.keys,
        )
    }
}

private fun testParticipant(id: String): TournamentParticipant = TournamentParticipant(
    id = id,
    displayName = id.uppercase(),
    aiFactory = { StaticNoMoveAi },
)

private object StaticNoMoveAi : AiStrategy {
    override fun chooseMove(game: DicewarsGame) = null
}

private fun completedRound(
    config: RoundConfig,
    winnerIndex: Int,
    actionsTaken: Int = 1,
): RoundResult {
    val participantIds = config.participants.map { it.id }
    val winner = config.participants[winnerIndex].id
    val eliminations = participantIds.filter { it != winner }
    return RoundResult(
        roundNumber = config.roundNumber,
        roundSeed = config.roundSeed,
        seatedParticipantIds = participantIds,
        maxActionsPerRound = config.maxActionsPerRound,
        completed = true,
        winnerParticipantId = winner,
        eliminationOrder = eliminations,
        scores = scoreCompletedRound(participantIds, winner, eliminations),
        actionsTaken = actionsTaken,
    )
}

private fun failedRound(config: RoundConfig): RoundResult {
    val participantIds = config.participants.map { it.id }
    return RoundResult(
        roundNumber = config.roundNumber,
        roundSeed = config.roundSeed,
        seatedParticipantIds = participantIds,
        maxActionsPerRound = config.maxActionsPerRound,
        completed = false,
        winnerParticipantId = null,
        eliminationOrder = emptyList(),
        scores = scoreFailedRound(participantIds),
        actionsTaken = config.maxActionsPerRound,
        failureReason = "max actions",
    )
}
