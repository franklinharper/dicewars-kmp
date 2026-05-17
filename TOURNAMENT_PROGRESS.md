# Dicewars Bot Tournament Progress

Package: `com.franklinharper.dicewarsport`

Plan: `TOURNAMENT_PLAN.md`

Progress status values:

- `Not Started`
- `In Progress`
- `Blocked`
- `Done`

## Phase Table

| Phase | Status | Red test evidence | Green implementation evidence | Commands/results | Notes/blockers |
|---|---|---|---|---|---|
| T0 - Plan | Done | N/A | `TOURNAMENT_PLAN.md` and `TOURNAMENT_PROGRESS.md` written | N/A | Decisions recorded: bot-only; no Compose UI for v1; rotate seating each round; optional seed; failed rounds give no points and are reported; text and CSV output first; smart wrapper should skip Gradle when source hash is unchanged. |
| T1 - Scoring model | Done | Added scoring tests; no separate red-only run captured for this phase | Added `TournamentConfig`, `TournamentParticipant`, `TournamentResult`, `RoundResult`, `BotScore`, `scoreCompletedRound`, `scoreFailedRound`, and `aggregateBotScores` | `./gradlew :shared:jvmTest --console=plain` passed | Tests cover 2-player, 3-player, failed-round zero scoring, aggregation, and wins. |
| T2 - Headless round runner | Done | Added `BotGameRunnerTest`; first integrated run failed at compile with missing `setAreaTc` import in test, then passed after correction | Added pure `RoundRunner`/`BotGameRunner` with injected game factory, AI-only turns, battle resolution, supply/end-turn handling, eliminations, winner detection, max-action failure | `./gradlew :shared:jvmTest --console=plain` passed | Illegal AI moves are treated as no attack/end turn. Runner is headless and does not depend on Compose. |
| T3 - Tournament runner | Done | Added `TournamentRunnerTest`; no separate red-only run captured for this phase | Added `TournamentRunner`, participant rotation, per-round derived seeds, built-in participant registry | `./gradlew :shared:jvmTest --console=plain` passed | Seed implementation uses one effective tournament seed and derives an independent seed per round. If no seed is supplied, a random effective seed is generated and reported. Seating rotates left each round. |
| T4 - Report formatters | Done | Added `TournamentReportFormatterTest`; no separate red-only run captured for this phase | Added `TournamentReportFormatter`, plain text formatter, and combined score/round CSV formatter | `./gradlew :shared:jvmTest --console=plain` passed | CSV v1 uses one stable combined table with `section` rows for scores and rounds. Text and CSV reports include the effective tournament seed. JSON/Markdown deferred. |
| T5 - CLI module | Done | Added `CliOptionsTest`; no separate red-only run captured for this phase | Added `tournamentCli` JVM module, manual CLI parser, `runTournamentCli`, Gradle `run`, fat JAR task, and `generateTournamentDist` | `./gradlew :tournamentCli:test --console=plain` passed; `./gradlew :tournamentCli:run --args="--bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format text --max-actions 1000" --console=plain` passed; CSV run passed | CLI supports `--bots`, `--rounds`, `--seed`, `--format`, `--out`, and `--max-actions`. |
| T6 - Smart wrapper script | Done | Manual first/second wrapper run validated rebuild then skip behavior | Added executable `scripts/run-tournament`; wrapper computes source hash, calls `:tournamentCli:generateTournamentDist` when needed, stores `dist/tournament-cli/source.hash`, and launches the fat JAR | First wrapper run rebuilt; second wrapper run printed `Tournament CLI unchanged; using existing build.`; `--out` CSV run wrote `build/tmp/tournament-report.csv` | Wrapper overwrites `source.hash` with its computed hash after Gradle build to keep skip behavior stable. Gradle hash code was corrected to use unsigned byte hex. |
| T7 - Validation | Done | N/A | Full tournament v1 validated | `./gradlew :shared:jvmTest :tournamentCli:test --console=plain` passed; Gradle CLI text and CSV runs passed; wrapper text and CSV/out runs passed | Gradle emits existing deprecation/problem-report notices, but commands pass. |
| R1 - Per-round repro seeds and metadata | Done | Added `RoundReproMetadataTest`; red run failed with unresolved `deriveRoundSeed`, missing `RoundResult.roundSeed`, `seatedParticipantIds`, and `maxActionsPerRound` | Added `deriveRoundSeed`; `RoundConfig` now carries `roundSeed`; `RoundResult` carries round seed, seats, and max actions; `TournamentRunner` derives per-round seeds | `./gradlew :shared:jvmTest --console=plain` passed | Failed rounds can now be reproduced without replaying earlier tournament rounds. |
| R2 - Shared round stepper and optional action logs | Done | Added `BotRoundStepperTest`; red run failed with unresolved `BotRoundStepper`, `RoundReplaySpec`, and `RoundConfig.logActions` | Added `BotRoundStepper`, `RoundReplaySpec`, `BotRoundState`, `BotRoundStepResult`, `RoundActionLogEntry`, and action types; refactored `BotGameRunner` to use the stepper | `./gradlew :shared:jvmTest --console=plain` passed | Action logs capture attack, illegal move, end turn, and terminal failed/won entries. |
| R3 - Failed-round action-log policy | Done | Added tests for `logFailedRounds` and `logAllRounds`; red run failed with missing `TournamentConfig` fields | Added `TournamentConfig.logFailedRounds` and `logAllRounds`; failed rounds are rerun with the same round seed when failed-only logging is requested | `./gradlew :shared:jvmTest --console=plain` passed | Failed-only logging avoids large logs for successful rounds while keeping failed rounds inspectable. |
| R4 - Report repro metadata | Done | Updated formatter tests; red run failed because text/CSV lacked round seed, seats, max actions, repro command, and action-log count | Updated plain text failed-round section and CSV columns to include repro metadata | `./gradlew :shared:jvmTest --console=plain` passed | Text reports include copyable `./scripts/replay-round ...` commands. |
| R5 - Replay CLI and wrapper | Done | Added CLI tests; red run failed with missing log flag fields and replay subcommand support | Added CLI log flags, `replay-round` subcommand, replay output formatting, and `scripts/replay-round` wrapper | `./gradlew :tournamentCli:test --console=plain` passed; wrapper replay smoke test passed | Replay wrapper reuses the same tournament CLI fat JAR/hash rebuild mechanism. |
| R6 - Repro validation | Done | N/A | Repro workflow validated end to end | `./gradlew :shared:jvmTest :tournamentCli:test --console=plain` passed; `./gradlew test --console=plain` passed; failed-round report and replay command smoke-tested | Full test run emits existing Compose deprecation warnings. |
| R7 - Last-step replay output and shared GUI-ready spec | Done | Added tests for `RoundReplaySpecParser`, `RoundActionDebugFormatter`, `ReplayOptions.lastSteps`, removed replay flags, last-step replay output, and report spec block; red run failed with unresolved parser/formatter and missing `lastSteps` | Added shared `RoundReplaySpecText`/`RoundReplaySpecParser`, shared `RoundActionDebugFormatter`; updated reports to include `ROUND_REPLAY_SPEC` and `--last-steps`; updated replay CLI to run to end and print rolling last entries; removed `--steps`, `--until-failed`, and `--until-complete` support/help | `./gradlew :shared:jvmTest :tournamentCli:test --console=plain` passed; wrapper smoke tests passed | Replay terminal entries now print `End:`. Future GUI can reuse parser and formatter. |
| G1 - Compose replay GUI state/reducer | Deferred |  |  |  | Future: paste replay spec, load, step forward/back 1/10/100, run to end, reset. Store every timeline state initially; optimize with checkpoints later if needed. |
| G2 - Compose replay debug screen | Deferred |  |  |  | Future UI over replay reducer and existing board renderer. Loading spec from report file is deferred beyond paste input. |
| N1 - Bot naming cleanup | Done | Existing tests referenced old class names and CLI IDs | Renamed AI classes and built-in participant IDs: `DefaultAi` -> `TargetTheLeader` (`target-leader`), `DefensiveAi` -> `CautiousBot` (`cautious`), `ExampleAi` -> `AlwaysAttackWhenStrongerBot` (`attack-when-stronger`) | `./gradlew :shared:jvmTest :tournamentCli:test --console=plain` passed; `./gradlew test --console=plain` passed; wrapper smoke tests with new IDs passed | Updated docs, tests, CLI help/examples, tournament registry, default reducer AI, and generated CLI dist. |

