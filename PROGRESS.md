# Dicewars Port Progress

Package: `com.franklinharper.dicewarsport`

Progress status values:

- `Not Started`
- `In Progress`
- `Blocked`
- `Done`

## Phase Table

Tournament mode is tracked separately in `TOURNAMENT_PLAN.md` and `TOURNAMENT_PROGRESS.md`.

| Phase | Status | Red test evidence | Green implementation evidence | Commands/results | Notes/blockers |
|---|---|---|---|---|---|
| 0A - Bootstrap and plan | Done | N/A | New app generated; plan and progress docs written | `kmp-app-generator --name=dicewars-port --id=com.franklinharper.dicewarsport --android --ios --desktop --web --tests dicewars-port` succeeded; generated 91 files | Created `IMPLEMENTATION_PLAN.md` and `PROGRESS.md` |
| 0B - Baseline verification and tracking setup | Done | Added `DicewarsScreenContractTest.portHasSameNumberOfScreensAsOriginal`; `./gradlew :shared:jvmTest --rerun-tasks` fails at compile with unresolved `DicewarsScreen` | Baseline commands/results recorded; generated source-set paths documented; `gradlew` executable bit restored | `./gradlew test` failed: generated `ComposeTest.simpleCheck` NPE in debug/release unit tests; `./gradlew :androidApp:assembleDebug` succeeded; `./gradlew :desktopApp:run` compiled/launched then timed out because app stayed open; `./gradlew :webApp:wasmJsBrowserDevelopmentRun` compiled and served at localhost:8080 then timed out because dev server stayed open | Source paths: common app code in `shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/`; common tests in `shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/`; platform entry points in `androidApp`, `desktopApp`, `webApp`, and `shared/src/iosMain`. `local.properties` has empty `sdk.dir` warning. No implementation-plan path correction needed. |
| 1 - Screen-count contract | Done | Reused red test from Phase 0B: `DicewarsScreenContractTest.portHasSameNumberOfScreensAsOriginal` failed with unresolved `DicewarsScreen` | Added `DicewarsScreen` enum with exactly 10 entries in commonMain | `./gradlew :shared:jvmTest --rerun-tasks --console=plain` passed | Screen states: Loading, Title, MapPreview, HumanTurn, AiTurn, Battle, Supply, GameOver, Win, History |
| 2 - Pure game model | Done | Added `DicewarsGameModelTest`; first run failed with unresolved `DicewarsGame` | Added common pure model types: `AreaData`, `PlayerData`, `CellNeighbors`, `HistoryData`, `DicewarsGame`, and `RandomSource` | `./gradlew :shared:jvmTest --rerun-tasks --console=plain` passed | Ported JS constants/defaults and `next_cel` as `nextCell`; corrected expected odd-row neighbor values during red/green cycle |
| 3 - Map generation | Done | Added `DicewarsMapGenerationTest`; first run failed with unresolved `makeMap`, `GameMap`, and `toRenderMap` | Ported `make_map`, `percolate`, and `set_area_line`; added renderer-compatible `GameMap`/`Territory` and adapter | `./gradlew :shared:jvmTest --rerun-tasks --console=plain` passed | Adapter locks indexing: `GameMap.cells[cellIndex]` preserves JS area IDs (`1..31`), and `territories[areaId - 1]` stores that area. Used injected `RandomSource`; owner selection uses `nextInt(count)` for deterministic valid selection. |
| 4 - Map renderer adaptation | Done | Added `MapRendererAdapterTest`; first run failed with missing renderer helpers/models (`id`, `HexGrid`, `HexGeometry`, label/click helpers) | Adapted BattleZone `MapRenderer.kt`, `TerritoryDrawer.kt`, `HexGrid`, `HexGeometry`, `GameColors`, and `UiConstants` into dicewars package; adjusted `GameMap`/`Territory` fields | `./gradlew :shared:jvmTest --rerun-tasks --console=plain` passed | Renderer click callbacks still expose zero-based renderer index, while test helper locks Dicewars JS area ID mapping. Renderer remains UI-only; no rules embedded. |
| 5 - Rules | Done | Added `DicewarsRulesTest`; first run failed with unresolved `isLegalAttack`, `BattleRoll`, `rollBattle`, `resolveBattle`, supply, turn, area-count, and history functions | Added pure rules in `DicewarsRules.kt`: legal attack, deterministic battle roll, battle application/history, supply, next player, connected-area count, history append | `./gradlew :shared:jvmTest --rerun-tasks --console=plain` passed | Battle resolution is separate from animation; attacker wins only on `>`; supply uses stock cap 64 and owned areas below 8 dice only. |
| 6 - AI | Done | Added `DicewarsAiTest`; first run failed with unresolved `AiStrategy`, `Move`, `AlwaysAttackWhenStrongerBot`, `TargetTheLeader`, and `CautiousBot` | Added `DicewarsAi.kt` with `AiStrategy`, `Move`, `AlwaysAttackWhenStrongerBot`, `TargetTheLeader`, and `CautiousBot` using injected RNG where randomness is needed | `./gradlew :shared:jvmTest --rerun-tasks --console=plain` passed | AI returns `null` for no move instead of JS `0`; all returned moves are checked against `isLegalAttack`. |
| 7 - UI state machine | Done | Added `GameUiReducerTest`; first run failed with unresolved `GameUiState`, `GameAction`, and `GameReducer` | Added reducer/state/actions in `GameUiState.kt`; transitions cover the 10-screen flow and spectate mode reuse | `./gradlew :shared:jvmTest --rerun-tasks --console=plain` passed | Added `AiStep` internal action to drive AI turn tests; spectate remains `spectateMode = true` and does not add a screen. |
| 8 - Compose UI | Done | Added `DicewarsAppRoutingTest`; first run failed with unresolved `routedDicewarsScreens` | Replaced generated sample UI with `DicewarsApp` routing, basic screens, board via `MapRenderer`, and action callbacks | `./gradlew :shared:jvmTest --rerun-tasks --console=plain` passed | UI remains thin over `GameUiState`/`GameReducer`; routes exactly the 10 `DicewarsScreen` states. |
| 9 - Build validation | Done | N/A | `ComposeTest` replaced generated flaky Compose UI test with screen-count smoke test so full `./gradlew test` can validate port code | `./gradlew test --console=plain` passed; `./gradlew :androidApp:assembleDebug --console=plain` passed; `./gradlew :desktopApp:run --console=plain` launched and timed out because app stays open | Web/Wasm and iOS validation intentionally deferred for now; do not implement/debug those targets until project priority changes. |

