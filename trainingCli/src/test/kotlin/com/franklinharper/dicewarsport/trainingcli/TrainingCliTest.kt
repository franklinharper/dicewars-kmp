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
    }
}