## Tracking Procedure

For each tournament phase:

1. Mark the phase `In Progress` when work starts.
2. Add or update red tests before implementation where practical.
3. Record the failing test name, compile error, or expected failure in the `Red test evidence` column.
4. Implement the smallest green change.
5. Record passing command summaries in the `Commands/results` column.
6. Mark a phase `Done` only after acceptance criteria are satisfied.
7. If blocked, mark `Blocked` and document the exact blocker and next action.
8. Document intentional behavior choices that affect experiments, reproducibility, or scoring.

## Phase Acceptance Criteria

### T0 - Plan

Done when:

- `TOURNAMENT_PLAN.md` exists
- `TOURNAMENT_PROGRESS.md` exists
- core decisions are recorded:
  - bot-only tournament mode
  - no Compose UI in v1
  - participants rotate seats every round
  - optional seed is supported
  - failed rounds award no points
  - failed rounds are reported
  - plain text and CSV reports are v1
  - smart wrapper skips rebuilds when source hash is unchanged

### T1 - Scoring model

Done when tests prove:

- with `N` players, eliminated bots receive `0, 1, 2...` in elimination order
- winner receives `2 * N`
- failed rounds give no points to any bot
- scores aggregate correctly across multiple round results
- wins are counted separately from points

Suggested tests:

