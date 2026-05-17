package com.franklinharper.dicewarsport.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.franklinharper.dicewarsport.APP_VERSION
import com.franklinharper.dicewarsport.GameAction
import com.franklinharper.dicewarsport.GameUiState
import com.franklinharper.dicewarsport.presentation.components.ScreenScaffold

@Composable
fun TitleScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    "Dicewars",
    showDebugIcon = state.debugMode,
    onGoToDebug = { onAction(GameAction.GoToDebug) },
    onTitleTap = { onAction(GameAction.TitleTapped) },
) {
    Text(
        text = "How many players?",
        style = MaterialTheme.typography.titleMedium,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (count in 2..8) {
            val selected = count == state.selectedPlayerCount
            Button(
                onClick = { onAction(GameAction.SelectPlayerCount(count)) },
                modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = if (selected) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            ) {
                Text("$count")
            }
        }
    }
    Button(onClick = { onAction(GameAction.StartPressed) }) { Text("Start") }
    Spacer(Modifier.weight(1f))
    Text(
        text = "Version $APP_VERSION",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
}
