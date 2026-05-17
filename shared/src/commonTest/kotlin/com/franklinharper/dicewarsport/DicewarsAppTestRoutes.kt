package com.franklinharper.dicewarsport

fun routedDicewarsScreens(): Set<DicewarsScreen> = setOf(
    DicewarsScreen.Loading,
    DicewarsScreen.Title,
    DicewarsScreen.MapPreview,
    DicewarsScreen.HumanTurn,
    DicewarsScreen.AiTurn,
    DicewarsScreen.GameOver,
    DicewarsScreen.Win,
    DicewarsScreen.Stats,
    DicewarsScreen.Debug,
)
