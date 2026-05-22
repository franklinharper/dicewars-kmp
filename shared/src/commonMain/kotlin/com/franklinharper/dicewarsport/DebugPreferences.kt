package com.franklinharper.dicewarsport

interface DebugPreferences {
    fun isDebugMode(): Boolean
    fun setDebugMode(enabled: Boolean)
    fun selectedBotIds(): Set<String>
    fun setSelectedBotIds(ids: Set<String>)
}

class NoOpDebugPreferences : DebugPreferences {
    override fun isDebugMode(): Boolean = false
    override fun setDebugMode(enabled: Boolean) {}
    override fun selectedBotIds(): Set<String> = emptySet()
    override fun setSelectedBotIds(ids: Set<String>) {}
}
