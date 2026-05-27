package com.franklinharper.dicewarsport.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.franklinharper.dicewarsport.GameAction
import com.franklinharper.dicewarsport.GameUiState
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
    Spacer(Modifier.weight(1f))
    Text(
        text = "${winnerName(state)} wins!",
        style = MaterialTheme.typography.displayMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.weight(1f))
    StatsTable(state.playerStatsHistory.sortedRecords())
    Button(
        onClick = { onAction(GameAction.BackToTitle) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("New Game") }
}

private fun winnerName(state: GameUiState): String {
    val winner = (0 until state.game.maxPlayers).firstOrNull { state.game.players[it].maxConnectedAreaCount > 0 }
    return winner?.let { state.playerNames[it] ?: "Player ${it + 1}" } ?: "Unknown player"
}
