package com.franklinharper.dicewarsport.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.franklinharper.dicewarsport.*
import com.franklinharper.dicewarsport.presentation.components.MapRenderer
import com.franklinharper.dicewarsport.presentation.components.ScreenScaffold
import kotlinx.coroutines.delay

@Composable
fun GameBoardScreen(state: GameUiState, onAction: (GameAction) -> Unit, title: String) = ScreenScaffold(
    title = title,
    showBackButton = true,
    showSoundToggle = true,
    soundEnabled = state.soundEnabled,
    showDebugIcon = state.debugMode,
    onBack = { onAction(GameAction.BackToTitle) },
    onToggleSound = { onAction(GameAction.ToggleSound) },
    onGoToDebug = { onAction(GameAction.GoToDebug) },
    horizontalPadding = 16.dp,
) {
    Board(state, onAction)
    PlayerStatusBar(state)
    Spacer(Modifier.height(24.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.resolvingAfterHumanEliminated) {
            LaunchedEffect(state.game) {
                delay(300)
                onAction(GameAction.AiStep)
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "You have been eliminated. Bots are finishing the game.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { onAction(GameAction.FinishMatchAfterHumanEliminated) }) { Text("Finish game") }
            }
        } else if (state.screen == DicewarsScreen.HumanTurn) {
            if (state.humanAutoplayEnabled) {
                LaunchedEffect(state.game) {
                    delay(300)
                    onAction(GameAction.HumanAutoplayStep)
                }
            }
            Text(
                text = "1. Click your area.\n2. Click neighbor to attack.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = "OR",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(16.dp))
            Button(onClick = { onAction(GameAction.EndTurn) }) { Text("End turn") }
        } else {
            LaunchedEffect(state.game) {
                delay(300)
                onAction(GameAction.AiStep)
            }
        }
    }
    Spacer(Modifier.weight(1f))
    if (!state.resolvingAfterHumanEliminated) {
        Button(
            onClick = { onAction(GameAction.ToggleHumanAutoplay) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.humanAutoplayEnabled) "Turn Autoplay off" else "Turn Autoplay on")
        }
    }
}

@Composable
private fun PlayerStatusBar(state: GameUiState) {
    val game = state.game
    Column(modifier = Modifier.fillMaxWidth()) {
        for (player in game.turnOrder.take(game.pmax)) {
            val isCurrentPlayer = player == game.currentPlayer()
            val isEliminated = game.players[player].maxConnectedAreaCount == 0
            val playerName = state.playerNames[player] ?: "Player $player"
            val supply = game.players[player].maxConnectedAreaCount
            val stock = game.players[player].stock

            val textColor = when {
                isEliminated -> MaterialTheme.colorScheme.outline
                isCurrentPlayer -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onBackground
            }
            val bgColor = when {
                isEliminated -> MaterialTheme.colorScheme.background
                isCurrentPlayer -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.background
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(bgColor)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(GameColors.getPlayerColor(player)),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (isEliminated) "✕" else if (stock > 0) "$supply+$stock" else "$supply",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
internal fun ColumnScope.Board(state: GameUiState, onAction: (GameAction) -> Unit) {
    val map = remember(state.game) { state.game.toRenderMap() }
    val density = LocalDensity.current

    val mapAspectRatio = (HexGrid.GRID_WIDTH + 0.5f) / (HexGrid.GRID_HEIGHT * 2f / 3f)
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().aspectRatio(mapAspectRatio),
        contentAlignment = Alignment.Center,
    ) {
        val availableWidthPx = constraints.maxWidth.toFloat()
        val cellWidth = availableWidthPx / (HexGrid.GRID_WIDTH + 0.5f)
        val cellHeight = cellWidth * 2f / 3f
        val fontSize = with(density) { (cellWidth * 0.8f).toSp() }.value.coerceIn(6f, 18f)

        MapRenderer(
            map = map,
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            fontSize = fontSize,
            highlightedTerritories = listOfNotNull(state.selectedFrom?.minus(1), state.selectedTo?.minus(1)).toSet(),
            attackFromTerritory = state.selectedFrom?.minus(1),
            onTerritoryClick = { territoryIndex -> onAction(GameAction.TerritoryClicked(territoryIndex + 1)) },
        )
    }
    Spacer(Modifier.height(8.dp))
}
