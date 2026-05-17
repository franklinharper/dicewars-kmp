package com.franklinharper.dicewarsport

interface DebugPreferences {
    fun isDebugMode(): Boolean
    fun setDebugMode(enabled: Boolean)
}

class NoOpDebugPreferences : DebugPreferences {
    override fun isDebugMode(): Boolean = false
    override fun setDebugMode(enabled: Boolean) {}
}
