package com.franklinharper.dicewarsport.ai.neural

import android.content.Context

object AndroidNeuralRuntime {
    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun requireContext(): Context =
        appContext ?: error(
            "AndroidNeuralRuntime is not initialized. Call AndroidNeuralRuntime.initialize(applicationContext) " +
                "from AppActivity.onCreate before creating neural bots.",
        )
}