```text
TournamentScoringTest.twoPlayerWinnerGetsFourPoints
TournamentScoringTest.threePlayerEliminationOrderScoresZeroOneAndWinnerSix
TournamentScoringTest.failedRoundAwardsNoPoints
TournamentScoringTest.aggregateScoresAcrossRounds
```

Suggested command:

```bash
./gradlew :shared:jvmTest --console=plain
```

### T2 - Headless round runner

Done when tests prove:

- a bot-only round can complete without Compose/UI/sound dependencies
- elimination order is recorded
- winner is recorded
- action count is recorded
- `maxActionsPerRound` failure is reported
- illegal AI moves are ignored or treated as no attack/end turn, according to the implementation choice documented in the notes
- no human-player path is required

Suggested tests:

```text
BotGameRunnerTest.completedRoundRecordsWinnerAndEliminations
BotGameRunnerTest.maxActionsExceededProducesFailedRound
BotGameRunnerTest.illegalAiMoveDoesNotCrashRunner
```

Suggested command:

```bash
./gradlew :shared:jvmTest --console=plain
```

### T3 - Tournament runner

Done when tests prove:

- configured number of rounds is attempted
- completed and failed rounds are counted separately
- participant seating rotates each round
- seed makes tournament results reproducible
- different seeds can produce different results where randomness is involved
- built-in AIs can be used as participants

Suggested tests:

```text
TournamentRunnerTest.attemptsConfiguredNumberOfRounds
TournamentRunnerTest.rotatesParticipantSeatingEachRound
TournamentRunnerTest.sameSeedProducesSameResult
TournamentRunnerTest.failedRoundsAreIncludedInFinalResult
```

Suggested command:

```bash
./gradlew :shared:jvmTest --console=plain
```

### T4 - Report formatters

Done when tests prove:

- plain text report includes rounds requested, completed, failed, seed, score table, wins, and failed round details
- CSV score output includes stable headers and one row per bot
- CSV round output includes stable headers and one row per round, or the chosen CSV layout is documented
- output order is deterministic, usually descending by score and then stable by participant ID/name

Suggested tests:

```text
TournamentReportFormatterTest.plainTextIncludesScoresAndFailedRounds
TournamentReportFormatterTest.csvIncludesHeadersAndBotRows
```

Suggested command:

```bash
./gradlew :shared:jvmTest --console=plain
```

### T5 - CLI module

Done when:

- `tournamentCli/` exists
- CLI supports:
  - `--bots`
  - `--rounds`
  - `--seed`
  - `--format text|csv`
  - `--out`
  - `--max-actions`
- CLI can print to stdout
- CLI can write to an output file
- invalid bot IDs and invalid arguments produce useful errors

Suggested commands:

```bash
./gradlew :tournamentCli:test --console=plain
./gradlew :tournamentCli:run --args="--bots target-leader,cautious,attack-when-stronger --rounds 10 --seed 42 --format text" --console=plain
./gradlew :tournamentCli:run --args="--bots target-leader,cautious,attack-when-stronger --rounds 10 --seed 42 --format csv" --console=plain
```

### T6 - Smart wrapper script

Done when:

- `scripts/run-tournament` exists and is executable
- first run builds the tournament CLI if needed
- second run skips Gradle when hash and artifact are unchanged
- source changes trigger rebuild
- wrapper passes all CLI arguments through unchanged
- wrapper follows the `kmp-app-generator` style of storing source hash under `dist/`

Expected files:

```text
scripts/run-tournament
dist/tournament-cli/source.hash
dist/tournament-cli/dicewars-tournament-cli-all.jar
```

Suggested commands:

```bash
./scripts/run-tournament --bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format text
./scripts/run-tournament --bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format text
```

The second command should report that the existing build is reused, or otherwise clearly skip the Gradle rebuild.

### T7 - Validation

Done when all relevant commands are run and summarized:

```bash
./gradlew :shared:jvmTest --console=plain
./gradlew :tournamentCli:test --console=plain
./gradlew :tournamentCli:run --args="--bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format text" --console=plain
./gradlew :tournamentCli:run --args="--bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format csv" --console=plain
./scripts/run-tournament --bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format text
./scripts/run-tournament --bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format csv
```

If any target is intentionally deferred, document it here and in the phase table.

## Command Log

### 2026-05-04 - T0

Created tournament planning and tracking docs:

```text
TOURNAMENT_PLAN.md
TOURNAMENT_PROGRESS.md
```

No build commands run because this phase only writes documentation.

### 2026-05-04 - T1 through T4

Added shared/common tournament implementation and tests:

```text
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/tournament/TournamentModels.kt
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/tournament/TournamentScoring.kt
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/tournament/BuiltInTournamentParticipants.kt
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/tournament/BotGameRunner.kt
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/tournament/TournamentRunner.kt
shared/src/commonMain/kotlin/com/franklinharper/dicewarsport/tournament/TournamentReportFormatters.kt
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/tournament/TournamentScoringTest.kt
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/tournament/BotGameRunnerTest.kt
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/tournament/TournamentRunnerTest.kt
shared/src/commonTest/kotlin/com/franklinharper/dicewarsport/tournament/TournamentReportFormatterTest.kt
```

First integrated test run:

```bash
./gradlew :shared:jvmTest --console=plain
```

Result: failed at compile because `BotGameRunnerTest` was missing the `setAreaTc` import. Added the import and reran.

```bash
./gradlew :shared:jvmTest --console=plain
```

Result: passed.

### 2026-05-04 - Report effective seed update

Updated tournament seed handling and reports so every tournament result has a reproducible seed:

- `TournamentRunner` now generates an effective seed when `TournamentConfig.seed` is omitted.
- `TournamentResult.seed` now contains the effective seed used by the run.
- Plain text reports continue to print `Seed: <value>`.
- CSV reports now include a `tournament_seed` column on score and round rows.
- Added tests for omitted-seed reporting.

```bash
./gradlew :shared:jvmTest :tournamentCli:test --console=plain
./gradlew test --console=plain
```

Result: passed. Full test run emitted existing Compose deprecation warnings.

Wrapper smoke checks:

```bash
./scripts/run-tournament --bots target-leader,cautious --rounds 1 --format text --max-actions 1
./scripts/run-tournament --bots target-leader,cautious --rounds 1 --format csv --max-actions 1
```

Result: text report printed an integer `Seed:` value, and CSV report included the same kind of value in `tournament_seed`.

### 2026-05-04 - T5

