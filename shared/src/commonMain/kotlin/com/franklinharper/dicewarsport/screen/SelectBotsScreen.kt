package com.franklinharper.dicewarsport.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.franklinharper.dicewarsport.GameAction
import com.franklinharper.dicewarsport.GameUiState
import com.franklinharper.dicewarsport.ai.BuiltInBots
import com.franklinharper.dicewarsport.presentation.components.ScreenScaffold

@Composable
fun SelectBotsScreen(state: GameUiState, onAction: (GameAction) -> Unit) = ScreenScaffold(
    title = "Select Bots",
    showBackButton = true,
    onBack = { onAction(GameAction.GoToDebug) },
) {
    var selectedBotIds by remember(state.selectedDebugBotIds) {
        mutableStateOf(
            state.selectedDebugBotIds
                .filter { it in BuiltInBots.byId }
                .toSet()
                .ifEmpty { BuiltInBots.all.map { it.id }.toSet() },
        )
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BuiltInBots.all.forEach { bot ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = bot.id in selectedBotIds,
                    onCheckedChange = { checked ->
                        selectedBotIds = if (checked) {
                            selectedBotIds + bot.id
                        } else {
                            selectedBotIds - bot.id
                        }
                    },
                )
                Text(bot.displayName)
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = { onAction(GameAction.SelectDebugBots(selectedBotIds)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = selectedBotIds.isNotEmpty(),
    ) {
        Text("OK")
    }
}
