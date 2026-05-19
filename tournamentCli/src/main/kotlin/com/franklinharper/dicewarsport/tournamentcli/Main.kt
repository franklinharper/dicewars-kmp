package com.franklinharper.dicewarsport.tournamentcli

import com.franklinharper.dicewarsport.tournament.BuiltInTournamentParticipants
import com.franklinharper.dicewarsport.tournament.CsvTournamentReportFormatter
import com.franklinharper.dicewarsport.tournament.BotGameRunner
import com.franklinharper.dicewarsport.tournament.BotRoundStepper
import com.franklinharper.dicewarsport.tournament.PlainTextTournamentReportFormatter
import com.franklinharper.dicewarsport.tournament.RoundActionDebugFormatter
import com.franklinharper.dicewarsport.tournament.RoundActionLogEntry
import com.franklinharper.dicewarsport.tournament.RoundActionType
import com.franklinharper.dicewarsport.tournament.RoundConfig
import com.franklinharper.dicewarsport.tournament.RoundReplaySpec
import com.franklinharper.dicewarsport.tournament.RoundResult
import com.franklinharper.dicewarsport.tournament.RoundRunner
import com.franklinharper.dicewarsport.tournament.TournamentConfig
import com.franklinharper.dicewarsport.tournament.TournamentReportFormatter
import com.franklinharper.dicewarsport.tournament.TournamentResult
import com.franklinharper.dicewarsport.tournament.TournamentRunner
import com.franklinharper.dicewarsport.tournament.aggregateBotScores
import com.franklinharper.dicewarsport.tournament.deriveRoundSeed
import com.franklinharper.dicewarsport.tournament.rotatedLeft
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = runTournamentCli(
        args = args,
        stdout = { print(it) },
        stderr = { System.err.println(it) },
    )
    if (exitCode != 0) exitProcess(exitCode)
}

fun runTournamentCli(
    args: Array<String>,
    stdout: (String) -> Unit,
    stderr: (String) -> Unit,
): Int {
    if (args.firstOrNull() == "replay-round") {
        return runReplayRoundCli(args.drop(1).toTypedArray(), stdout, stderr)
    }
    val runArgs = if (args.firstOrNull() == "run-tournament") args.drop(1).toTypedArray() else args
    val options = try {
        CliOptions.parse(runArgs)
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}\n\n${usage()}")
        return 2
    }

    if (options.help) {
        stdout(usage() + "\n")
        return 0
    }

    val participants = try {
        options.botIds.map { botId ->
            BuiltInTournamentParticipants.byId[botId]
                ?: throw IllegalArgumentException("Unknown bot '$botId'. Available bots: ${BuiltInTournamentParticipants.byId.keys.sorted().joinToString(",")}")
        }
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}")
        return 2
    }

    val formatter = try {
        formatterFor(options.format)
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}")
        return 2
    }

    val parallelism = options.parallel
    val result = if (parallelism > 1) {
        ParallelTournamentRunner().run(
            TournamentConfig(
                participants = participants,
                rounds = options.rounds,
                seed = options.seed,
                maxActionsPerRound = options.maxActions,
                logFailedRounds = options.logFailedRounds,
                logAllRounds = options.logAllRounds,
            ),
            parallelism = parallelism,
        )
    } else {
        TournamentRunner().run(
            TournamentConfig(
                participants = participants,
                rounds = options.rounds,
                seed = options.seed,
                maxActionsPerRound = options.maxActions,
                logFailedRounds = options.logFailedRounds,
                logAllRounds = options.logAllRounds,
            ),
        )
    }
    val report = formatter.format(result.copy(debug = options.debug))

    if (options.outPath == null) {
        stdout(report)
    } else {
        File(options.outPath).also { file ->
            file.parentFile?.mkdirs()
            file.writeText(report)
        }
    }

    return 0
}

