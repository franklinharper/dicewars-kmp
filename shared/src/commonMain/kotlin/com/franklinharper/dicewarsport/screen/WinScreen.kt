package com.franklinharper.dicewarsport.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.franklinharper.dicewarsport.GameAction
import com.franklinharper.dicewarsport.GameUiState
import com.franklinharper.dicewarsport.presentation.components.AnimatedTrophy
import com.franklinharper.dicewarsport.presentation.components.ScreenScaffold

@Composable
fun WinScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    title = "You Win",
    showBackButton = true,
    showSoundToggle = true,
    soundEnabled = state.soundEnabled,
    showDebugIcon = state.debugMode,
    onBack = { onAction(GameAction.BackToTitle) },
    onToggleSound = { onAction(GameAction.ToggleSound) },
    onGoToDebug = { onAction(GameAction.GoToDebug) },
) {
    AnimatedTrophy(modifier = Modifier.weight(1f).fillMaxWidth())
    StatsTable(state.playerStatsHistory.sortedRecords())
    Button(onClick = { onAction(GameAction.BackToTitle) }) { Text("New Game") }
}