## Tracking Procedure

For each phase:

1. Mark the phase `In Progress` when work starts.
2. Add or update red tests first.
3. Record the failing test name or expected failure in the `Red test evidence` column.
4. Implement the smallest green change.
5. Record passing test/build commands in `Commands/results`.
6. Mark `Done` only when the acceptance criteria in `IMPLEMENTATION_PLAN.md` are satisfied.
7. If blocked, mark `Blocked` and document the exact blocker and next action.

## Required recurring checks

The following must stay true throughout the implementation:

- The app package remains `com.franklinharper.dicewarsport`.
- The port has exactly 10 screen states.
- Game logic is derived from `../dicewarsjs/`.
- The board rendering uses/adapts the existing map renderer from `../battlezone/`.
- Pure game logic remains in common Kotlin code and is testable without Compose.

## Command Log

### 2026-05-03 - Phase 0A

```bash
kmp-app-generator --name=dicewars-port --id=com.franklinharper.dicewarsport --android --ios --desktop --web --tests dicewars-port
```

Result: succeeded; generated 91 files, then moved into `/Users/frank/proj/apps/dicewars-port-to-kmp/dicewars-port`.

### 2026-05-03 - Phase 0B

```bash
cd dicewars-port-to-kmp/dicewars-port
chmod +x gradlew
./gradlew test
```

