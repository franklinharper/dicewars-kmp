# Dicewars Bot Tournament Plan

Package: `com.franklinharper.dicewarsport`

This document plans a bot-only tournament mode for running unattended AI experiments. No Compose UI will be built for the first tournament version.

## Goals

1. Run games between AI bots without human input.
2. Run a configurable number of rounds.
3. Use reproducible randomness when a seed is provided.
4. Rotate participant seating each round for fairness.
5. Report final scores and failed rounds using text-only outputs.
6. Provide a wrapper script that rebuilds the tournament CLI only when source inputs changed.

## Non-goals for v1

- No Compose tournament screen.
- No animated tournament playback.
- No human players.
- No Web/Wasm or iOS tournament entry point.
- No complex bot-configuration DSL.

## Tournament Inputs

Primary tournament parameters:

```kotlin
data class TournamentConfig(
    val participants: List<TournamentParticipant>,
    val rounds: Int,
    val seed: Int? = null,
    val maxActionsPerRound: Int = 100_000,
)
```

Participants are bot definitions:

```kotlin
data class TournamentParticipant(
    val id: String,
    val displayName: String,
    val aiFactory: (RandomSource) -> AiStrategy,
)
```

Initial built-in participant IDs:

| ID | Strategy |
|---|---|
| `bully` | `AlwaysAttackWhenStrongerBot` |
| `rebel` | `TargetTheLeader` |
| `turtle` | `CautiousBot` |

## Round Lifecycle

Each round:

1. Creates a fresh game map with `DicewarsGame.generate()`.
2. Seats bots into player slots using the round's rotated participant order.
3. Runs only AI turns.
4. Continues until one bot remains active or the max-action guard is reached.
5. Records elimination order, winner, actions taken, and completion/failure status.

The tournament engine should be pure/headless and should not depend on Compose, sound, animation, or UI reducer timing.

## Seating Fairness

Participant seating must rotate each round.

Example with participants `A`, `B`, `C`, `D`:

| Round | Seat order |
|---:|---|
| 1 | A, B, C, D |
| 2 | B, C, D, A |
| 3 | C, D, A, B |
| 4 | D, A, B, C |
| 5 | A, B, C, D |

This is deterministic and avoids one bot always receiving the same player slot.

## Randomness and Reproducibility

The tournament supports an optional seed.

- If `seed` is provided, the full tournament should be reproducible.
- If `seed` is omitted, generate an effective tournament seed and include it in reports so the result can be reproduced.
- Tests should use deterministic fake or seeded random sources.

Chosen approach:

- generate/report one effective tournament seed
- derive an independent round seed from the tournament seed and round number
- include each failed round's round seed, seat order, and max-action limit in reports

This lets a failed round be reproduced directly without replaying earlier tournament rounds.

## Max-action Guard and Failed Rounds

Each round has a safety limit:

```kotlin
maxActionsPerRound = 100_000
```

If a round reaches the limit before a winner is found:

- mark the round as failed/incomplete
- award no points to any bot for that round
- include the failed round in reports
- include repro metadata: tournament seed, round seed, seat order, and max actions
- continue the tournament unless the CLI or caller explicitly fails on incomplete rounds in a future version

## Scoring

For each completed round with `N` players:

| Placement | Points |
|---|---:|
| first bot eliminated | 0 |
| second bot eliminated | 1 |
| third bot eliminated | 2 |
| ... | ... |
| winner | `2 * N` |

Examples:

### 2 players

| Placement | Points |
|---|---:|
| first eliminated | 0 |
| winner | 4 |

### 3 players

| Placement | Points |
|---|---:|
| first eliminated | 0 |
| second eliminated | 1 |
| winner | 6 |

### 4 players

| Placement | Points |
|---|---:|
| first eliminated | 0 |
| second eliminated | 1 |
| third eliminated | 2 |
| winner | 8 |

Failed rounds award `0` points to every bot.

## Result Model Draft

```kotlin
data class TournamentResult(
    val roundsRequested: Int,
    val roundsCompleted: Int,
    val roundsFailed: Int,
    val seed: Int?,
    val botScores: List<BotScore>,
    val roundResults: List<RoundResult>,
)

data class BotScore(
    val participantId: String,
    val displayName: String,
    val score: Int,
    val wins: Int,
)

data class RoundResult(
    val roundNumber: Int,
    val completed: Boolean,
    val winnerParticipantId: String?,
    val eliminationOrder: List<String>,
    val scores: Map<String, Int>,
    val actionsTaken: Int,
    val failureReason: String? = null,
)
```

## Headless Runner Draft

```kotlin
class BotGameRunner {
    fun runRound(config: RoundConfig): RoundResult
}

class TournamentRunner {
    fun run(config: TournamentConfig): TournamentResult
}
```

The runner should:

