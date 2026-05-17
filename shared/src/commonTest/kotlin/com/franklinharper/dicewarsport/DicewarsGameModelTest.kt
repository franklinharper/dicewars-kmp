package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals

class DicewarsGameModelTest {

    @Test
    fun cellNeighborCalculationMatchesJsNextCel() {
        assertEquals(-1, DicewarsGame.nextCell(0, 0))
        assertEquals(1, DicewarsGame.nextCell(0, 1))
        assertEquals(28, DicewarsGame.nextCell(0, 2))
        assertEquals(-1, DicewarsGame.nextCell(0, 3))
        assertEquals(-1, DicewarsGame.nextCell(0, 4))
        assertEquals(-1, DicewarsGame.nextCell(0, 5))

        assertEquals(1, DicewarsGame.nextCell(28, 0))
        assertEquals(29, DicewarsGame.nextCell(28, 1))
        assertEquals(57, DicewarsGame.nextCell(28, 2))
        assertEquals(56, DicewarsGame.nextCell(28, 3))
        assertEquals(-1, DicewarsGame.nextCell(28, 4))
        assertEquals(0, DicewarsGame.nextCell(28, 5))

        assertEquals(-1, DicewarsGame.nextCell(55, 0))
        assertEquals(-1, DicewarsGame.nextCell(55, 1))
        assertEquals(-1, DicewarsGame.nextCell(55, 2))
        assertEquals(83, DicewarsGame.nextCell(55, 3))
        assertEquals(54, DicewarsGame.nextCell(55, 4))
        assertEquals(27, DicewarsGame.nextCell(55, 5))
    }

    @Test
    fun precomputedCellNeighborsUseNextCell() {
        val game = DicewarsGame()

        assertEquals((0 until 6).map { DicewarsGame.nextCell(29, it) }, game.cellNeighbors[29].directions)
    }
}