data class CliOptions(
    val botIds: List<String>,
    val rounds: Int,
    val seed: Int?,
    val format: String,
    val outPath: String?,
    val maxActions: Int,
    val parallel: Int,
    val logFailedRounds: Boolean = false,
    val logAllRounds: Boolean = false,
    val debug: Boolean = false,
    val help: Boolean = false,
) {
    companion object {
        fun parse(args: Array<String>): CliOptions {
            if (args.any { it == "--help" || it == "-h" }) {
                return CliOptions(
                    botIds = emptyList(),
                    rounds = 1,
                    seed = null,
                    format = "text",
                    outPath = null,
                    maxActions = 100_000,
                    parallel = defaultParallelism(),
                    debug = false,
                    help = true,
                )
            }

            val values = parseKeyValues(args)
            val botIds = values["bots"]
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Missing required --bots option")
            require(botIds.size >= 2) { "At least two bots are required" }

            val rounds = values["rounds"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Missing or invalid required --rounds option")
            require(rounds > 0) { "--rounds must be greater than zero" }

            val maxActions = values["max-actions"]?.toIntOrNull() ?: 100_000
            require(maxActions > 0) { "--max-actions must be greater than zero" }

            val parallel = values["parallel"]?.toIntOrNull()
                ?: values["parallel"]?.let { throw IllegalArgumentException("--parallel must be an integer") }
                ?: defaultParallelism()
            require(parallel > 0) { "--parallel must be greater than zero" }

            val seed = values["seed"]?.toIntOrNull()
                ?: values["seed"]?.let { throw IllegalArgumentException("--seed must be an integer") }

            return CliOptions(
                botIds = botIds,
                rounds = rounds,
                seed = seed,
                format = values["format"] ?: "text",
                outPath = values["out"],
                maxActions = maxActions,
                parallel = parallel,
                logFailedRounds = values["log-failed-rounds"] == "true",
                logAllRounds = values["log-all-rounds"] == "true",
                debug = values["debug"] == "true",
            )
        }

        private fun defaultParallelism(): Int = maxOf(1, Runtime.getRuntime().availableProcessors() - 1)

        private fun parseKeyValues(args: Array<String>): Map<String, String> {
            val values = mutableMapOf<String, String>()
            var index = 0
            while (index < args.size) {
                val arg = args[index]
                require(arg.startsWith("--")) { "Unexpected argument '$arg'" }
                val body = arg.removePrefix("--")
                if (body in setOf("log-failed-rounds", "log-all-rounds", "debug")) {
                    values[body] = "true"
                    index++
                } else if ('=' in body) {
                    val key = body.substringBefore('=')
                    val value = body.substringAfter('=')
                    require(key.isNotBlank()) { "Invalid option '$arg'" }
                    values[key] = value
                    index++
                } else {
                    require(index + 1 < args.size) { "Missing value for option '$arg'" }
                    val value = args[index + 1]
                    require(!value.startsWith("--")) { "Missing value for option '$arg'" }
                    values[body] = value
                    index += 2
                }
            }
            return values
        }
    }
}

private fun runReplayRoundCli(
    args: Array<String>,
    stdout: (String) -> Unit,
    stderr: (String) -> Unit,
): Int {
    val options = try {
        ReplayOptions.parse(args)
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}\n\n${replayUsage()}")
        return 2
    }

    val participants = try {
        options.seatIds.map { botId ->
            BuiltInTournamentParticipants.byId[botId]
                ?: throw IllegalArgumentException("Unknown bot '$botId'. Available bots: ${BuiltInTournamentParticipants.byId.keys.sorted().joinToString(",")}")
        }
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}")
        return 2
    }

    val stepper = BotRoundStepper()
    var state = stepper.initialState(
        RoundReplaySpec(
            roundSeed = options.roundSeed,
            participants = participants,
            maxActionsPerRound = options.maxActions,
        ),
    )

    val replayEntries = ArrayDeque<RoundActionLogEntry>()
    fun addReplayEntry(entry: RoundActionLogEntry) {
        replayEntries.addLast(entry)
        while (replayEntries.size > options.lastSteps) replayEntries.removeFirst()
    }

    while (true) {
        val step = stepper.step(state)
        state = step.state
        addReplayEntry(step.actionLogEntry)
        if (step.actionLogEntry.actionType.isEndEntry()) break
        if (state.completed || state.failed) {
            val terminalStep = stepper.step(state)
            state = terminalStep.state
            addReplayEntry(terminalStep.actionLogEntry)
            break
        }
    }

    val output = buildString {
        appendLine("Round replay")
        appendLine("Round seed: ${options.roundSeed}")
        appendLine("Seats: ${options.seatIds.joinToString(",")}")
        appendLine("Max actions: ${options.maxActions}")
        appendLine("Last steps: ${options.lastSteps}")
        appendLine()

        replayEntries.forEach { entry -> appendLine(RoundActionDebugFormatter.format(entry)) }

        if (state.completed) appendLine("Completed: winner=${state.winnerParticipantId}")
        if (state.failed) appendLine("Failed: ${state.failureReason}")
    }
    stdout(output)
    return 0
}

