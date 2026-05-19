# Test Audit

Scope: Kotlin test files under `shared/src/commonTest` and `tournamentCli/src/test`. No code changes were made for this audit.

## Summary

The suite is generally valuable: most tests target pure rules, reducers, tournament scoring, replay metadata, and CLI parsing. The highest-value tests are deterministic and check domain invariants. The weakest areas are tests that lock incidental UI/routing/version details, tests that over-specify heuristic choices for bots, and a few tests whose assertions are tightly coupled to implementation details rather than user-observable behavior.

## Candidates to remove or replace

### 1. `AppVersionTest.appVersionIsCurrentReleaseVersion`

File: `shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/AppVersionTest.kt`

Issue: Low value and high maintenance. It asserts the current literal app version, so every intentional version bump fails until the test is edited. This is exactly what happened during the `0.2` bump. It does not verify app behavior or prevent a meaningful regression.

Recommendation: Remove it, or replace it with a shape/format test only if needed, e.g. version matches `major.minor` or is non-blank.

### 2. `DicewarsAppRoutingTest.routedScreensCoverEveryScreenState` plus `DicewarsAppTestRoutes.kt`

Files:
- `shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsAppRoutingTest.kt`
- `shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsAppTestRoutes.kt`

Issue: The test compares `DicewarsScreen.entries` to a hand-maintained test-only set. It does not exercise actual `DicewarsApp` routing. A developer can make the test pass by updating the test-only list while forgetting to route the composable.

Recommendation: Remove, or replace with a real routing smoke test if Compose UI testing is stable. If the goal is to preserve screen count, test the count directly or test reducer navigation paths.

## Brittle tests worth refactoring

### 3. `DicewarsAiTest.botDisplayNamesAreFunNames`

File: `shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsAiTest.kt`

Issue: Locks exact copy/display names (`Bully`, `Rebel`, `Turtle`, `Emperor`). These names are user-visible, so some coverage is useful, but this test can be brittle if naming is product copy rather than logic.

Recommendation: Keep only if these names are a contract. Otherwise move expected names to a central registry and test consistency between game UI names, stats names, and tournament names rather than hardcoding strings in multiple places.

### 4. AI heuristic tests over-specify individual choices

Files:
- `shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsAiTest.kt`
- `shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsAiHeuristicTest.kt`

Examples:
- `alwaysAttackWhenStrongerBotAttacksOnlyWeakerAdjacentEnemyAreas`
- `targetTheLeaderTargetsDominantPlayerWhenOneExists`
- `targetTheLeaderSkipsEqualDiceAttackWhenNeitherSideIsTopRankedAndRandomRollIsLow`
- `targetTheLeaderAllowsEqualDiceAttackWhenRandomRollIsHigh`
- `turtleBotSkipsAttackFromVulnerableAreaWhenEstablishedAndNoStock`

Issue: These tests are valuable if the current AI personalities are a contract, but brittle if heuristics are expected to evolve. They often assert a particular move or decision from a handcrafted board. Small heuristic improvements can break them even when the bot still returns legal and reasonable moves.

Recommendation: Keep core legality tests (`aiStrategiesNeverReturnIllegalMoves`, `noValidMovesReturnsNull`). For personality tests, rename/document them as behavior contracts, or soften assertions to properties (targets leader when clearly dominant, never attacks stronger territory for specific bot, etc.).

### 5. `DicewarsMapGenerationTest.generatedMapRandomizesTurnOrder`

File: `shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsMapGenerationTest.kt`

Issue: Uses `ZeroRandomSource` and asserts `currentPlayer() != 0`. This is testing a specific shuffle implementation behavior under a fake RNG. If shuffle implementation changes but still randomizes correctly, this may fail.

Recommendation: Replace with a deterministic test that verifies `turnOrder` is a permutation and a statistical/seeded test over several random sources that at least one generated order differs from identity, if desired. Or only assert permutation here and leave randomness behavior untested.

### 6. Autoplay reducer tests depend on fixed battle outcome details

File: `shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/GameUiReducerTest.kt`

Examples:
- `humanAutoplayAttacksOnlyWhenReserveCanReplenishAllOwnedTerritoriesAfterEitherOutcome`
- `humanAutoplayDoesNotAttackWhenReserveCannotReplenishWinOutcome`

Issue: These tests use `UiFixedRandom`, which makes battle rolls always succeed in current dice configurations. They assert resulting owner/dice values after the battle. The reserve rule itself is the important behavior; battle outcome assertions make the tests somewhat coupled to dice rolling details.

Recommendation: Keep the scenarios, but consider extracting the autoplay move predicate into a pure function and test that directly for reserve conditions. Then keep one reducer integration test that verifies the action is wired.

### 7. `GameUiReducerTest.startAssignsNamesToHumanAndAiPlayers`

File: `shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/GameUiReducerTest.kt`

Issue: Expects all generated AIs to be `Rebel` because `UiFixedRandom` always selects the first AI factory. This locks factory ordering and fake RNG behavior rather than just verifying names are assigned.

Recommendation: Assert human is named `Human` and AI names are non-blank / match the strategy assigned, or inject explicit strategies to avoid depending on factory order.

### 8. Debug unlock tap tests are time-sensitive in design

File: `shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/GameUiReducerTest.kt` and edge cases in `GameUiReducerEdgeCaseTest.kt`

Issue: The reducer reads `Clock.System.now()` directly. Tests execute taps immediately and rely on real time being within the configured tap window. This is usually stable, but still tied to wall-clock behavior.

Recommendation: Inject a clock/time source into `GameReducer` so tests can advance time deterministically.

## Tests that are valuable and should generally stay

- Rules/model tests:
  - `DicewarsRulesTest`
  - `DicewarsRulesEdgeCaseTest`
  - `DicewarsGameModelTest`
  - `HexGeometryTest`
- Stats tests:
  - `PlayerStatsHistoryTest`, especially duplicate-strategy aggregation.
- Tournament scoring/repro tests:
  - `TournamentScoringTest`
  - `RoundReproMetadataTest`
  - `RoundReplaySpecTextTest`
- CLI parser/error tests:
  - `CliOptionsTest` parser and removed-flag coverage.

These tests cover stable domain behavior and are not primarily tied to incidental UI copy or implementation ordering.

## Gaps noticed during audit

1. Android `SharedPreferences` stats persistence is not directly tested. Consider a Robolectric/instrumented test later, or factor JSON encode/decode into common pure code and test it.
2. The new Android back-gesture stack is mostly in `App` composable state and is not directly tested. Reducer-level `BackFromStats` is tested, but global back stack behavior is not.
3. Map width scaling/layout is not covered by tests. This may be acceptable because layout tests are costly, but current behavior has been adjusted manually several times.
4. Autoplay bottom-button placement is not tested. Again likely acceptable unless UI snapshot/layout tests are introduced.

## Suggested cleanup priority

1. Remove or relax `AppVersionTest`.
2. Replace `DicewarsAppRoutingTest` with a more meaningful screen-count or routing test, or remove it.
3. Refactor autoplay reserve condition into a pure function and test it without battle-roll side effects.
4. Inject a clock into `GameReducer` for debug tap tests.
5. Review AI heuristic tests and decide which bot behaviors are true product contracts versus implementation details.
