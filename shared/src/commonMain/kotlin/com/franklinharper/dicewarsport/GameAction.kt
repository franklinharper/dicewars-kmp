package com.franklinharper.dicewarsport

sealed interface GameAction {
    data object LoadingFinished : GameAction
    data class SelectPlayerCount(val count: Int) : GameAction
    data object StartPressed : GameAction
    data object AcceptMap : GameAction
    data object RejectMap : GameAction
    data class TerritoryClicked(val territoryId: Int) : GameAction
    data object EndTurn : GameAction
    data object AiStep : GameAction
    data object BackToTitle : GameAction
    data object StartSpectate : GameAction
    data object ToggleSound : GameAction
    data object TitleTapped : GameAction
    data object GoToDebug : GameAction
    data class ShowDebugScreen(val screen: DicewarsScreen) : GameAction
    data object DisableDebugMode : GameAction
}