data class ReplayOptions(
    val roundSeed: Int,
    val seatIds: List<String>,
    val maxActions: Int,
    val lastSteps: Int,
) {
    companion object {
        fun parse(args: Array<String>): ReplayOptions {
            val values = parseReplayKeyValues(args)
            val roundSeed = values["round-seed"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Missing or invalid required --round-seed option")
            val seatIds = values["seats"]
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Missing required --seats option")
            require(seatIds.size >= 2) { "At least two seats are required" }
            val maxActions = values["max-actions"]?.toIntOrNull() ?: 100_000
            require(maxActions > 0) { "--max-actions must be greater than zero" }
            val allowedKeys = setOf("round-seed", "seats", "max-actions", "last-steps")
            val unknownKeys = values.keys - allowedKeys
            require(unknownKeys.isEmpty()) { "Unexpected option '--${unknownKeys.first()}'" }
            val lastSteps = values["last-steps"]?.toIntOrNull() ?: 50
            require(lastSteps > 0) { "--last-steps must be greater than zero" }
            return ReplayOptions(
                roundSeed = roundSeed,
                seatIds = seatIds,
                maxActions = maxActions,
                lastSteps = lastSteps,
            )
        }

        private fun parseReplayKeyValues(args: Array<String>): Map<String, String> {
            val values = mutableMapOf<String, String>()
            var index = 0
            while (index < args.size) {
                val arg = args[index]
                require(arg.startsWith("--")) { "Unexpected argument '$arg'" }
                val body = arg.removePrefix("--")
                if ('=' in body) {
                    values[body.substringBefore('=')] = body.substringAfter('=')
                    index++
                } else {
                    require(index + 1 < args.size) { "Missing value for option '$arg'" }
                    val value = args[index + 1]
                    require(!value.startsWith("--")) { "Missing value for option '$arg'" }
                    values[body] = value
                    index += 2
                }
            }
            return values
        }
    }
}

class ParallelTournamentRunner(
    private val roundRunner: RoundRunner = BotGameRunner(),
) {
    fun run(config: TournamentConfig, parallelism: Int): TournamentResult {
        val startedAt = kotlin.time.TimeSource.Monotonic.markNow()
        val effectiveSeed = config.seed ?: java.util.Random().nextInt()
        val executor = Executors.newFixedThreadPool(parallelism)

        try {
            // Build all round configs upfront (deterministic, no shared state)
            val roundConfigs = (1..config.rounds).map { roundNumber ->
                val seated = config.participants.rotatedLeft((roundNumber - 1) % config.participants.size)
                RoundConfig(
                    roundNumber = roundNumber,
                    participants = seated,
                    roundSeed = deriveRoundSeed(effectiveSeed, roundNumber),
                    maxActionsPerRound = config.maxActionsPerRound,
                    logActions = config.logAllRounds,
                )
            }

            // Submit all rounds to the pool
            val futures: List<Future<RoundResult>> = roundConfigs.map { rc ->
                executor.submit<RoundResult> {
                    val initial = roundRunner.runRound(rc)
                    if (config.logFailedRounds && !config.logAllRounds && !initial.completed) {
                        roundRunner.runRound(rc.copy(logActions = true))
                    } else {
                        initial
                    }
                }
            }

            // Collect results in order
            val roundResults = futures.map { it.get() }

            return TournamentResult(
                roundsRequested = config.rounds,
                roundsCompleted = roundResults.count { it.completed },
                roundsFailed = roundResults.count { !it.completed },
                seed = effectiveSeed,
                parallelism = parallelism,
                durationMillis = startedAt.elapsedNow().inWholeMilliseconds,
                botScores = aggregateBotScores(config.participants, roundResults),
                roundResults = roundResults,
            )
        } finally {
            executor.shutdown()
        }
    }
}

private fun RoundActionType.isEndEntry(): Boolean =
    this == RoundActionType.RoundFailed || this == RoundActionType.RoundWon

private fun formatterFor(format: String): TournamentReportFormatter = when (format.lowercase()) {
    "text", "plain" -> PlainTextTournamentReportFormatter
    "csv" -> CsvTournamentReportFormatter
    else -> throw IllegalArgumentException("Unknown format '$format'. Supported formats: text,csv")
}

private fun usage(): String = """
Dicewars bot tournament

Usage:
  run-tournament --bots rebel,turtle,bully --rounds 100 [options]

Options:
  --bots <ids>          Comma-separated bot IDs. Available: ${BuiltInTournamentParticipants.byId.keys.sorted().joinToString(",")}
  --rounds <count>      Number of rounds to attempt.
  --seed <int>          Optional seed for reproducible tournaments.
  --format <text|csv>   Output format. Default: text.
  --out <path>          Optional output path. Defaults to stdout.
  --max-actions <int>   Max actions per round. Default: 100000.
  --parallel <int>      Number of rounds to run concurrently. Default: CPU count minus 1, minimum 1.
  --log-failed-rounds   Capture action logs for failed rounds by rerunning failed rounds with the same round seed.
  --log-all-rounds      Capture action logs for every round.
  --debug               Include debug details such as failed-round repro information in reports.
  --help, -h            Show this help.

Replay:
  run-tournament replay-round --round-seed 123 --seats rebel,turtle --max-actions 100000 --last-steps 50
""".trimIndent()

private fun replayUsage(): String = """
Dicewars round replay

Usage:
  replay-round --round-seed <int> --seats rebel,turtle,bully [options]

Options:
  --round-seed <int>    Required round seed from a tournament report.
  --seats <ids>         Required comma-separated bot IDs in seated order.
  --max-actions <int>   Max actions for the round. Default: 100000.
  --last-steps <int>    Number of final replay entries to print. Default: 50.
""".trimIndent()