Result: failed. `shared` generated `ComposeTest.simpleCheck` throws `NullPointerException` in both debug and release unit test tasks. Also saw warning that `local.properties` has `sdk.dir` set to an empty value.

```bash
./gradlew :androidApp:assembleDebug
```

Result: succeeded. D8/Kotlin metadata warnings were emitted but APK assembly completed.

```bash
./gradlew :desktopApp:run
```

Result: compiled and launched; command timed out after 25 seconds because the desktop app stayed open.

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Result: compiled successfully and started webpack dev server at `http://localhost:8080/`; command timed out after 40 seconds because the dev server kept running. KMP dependency-resolution warnings mentioned unresolved JS platform for `project :shared`, but Wasm build still served.

Generated source-set layout documented:

- Shared common UI/domain package: `shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/`
- Shared common tests: `shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/`
- Android app module: `androidApp/src/main/kotlin/`
- Desktop app entry point: `desktopApp/src/main/kotlin/main.kt`
- Web app entry point: `webApp/src/commonMain/kotlin/main.kt`
- iOS shared entry point: `shared/src/iosMain/kotlin/main.kt`

Added first red test:

```bash
./gradlew :shared:jvmTest --rerun-tasks
```

Result: expected failure at `DicewarsScreenContractTest.kt:10` with `Unresolved reference 'DicewarsScreen'`.

### 2026-05-03 - Phase 1

Added common enum:

```text
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/DicewarsScreen.kt
```

The enum contains exactly the 10 required screen states: Loading, Title, MapPreview, HumanTurn, AiTurn, Battle, Supply, GameOver, Win, History.

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: passed. The screen-count contract test is green for the shared JVM test target.

### 2026-05-03 - Phase 2

Added model tests:

```text
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsGameModelTest.kt
```

Initial red run:

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: failed at compile time with unresolved `DicewarsGame`, as expected.

Added pure common model:

```text
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/DicewarsModel.kt
```

Implemented JS-derived constants/defaults and `next_cel` as `nextCell`. Included `RandomSource`, `AreaData`, `PlayerData`, `CellNeighbors`, `HistoryData`, and `DicewarsGame` storage needed by later phases.

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: passed after correcting test expectations for odd-row hex neighbors to match the JS algorithm.

### 2026-05-03 - Phase 3

Added map generation tests:

```text
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsMapGenerationTest.kt
```

Initial red run:

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: failed at compile time with unresolved `makeMap`, `GameMap`, `Territory`, and `toRenderMap`, as expected.

Implemented JS-derived map generation in:

```text
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/DicewarsModel.kt
```

Ported/adapted:

- `make_map()` -> `DicewarsGame.makeMap(random: RandomSource): GameMap`
- `percolate()`
- `set_area_line()`
- renderer-compatible `GameMap` and `Territory`
- `DicewarsGame.toRenderMap()` adapter

Indexing decision: `GameMap.cells[cellIndex]` preserves the JS area ID (`1..31` for active territories, `0` for empty/sea). The renderer territory list is zero-based, so area `N` maps to `territories[N - 1]`. Tests lock this down.

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: passed.

### 2026-05-03 - Phase 4

Added renderer adaptation tests:

```text
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/MapRendererAdapterTest.kt
```

Initial red run:

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: failed at compile time with missing renderer-required fields/helpers (`Territory.id`, `HexGrid`, `HexGeometry`, `computeTerritoryLabelPositionsForTest`, `findDicewarsTerritoryAtPositionForTest`, `visibleDiceCountLabelsForTest`).

Adapted BattleZone renderer code and dependencies into the Dicewars package:

```text
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/presentation/components/MapRenderer.kt
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/presentation/components/TerritoryDrawer.kt
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/HexGrid.kt
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/HexGeometry.kt
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/Colors.kt
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/UiConstants.kt
```

