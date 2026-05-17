# Dicewars Kotlin Multiplatform Port - Implementation Plan

Package name: `com.franklinharper.dicewarsport`

New app directory: `dicewars-port-to-kmp/dicewars-port/`

Generated with:

```bash
kmp-app-generator \
  --name=dicewars-port \
  --id=com.franklinharper.dicewarsport \
  --android --ios --desktop --web --tests \
  dicewars-port
```

Then moved into:

```text
dicewars-port-to-kmp/dicewars-port/
```

All commands in this plan should be run from:

```bash
cd dicewars-port-to-kmp/dicewars-port
```

## Source Inputs

### Game logic source of truth

Port game logic from:

```text
../dicewarsjs/game.js
../dicewarsjs/ai_default.js
../dicewarsjs/ai_defensive.js
../dicewarsjs/ai_example.js
../dicewarsjs/main.js
```

### Existing map renderer source

Reuse/adapt the map renderer from:

```text
../battlezone/dicewars-kt/BattleZone/composeApp/src/commonMain/kotlin/com/franklinharper/battlezone/presentation/components/MapRenderer.kt
../battlezone/dicewars-kt/BattleZone/composeApp/src/commonMain/kotlin/com/franklinharper/battlezone/presentation/components/TerritoryDrawer.kt
```

Renderer dependencies to adapt:

```text
../battlezone/dicewars-kt/BattleZone/shared/src/commonMain/kotlin/com/franklinharper/battlezone/MapDataStructures.kt
../battlezone/dicewars-kt/BattleZone/shared/src/commonMain/kotlin/com/franklinharper/battlezone/HexGrid.kt
../battlezone/dicewars-kt/BattleZone/shared/src/commonMain/kotlin/com/franklinharper/battlezone/HexGeometry.kt
../battlezone/dicewars-kt/BattleZone/composeApp/src/commonMain/kotlin/com/franklinharper/battlezone/Colors.kt
../battlezone/dicewars-kt/BattleZone/composeApp/src/commonMain/kotlin/com/franklinharper/battlezone/UiConstants.kt
```

All copied/adapted code must use package:

```kotlin
package com.franklinharper.dicewarsport
```

---

# Original Screen Mapping

The original JS implementation has exactly 10 user-visible screen/state categories. The port must preserve this count.

| Port screen | Original JS function/state |
|---|---|
| `Loading` | `fake_loading()` |
| `Title` | `start_title()` |
| `MapPreview` | `make_map()` with "Play this board?" prompt |
| `HumanTurn` | `start_man()`, `first_click()`, `second_click()` |
| `AiTurn` | `start_com()`, `com_from()`, `com_to()` |
| `Battle` | `start_battle()`, `battle_dice()`, `after_battle()` |
| `Supply` | `start_supply()`, `supply_waiting()`, `supply_dice()` |
| `GameOver` | `start_gameover()`, `gameover()` |
| `Win` | `start_win()`, `win()` |
| `History` | `start_history()`, `play_history()` |

## Spectate Treatment

The original JS has `start_spectate()`, but spectate is treated as a mode, not an additional screen. In the port, spectate must be represented with:

```kotlin
spectateMode = true
```

while reusing existing `AiTurn`, `GameOver`, and `History` states. Spectate must not increase the screen count beyond 10.

---

# Cross-Cutting Contracts

## Renderer Adapter Contract

The battlezone map renderer will be reused for territory geometry, click detection, fills, borders, highlights, and text labels. The Dicewars domain model must be adapted into the renderer model with an explicit adapter:

```kotlin
fun DicewarsGame.toRenderMap(): GameMap
```

Required field mapping:

| JS/Dicewars model | Renderer model |
|---|---|
| `cel[cellIndex]` | `GameMap.cells[cellIndex]` |
| `adat[i].arm` | `Territory.owner` |
| `adat[i].dice` | `Territory.armyCount` |
| `adat[i].cpos` | `Territory.centerPos` |
| `adat[i].size` | `Territory.size` |
| `adat[i].join` | `Territory.adjacentTerritories` |
| `join[cellIndex].dir` | `GameMap.cellNeighbors[cellIndex].directions` |

