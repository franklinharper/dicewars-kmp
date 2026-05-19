package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.ai.AiStrategy
import com.franklinharper.dicewarsport.ai.AlwaysAttackWhenStrongerBot
import com.franklinharper.dicewarsport.ai.CautiousBot
import com.franklinharper.dicewarsport.ai.MaxBot
import com.franklinharper.dicewarsport.ai.StrategicBot
import com.franklinharper.dicewarsport.ai.TargetTheLeader
import kotlin.time.Clock

class GameReducer(
    private val random: RandomSource,
    private val aiStrategies: Map<Int, AiStrategy> = emptyMap(),
    private val playerNames: Map<Int, String> = emptyMap(),
    private val debugPreferences: DebugPreferences = NoOpDebugPreferences(),
    private val playerStatsStore: PlayerStatsStore = InMemoryPlayerStatsStore(),
) {
    private var activeAiStrategies: Map<Int, AiStrategy> = aiStrategies

    private val availableAiFactories: List<(RandomSource) -> AiStrategy> = listOf(
        { rng -> TargetTheLeader(rng) },
        { CautiousBot() },
        { rng -> AlwaysAttackWhenStrongerBot(rng) },
        { rng -> StrategicBot(rng) },
        { MaxBot() },
    )

    private fun assignAiStrategiesFor(game: DicewarsGame, spectateMode: Boolean): Map<Int, AiStrategy> {
        val assigned = mutableMapOf<Int, AiStrategy>()
        for (p in 0 until game.pmax) {
            if (!spectateMode && p == game.user) continue
            assigned[p] = aiStrategies[p] ?: availableAiFactories[random.nextInt(availableAiFactories.size)](random)
        }
        activeAiStrategies = assigned
        return assigned
    }

    private fun playerNamesFor(game: DicewarsGame, spectateMode: Boolean, assignedAiStrategies: Map<Int, AiStrategy>): Map<Int, String> {
        val names = mutableMapOf<Int, String>()
        for (p in 0 until game.pmax) {
            names[p] = playerNames[p] ?: when {
                !spectateMode && p == game.user -> "Human"
                assignedAiStrategies.containsKey(p) -> assignedAiStrategies[p]!!.name
                else -> "Human"
            }
        }
        return names
    }

    private fun playerIdsFor(game: DicewarsGame, spectateMode: Boolean, assignedAiStrategies: Map<Int, AiStrategy>): Map<Int, String> {
        val ids = mutableMapOf<Int, String>()
        for (p in 0 until game.pmax) {
            ids[p] = when {
                !spectateMode && p == game.user -> "human"
                else -> aiIdFor(assignedAiStrategies[p])
            }
        }
        return ids
    }

    private fun aiIdFor(strategy: AiStrategy?): String = when (strategy) {
        is TargetTheLeader -> "rebel"
        is CautiousBot -> "turtle"
        is AlwaysAttackWhenStrongerBot -> "bully"
        is StrategicBot -> "emperor"
        is MaxBot -> "max"
        else -> strategy?.name ?: "unknown-bot"
    }

    companion object {
        private const val TAP_THRESHOLD = 5
        private const val TAP_WINDOW_MS = 3000L
    }

    data class Result(
        val state: GameUiState,
        val soundEvents: List<SoundEvent> = emptyList(),
    )

    fun reduce(state: GameUiState, action: GameAction): Result = when (action) {
        GameAction.LoadingFinished -> {
            val debugMode = debugPreferences.isDebugMode()
            Result(state.copy(screen = DicewarsScreen.Title, debugMode = debugMode, playerStatsHistory = playerStatsStore.load()))
        }
        is GameAction.SelectPlayerCount -> Result(
            state.copy(selectedPlayerCount = action.count),
            listOf(SoundEvent.BUTTON),
        )
        GameAction.StartPressed -> {
            val newGame = DicewarsGame.generate(state.selectedPlayerCount, random)
            val assignedAiStrategies = assignAiStrategiesFor(newGame, spectateMode = false)
            Result(state.copy(game = newGame, screen = DicewarsScreen.MapPreview, spectateMode = false, playerNames = playerNamesFor(newGame, spectateMode = false, assignedAiStrategies), playerIds = playerIdsFor(newGame, spectateMode = false, assignedAiStrategies), eliminatedPlayerIds = emptyList(), eliminatedPlayerSeats = emptyList(), gameStatsRecorded = false, confirmResetStats = false, humanAutoplayEnabled = false))
        }
        GameAction.StartSpectate -> {
            val newGame = DicewarsGame.generate(state.selectedPlayerCount, random)
            val assignedAiStrategies = assignAiStrategiesFor(newGame, spectateMode = true)
            Result(state.copy(game = newGame, screen = DicewarsScreen.MapPreview, spectateMode = true, playerNames = playerNamesFor(newGame, spectateMode = true, assignedAiStrategies), playerIds = playerIdsFor(newGame, spectateMode = true, assignedAiStrategies), eliminatedPlayerIds = emptyList(), eliminatedPlayerSeats = emptyList(), gameStatsRecorded = false, confirmResetStats = false, humanAutoplayEnabled = false))
        }
        GameAction.AcceptMap -> {
            val newScreen = turnScreenFor(state.game, state.spectateMode)
            Result(state.copy(screen = newScreen), newScreen.soundEvents)
        }
        GameAction.RejectMap -> {
            val newGame = DicewarsGame.generate(state.game.pmax, random)
            val assignedAiStrategies = assignAiStrategiesFor(newGame, state.spectateMode)
            Result(state.copy(game = newGame, screen = DicewarsScreen.MapPreview, playerNames = playerNamesFor(newGame, state.spectateMode, assignedAiStrategies), playerIds = playerIdsFor(newGame, state.spectateMode, assignedAiStrategies), eliminatedPlayerIds = emptyList(), eliminatedPlayerSeats = emptyList(), gameStatsRecorded = false, confirmResetStats = false, humanAutoplayEnabled = false))
        }
        is GameAction.TerritoryClicked -> onTerritoryClicked(state, action.territoryId)
        GameAction.EndTurn -> onTurnFinished(state)
        GameAction.AiStep -> onAiStep(state)
        GameAction.FinishMatchAfterHumanEliminated -> finishMatchAfterHumanEliminated(state)
        GameAction.HumanAutoplayStep -> onHumanAutoplayStep(state)
        GameAction.ToggleHumanAutoplay -> Result(state.copy(humanAutoplayEnabled = !state.humanAutoplayEnabled), listOf(SoundEvent.BUTTON))
        GameAction.BackToTitle -> Result(state.copy(screen = DicewarsScreen.Title, selectedFrom = null, selectedTo = null, confirmResetStats = false))
        GameAction.ToggleSound -> Result(state.copy(soundEnabled = !state.soundEnabled))
        GameAction.ShowStats -> Result(state.copy(screen = DicewarsScreen.Stats, statsReturnScreen = state.screen, confirmResetStats = false, playerStatsHistory = playerStatsStore.load()))
        GameAction.BackFromStats -> Result(state.copy(screen = state.statsReturnScreen, confirmResetStats = false))
        GameAction.ResetStatsRequested -> Result(state.copy(confirmResetStats = true))
        GameAction.ResetStatsCancelled -> Result(state.copy(confirmResetStats = false))
        GameAction.ResetStatsConfirmed -> {
            playerStatsStore.clear()
            Result(state.copy(playerStatsHistory = PlayerStatsHistory.default(), confirmResetStats = false))
        }
        GameAction.TitleTapped -> onTitleTapped(state)
        GameAction.GoToDebug -> Result(state.copy(screen = DicewarsScreen.Debug))
        is GameAction.ShowDebugScreen -> onShowDebugScreen(state, action.screen)
        GameAction.DisableDebugMode -> {
            debugPreferences.setDebugMode(false)
            Result(state.copy(debugMode = false, screen = DicewarsScreen.Title))
        }
    }

    private fun onTerritoryClicked(state: GameUiState, territoryId: Int): Result {
        if (state.screen != DicewarsScreen.HumanTurn) return Result(state)

        val selectedFrom = state.selectedFrom
        if (selectedFrom == null) {
            val area = state.game.areas.getOrNull(territoryId) ?: return Result(state)
            return if (area.size > 0 && area.owner == state.game.currentPlayer() && area.dice > 1) {
                Result(state.copy(selectedFrom = territoryId), listOf(SoundEvent.CLICK))
            } else {
                Result(state)
            }
        }

        if (territoryId == selectedFrom) return Result(state.copy(selectedFrom = null, selectedTo = null))
        if (!state.game.isLegalAttack(selectedFrom, territoryId)) return Result(state)

        val roll = rollBattle(
            attackerDiceCount = state.game.areas[selectedFrom].dice,
            defenderDiceCount = state.game.areas[territoryId].dice,
            random = random,
        )
        val newGame = state.game.resolveBattle(selectedFrom, territoryId, roll)
        val newEliminations = appendNewEliminations(state, newGame)

        val battleSounds = listOf(SoundEvent.DICE, if (roll.success) SoundEvent.SUCCESS else SoundEvent.FAIL)

        val terminalScreen = terminalScreenOrNull(newGame, state.spectateMode)
        if (terminalScreen != null) {
            val terminalSounds = battleSounds + terminalScreen.soundEvents
            return Result(
                recordStatsIfNeeded(state.copy(game = newGame, screen = terminalScreen, selectedFrom = null, selectedTo = null, eliminatedPlayerIds = newEliminations.playerIds, eliminatedPlayerSeats = newEliminations.seatIds)),
                terminalSounds,
            )
        }

        return Result(
            state.copy(
                game = newGame,
                screen = turnScreenFor(newGame, state.spectateMode),
                selectedFrom = null,
                selectedTo = null,
                eliminatedPlayerIds = newEliminations.playerIds, eliminatedPlayerSeats = newEliminations.seatIds,
                resolvingAfterHumanEliminated = shouldResolveAfterHumanEliminated(newGame, state.spectateMode),
            ),
            battleSounds,
        )
    }

    private fun onHumanAutoplayStep(state: GameUiState): Result {
        if (state.screen != DicewarsScreen.HumanTurn || !state.humanAutoplayEnabled) return Result(state)
        val player = state.game.currentPlayer()
        val move = chooseAutoplayMove(state.game, player) ?: return onTurnFinished(state)
        val roll = rollBattle(
            attackerDiceCount = state.game.areas[move.first].dice,
            defenderDiceCount = state.game.areas[move.second].dice,
            random = random,
        )
        val newGame = state.game.resolveBattle(move.first, move.second, roll)
        val newEliminations = appendNewEliminations(state, newGame)
        val battleSounds = listOf(SoundEvent.DICE, if (roll.success) SoundEvent.SUCCESS else SoundEvent.FAIL)
        val terminalScreen = terminalScreenOrNull(newGame, state.spectateMode)
        if (terminalScreen != null) {
            return Result(
                recordStatsIfNeeded(state.copy(game = newGame, screen = terminalScreen, selectedFrom = null, selectedTo = null, eliminatedPlayerIds = newEliminations.playerIds, eliminatedPlayerSeats = newEliminations.seatIds)),
                battleSounds + terminalScreen.soundEvents,
            )
        }
        return Result(
            state.copy(
                game = newGame,
                screen = turnScreenFor(newGame, state.spectateMode),
                selectedFrom = null,
                selectedTo = null,
                eliminatedPlayerIds = newEliminations.playerIds,
                eliminatedPlayerSeats = newEliminations.seatIds,
                resolvingAfterHumanEliminated = shouldResolveAfterHumanEliminated(newGame, state.spectateMode),
            ),
            battleSounds,
        )
    }


    private fun onAiStep(state: GameUiState): Result {
        if (state.screen != DicewarsScreen.AiTurn && !state.resolvingAfterHumanEliminated) return Result(state)
        val player = state.game.currentPlayer()
        val strategy = activeAiStrategies[player] ?: aiStrategies[player] ?: TargetTheLeader(random)
        val move = strategy.chooseMove(state.game) ?: return onTurnFinished(state)
        if (!state.game.isLegalAttack(move.from, move.to, player)) return onTurnFinished(state)

        val roll = rollBattle(
            attackerDiceCount = state.game.areas[move.from].dice,
            defenderDiceCount = state.game.areas[move.to].dice,
            random = random,
        )
        val newGame = state.game.resolveBattle(move.from, move.to, roll)
        val newEliminations = appendNewEliminations(state, newGame)

        val battleSounds = listOf(SoundEvent.DICE, if (roll.success) SoundEvent.SUCCESS else SoundEvent.FAIL)

        val terminalScreen = terminalScreenOrNull(newGame, state.spectateMode)
        if (terminalScreen != null) {
            val terminalSounds = battleSounds + terminalScreen.soundEvents
            return Result(
                recordStatsIfNeeded(state.copy(game = newGame, screen = terminalScreen, selectedFrom = null, selectedTo = null, eliminatedPlayerIds = newEliminations.playerIds, eliminatedPlayerSeats = newEliminations.seatIds)),
                terminalSounds,
            )
        }

        return Result(
            state.copy(
                game = newGame,
                screen = turnScreenFor(newGame, state.spectateMode),
                selectedFrom = null,
                selectedTo = null,
                eliminatedPlayerIds = newEliminations.playerIds, eliminatedPlayerSeats = newEliminations.seatIds,
                resolvingAfterHumanEliminated = shouldResolveAfterHumanEliminated(newGame, state.spectateMode),
            ),
            battleSounds,
        )
    }

    private fun finishMatchAfterHumanEliminated(state: GameUiState): Result {
        if (!state.resolvingAfterHumanEliminated) return Result(state)
        var current = state
        var guard = 0
        while (terminalScreenOrNull(current.game, current.spectateMode) == null && guard < 100_000) {
            val player = current.game.currentPlayer()
            val strategy = activeAiStrategies[player] ?: aiStrategies[player] ?: TargetTheLeader(random)
            val move = strategy.chooseMove(current.game)
            current = if (move != null && current.game.isLegalAttack(move.from, move.to, player)) {
                val roll = rollBattle(
                    attackerDiceCount = current.game.areas[move.from].dice,
                    defenderDiceCount = current.game.areas[move.to].dice,
                    random = random,
                )
                val newGame = current.game.resolveBattle(move.from, move.to, roll)
                val newEliminations = appendNewEliminations(current, newGame)
                current.copy(
                    game = newGame,
                    selectedFrom = null,
                    selectedTo = null,
                    eliminatedPlayerIds = newEliminations.playerIds,
                    eliminatedPlayerSeats = newEliminations.seatIds,
                )
            } else {
                finishTurnWithoutSounds(current)
            }
            guard++
        }
        val terminalScreen = terminalScreenOrNull(current.game, current.spectateMode) ?: DicewarsScreen.GameOver
        return Result(recordStatsIfNeeded(current.copy(screen = terminalScreen, resolvingAfterHumanEliminated = false)))
    }

    private fun finishTurnWithoutSounds(state: GameUiState): GameUiState {
        val player = state.game.currentPlayer()
        var game = state.game.startSupply(player)
        while (true) {
            val (newGame, areaNumber) = game.supplyOneDie(player, random)
            game = newGame
            if (areaNumber == null) break
        }
        game = game.nextPlayer()
        return state.copy(game = game, screen = turnScreenFor(game, state.spectateMode), selectedFrom = null, selectedTo = null)
    }

    private fun onTurnFinished(state: GameUiState): Result {
        val player = state.game.currentPlayer()
        var game = state.game.startSupply(player)
        while (true) {
            val (newGame, areaNumber) = game.supplyOneDie(player, random)
            game = newGame
            if (areaNumber == null) break
        }
        game = game.nextPlayer()
        val newScreen = turnScreenFor(game, state.spectateMode)
        return Result(
            state.copy(
                game = game,
                screen = newScreen,
                selectedFrom = null,
                selectedTo = null,
            ),
            newScreen.soundEvents,
        )
    }

    private data class Eliminations(val playerIds: List<String>, val seatIds: List<Int>)

    private fun appendNewEliminations(state: GameUiState, newGame: DicewarsGame): Eliminations {
        val eliminatedIds = state.eliminatedPlayerIds.toMutableList()
        val eliminatedSeats = state.eliminatedPlayerSeats.toMutableList()
        for (p in 0 until newGame.pmax) {
            val id = state.playerIds[p] ?: continue
            if (p !in eliminatedSeats && state.game.players[p].maxConnectedAreaCount > 0 && newGame.players[p].maxConnectedAreaCount == 0) {
                eliminatedSeats += p
                eliminatedIds += id
            }
        }
        return Eliminations(eliminatedIds, eliminatedSeats)
    }

    private fun recordStatsIfNeeded(state: GameUiState): GameUiState {
        if (state.spectateMode || state.gameStatsRecorded) return state
        val winner = (0 until state.game.pmax).firstOrNull { state.game.players[it].maxConnectedAreaCount > 0 } ?: return state
        val participants = (0 until state.game.pmax).mapNotNull { p ->
            val id = state.playerIds[p] ?: return@mapNotNull null
            val name = state.playerNames[p] ?: id
            PlayerStatsParticipant(seatId = p, playerId = id, displayName = name)
        }
        val history = state.playerStatsHistory.recordGameResult(participants, winner, state.eliminatedPlayerSeats)
        playerStatsStore.save(history)
        return state.copy(playerStatsHistory = history, gameStatsRecorded = true)
    }

    private fun shouldResolveAfterHumanEliminated(game: DicewarsGame, spectateMode: Boolean): Boolean =
        !spectateMode && game.players[game.user].maxConnectedAreaCount == 0

    private fun terminalScreenOrNull(game: DicewarsGame, spectateMode: Boolean): DicewarsScreen? {
        val activePlayers = (0 until game.pmax).count { game.players[it].maxConnectedAreaCount > 0 }
        if (activePlayers == 1) return if (spectateMode || game.players[game.user].maxConnectedAreaCount == 0) DicewarsScreen.GameOver else DicewarsScreen.Win
        return null
    }

    private fun turnScreenFor(game: DicewarsGame, spectateMode: Boolean): DicewarsScreen =
        if (!spectateMode && game.currentPlayer() == game.user) DicewarsScreen.HumanTurn else DicewarsScreen.AiTurn

    private fun onTitleTapped(state: GameUiState): Result {
        val now = Clock.System.now().toEpochMilliseconds()
        val withinWindow = (now - state.titleTapTimestamp) < TAP_WINDOW_MS
        val newCount = if (withinWindow) state.titleTapCount + 1 else 1
        val newDebugMode = if (newCount >= TAP_THRESHOLD) !state.debugMode else state.debugMode
        if (newDebugMode != state.debugMode) {
            debugPreferences.setDebugMode(newDebugMode)
        }
        return Result(
            state.copy(
                debugMode = newDebugMode,
                titleTapCount = if (newDebugMode != state.debugMode) 0 else newCount,
                titleTapTimestamp = if (newDebugMode != state.debugMode) 0L else now,
            ),
        )
    }

    private fun onShowDebugScreen(state: GameUiState, targetScreen: DicewarsScreen): Result {
        val needsGame = targetScreen in setOf(
            DicewarsScreen.HumanTurn, DicewarsScreen.AiTurn,
        )
        val game = if (needsGame) DicewarsGame.generate(state.selectedPlayerCount, random) else state.game
        val assignedAiStrategies = if (needsGame) assignAiStrategiesFor(game, state.spectateMode) else activeAiStrategies
        return Result(
            state.copy(
                screen = targetScreen,
                game = game,
                selectedFrom = null,
                selectedTo = null,
                playerNames = if (needsGame) playerNamesFor(game, state.spectateMode, assignedAiStrategies) else state.playerNames,
            ),
            targetScreen.soundEvents,
        )
    }
}
