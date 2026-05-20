package com.franklinharper.dicewarsport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
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
    playerStatsStore: PlayerStatsStore = InMemoryPlayerStatsStore(),
    backGestureHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit = { _, _ -> },
) = AppTheme(onThemeChanged) {
    val random = remember { KotlinRandomSource() }
    val reducer = remember { GameReducer(random, debugPreferences = debugPreferences, playerStatsStore = playerStatsStore) }
    var state by remember {
        mutableStateOf(GameUiState(screen = DicewarsScreen.Loading, game = DicewarsGame(), playerStatsHistory = playerStatsStore.load()))
    }
    val screenBackStack = remember { mutableStateListOf<DicewarsScreen>() }

    val dispatchAction: (GameAction) -> Unit = { action ->
        val oldScreen = state.screen
        val result = reducer.reduce(state, action)
        val newScreen = result.state.screen
        if (oldScreen != newScreen && action !is GameAction.BackNavigation) {
            screenBackStack += oldScreen
        }
        state = result.state
        if (state.soundEnabled) {
            result.soundEvents.forEach { soundPlayer.play(it) }
        }
    }

    backGestureHandler(
        state.screen != DicewarsScreen.Loading &&
            state.screen != DicewarsScreen.Title &&
            screenBackStack.isNotEmpty(),
    ) {
        val previousScreen = screenBackStack.removeAt(screenBackStack.lastIndex)
        state = state.copy(screen = previousScreen, confirmResetStats = false)
    }

    DicewarsApp(
        state = state,
        onAction = dispatchAction,
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
        DicewarsScreen.HumanTurn -> GameBoardScreen(state, onAction, title = "Your turn")
        DicewarsScreen.AiTurn -> GameBoardScreen(state, onAction, title = "Bots playing")
        DicewarsScreen.GameOver -> GameOverScreen(state, onAction)
        DicewarsScreen.Win -> WinScreen(state, onAction)
        DicewarsScreen.Stats -> StatsScreen(state, onAction)
        DicewarsScreen.Debug -> DebugScreen(state, onAction)
    }
}

