from __future__ import annotations

import argparse
import json
from pathlib import Path


REQUIRED_ARGS = (
    "model_id",
    "git_commit",
    "training_config_hash",
    "training_seed",
    "environment_version",
    "self_play_game_count",
    "optimizer_settings",
    "network_architecture",
    "checkpoint_path",
    "export_timestamp",
    "onnx_opset",
    "quantization",
    "evaluation_tournament_results",
)


def build_metadata(args: argparse.Namespace) -> dict:
    metadata = {
        "model_id": args.model_id,
        "git_commit": args.git_commit,
        "training_config_hash": args.training_config_hash,
        "training_seed": args.training_seed,
        "environment_version": args.environment_version,
        "map_dataset_hash": args.map_dataset_hash,
        "self_play_game_count": args.self_play_game_count,
        "optimizer_settings": args.optimizer_settings,
        "network_architecture": args.network_architecture,
        "checkpoint_path": args.checkpoint_path,
        "export_timestamp": args.export_timestamp,
        "onnx_opset": args.onnx_opset,
        "quantization": args.quantization,
        "evaluation_tournament_results": args.evaluation_tournament_results,
    }
    validate_metadata(metadata)
    return metadata


def validate_metadata(metadata: dict) -> None:
    for key in REQUIRED_ARGS:
        value = metadata.get(key)
        if isinstance(value, str):
            if not value:
                raise ValueError(f"{key} must not be blank")
        elif value is None:
            raise ValueError(f"{key} is required")
    if metadata["training_seed"] < 0:
        raise ValueError("training_seed must be non-negative")
    if metadata["self_play_game_count"] < 0:
        raise ValueError("self_play_game_count must be non-negative")
    if metadata["onnx_opset"] <= 0:
        raise ValueError("onnx_opset must be positive")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Export Dicewars neural model metadata JSON")
    parser.add_argument("--out", required=True, help="Output model.metadata.json path")
    parser.add_argument("--model-id", required=True)
    parser.add_argument("--git-commit", required=True)
    parser.add_argument("--training-config-hash", required=True)
    parser.add_argument("--training-seed", required=True, type=int)
    parser.add_argument("--environment-version", required=True)
    parser.add_argument("--map-dataset-hash")
    parser.add_argument("--self-play-game-count", required=True, type=int)
    parser.add_argument("--optimizer-settings", required=True)
    parser.add_argument("--network-architecture", required=True)
    parser.add_argument("--checkpoint-path", required=True)
    parser.add_argument("--export-timestamp", required=True)
    parser.add_argument("--onnx-opset", required=True, type=int)
    parser.add_argument("--quantization", required=True)
    parser.add_argument("--evaluation-tournament-results", required=True)
    args = parser.parse_args(argv)

    metadata = build_metadata(args)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(metadata, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"Wrote metadata: {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
