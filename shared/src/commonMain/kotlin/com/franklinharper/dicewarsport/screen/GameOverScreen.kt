package com.franklinharper.dicewarsport.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.franklinharper.dicewarsport.GameAction
import com.franklinharper.dicewarsport.GameUiState
import com.franklinharper.dicewarsport.presentation.components.AnimatedRobot
import com.franklinharper.dicewarsport.presentation.components.ScreenScaffold

@Composable
fun GameOverScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    title = "Game Over",
    showBackButton = true,
    showSoundToggle = true,
    soundEnabled = state.soundEnabled,
    showDebugIcon = state.debugMode,
    onBack = { onAction(GameAction.BackToTitle) },
    onToggleSound = { onAction(GameAction.ToggleSound) },
    onGoToDebug = { onAction(GameAction.GoToDebug) },
) {
    AnimatedRobot(modifier = Modifier.weight(1f).fillMaxWidth())
    StatsTable(state.playerStatsHistory.sortedRecords())
    Button(
        onClick = { onAction(GameAction.BackToTitle) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("New Game") }
}
