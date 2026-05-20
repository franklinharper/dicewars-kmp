package com.franklinharper.dicewarsport.trainingcli

import java.nio.file.Files
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TrainingCliTest {
    @Test
    fun benchmarkSimulatorOptionsParseDefaultsAndOverrides() {
        val options = TrainingCliOptions.parse(
            arrayOf(
                "benchmark-simulator",
                "--rounds", "25",
                "--seed", "42",
                "--bots", "bully,turtle",
                "--parallel", "2",
            ),
        )

        assertEquals(TrainingCommand.BenchmarkSimulator, options.command)
        assertEquals(25, options.rounds)
        assertEquals(42, options.seed)
        assertEquals(listOf("bully", "turtle"), options.botIds)
        assertEquals(2, options.parallel)
    }

    @Test
    fun rejectsUnknownCommand() {
        assertFailsWith<IllegalArgumentException> {
            TrainingCliOptions.parse(arrayOf("unknown-command"))
        }
    }

    @Test
    fun rejectsInvalidBotIdDuringRun() {
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val exitCode = runTrainingCli(
            args = arrayOf("benchmark-simulator", "--rounds", "1", "--bots", "not-a-bot,turtle"),
            stdout = { stdout.append(it) },
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(2, exitCode)
        assertContains(stderr.toString(), "Unknown bot 'not-a-bot'")
    }

    @Test
    fun benchmarkSimulatorRunsSmallFixedTournament() {
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val exitCode = runTrainingCli(
            args = arrayOf(
                "benchmark-simulator",
                "--rounds", "1",
                "--seed", "1",
                "--bots", "bully,turtle",
                "--parallel", "1",
            ),
            stdout = { stdout.append(it) },
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(0, exitCode, stderr.toString())
        assertContains(stdout.toString(), "Training simulator benchmark")
        assertContains(stdout.toString(), "Rounds requested: 1")
        assertContains(stdout.toString(), "Rounds completed:")
        assertContains(stdout.toString(), "Rounds/sec:")
    }

    @Test
    fun generateImitationDataWritesGzippedJsonlRecords() {
        val out = Files.createTempFile("dicewars-imitation", ".jsonl.gz")
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val exitCode = runTrainingCli(
            args = arrayOf(
                "generate-imitation-data",
                "--rounds", "1",
                "--seed", "1",
                "--bots", "bully,turtle",
                "--out", out.toString(),
                "--max-actions", "200",
            ),
            stdout = { stdout.append(it) },
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(0, exitCode, stderr.toString())
        assertContains(stdout.toString(), "Imitation data written")
        val lines = GZIPInputStream(Files.newInputStream(out)).bufferedReader().readLines()
        assertTrue(lines.isNotEmpty(), "expected at least one training record")
        val first = lines.first()
        assertContains(first, "\"schema_version\":1")
        assertContains(first, "\"encoder_version\":1")
        assertContains(first, "\"action_space_version\":1")
        assertContains(first, "\"chosen_action_index\":")
        assertContains(first, "\"legal_action_mask\":")
        assertContains(first, "\"policy_weight\":")
        assertContains(first, "\"value_target\":")
    }

    @Test
    fun generateImitationDataRequiresOutputPath() {
        assertFailsWith<IllegalArgumentException> {
            TrainingCliOptions.parse(arrayOf("generate-imitation-data", "--rounds", "1"))
        }
    }

    @Test
    fun validateModelMetadataAcceptsCurrentModelDirectory() {
        val root = Files.createTempDirectory("neural-models")
        val version = root.resolve("neuralbot-20260520-1430-a1b2c3d")
        Files.createDirectories(version)
        Files.writeString(root.resolve("current.txt"), "neuralbot-20260520-1430-a1b2c3d\n")
        Files.writeString(version.resolve("model.metadata.json"), validMetadataJson())
        Files.write(version.resolve("model.onnx"), byteArrayOf(1, 2, 3))
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val exitCode = runTrainingCli(
            args = arrayOf("validate-model-metadata", "--models-dir", root.toString()),
            stdout = { stdout.append(it) },
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(0, exitCode, stderr.toString())
        assertContains(stdout.toString(), "Model metadata valid")
        assertContains(stdout.toString(), "neuralbot-20260520-1430-a1b2c3d")
    }

    @Test
    fun validateModelMetadataFailsForMissingCurrentPointer() {
        val root = Files.createTempDirectory("neural-models")
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val exitCode = runTrainingCli(
            args = arrayOf("validate-model-metadata", "--models-dir", root.toString()),
            stdout = { stdout.append(it) },
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(2, exitCode)
        assertContains(stderr.toString(), "Missing current.txt")
    }

    @Test
    fun validateModelMetadataFailsForOversizedModel() {
        val root = Files.createTempDirectory("neural-models")
        val version = root.resolve("neuralbot-20260520-1430-a1b2c3d")
        Files.createDirectories(version)
        Files.writeString(root.resolve("current.txt"), "neuralbot-20260520-1430-a1b2c3d")
        Files.writeString(version.resolve("model.metadata.json"), validMetadataJson())
        Files.newOutputStream(version.resolve("model.onnx")).use { out ->
            out.write(ByteArray(25 * 1024 * 1024 + 1))
        }
        val stderr = StringBuilder()

        val exitCode = runTrainingCli(
            args = arrayOf("validate-model-metadata", "--models-dir", root.toString()),
            stdout = {},
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(2, exitCode)
        assertContains(stderr.toString(), "exceeds hard max")
    }

    fun helpPrintsUsage() {
        val stdout = StringBuilder()

        val exitCode = runTrainingCli(
            args = arrayOf("--help"),
            stdout = { stdout.append(it) },
            stderr = {},
        )

        assertEquals(0, exitCode)
        assertContains(stdout.toString(), "Dicewars neural training CLI")
        assertContains(stdout.toString(), "benchmark-simulator")
        assertContains(stdout.toString(), "generate-imitation-data")
        assertContains(stdout.toString(), "validate-model-metadata")
    }

    private fun validMetadataJson(): String = """
        {
          "model_id": "neuralbot-20260520-1430-a1b2c3d",
          "git_commit": "a1b2c3d",
          "training_config_hash": "cfg123",
          "training_seed": 123456789,
          "environment_version": "encoder-v1-action-v1",
          "map_dataset_hash": null,
          "self_play_game_count": 250000,
          "optimizer_settings": "adamw lr=0.001",
          "network_architecture": "gnn hidden=64 layers=4",
          "checkpoint_path": "checkpoints/model.pt",
          "export_timestamp": "2026-05-20T14:30:00Z",
          "onnx_opset": 18,
          "quantization": "none",
          "evaluation_tournament_results": "10k score=12345 wins=678"
        }
    """.trimIndent()
}
