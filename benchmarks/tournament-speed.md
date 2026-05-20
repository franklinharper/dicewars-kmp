# Tournament Speed Benchmarks

Purpose: track tournament-mode performance while verifying that speed optimizations do not change deterministic tournament results.

## Fixed benchmark configuration

```bash
./scripts/run-tournament \
  --rounds 1000 \
  --seed 20260520 \
  --bots bully,emperor,frontier-commander,max,optimus,terminator2,turtle \
  --parallel 8
```

Golden deterministic report:

```text
benchmarks/tournament-speed-baseline-1000.report.txt
```

Determinism check:

```bash
./scripts/check-tournament-speed-baseline
```

The check compares the current report to the golden report after removing the non-deterministic `Duration:` line.

## Results

| Label | Commit | Rounds | Seed | Bots | Parallel | Report duration | real | user | sys | Rounds/sec (real) | Notes |
|---|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---|
| Baseline 1k | 885f3d6 + working tree | 1,000 | 20260520 | bully,emperor,frontier-commander,max,optimus,terminator2,turtle | 8 | 10s 709ms | 11.52s | 70.01s | 0.76s | 86.81 | Iteration baseline |
| Baseline 10k | 885f3d6 + working tree | 10,000 | 20260520 | bully,emperor,frontier-commander,max,optimus,terminator2,turtle | 8 | 1m 44s 40ms | 104.85s | 701.71s | 6.72s | 95.37 | Long-run confirmation baseline |
| Optimized 10k | a8fffe0 + working tree | 10,000 | 20260520 | bully,emperor,frontier-commander,max,optimus,terminator2,turtle | 8 | 38s 269ms | ~39s | — | — | ~262 | FastGameState + setAreaTc optimization. Scores identical to baseline. |
| FastGameState v1 | a8fffe0 + working tree | 1,000 | 20260520 | bully,emperor,frontier-commander,max,optimus,terminator2,turtle | 8 | ~10.4s | ~10.8s | ~70.8s | ~0.6s | ~92.6 | Mutable game state, snapshot for bot calls, fast battle rolls |
| + setAreaTc avoid precomputeNeighbors | a8fffe0 + working tree | 1,000 | 20260520 | bully,emperor,frontier-commander,max,optimus,terminator2,turtle | 8 | 7s 191ms | 7.3s | ~52s | ~0.5s | ~137 | Skip precomputeNeighbors in setAreaTc, use adjacentAreas directly + cached neighbors on DicewarsGame |

## Appending successful optimizations

For each successful optimization, append a row with the same fixed benchmark configuration and run:

```bash
./scripts/check-tournament-speed-baseline
```

Only record an optimization as successful if:

1. the deterministic report matches the golden baseline, and
2. the benchmark time improves enough to be meaningful.
