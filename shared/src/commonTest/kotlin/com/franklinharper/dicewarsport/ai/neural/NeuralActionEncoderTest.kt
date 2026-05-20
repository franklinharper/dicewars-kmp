package com.franklinharper.dicewarsport.ai.neural

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.ai.Move
import com.franklinharper.dicewarsport.aiGame
import com.franklinharper.dicewarsport.isLegalAttack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NeuralActionEncoderTest {
    @Test
    fun actionSpaceHasOneAttackSlotPerFromToPairPlusEndTurn() {
        assertEquals(DicewarsGame.AREA_MAX * DicewarsGame.AREA_MAX + 1, NeuralActionEncoder.ACTION_COUNT)
        assertEquals(DicewarsGame.AREA_MAX * DicewarsGame.AREA_MAX, NeuralActionEncoder.END_TURN_INDEX)
    }

    @Test
    fun mapsMoveToActionIndexAndBack() {
        val move = Move(from = 1, to = 2)
        val index = NeuralActionEncoder.actionIndexFor(move)

        assertEquals(34, index)
        assertEquals(move, NeuralActionEncoder.moveForActionIndex(index))
    }

    @Test
    fun mapsHighestAreaMoveToActionIndexAndBack() {
        val move = Move(from = 31, to = 30)
        val index = NeuralActionEncoder.actionIndexFor(move)

        assertEquals(1022, index)
        assertEquals(move, NeuralActionEncoder.moveForActionIndex(index))
    }

    @Test
    fun mapsNullMoveToEndTurnAction() {
        assertEquals(NeuralActionEncoder.END_TURN_INDEX, NeuralActionEncoder.actionIndexFor(null))
        assertEquals(null, NeuralActionEncoder.moveForActionIndex(NeuralActionEncoder.END_TURN_INDEX))
    }

    @Test
    fun rejectsOutOfRangeMovesAndActionIndices() {
        assertFailsWith<IllegalArgumentException> { NeuralActionEncoder.actionIndexFor(Move(-1, 2)) }
        assertFailsWith<IllegalArgumentException> { NeuralActionEncoder.actionIndexFor(Move(1, DicewarsGame.AREA_MAX)) }
        assertFailsWith<IllegalArgumentException> { NeuralActionEncoder.moveForActionIndex(-1) }
        assertFailsWith<IllegalArgumentException> { NeuralActionEncoder.moveForActionIndex(NeuralActionEncoder.ACTION_COUNT) }
    }

    @Test
    fun legalActionMaskIncludesOnlyLegalAttacksAndEndTurn() {
        val game = aiGame()
        val mask = NeuralActionEncoder.legalActionMask(game, actorPlayer = 0)

        assertEquals(NeuralActionEncoder.ACTION_COUNT, mask.size)
        assertTrue(mask[NeuralActionEncoder.actionIndexFor(Move(1, 2))])
        assertTrue(mask[NeuralActionEncoder.actionIndexFor(Move(1, 3))])
        assertTrue(mask[NeuralActionEncoder.actionIndexFor(Move(4, 2))])
        assertTrue(mask[NeuralActionEncoder.END_TURN_INDEX])

        assertFalse(mask[NeuralActionEncoder.actionIndexFor(Move(0, 1))], "area 0 is not a real source")
        assertFalse(mask[NeuralActionEncoder.actionIndexFor(Move(1, 0))], "area 0 is not a real target")
        assertFalse(mask[NeuralActionEncoder.actionIndexFor(Move(1, 4))], "cannot attack own territory")
        assertFalse(mask[NeuralActionEncoder.actionIndexFor(Move(4, 3))], "cannot attack non-adjacent territory")
        assertFalse(mask[NeuralActionEncoder.actionIndexFor(Move(2, 1))], "actor cannot attack from enemy territory")
    }

    @Test
    fun legalActionMaskMatchesGameLegalityForEveryAttackAction() {
        val game = aiGame()
        val actor = game.currentPlayer()
        val mask = NeuralActionEncoder.legalActionMask(game, actorPlayer = actor)

        for (from in 0 until DicewarsGame.AREA_MAX) {
            for (to in 0 until DicewarsGame.AREA_MAX) {
                val move = Move(from, to)
                assertEquals(
                    game.isLegalAttack(from, to, actor),
                    mask[NeuralActionEncoder.actionIndexFor(move)],
                    "mask mismatch for $move",
                )
            }
        }
    }
}
