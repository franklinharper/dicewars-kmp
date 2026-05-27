# AGENTS.md

Guidance for coding agents working in this repository.

## Project overview

This is `dicewars-port`, a Kotlin Multiplatform Dicewars project with Android, Desktop, Web/Wasm, shared game logic, and CLI tools for tournaments/training.

Key modules:

- `shared/` — shared Kotlin Multiplatform game/model logic.
- `androidApp/` — Android application.
- `desktopApp/` — Compose Desktop application.
- `webApp/` — Compose Web/Wasm application.
- `tournamentCli/` — headless tournament CLI.
- `trainingCli/` — training CLI.
- `scripts/` — helper scripts such as tournament and replay runners.

## Common commands

Prefer targeted Gradle tasks over full-project builds when possible.

```bash
# Build/check common code and affected modules
./gradlew build

# Android debug APK
./gradlew :androidApp:assembleDebug

# Run desktop app
./gradlew :desktopApp:run

# Run desktop app with hot reload
./gradlew :desktopApp:hotRun --auto

# Build web distribution
./gradlew :webApp:composeCompatibilityBrowserDistribution

# Run a tournament
./scripts/run-tournament --bots rebel,turtle,bully,max --rounds 100 --seed 1

# Replay a tournament round
./scripts/replay-round --round-seed 123 --seats rebel,turtle,max --max-actions 100000 --last-steps 50
```

## Working conventions

- Keep changes focused and minimal.
- Use Kotlin Multiplatform-compatible APIs in `shared/`.
- Do not introduce platform-specific code into shared logic unless it is behind proper expect/actual boundaries or platform modules.
- Prefer deterministic behavior for game simulation, bot logic, tournament, replay, and training code. Preserve seeds and replayability.
- Update README or task/plan docs when user-facing commands, bot behavior, or workflows change.
- Avoid committing generated build outputs (`build/`, `.gradle/`, distribution artifacts, APKs, etc.).
- Do not commit files larger than 100 KB. If a necessary artifact would exceed this limit, ask for guidance or document how to generate it instead.

## Validation guidance

- Run the most specific Gradle task for the changed module.
- For shared game logic or bot/tournament changes, run relevant CLI/tournament commands in addition to compilation.
- Web/Wasm and iOS validation may be deferred unless the task specifically targets those platforms; record any deferred validation in your final response.

## Notes for bots and tournaments

- Bot IDs and behavior are documented in `README.MD`.
- Tournament scripts rebuild the CLI only when needed and then run from `dist/tournament-cli/`.
- When changing bot policy, replay, or scoring, check for determinism and include enough information to reproduce failures by seed.
