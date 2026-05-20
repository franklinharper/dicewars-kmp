package com.franklinharper.dicewarsport.trainingcli

import com.franklinharper.dicewarsport.tournament.BuiltInTournamentParticipants
import com.franklinharper.dicewarsport.tournament.FastBotGameRunner
import com.franklinharper.dicewarsport.tournament.TournamentConfig
import com.franklinharper.dicewarsport.tournament.TournamentRunner
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = runTrainingCli(
        args = args,
        stdout = { print(it) },
        stderr = { System.err.println(it) },
    )
    if (exitCode != 0) exitProcess(exitCode)
}

fun runTrainingCli(
    args: Array<String>,
    stdout: (String) -> Unit,
    stderr: (String) -> Unit,
): Int {
    val options = try {
        TrainingCliOptions.parse(args)
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}\n\n${usage()}")
        return 2
    }

    if (options.help) {
        stdout(usage() + "\n")
        return 0
    }

    return when (options.command) {
        TrainingCommand.BenchmarkSimulator -> runBenchmarkSimulator(options, stdout, stderr)
        TrainingCommand.GenerateImitationData -> runGenerateImitationData(options, stdout, stderr)
        TrainingCommand.ValidateModelMetadata -> runValidateModelMetadata(options, stdout, stderr)
        TrainingCommand.EvaluateCandidateReport -> runEvaluateCandidateReport(options, stdout, stderr)
    }
}

private fun runBenchmarkSimulator(
    options: TrainingCliOptions,
    stdout: (String) -> Unit,
    stderr: (String) -> Unit,
): Int {
    val participants = try {
        options.botIds.map { botId ->
            BuiltInTournamentParticipants.byId[botId]
                ?: throw IllegalArgumentException("Unknown bot '$botId'. Available bots: ${BuiltInTournamentParticipants.byId.keys.sorted().joinToString(",")}")
        }
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}")
        return 2
    }

    val result = TournamentRunner(FastBotGameRunner()).run(
        TournamentConfig(
            participants = participants,
            rounds = options.rounds,
            seed = options.seed,
            maxActionsPerRound = options.maxActions,
        ),
    )
    val seconds = result.durationMillis.coerceAtLeast(1).toDouble() / 1000.0
    val roundsPerSecond = result.roundsRequested / seconds

    stdout(buildString {
        appendLine("Training simulator benchmark")
        appendLine("Rounds requested: ${result.roundsRequested}")
        appendLine("Rounds completed: ${result.roundsCompleted}")
        appendLine("Rounds failed: ${result.roundsFailed}")
        appendLine("Seed: ${result.seed}")
        appendLine("Parallel processes: 1")
        appendLine("Duration: ${result.durationMillis}ms")
        appendLine("Rounds/sec: ${roundsPerSecond.formatOneDecimal()}")
    })
    return 0
}

private fun runGenerateImitationData(
    options: TrainingCliOptions,
    stdout: (String) -> Unit,
    stderr: (String) -> Unit,
): Int {
    val participants = try {
        options.botIds.map { botId ->
            BuiltInTournamentParticipants.byId[botId]
                ?: throw IllegalArgumentException("Unknown bot '$botId'. Available bots: ${BuiltInTournamentParticipants.byId.keys.sorted().joinToString(",")}")
        }
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}")
        return 2
    }

    val outPath = options.outPath ?: run {
        stderr("Error: --out is required for generate-imitation-data")
        return 2
    }
    val result = ImitationDataGenerator().generate(
        participants = participants,
        rounds = options.rounds,
        seed = options.seed,
        maxActionsPerRound = options.maxActions,
        outPath = java.io.File(outPath),
    )
    stdout("Imitation data written: ${result.recordsWritten} records to $outPath\n")
    return 0
}

private fun runEvaluateCandidateReport(
    options: TrainingCliOptions,
    stdout: (String) -> Unit,
    stderr: (String) -> Unit,
): Int {
    val reportPath = options.reportPath ?: run {
        stderr("Error: --report is required for evaluate-candidate-report")
        return 2
    }
    return try {
        val scores = TournamentReportParser.parseBotScores(java.io.File(reportPath).readText())
        val decision = NeuralCandidateEvaluator.evaluate(scores)
        stdout(buildString {
            appendLine("Candidate accepted: ${decision.accepted}")
            appendLine("Reason: ${decision.reason}")
            appendLine("Eliminate: ${decision.eliminatedBotId ?: "none"}")
        })
        if (decision.accepted) 0 else 1
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}")
        2
    }
}

private fun runValidateModelMetadata(
    options: TrainingCliOptions,
    stdout: (String) -> Unit,
    stderr: (String) -> Unit,
): Int {
    val modelsDir = options.modelsDir ?: run {
        stderr("Error: --models-dir is required for validate-model-metadata")
        return 2
    }
    return try {
        val result = ModelArtifactValidator().validate(java.io.File(modelsDir))
        stdout("Model metadata valid: ${result.modelId} (${result.modelBytes} bytes)\n")
        if (result.warning != null) stdout("Warning: ${result.warning}\n")
        0
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}")
        2
    }
}