- use `AiStrategy.chooseMove(game)` for the current bot
- validate every move with `DicewarsGame.isLegalAttack()`
- treat `null` or illegal moves as no attack and end/supply the bot's turn
- detect newly eliminated bots after battles and/or turns
- stop when one active bot remains
- stop with failure when `maxActionsPerRound` is reached

## Text-only Output Options

### Plain text

Default human-readable output.

Example:

```text
Dicewars Bot Tournament
Rounds requested: 100
Rounds completed: 98
Rounds failed: 2
Seed: 42

Scores:
1. Turtle  522 pts  41 wins
2. Rebel    486 pts  36 wins
3. Bully        129 pts  21 wins

Failed rounds:
- Round 17 exceeded maxActionsPerRound=100000
- Round 83 exceeded maxActionsPerRound=100000
```

### CSV

Machine-friendly output for spreadsheets and scripts.

Score CSV example:

```csv
section,tournament_seed,round_seed,seats,max_actions,action_log_entries,rank,bot_id,bot_name,score,wins,round,completed,winner,actions_taken,failure_reason
score,42,,,,,1,turtle,Turtle,522,41,,,,,
score,42,,,,,2,rebel,Rebel,486,36,,,,,
score,42,,,,,3,bully,Bully,129,21,,,,,
```

Round CSV example:

```csv
section,tournament_seed,round_seed,seats,max_actions,action_log_entries,rank,bot_id,bot_name,score,wins,round,completed,winner,actions_taken,failure_reason
round,42,1502463084,"rebel,turtle,bully",100000,0,,,,,,1,true,turtle,918,
round,42,-12938422,"turtle,bully,rebel",100000,0,,,,,,2,true,rebel,731,
round,42,92815594,"bully,rebel,turtle",100000,100001,,,,,,17,false,,100000,max actions exceeded
```

CSV is part of v1.

### JSON and Markdown

JSON and Markdown are useful future formats but are not required for the first implementation.

## Round Replay and Failed-round Repro Plan

Failed rounds must be reproducible directly from the report.

Each `RoundResult` includes:

```kotlin
val roundSeed: Int
val seatedParticipantIds: List<String>
val maxActionsPerRound: Int
val actionLog: List<RoundActionLogEntry>
```

Action logs are optional:

- default: no action logs
- `--log-failed-rounds`: rerun failed rounds with the same round seed and capture their action logs
- `--log-all-rounds`: capture logs for every round

### Replay spec text block

Reports should include a pasteable replay spec block that can be consumed by both CLI tooling and a future Compose replay GUI:

```text
ROUND_REPLAY_SPEC
roundSeed=1502463084
seats=rebel,turtle
maxActions=2
lastSteps=50
END_ROUND_REPLAY_SPEC
```

Shared parser/formatter shape:

```kotlin
data class RoundReplaySpecText(
    val roundSeed: Int,
    val seatIds: List<String>,
    val maxActions: Int,
    val lastSteps: Int = 50,
)

object RoundReplaySpecParser {
    fun parse(text: String): RoundReplaySpecText
    fun format(spec: RoundReplaySpecText): String
}
```

Notes:

- `roundSeed`, `seats`, and `maxActions` are round-repro inputs.
- `lastSteps` is a CLI display preference only.
- The GUI should ignore `lastSteps` for stepping/navigation and use its own interactive controls.
- Loading replay specs from a report file is deferred; pasteable text input comes first.

Replay command:

```bash
./scripts/replay-round \
  --round-seed 1502463084 \
  --seats rebel,turtle \
  --max-actions 2 \
  --last-steps 50
```

Failed-round text reports include a copyable repro command:

```text
./scripts/replay-round --round-seed 1502463084 --seats rebel,turtle --max-actions 2 --last-steps 50
```

The replay CLI uses the same `BotRoundStepper` as the tournament runner, so replay and unattended tournament execution stay aligned.

### Replay CLI last-step output

`replay-round` is non-interactive. It should replay to terminal state, keep a rolling buffer of the last N replay entries, and print that buffer.

CLI flags:

```text
--round-seed <int>    Required round seed from report/spec.
--seats <ids>         Required comma-separated bot IDs in seated order.
--max-actions <int>   Required/explicit original round max-action limit for exact repro.
--last-steps <int>    Number of final replay entries to print. Default: 50.
```

Remove completely:

```text
--steps
--until-failed
--until-complete
```

No custom compatibility/help messages are needed for removed flags.

Final replay entries use normal step labels for bot actions and `End:` for terminal entries:

```text
Step 99999: rebel attacks 11 -> 12, fail, eliminated: none
Step 100000: turtle ends turn, supplied: none, eliminated: none
End: round failed, eliminated: none
Failed: maxActionsPerRound=100000 exceeded
```

### Shared action debug formatting

