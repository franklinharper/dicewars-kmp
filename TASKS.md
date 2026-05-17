# Tasks

Small changes, bug fixes, and deferred work for Dicewars KMP.

## Deferred validation

- [ ] Validate Web/Wasm target when project priority changes.
  - Suggested command: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
  - Current status: intentionally deferred during core port validation.
- [ ] Validate iOS target when project priority changes.
  - Suggested command: `./gradlew :shared:embedAndSignAppleFrameworkForXcode`
  - If the generated task name differs, record the actual task here.
  - Current status: intentionally deferred during core port validation.

## Deferred stats persistence

- [ ] Add Desktop persistence for win/loss/score stats.
- [ ] Add Web/Wasm persistence for win/loss/score stats.
- [ ] Add iOS persistence for win/loss/score stats.

## Replay GUI follow-ups

- [ ] Compose replay GUI state/reducer.
  - Support paste replay spec, load, step forward/back 1/10/100, run to end, and reset.
  - Store every timeline state initially; optimize with checkpoints later if needed.
- [ ] Compose replay debug screen.
  - Build UI over replay reducer and existing board renderer.
  - Loading a replay spec from a report file remains deferred beyond paste input.
