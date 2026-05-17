package com.franklinharper.dicewarsport.androidApp

import android.content.Context
import com.franklinharper.dicewarsport.DebugPreferences

class AndroidDebugPreferences(context: Context) : DebugPreferences {

    private val prefs = context.getSharedPreferences("dicewars_debug", Context.MODE_PRIVATE)

    override fun isDebugMode(): Boolean = prefs.getBoolean("debug_mode", false)

    override fun setDebugMode(enabled: Boolean) {
        prefs.edit().putBoolean("debug_mode", enabled).apply()
    }
}
