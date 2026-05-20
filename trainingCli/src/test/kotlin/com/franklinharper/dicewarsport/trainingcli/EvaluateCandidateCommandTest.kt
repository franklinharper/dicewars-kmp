package com.franklinharper.dicewarsport.trainingcli

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class EvaluateCandidateCommandTest {
    @Test
    fun evaluateCandidateReportAcceptsNeuralAndPrintsElimination() {
        val report = Files.createTempFile("neural-candidate", ".txt")
        Files.writeString(report, acceptedReport())
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val exitCode = runTrainingCli(
            args = arrayOf("evaluate-candidate-report", "--report", report.toString()),
            stdout = { stdout.append(it) },
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(0, exitCode, stderr.toString())
        assertContains(stdout.toString(), "Candidate accepted: true")
        assertContains(stdout.toString(), "Eliminate: bully")
    }

    @Test
    fun evaluateCandidateReportRejectsNeuralLast() {
        val report = Files.createTempFile("neural-candidate", ".txt")
        Files.writeString(report, rejectedReport())
        val stdout = StringBuilder()

        val exitCode = runTrainingCli(
            args = arrayOf("evaluate-candidate-report", "--report", report.toString()),
            stdout = { stdout.append(it) },
            stderr = {},
        )

        assertEquals(1, exitCode)
        assertContains(stdout.toString(), "Candidate accepted: false")
        assertContains(stdout.toString(), "Reason: neural finished last")
    }

    private fun acceptedReport(): String = """
        Scores:
        1. Terminator 2        52,531 pts  2,503 wins
        2. Neural              46,410 pts    731 wins
        3. Max                 44,641 pts  1,714 wins
        4. Bully               30,661 pts    718 wins
    """.trimIndent()

    private fun rejectedReport(): String = """
        Scores:
        1. Terminator 2        52,531 pts  2,503 wins
        2. Max                 44,641 pts  1,714 wins
        3. Bully               30,661 pts    718 wins
        4. Neural              20,000 pts    100 wins
    """.trimIndent()
}