data class TrainingCliOptions(
    val command: TrainingCommand,
    val rounds: Int = 1000,
    val seed: Int? = null,
    val botIds: List<String> = listOf("bully", "emperor", "frontier-commander", "max", "optimus", "terminator2", "turtle"),
    val parallel: Int = 1,
    val maxActions: Int = 100_000,
    val outPath: String? = null,
    val modelsDir: String? = null,
    val reportPath: String? = null,
    val help: Boolean = false,
) {
    companion object {
        fun parse(args: Array<String>): TrainingCliOptions {
            if (args.isEmpty() || args.any { it == "--help" || it == "-h" }) {
                return TrainingCliOptions(command = TrainingCommand.BenchmarkSimulator, help = true)
            }
            val command = TrainingCommand.fromCliName(args.first())
            val values = parseKeyValues(args.drop(1).toTypedArray())
            val rounds = values["rounds"]?.toIntOrNull() ?: 1000
            require(rounds > 0) { "--rounds must be greater than zero" }
            val seed = values["seed"]?.toIntOrNull()
                ?: values["seed"]?.let { throw IllegalArgumentException("--seed must be an integer") }
            val parallel = values["parallel"]?.toIntOrNull() ?: 1
            require(parallel > 0) { "--parallel must be greater than zero" }
            val maxActions = values["max-actions"]?.toIntOrNull() ?: 100_000
            require(maxActions > 0) { "--max-actions must be greater than zero" }
            val botIds = values["bots"]
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: listOf("bully", "emperor", "frontier-commander", "max", "optimus", "terminator2", "turtle")
            require(botIds.size >= 2) { "At least two bots are required" }
            val outPath = values["out"]
            val modelsDir = values["models-dir"]
            val reportPath = values["report"]
            require(command != TrainingCommand.GenerateImitationData || !outPath.isNullOrBlank()) {
                "--out is required for generate-imitation-data"
            }
            require(command != TrainingCommand.ValidateModelMetadata || !modelsDir.isNullOrBlank()) {
                "--models-dir is required for validate-model-metadata"
            }
            require(command != TrainingCommand.EvaluateCandidateReport || !reportPath.isNullOrBlank()) {
                "--report is required for evaluate-candidate-report"
            }
            return TrainingCliOptions(
                command = command,
                rounds = rounds,
                seed = seed,
                botIds = botIds,
                parallel = parallel,
                maxActions = maxActions,
                outPath = outPath,
                modelsDir = modelsDir,
                reportPath = reportPath,
            )
        }

        private fun parseKeyValues(args: Array<String>): Map<String, String> {
            val values = mutableMapOf<String, String>()
            var index = 0
            while (index < args.size) {
                val arg = args[index]
                require(arg.startsWith("--")) { "Unexpected argument '$arg'" }
                val body = arg.removePrefix("--")
                if ('=' in body) {
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

enum class TrainingCommand(val cliName: String) {
    BenchmarkSimulator("benchmark-simulator"),
    GenerateImitationData("generate-imitation-data"),
    ValidateModelMetadata("validate-model-metadata"),
    EvaluateCandidateReport("evaluate-candidate-report");

    companion object {
        fun fromCliName(value: String): TrainingCommand = entries.firstOrNull { it.cliName == value }
            ?: throw IllegalArgumentException("Unknown command '$value'")
    }
}

private fun usage(): String = """
Dicewars neural training CLI

Usage:
  training-cli benchmark-simulator [options]
  training-cli generate-imitation-data --out <path.jsonl.gz> [options]
  training-cli validate-model-metadata --models-dir <models/neuralbot>
  training-cli evaluate-candidate-report --report <tournament-report.txt>

Commands:
  benchmark-simulator        Run a small headless simulator benchmark.
  generate-imitation-data    Generate gzip-compressed JSONL imitation examples.
  validate-model-metadata    Validate current.txt, model metadata, and model size limits.
  evaluate-candidate-report  Apply neural candidate acceptance rule to a tournament report.

Options:
  --rounds <count>       Number of rounds. Default: 1000.
  --seed <int>           Optional tournament seed.
  --bots <ids>           Comma-separated bot ids.
  --parallel <count>     Reserved for parallel benchmark workers. Default: 1.
  --max-actions <count>  Max actions per round. Default: 100000.
  --out <path>           Output path for commands that write files.
  --models-dir <path>    Model artifact root containing current.txt.
  --report <path>        Tournament report path for candidate evaluation.
  --help, -h             Show this help.
""".trimIndent()

private fun Double.formatOneDecimal(): String = ((this * 10.0).toInt() / 10.0).toString()
