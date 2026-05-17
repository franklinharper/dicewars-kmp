package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals

class AppVersionTest {
    @Test
    fun appVersionIsCurrentReleaseVersion() {
        assertEquals("0.1", APP_VERSION)
    }
}
