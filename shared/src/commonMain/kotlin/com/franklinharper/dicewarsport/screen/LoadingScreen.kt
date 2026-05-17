package com.franklinharper.dicewarsport.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.franklinharper.dicewarsport.DicewarsScreen
import com.franklinharper.dicewarsport.GameAction
import com.franklinharper.dicewarsport.GameUiState
import com.franklinharper.dicewarsport.presentation.components.ScreenScaffold
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(onAction: (GameAction) -> Unit) {
    LaunchedEffect(Unit) {
        delay(500)
        onAction(GameAction.LoadingFinished)
    }

    ScreenScaffold("Loading") {
    }
}