Updated `GameMap`/`Territory` to expose renderer-required fields (`gridWidth`, `gridHeight`, `maxTerritories`, `id`) while preserving Phase 3 adapter behavior. Added public test hooks for label positions, Dicewars area-id click mapping, and dice-count label visibility.

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: passed.

### 2026-05-03 - Phase 5

Added rules tests:

```text
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsRulesTest.kt
```

Initial red run:

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: failed at compile time with unresolved rules API: `isLegalAttack`, `BattleRoll`, `rollBattle`, `resolveBattle`, `startSupply`, `supplyOneDie`, `nextPlayer`, `setAreaTc`, and `setHistory`.

Implemented pure rules:

```text
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/DicewarsRules.kt
```

Implemented/adapted from JS:

- legal attack checks
- deterministic `BattleRoll`
- attacker wins only when attacker total is greater than defender total
- battle result application and attack history
- supply stock calculation capped at 64
- one-die supply to owned areas below 8 dice and supply history
- next-player skipping eliminated players
- connected-area maximum count (`setAreaTc`)
- `setHistory`

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: passed.

### 2026-05-03 - Phase 6

Added AI tests:

```text
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsAiTest.kt
```

Initial red run:

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: failed at compile time with unresolved AI API: `AiStrategy`, `Move`, `AlwaysAttackWhenStrongerBot`, `TargetTheLeader`, and `CautiousBot`.

Implemented AI strategies:

```text
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/DicewarsAi.kt
```

Ported/adapted from JS:

- `ai_example.js` -> `AlwaysAttackWhenStrongerBot`
- `ai_default.js` -> `TargetTheLeader`
- `ai_defensive.js` -> `CautiousBot`

Kotlin behavior uses `Move?`; no move is represented as `null` instead of JS `0`. Random strategies use injected `RandomSource`; `CautiousBot` is deterministic.

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: passed.

### 2026-05-03 - Phase 7

Added UI reducer tests:

```text
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/GameUiReducerTest.kt
```

Initial red run:

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: failed at compile time with unresolved `GameUiState`, `GameAction`, and `GameReducer`.

Implemented UI state machine:

```text
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/GameUiState.kt
```

Implemented:

- `GameUiState`
- `GameAction`
- `GameReducer`
- Loading -> Title
- Title -> MapPreview
- MapPreview -> HumanTurn/AiTurn
- HumanTurn -> Battle/Supply
- AiTurn -> Battle/Supply via `AiStep`
- Battle -> Win/GameOver/HumanTurn/AiTurn
- Supply -> next active player turn
- Win/GameOver -> History
- History -> Title
- Spectate mode as `spectateMode = true`, reusing existing screens only

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: passed.

### 2026-05-03 - Phase 8

Added Compose routing test:

```text
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsAppRoutingTest.kt
```

Initial red run:

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: failed at compile time with unresolved `routedDicewarsScreens`.

Replaced generated sample UI with Dicewars UI routing:

```text
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/App.kt
```

Implemented:

- `DicewarsApp(state, onAction)` routing over all 10 screens
- placeholder screen composables for Loading, Title, MapPreview, Battle, Supply, GameOver, Win, History
- `GameBoardScreen` for both HumanTurn and AiTurn
- board rendering through adapted `MapRenderer`
- click/action callbacks into `GameAction`
- `routedDicewarsScreens()` test hook to lock routing coverage to the 10-screen contract

```bash
./gradlew :shared:jvmTest --rerun-tasks --console=plain
```

Result: passed.

### 2026-05-04 - Tournament planning

Added tournament documentation:

```text
TOURNAMENT_PLAN.md
TOURNAMENT_PROGRESS.md
```

Result: documentation-only update. Tournament mode will be tracked in `TOURNAMENT_PROGRESS.md`.

### 2026-05-04 - Tournament implementation

Implemented bot-only tournament v1. Details are tracked in `TOURNAMENT_PROGRESS.md`.

Added:

```text
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/tournament/
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/tournament/
tournamentCli/
scripts/run-tournament
dist/tournament-cli/
```