Added JVM CLI module:

```text
tournamentCli/build.gradle.kts
tournamentCli/src/main/kotlin/com/franklinharper/dicewarsport/tournamentcli/Main.kt
tournamentCli/src/test/kotlin/com/franklinharper/dicewarsport/tournamentcli/CliOptionsTest.kt
```

Updated `settings.gradle.kts` to include `:tournamentCli`.

```bash
./gradlew :tournamentCli:test --console=plain
```

Result: passed.

```bash
./gradlew :tournamentCli:run --args="--bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format text --max-actions 1000" --console=plain
```

Result: passed and printed a plain text tournament report.

```bash
./gradlew :tournamentCli:run --args="--bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format csv --max-actions 1000" --console=plain
```

Result: passed and printed combined score/round CSV.

### 2026-05-04 - T6

Added smart wrapper:

```text
scripts/run-tournament
```

Added dist artifact/hash generation through:

```bash
./gradlew :tournamentCli:generateTournamentDist --console=plain
```

Initial run produced the dist files but reported configuration-cache storage problems. The wrapper now invokes the dist task with `--no-configuration-cache`. The Gradle hash code was also corrected to format SHA-256 bytes as unsigned hex.

Wrapper validation:

```bash
./scripts/run-tournament --bots target-leader,cautious,attack-when-stronger --rounds 1 --seed 1 --format text --max-actions 1000
./scripts/run-tournament --bots target-leader,cautious,attack-when-stronger --rounds 1 --seed 1 --format text --max-actions 1000
```

Result: first command rebuilt the CLI; second command printed `Tournament CLI unchanged; using existing build.`

```bash
./scripts/run-tournament --bots target-leader,cautious,attack-when-stronger --rounds 3 --seed 1 --format csv --max-actions 1000 --out build/tmp/tournament-report.csv
```

Result: wrapper reused existing build and wrote CSV output to `build/tmp/tournament-report.csv`.

### 2026-05-04 - Replay UX planning

Updated `TOURNAMENT_PLAN.md` with the next replay/debug UX decisions:

- replace `--steps` with `--last-steps`
- replay CLI should run to terminal state and print only the last N entries
- remove `--until-failed` and `--until-complete`
- terminal entries should be labeled `End:`
- add a pasteable `ROUND_REPLAY_SPEC` block for reports, CLI tooling, and future GUI input
- add shared `RoundReplaySpecParser`/formatter and `RoundActionDebugFormatter`
- future GUI should support paste spec, Run To End, forward/back 1/10/100, and Reset
- GUI can Run To End and then step backward by storing a replay timeline; checkpoint optimization can come later

Added future tracking rows R7, G1, and G2. No code changes for this planning update.

### 2026-05-04 - Failed-round repro and replay updates

Red/green cycles:

1. Added `RoundReproMetadataTest`.
   - Red: `./gradlew :shared:jvmTest --console=plain` failed with unresolved `deriveRoundSeed` and missing round repro fields on `RoundResult`/`RoundConfig`.
   - Green: added per-round seed derivation and repro metadata; rerun passed.
2. Added `BotRoundStepperTest`.
   - Red: `./gradlew :shared:jvmTest --console=plain` failed with unresolved `BotRoundStepper`, `RoundReplaySpec`, and `RoundConfig.logActions`.
   - Green: added stepper/action-log model and refactored `BotGameRunner`; rerun passed.
3. Added `TournamentRunnerTest` cases for `logFailedRounds` and `logAllRounds`.
   - Red: missing `TournamentConfig.logFailedRounds`/`logAllRounds`.
   - Green: added logging policy; rerun passed.
4. Updated `TournamentReportFormatterTest` for repro metadata.
   - Red: text/CSV reports lacked round seed, seats, max actions, replay command, and action-log count.
   - Green: updated formatters; rerun passed.
5. Updated `CliOptionsTest` for action-log flags and replay subcommand.
   - Red: missing CLI log flag fields and replay support.
   - Green: added flags/subcommand and replay formatting; rerun passed.

Smoke-tested failed-round report:

