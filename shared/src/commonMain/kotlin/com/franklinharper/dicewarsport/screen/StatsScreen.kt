package com.franklinharper.dicewarsport.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.franklinharper.dicewarsport.GameAction
import com.franklinharper.dicewarsport.GameUiState
import com.franklinharper.dicewarsport.PlayerStatsRecord
import com.franklinharper.dicewarsport.presentation.components.ScreenScaffold

@Composable
fun StatsScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    title = "Stats",
    showBackButton = true,
    showSoundToggle = true,
    soundEnabled = state.soundEnabled,
    showDebugIcon = state.debugMode,
    onBack = { onAction(GameAction.BackToTitle) },
    onToggleSound = { onAction(GameAction.ToggleSound) },
    onGoToDebug = { onAction(GameAction.GoToDebug) },
) {
    StatsTable(state.playerStatsHistory.sortedRecords())
    Button(
        onClick = { onAction(GameAction.ResetStatsRequested) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Reset Stats") }

    if (state.confirmResetStats) {
        AlertDialog(
            onDismissRequest = { onAction(GameAction.ResetStatsCancelled) },
            title = { Text("Reset stats?") },
            text = { Text("This clears all saved win and score history.") },
            confirmButton = {
                TextButton(onClick = { onAction(GameAction.ResetStatsConfirmed) }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { onAction(GameAction.ResetStatsCancelled) }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun StatsTable(records: List<PlayerStatsRecord>) {
    StatsRow(
        player = "Player",
        score = "Score",
        winPercent = "Win %",
        wins = "Wins",
        played = "Played",
    )
    if (records.isEmpty()) {
        Text("No completed games yet.")
        return
    }
    records.forEach { record ->
        StatsRow(
            player = record.displayName,
            score = record.score.toString(),
            winPercent = "${record.winRatioPercent}%",
            wins = record.wins.toString(),
            played = record.gamesPlayed.toString(),
        )
    }
}

@Composable
private fun StatsRow(
    player: String,
    score: String,
    winPercent: String,
    wins: String,
    played: String,
) {
    Row(Modifier.fillMaxWidth()) {
        Text(player, modifier = Modifier.weight(2.1f))
        Text(score, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
        Text(winPercent, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
        Text(wins, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
        Text(played, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
    }
}