Replay event text should be produced by common code, not CLI-only code:

```kotlin
object RoundActionDebugFormatter {
    fun format(entry: RoundActionLogEntry): String
}
```

This formatter should be used by:

- `replay-round` CLI
- future Compose replay GUI action log/sidebar
- tests that lock down wording

### Future Compose replay GUI plan

The Compose replay GUI should start as a debug screen/tool, not a full tournament UI.

Input:

- multiline text field where the user pastes a `ROUND_REPLAY_SPEC` block
- `Load Spec` button parses the block with `RoundReplaySpecParser`

Controls:

```text
Back 100
Back 10
Back 1
Forward 1
Forward 10
Forward 100
Run To End
Reset
```

The GUI uses:

```kotlin
BotRoundStepper
BotRoundState
RoundActionLogEntry
RoundActionDebugFormatter
```

Suggested GUI state:

```kotlin
data class ReplayTimelineEntry(
    val action: RoundActionLogEntry?,
    val state: BotRoundState,
)

data class ReplayUiState(
    val inputText: String = "",
    val parsedSpec: RoundReplaySpecText? = null,
    val timeline: List<ReplayTimelineEntry> = emptyList(),
    val currentIndex: Int = 0,
    val error: String? = null,
)
```

The first timeline entry is the initial state:

```kotlin
ReplayTimelineEntry(action = null, state = initialState)
```

Each forward step appends:

```kotlin
ReplayTimelineEntry(action = step.actionLogEntry, state = step.state)
```

The board renders:

```kotlin
timeline[currentIndex].state.game
```

`RunToEnd` should step until the round completes or fails, append all timeline entries, and set:

```kotlin
currentIndex = timeline.lastIndex
```

After `RunToEnd`, the user can step backward immediately because previous states are already in the timeline:

```kotlin
currentIndex = (currentIndex - 1).coerceAtLeast(0)
currentIndex = (currentIndex - 10).coerceAtLeast(0)
currentIndex = (currentIndex - 100).coerceAtLeast(0)
```

Initial implementation may store every state. If memory/performance becomes a problem for very long failed rounds, the internals can later be replaced with checkpoints plus replay-forward-from-checkpoint without changing the user-facing controls.

Future deferred enhancement:

- load replay spec from a report file instead of paste-only input

## CLI Plan

Add a JVM-only CLI module:

```text
tournamentCli/
```

Expected command:

```bash
./gradlew :tournamentCli:run --args="--bots rebel,turtle,bully --rounds 100 --seed 42 --format text"
```

CLI options:

| Option | Required | Default | Description |
|---|---|---|---|
| `--bots` | yes | N/A | comma-separated participant IDs |
| `--rounds` | yes | N/A | number of rounds to attempt |
| `--seed` | no | random | integer seed for reproducibility |
| `--format` | no | `text` | `text` or `csv` in v1 |
| `--out` | no | stdout | optional output file path |
| `--max-actions` | no | `100000` | max actions per round |

## Smart Wrapper Script Plan

Add:

```text
scripts/run-tournament
```

The wrapper should:

1. compute a source hash
2. compare it to `dist/tournament-cli/source.hash`
3. skip Gradle if the hash matches and the CLI artifact exists
4. rebuild if the hash differs or the artifact is missing
5. update `dist/tournament-cli/source.hash` after successful build
6. launch the CLI and pass all arguments through unchanged

Target user command:

```bash
./scripts/run-tournament \
  --bots rebel,turtle,bully \
  --rounds 1000 \
  --seed 12345 \
  --format csv \
  --out reports/ai-tournament-1000.csv
```

## Wrapper Hash Inputs

The source hash should include at least:

```text
shared/src/commonMain/kotlin/
shared/src/jvmMain/kotlin/
tournamentCli/src/main/kotlin/
build.gradle.kts
settings.gradle.kts
shared/build.gradle.kts
tournamentCli/build.gradle.kts
gradle/libs.versions.toml
```

This follows the same idea as `kmp-app-generator`, where `dist/source.hash` allows a session hook or launcher to skip Gradle when sources are unchanged.

## Gradle Distribution Task Plan

Add a task such as:

```bash
./gradlew :tournamentCli:generateTournamentDist
```

The task should:

- build the runnable CLI artifact
- copy it into `dist/tournament-cli/`
- write the matching source hash
- optionally write or refresh a generated launcher script

## Acceptance Summary

Tournament v1 is complete when:

1. scoring is tested
2. a headless round runner is tested
3. a multi-round tournament runner is tested
4. seating rotation is tested
5. seeded reproducibility is tested
6. failed rounds award no points and are reported
7. plain text and CSV report formatters are tested
8. the CLI runs from Gradle
9. the smart wrapper skips rebuilds when unchanged and rebuilds when needed
10. follow-up tournament work is recorded in `TASKS.md`