```bash
./scripts/run-tournament --bots target-leader,cautious --rounds 1 --seed 42 --format text --max-actions 2 --log-failed-rounds
```

Result: report included tournament seed `42`, round seed `1502463084`, seats `target-leader,cautious`, max actions `2`, action-log count, and a copyable `./scripts/replay-round ...` command.

Smoke-tested replay:

```bash
./scripts/replay-round --round-seed 1502463084 --seats target-leader,cautious --max-actions 2 --last-steps 3
```

Result: printed replay entries and failed at the same max-action limit.

### 2026-05-04 - Bot naming cleanup

Renamed bots in code, CLI IDs, tests, and docs:

| Old class / ID | New class / ID |
|---|---|
| `DefaultAi` / `default` | `TargetTheLeader` / `target-leader` |
| `DefensiveAi` / `defensive` | `CautiousBot` / `cautious` |
| `ExampleAi` / `example` | `AlwaysAttackWhenStrongerBot` / `attack-when-stronger` |

Validation:

```bash
./gradlew :shared:jvmTest :tournamentCli:test --console=plain
./gradlew test --console=plain
./scripts/run-tournament --bots target-leader,cautious,attack-when-stronger --rounds 1 --seed 1 --format text --max-actions 1000
./scripts/replay-round --round-seed 1 --seats target-leader,cautious --max-actions 2 --last-steps 2
```

Result: tests and wrapper smoke checks passed. Full test run emitted existing Compose deprecation warnings.

### 2026-05-04 - R7 implementation

Red/green cycles:

1. Added `RoundReplaySpecTextTest`.
   - Red: unresolved `RoundReplaySpecText` and `RoundReplaySpecParser`.
   - Green: added shared pasteable replay spec parser/formatter.
2. Added `RoundActionDebugFormatterTest`.
   - Red: unresolved `RoundActionDebugFormatter`.
   - Green: added shared formatter with `Step` and `End` output.
3. Updated report formatter tests to require `ROUND_REPLAY_SPEC` and `--last-steps 50`.
   - Red: reports lacked spec block and last-step replay command.
   - Green: updated plain text failed-round report formatting.
4. Updated CLI tests to require `ReplayOptions.lastSteps`, reject removed flags, and print last-step replay output.
   - Red: missing `lastSteps` and old replay behavior still used first `--steps` entries.
   - Green: replay CLI now runs to terminal state, keeps a rolling last-N buffer, prints `End:`, and no longer supports/help-documents `--steps`, `--until-failed`, or `--until-complete`.

Validation:

```bash
./gradlew :shared:jvmTest :tournamentCli:test --console=plain
./gradlew test --console=plain
./scripts/run-tournament --bots target-leader,cautious --rounds 1 --seed 42 --format text --max-actions 2 --log-failed-rounds
./scripts/replay-round --round-seed 1502463084 --seats target-leader,cautious --max-actions 5 --last-steps 2
```

Result: tests passed. Full test run emitted existing Compose deprecation warnings. Tournament report included `ROUND_REPLAY_SPEC` and a `--last-steps 50` replay command. Replay output printed only the final entries, including `End: round failed`.

### 2026-05-04 - T7

Final validation:

```bash
./gradlew :shared:jvmTest :tournamentCli:test --console=plain
```

Result: passed.

```bash
./gradlew test --console=plain
```

Result: passed. The build emitted existing Android SDK XML, Compose deprecation, Gradle deprecation, and problems-report notices, but tests completed successfully.

Gradle CLI and wrapper text/CSV commands listed above also passed. Gradle emits existing deprecation/problem-report notices, but the commands complete successfully.

## Required Recurring Checks

The following must remain true throughout tournament implementation:

- Tournament core is bot-only.
- Tournament core is headless and independent from Compose UI.
- Existing game rules and AI strategies remain in common/testable Kotlin where practical.
- Participants rotate seats each round.
- Seeded tournaments are reproducible.
- Failed rounds award no points and are included in final results.
- Text-only outputs are supported before any future UI work.
- Wrapper rebuild skipping is based on a source hash, modeled after `kmp-app-generator`'s `dist/source.hash` approach.
