package com.franklinharper.dicewarsport.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = { onAction(GameAction.ShowDebugScreen(DicewarsScreen.Win)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show Win Screen") }
        Button(
            onClick = { onAction(GameAction.ShowDebugScreen(DicewarsScreen.GameOver)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show Game Over Screen") }
        Button(
            onClick = { onAction(GameAction.ShowDebugScreen(DicewarsScreen.HumanTurn)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show Human Turn") }
        Button(
            onClick = { onAction(GameAction.ShowDebugScreen(DicewarsScreen.AiTurn)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show AI Turn") }
        Button(
            onClick = { onAction(GameAction.ShowDebugScreen(DicewarsScreen.Title)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show Title Screen") }
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
}
