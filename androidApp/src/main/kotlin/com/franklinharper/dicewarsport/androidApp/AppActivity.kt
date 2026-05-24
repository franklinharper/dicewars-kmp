package com.franklinharper.dicewarsport.androidApp

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.franklinharper.dicewarsport.App
import com.franklinharper.dicewarsport.ai.neural.AndroidNeuralRuntime


class AppActivity : ComponentActivity() {

    private var soundPlayer: AndroidSoundPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AndroidNeuralRuntime.initialize(applicationContext)
        val player = AndroidSoundPlayer(this)
        soundPlayer = player
        val debugPrefs = AndroidDebugPreferences(this)
        val statsStore = AndroidPlayerStatsStore(this)
        val gameStateStore = AndroidGameStateStore(this)
        setContent {
            App(
                onThemeChanged = { ThemeChanged(it) },
                soundPlayer = player,
                debugPreferences = debugPrefs,
                playerStatsStore = statsStore,
                gameStateStore = gameStateStore,
                backGestureHandler = { enabled, onBack -> BackHandler(enabled = enabled, onBack = onBack) },
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPlayer?.release()
        soundPlayer = null
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isDark
            isAppearanceLightNavigationBars = isDark
        }
    }
}
