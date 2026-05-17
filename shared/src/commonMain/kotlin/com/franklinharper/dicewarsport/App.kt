package com.franklinharper.dicewarsport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.franklinharper.dicewarsport.presentation.components.ScreenScaffold
import com.franklinharper.dicewarsport.screen.*
import com.franklinharper.dicewarsport.theme.AppTheme

@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
    soundPlayer: SoundPlayer = NoOpSoundPlayer(),
    debugPreferences: DebugPreferences = NoOpDebugPreferences(),
) = AppTheme(onThemeChanged) {
    val random = remember { KotlinRandomSource() }
    val reducer = remember { GameReducer(random, debugPreferences = debugPreferences) }
    var state by remember {
        mutableStateOf(GameUiState(screen = DicewarsScreen.Loading, game = DicewarsGame()))
    }

    DicewarsApp(
        state = state,
        onAction = { action ->
            val result = reducer.reduce(state, action)
            state = result.state
            if (state.soundEnabled) {
                result.soundEvents.forEach { soundPlayer.play(it) }
            }
        },
    )
}

@Composable
fun DicewarsApp(
    state: GameUiState,
    onAction: (GameAction) -> Unit = {},
) {
    when (state.screen) {
        DicewarsScreen.Loading -> LoadingScreen(onAction)
        DicewarsScreen.Title -> TitleScreen(state, onAction)
        DicewarsScreen.MapPreview -> MapPreviewScreen(state, onAction)
        DicewarsScreen.HumanTurn -> GameBoardScreen(state, onAction, title = "Your turn")
        DicewarsScreen.AiTurn -> GameBoardScreen(state, onAction, title = "AI turn")
        DicewarsScreen.GameOver -> GameOverScreen(state, onAction)
        DicewarsScreen.Win -> WinScreen(state, onAction)
        DicewarsScreen.Debug -> DebugScreen(state, onAction)
    }
}

