# Neural Bot Requirements

## Goal

Build a neural-network-based Dicewars bot optimized for **tournament score**.

Primary metric:

```text
10,000-round tournament score
```

The bot should improve through both:

- playing against current heuristic bots,
- self-play / league play.

## Initial tournament field

Each tournament has **7 participants**.

Initial field:

```text
neural
bully
emperor
frontier-commander
max
optimus
terminator2
```

Initially excluded:

```text
rebel
terminator
```

Reason:

- `rebel` is the weakest heuristic bot.
- `terminator` is excluded to make room for the neural bot while keeping tournament size at 7.

## Replacement rule

After each 10,000-round evaluation tournament:

- If neural bot is **last**, reject candidate.
- If neural bot is **not last**, eliminate the lowest-performing **non-neural** bot.
- Continue with the updated league.

## Training approach

Weights start **from scratch**.

But training should not be pure random self-play initially.

Recommended pipeline:

1. **Supervised warm start**
   - Generate training examples from heuristic bots.
   - Learn basic legal/playable behavior.
2. **Self-play**
   - Neural bot improves against itself.
3. **League/tournament fine-tuning**
   - Train/evaluate against current surviving bot pool.

## Algorithm choice

Use **AlphaZero-style stochastic MCTS**, not full MuZero initially.

Reason:

- Dicewars has known rules.
- Battle randomness can be modeled with exact chance probabilities.
- MuZero is more complex and unnecessary as a first step.

Search should include:

```text
decision nodes: choose attack/end turn
chance nodes: battle win/loss
value estimates: neural network
policy priors: neural network
```

This allows the bot to evaluate multi-step plans, including multiple attacks in one turn.

## Multi-step strategy requirement

The model/search must be able to see beneficial move sequences, not just one-step gains.

Plan:

- Model chooses one action at a time.
- MCTS searches forward through multiple actions.
- Battle outcomes are represented as chance nodes.
- Runtime budget controls how deep/wide search is.

## Runtime strength settings

Runtime budget is selectable in user settings.

Initial levels:

```text
Policy only
64 simulations
256 simulations
1024 simulations
```

Default recommendation:

```text
64 or 256 simulations
```

depending on Android performance.

## Simulator/training infrastructure

Do **not** rewrite the full simulator in Python as the primary source of truth.

Use:

```text
Kotlin simulator/game engine as authoritative
Python/PyTorch for neural-network training
ONNX for inference
```

Recommended architecture:

```text
Kotlin trainingCli -> generate data / evaluate models
Python PyTorch -> train model
ONNX model -> loaded by app and tournament/training tools
```

## CLI decision

Create a separate module:

```text
trainingCli
```

Do not overload the existing tournament CLI.

Existing tournament CLI remains for:

- official tournaments,
- final validation,
- reports,
- replay/debugging.

New `trainingCli` handles:

- simulator benchmarking,
- imitation data generation,
- self-play data generation,
- model evaluation,
- metadata export,
- training-oriented output formats.

Suggested commands:

```bash
./scripts/training-cli benchmark-simulator
./scripts/training-cli generate-imitation-data
./scripts/training-cli generate-self-play-data
./scripts/training-cli evaluate-model
./scripts/training-cli export-model-metadata
```

## Speed/performance decisions

Use existing Kotlin game logic initially, but optimize for training.

Potential future optimizations:

- mutable headless state representation,
- primitive arrays for owners/dice/stocks,
- precomputed adjacency,
- global cached dice probabilities,
- reduced object allocation,
- parallel self-play workers,
- benchmark harness measuring:
  - rounds/sec,
  - moves/sec,
  - self-play games/sec,
  - MCTS nodes/sec.

## Model architecture recommendation

Use a graph-based neural network.

Dicewars is naturally a graph:

```text
nodes = territories
edges = adjacency
```

Inputs:

- owner,
- dice count,
- territory size,
- current-player-relative ownership,
- player stock,
- max connected component,
- turn/player context.

Outputs:

- policy over fixed action space,
- value estimating expected tournament score / placement.

Action space:

```text
all from/to territory attack pairs + end turn
```

Invalid actions must be masked.

## Model bridge into app

Use:

```text
ONNX Runtime
```

Pipeline:

```text
PyTorch checkpoint
-> ONNX export
-> ONNX optimization
-> optional quantization
-> app/tournament inference
```

The initial shipped app target is Android, but model storage should not be Android-specific.

## Model artifact location

Do not store model only under `androidApp`.

Use shared platform-neutral location:

```text
models/neuralbot/current/
```

Suggested contents:

```text
models/neuralbot/current/model.onnx
models/neuralbot/current/model.metadata.json
models/neuralbot/current/training-config.yaml
models/neuralbot/current/evaluation-report.md
```

Android build should package/copy from this shared directory.

## Model size limits

Because the neural bot ships in the app:

```text
target size: <= 10 MB
hard max:    <= 25 MB
```

Use:

- model optimization,
- float16,
- quantization if accuracy holds.

## Failure behavior

If the model fails to load:

```text
fail loudly
```

No silent fallback to heuristic bots.

This should apply in:

- app runtime,
- tournament CLI,
- training CLI evaluation.

## Reproducibility requirements

Every model should have metadata containing:

```text
model_id
git_commit
training_config_hash
training_seed
environment_version
map_dataset_hash, if applicable
self_play_game_count
optimizer settings
network architecture
checkpoint path
export timestamp
ONNX opset
quantization settings
evaluation tournament results
```

Use immutable model IDs:

```text
neuralbot-YYYYMMDD-HHMM-<short_git_sha>
```

## Evaluation protocol

Official candidate evaluation:

```bash
./scripts/run-tournament --rounds 10000 --bots neural,bully,emperor,frontier-commander,max,optimus,terminator2 --parallel 8
```

Acceptance:

- neural bot must not finish last,
- neural bot must beat at least one non-neural bot,
- lowest non-neural bot is eliminated if neural is accepted,
- compare by tournament score first, wins second.

Final validation uses existing tournament CLI, not training CLI.

## Testing requirements

### Unit tests

- state encoder shape,
- legal action mask,
- illegal action masking,
- action index <-> move conversion,
- end-turn action,
- model metadata loading,
- model load failure throws loudly.

### Integration tests

- neural bot returns legal moves or null,
- can run on generated games,
- can run in tournament CLI,
- Android asset/model loading works.

### Golden board tests

Fixed boards for:

- one legal attack,
- no legal attacks,
- equal-dice attack,
- dominant economy,
- near elimination,
- endgame duel.

### Tournament tests

- CI smoke: small tournament,
- nightly/larger: larger tournament,
- release gate: 10,000-round evaluation.