Important indexing rule:

- The original JS uses area IDs mostly in `1..31`; area `0` is non-playable/empty.
- The renderer stores territories in a zero-based array.
- Adapter tests must lock down whether `cells[cellIndex]` is preserved as JS area ID or translated to `territoryIndex + 1`; all renderer click mapping must be consistent with this decision.

## Dice Rendering Contract

The reused map renderer does not provide a full Dicewars dice-stack visual by itself. Initial visual parity may render dice counts as labels through `Territory.armyCount`. Later UI work may replace labels with dice icons/stacks.

Minimum acceptance:

- territory color matches owner
- territory border/highlight works
- click detection maps to correct territory
- dice count is visible per active territory

## Sound Strategy

Sound is non-blocking for domain/rules/UI TDD. Add a common abstraction before platform sound work:

```kotlin
interface SoundPlayer {
    fun play(sound: GameSound)
}

enum class GameSound {
    Button,
    Clear,
    Click,
    Dice,
    Fail,
    MyTurn,
    Over,
    Success
}
```

Tests should use `NoOpSoundPlayer` or a fake recorder. Platform implementations can later reuse files from:

```text
../dicewarsjs/sound/button.wav
../dicewarsjs/sound/clear.wav
../dicewarsjs/sound/click.wav
../dicewarsjs/sound/dice.wav
../dicewarsjs/sound/fail.wav
../dicewarsjs/sound/myturn.wav
../dicewarsjs/sound/over.wav
../dicewarsjs/sound/success.wav
```

## Battle Roll Contract

Separate pure battle resolution from animation. Battle resolution must be deterministic under injected RNG and produce a value object:

```kotlin
data class BattleRoll(
    val attackerDice: List<Int>,
    val defenderDice: List<Int>,
    val attackerTotal: Int,
    val defenderTotal: Int,
    val success: Boolean
)
```

Rules:

- attacker rolls one die per die in source territory
- defender rolls one die per die in target territory
- each die result is `1..6`
- attacker wins only if `attackerTotal > defenderTotal`
- animation displays an already-computed `BattleRoll`; animation must not decide the result

## History Replay Contract

The original history supports both attacks and supply entries:

```text
Attack: from = source area, to = target area, res = 0/1
Supply: from = supplied area, to = 0, res = 0
```

History replay tests must cover:

- restoring the initial owner/dice snapshot
- replaying a supply entry
- replaying a failed attack
- replaying a successful attack
- completing replay without changing the final recorded history

---

# Phases

## Phase 0A - Bootstrap and plan

Status: Done

Goals:

1. Create a brand new KMP app in `dicewars-port-to-kmp/dicewars-port/`.
2. Use package `com.franklinharper.dicewarsport`.
3. Write this implementation plan into the new directory.
4. Create progress-tracking documentation.

Acceptance:

- `dicewars-port-to-kmp/dicewars-port/` exists.
- Generated project contains Android, iOS, Desktop, Web, and tests.
- `IMPLEMENTATION_PLAN.md` exists.
- `PROGRESS.md` exists.

## Phase 0B - Baseline verification and tracking setup

Goals:

1. Verify the generated app builds before port work begins.
2. Record baseline commands/results in `PROGRESS.md`.
3. Identify generated source-set layout and exact destination paths.
4. Add a first failing test for the 10-screen contract.

Suggested commands:

```bash
cd dicewars-port-to-kmp/dicewars-port
./gradlew test
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Acceptance:

- Baseline test/build status recorded in `PROGRESS.md`.
- Destination package/source paths documented.
- `IMPLEMENTATION_PLAN.md` updated if generated source paths differ from expected paths.
- First red test exists for the required screen count.

## Phase 1 - Establish screen-count contract

Original JS has exactly 10 screen states. The port must also have exactly 10.

Create:

```kotlin
enum class DicewarsScreen {
    Loading,
    Title,
    MapPreview,
    HumanTurn,
    AiTurn,
    Battle,
    Supply,
    GameOver,
    Win,
    History
}
```

Red test:

```kotlin
@Test
fun portHasSameNumberOfScreensAsOriginal() {
    assertEquals(10, DicewarsScreen.entries.size)
}
```

Green:

- Add only the enum and minimal package structure needed to pass.

Acceptance:

- Common test proves there are exactly 10 screen states.

## Phase 2 - Port pure game model from JS

Port from `game.js`:

```kotlin
AreaData
PlayerData
HistoryData
DicewarsGame
```

Keep JS constants:

```kotlin
XMAX = 28
YMAX = 32
AREA_MAX = 32
STOCK_MAX = 64
MAX_DICE = 8
```

Use injectable RNG:

```kotlin
interface RandomSource {
    fun nextInt(bound: Int): Int
}
```

Red tests:

- hex neighbor calculation matches JS `next_cel`
- default `pmax == 7`
- default `user == 0`
- `AREA_MAX == 32`
- `STOCK_MAX == 64`

Acceptance:

- Core model compiles in `commonMain`.
- Pure model tests pass in `commonTest`.

## Phase 3 - Port map generation

Port from `game.js`:

```text
make_map()
percolate()
set_area_line()
```

Adapt output to renderer-compatible data:

```kotlin
GameMap
Territory
CellNeighbors
```

Compatibility rule:

- JS area IDs are mostly `1..31`.
- The renderer expects `cells[cellIndex] = territoryId + 1` when using zero-based territory array indexing.
- Add adapter tests to lock this down.

Red tests:

- active territories have size > 0
- dice are 1..8
- owner is in `0 until pmax`
- adjacency is symmetric
- every active cell references an active territory
- generated map can be consumed by copied `MapRenderer` model
- `DicewarsGame.toRenderMap()` maps owner, dice count, center position, size, cell IDs, and adjacency consistently

Acceptance:

- Deterministic tests pass with injected RNG.
- Map model is renderer-compatible before UI work begins.
- Adapter indexing behavior is documented and covered by tests.

## Phase 4 - Copy/adapt existing map renderer

Copy/adapt renderer files into the new app package.

Expected destination:

```text
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/presentation/components/MapRenderer.kt
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/presentation/components/TerritoryDrawer.kt
```

Domain/render dependencies should live in common source under package `com.franklinharper.dicewarsport`.

Red tests:

- `DicewarsGame.makeMap()` returns a `GameMap`.
- all `MapRenderer` required fields are present.
- territory label-position helpers do not crash for empty territories.
- click mapping returns the expected Dicewars territory ID through the adapter.
- dice count labels are visible for active territories.

Acceptance:

- Renderer compiles under the new package.
- No game rules are embedded in renderer code.

## Phase 5 - Port rules

From `main.js` and `game.js`, port pure rules:

```kotlin
isLegalAttack(from, to)
resolveBattle(...)
startSupply()
supplyOneDie()
nextPlayer()
setAreaTc()
setHistory()
```

Red tests:

- attacker must own source
- source dice must be > 1
- target must be enemy
- target must be adjacent
- `BattleRoll` contains attacker dice, defender dice, totals, and success
- attacker wins only when attacker total > defender total
- attacker loss sets source dice to 1
- attacker win transfers owner and dice correctly
- supply capped at 64
- supply only affects owned areas with dice < 8

Acceptance:

- All rules are platform-independent and tested in common tests.

## Phase 6 - Port AI

Port from:

```text
ai_example.js
ai_default.js
ai_defensive.js
```

Kotlin shape:

```kotlin
interface AiStrategy {
    fun chooseMove(game: DicewarsGame): Move?
}
```

Implement:

```kotlin
AlwaysAttackWhenStrongerBot
TargetTheLeader
CautiousBot
```

Red tests:

- AI never returns illegal moves.
- `AlwaysAttackWhenStrongerBot` attacks only weaker adjacent enemy areas.
- no valid moves returns `null`.

Acceptance:

- AI strategies are deterministic under injected RNG.
- All AI moves pass `isLegalAttack`.

## Phase 7 - UI state machine

Create reducer:

```kotlin
data class GameUiState(
    val screen: DicewarsScreen,
    val game: DicewarsGame,
    val selectedFrom: Int? = null,
    val selectedTo: Int? = null,
    val spectateMode: Boolean = false
)
```

Actions:

```kotlin
sealed interface GameAction {
    data object LoadingFinished : GameAction
    data class SelectPlayerCount(val count: Int) : GameAction
    data object StartPressed : GameAction
    data object AcceptMap : GameAction
    data object RejectMap : GameAction
    data class TerritoryClicked(val territoryId: Int) : GameAction
    data object EndTurn : GameAction
    data object BattleAnimationFinished : GameAction
    data object SupplyAnimationFinished : GameAction
    data object OpenHistory : GameAction
    data object BackToTitle : GameAction
    data object StartSpectate : GameAction
}
```

Red tests:

- `Loading -> Title`
- `Title -> MapPreview`
- `MapPreview -> HumanTurn/AiTurn`
- `HumanTurn -> Battle`
- `HumanTurn -> Supply`
- `AiTurn -> Battle/Supply`
- `Battle -> Win/GameOver/HumanTurn/AiTurn`
- `Win/GameOver -> History`
- `History -> Title`

Acceptance:

- UI flow is tested without Compose.
- Screen count remains exactly 10.
- `spectateMode` reuses existing screens and does not add an 11th screen.

## Phase 8 - Compose UI

Use the same 10 screen states.

Composable routing:

```kotlin
@Composable
fun DicewarsApp(state: GameUiState) {
    when (state.screen) {
        DicewarsScreen.Loading -> LoadingScreen()
        DicewarsScreen.Title -> TitleScreen()
        DicewarsScreen.MapPreview -> MapPreviewScreen()
        DicewarsScreen.HumanTurn -> GameBoardScreen()
        DicewarsScreen.AiTurn -> GameBoardScreen()
        DicewarsScreen.Battle -> BattleScreen()
        DicewarsScreen.Supply -> SupplyScreen()
        DicewarsScreen.GameOver -> GameOverScreen()
        DicewarsScreen.Win -> WinScreen()
        DicewarsScreen.History -> HistoryScreen()
    }
}
```

Acceptance:

- UI is a thin Compose layer over tested state and render models.
- Existing map renderer is used for the board.

## Phase 9 - Build validation

Run from new project:

```bash
cd dicewars-port-to-kmp/dicewars-port
./gradlew test
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Web/Wasm and iOS are intentionally deferred for the time being. Do not spend implementation/debugging time on Web/Wasm or iOS validation in this port phase unless the project priority changes.

If iOS work is later resumed, identify and run the generated iOS validation task if available, for example:

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

If the generated task name differs, record the actual task in `PROGRESS.md`.

Acceptance:

- Tests pass.
- Android builds.
- Desktop runs or is documented as launched/kept running in this harness.
- Web/Wasm validation is documented as deferred.
- iOS validation is documented as deferred.

---

# Progress Tracking

Progress is tracked in `PROGRESS.md` using one table row per phase.

Each phase records:

- status: `Not Started`, `In Progress`, `Blocked`, or `Done`
- red test evidence
- green implementation evidence
- commands run
- notes/blockers

Rules:

1. A phase cannot be marked `Done` without passing tests or a documented reason.
2. Each red/green cycle should update the phase notes.
3. Any intentional deviation from the JS source must be documented in the phase notes.
4. The 10-screen contract must remain visible in every UI/state-machine phase.
5. Build/test command output summaries should be recorded, not pasted in full unless needed for debugging.
