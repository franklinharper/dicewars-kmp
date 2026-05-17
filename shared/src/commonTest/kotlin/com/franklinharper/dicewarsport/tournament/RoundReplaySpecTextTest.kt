package com.franklinharper.dicewarsport.tournament

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoundReplaySpecTextTest {
    @Test
    fun formatsAndParsesPasteableReplaySpecBlock() {
        val spec = RoundReplaySpecText(
            roundSeed = 1502463084,
            seatIds = listOf("target-leader", "cautious"),
            maxActions = 2,
            lastSteps = 50,
        )

        val text = RoundReplaySpecParser.format(spec)

        assertEquals(
            """
            ROUND_REPLAY_SPEC
            roundSeed=1502463084
            seats=target-leader,cautious
            maxActions=2
            lastSteps=50
            END_ROUND_REPLAY_SPEC
            """.trimIndent(),
            text,
        )
        assertEquals(spec, RoundReplaySpecParser.parse(text))
    }

    @Test
    fun parsesSpecBlockEmbeddedInLargerReport() {
        val parsed = RoundReplaySpecParser.parse(
            """
            Failed rounds:
            ROUND_REPLAY_SPEC
            roundSeed=7
            seats=a,b,c
            maxActions=99
            END_ROUND_REPLAY_SPEC
            CLI: ./scripts/replay-round ...
            """.trimIndent(),
        )

        assertEquals(RoundReplaySpecText(roundSeed = 7, seatIds = listOf("a", "b", "c"), maxActions = 99), parsed)
    }

    @Test
    fun invalidSpecThrows() {
        assertFailsWith<IllegalArgumentException> {
            RoundReplaySpecParser.parse("roundSeed=7")
        }
    }
}
