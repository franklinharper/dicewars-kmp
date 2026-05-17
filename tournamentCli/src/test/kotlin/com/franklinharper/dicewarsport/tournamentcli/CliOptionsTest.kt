package com.franklinharper.dicewarsport.tournamentcli

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CliOptionsTest {
    @Test
    fun parsesRequiredAndOptionalArguments() {
        val options = CliOptions.parse(
            arrayOf(
                "--bots", "target-leader,cautious,attack-when-stronger",
                "--rounds", "25",
                "--seed", "42",
                "--format", "csv",
                "--out", "reports/out.csv",
                "--max-actions", "1234",
            ),
        )

        assertEquals(listOf("target-leader", "cautious", "attack-when-stronger"), options.botIds)
        assertEquals(25, options.rounds)
        assertEquals(42, options.seed)
        assertEquals("csv", options.format)
        assertEquals("reports/out.csv", options.outPath)
        assertEquals(1234, options.maxActions)
    }

    @Test
    fun supportsEqualsStyleArguments() {
        val options = CliOptions.parse(arrayOf("--bots=target-leader,cautious", "--rounds=3"))

        assertEquals(listOf("target-leader", "cautious"), options.botIds)
        assertEquals(3, options.rounds)
        assertEquals("text", options.format)
    }

    @Test
    fun invalidArgumentsReturnUsefulErrors() {
        assertFailsWith<IllegalArgumentException> {
            CliOptions.parse(arrayOf("--bots", "target-leader", "--rounds", "0"))
        }
    }

    @Test
    fun cliPrintsTextReportToStdout() {
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val exitCode = runTournamentCli(
            args = arrayOf(
                "--bots", "target-leader,cautious",
                "--rounds", "1",
                "--seed", "1",
                "--max-actions", "1",
                "--format", "text",
            ),
            stdout = { stdout.append(it) },
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(0, exitCode, stderr.toString())
        assertContains(stdout.toString(), "Dicewars Bot Tournament")
        assertContains(stdout.toString(), "Rounds requested: 1")
    }

    @Test
    fun parsesActionLogFlags() {
        val failedOnly = CliOptions.parse(arrayOf("--bots", "target-leader,cautious", "--rounds", "1", "--log-failed-rounds"))
        val allRounds = CliOptions.parse(arrayOf("--bots", "target-leader,cautious", "--rounds", "1", "--log-all-rounds"))

        assertTrue(failedOnly.logFailedRounds)
        assertTrue(allRounds.logAllRounds)
    }

    @Test
    fun replayOptionsParseLastSteps() {
        val options = ReplayOptions.parse(
            arrayOf(
                "--round-seed", "123",
                "--seats", "target-leader,cautious",
                "--max-actions", "100",
                "--last-steps", "5",
            ),
        )

        assertEquals(123, options.roundSeed)
        assertEquals(listOf("target-leader", "cautious"), options.seatIds)
        assertEquals(100, options.maxActions)
        assertEquals(5, options.lastSteps)
    }

    @Test
    fun replayOptionsRejectRemovedFlags() {
        assertFailsWith<IllegalArgumentException> {
            ReplayOptions.parse(arrayOf("--round-seed", "123", "--seats", "target-leader,cautious", "--steps", "5"))
        }
        assertFailsWith<IllegalArgumentException> {
            ReplayOptions.parse(arrayOf("--round-seed", "123", "--seats", "target-leader,cautious", "--until-failed"))
        }
        assertFailsWith<IllegalArgumentException> {
            ReplayOptions.parse(arrayOf("--round-seed", "123", "--seats", "target-leader,cautious", "--until-complete"))
        }
    }

    @Test
    fun replayRoundSubcommandPrintsLastStepsOutput() {
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val exitCode = runTournamentCli(
            args = arrayOf(
                "replay-round",
                "--round-seed", "123",
                "--seats", "target-leader,cautious",
                "--max-actions", "5",
                "--last-steps", "2",
            ),
            stdout = { stdout.append(it) },
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(0, exitCode, stderr.toString())
        assertContains(stdout.toString(), "Round replay")
        assertContains(stdout.toString(), "Round seed: 123")
        assertContains(stdout.toString(), "Seats: target-leader,cautious")
        assertContains(stdout.toString(), "End:")
        assertTrue(!stdout.toString().contains("Step 1:"), stdout.toString())
    }

    @Test
    fun cliReportsGeneratedSeedWhenSeedIsOmitted() {
        val stdout = StringBuilder()

        val exitCode = runTournamentCli(
            args = arrayOf(
                "--bots", "target-leader,cautious",
                "--rounds", "1",
                "--max-actions", "1",
                "--format", "text",
            ),
            stdout = { stdout.append(it) },
            stderr = {},
        )

        assertEquals(0, exitCode)
        assertTrue(Regex("Seed: -?\\d+").containsMatchIn(stdout.toString()))
        assertTrue(!stdout.toString().contains("Seed: random"))
    }

    @Test
    fun unknownBotReturnsErrorExitCode() {
        val stderr = StringBuilder()

        val exitCode = runTournamentCli(
            args = arrayOf("--bots", "target-leader,nope", "--rounds", "1"),
            stdout = {},
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(2, exitCode)
        assertTrue(stderr.toString().contains("Unknown bot 'nope'"))
    }
}
