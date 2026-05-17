package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals

class DicewarsAppRoutingTest {

    @Test
    fun routedScreensCoverEveryScreenState() {
        assertEquals(DicewarsScreen.entries.toSet(), routedDicewarsScreens())
    }
}