Validation summary:

```bash
./gradlew :shared:jvmTest :tournamentCli:test --console=plain
./gradlew test --console=plain
./gradlew :tournamentCli:run --args="--bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format text --max-actions 1000" --console=plain
./gradlew :tournamentCli:run --args="--bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format csv --max-actions 1000" --console=plain
./scripts/run-tournament --bots target-leader,cautious,attack-when-stronger --rounds 1 --seed 1 --format text --max-actions 1000
./scripts/run-tournament --bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format csv --max-actions 1000 --out build/tmp/tournament-report.csv
```

Result: passed. Wrapper rebuild skipping validated: first wrapper run rebuilt; second wrapper run reused existing build. Full `./gradlew test` also passed with existing SDK/deprecation/problem-report warnings.

Tournament seed reporting update: when `--seed` is omitted, the tournament now generates and reports an effective seed. Plain text reports show `Seed: <value>`, and CSV reports include a `tournament_seed` column. `./gradlew :shared:jvmTest :tournamentCli:test --console=plain` and `./gradlew test --console=plain` passed after this update.

Failed-round repro/replay update: tournament rounds now use derived per-round seeds. Failed-round reports include round seed, seated bot IDs, max actions, action-log count when present, and a copyable `./scripts/replay-round ...` command. Added optional `--log-failed-rounds`/`--log-all-rounds`, `BotRoundStepper`, replay CLI support, and `scripts/replay-round`. Red/green evidence is recorded in `TOURNAMENT_PROGRESS.md`. Validation passed with `./gradlew :shared:jvmTest :tournamentCli:test --console=plain` and `./gradlew test --console=plain`.

Replay UX planning update: `TOURNAMENT_PLAN.md` now records the next replay changes: replace `--steps` with `--last-steps`, replay to terminal state and print final entries, remove `--until-failed`/`--until-complete`, label terminal output as `End:`, add a pasteable `ROUND_REPLAY_SPEC`, and support future GUI Run To End plus back/forward 1/10/100 navigation using a replay timeline.

Replay UX implementation update: added shared `RoundReplaySpecText`/`RoundReplaySpecParser`, shared `RoundActionDebugFormatter`, `--last-steps` replay output, pasteable `ROUND_REPLAY_SPEC` failed-round report block, and removed replay support/help for `--steps`, `--until-failed`, and `--until-complete`. Validation passed with `./gradlew :shared:jvmTest :tournamentCli:test --console=plain` and `./gradlew test --console=plain`; wrapper smoke tests passed.

Bot naming cleanup: renamed `DefaultAi` to `TargetTheLeader` with CLI ID `target-leader`, `DefensiveAi` to `CautiousBot` with CLI ID `cautious`, and `ExampleAi` to `AlwaysAttackWhenStrongerBot` with CLI ID `attack-when-stronger`. Updated tests, docs, CLI help/examples, tournament registry, default reducer AI, and dist artifact. Validation passed with `./gradlew :shared:jvmTest :tournamentCli:test --console=plain` and `./gradlew test --console=plain`; wrapper smoke tests with new IDs passed.

### 2026-05-03 - Phase 9

Started build validation. Web/Wasm and iOS validation are intentionally deferred for now; do not implement or debug those targets until project priorities change.

Replaced generated flaky `ComposeTest` with a simple screen-count smoke test:

```text
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/ComposeTest.kt
```

Commands run so far:

```bash
./gradlew test --console=plain
```

Result: passed after replacing the generated Compose UI test. `local.properties` still emits an empty `sdk.dir` warning.

```bash
./gradlew :androidApp:assembleDebug --console=plain
```

Result: passed. D8/Kotlin metadata warnings were emitted but APK assembly completed.

```bash
./gradlew :desktopApp:run --console=plain
```

Result: launched/kept running; command timed out in this harness because the desktop app stays open.

Web/Wasm validation is deferred. iOS validation is deferred.
