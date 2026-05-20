package com.franklinharper.dicewarsport.trainingcli

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    }
}
