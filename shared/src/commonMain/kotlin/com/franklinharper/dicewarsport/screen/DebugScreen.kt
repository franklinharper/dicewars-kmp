package com.franklinharper.dicewarsport.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.franklinharper.dicewarsport.DicewarsScreen
import com.franklinharper.dicewarsport.GameAction
import com.franklinharper.dicewarsport.GameUiState
import com.franklinharper.dicewarsport.presentation.components.ScreenScaffold

@Composable
fun DebugScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    title = "Debug Menu",
    showBackButton = true,
    onBack = { onAction(GameAction.BackToTitle) },
) {
    var showScreenDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = { showScreenDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show Screen") }
        Button(
            onClick = { onAction(GameAction.GoToSelectBots) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Select Bots") }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onAction(GameAction.DisableDebugMode) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) { Text("Disable Debug Mode") }
    }

    if (showScreenDialog) {
        AlertDialog(
            onDismissRequest = { showScreenDialog = false },
            title = { Text("Show Screen") },
            text = {
                Column {
                    debugScreenOptions.forEach { option ->
                        TextButton(
                            onClick = {
                                showScreenDialog = false
                                onAction(GameAction.ShowDebugScreen(option.screen))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(option.label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showScreenDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private data class DebugScreenOption(
    val screen: DicewarsScreen,
    val label: String,
)

private val debugScreenOptions = listOf(
    DebugScreenOption(DicewarsScreen.Win, "Win Screen"),
    DebugScreenOption(DicewarsScreen.GameOver, "Game Over Screen"),
    DebugScreenOption(DicewarsScreen.HumanTurn, "Human Turn"),
    DebugScreenOption(DicewarsScreen.AiTurn, "AI Turn"),
)
