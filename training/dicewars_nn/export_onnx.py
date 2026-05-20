from __future__ import annotations

import argparse
from pathlib import Path

from dicewars_nn.model import AREA_MAX, PLAYER_MAX, ModelConfig, build_model, output_shapes


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Export the Dicewars neural bot model to ONNX")
    parser.add_argument("--out", required=True, help="Output ONNX path")
    parser.add_argument("--batch-size", type=int, default=1, help="Dummy export batch size")
    parser.add_argument("--dry-run", action="store_true", help="Validate export config without requiring torch")
    args = parser.parse_args(argv)

    config = ModelConfig()
    shapes = output_shapes(args.batch_size, config)
    out = Path(args.out)

    if args.dry_run:
        print(f"ONNX export dry run")
        print(f"Output: {out}")
        print(f"Inputs: node_features, adjacency, global_features, area_mask, player_mask")
        print(f"Outputs: policy{shapes['policy']}, value{shapes['value']}")
        return 0

    try:
        import torch
    except ImportError as exc:
        raise RuntimeError("PyTorch is required for ONNX export") from exc

    model = build_model(config)
    model.eval()
    batch = args.batch_size
    dummy_inputs = (
        torch.zeros(batch, AREA_MAX, config.node_feature_count, dtype=torch.float32),
        torch.zeros(batch, AREA_MAX, AREA_MAX, dtype=torch.float32),
        torch.zeros(batch, config.global_feature_count, dtype=torch.float32),
        torch.ones(batch, AREA_MAX, dtype=torch.bool),
        torch.ones(batch, PLAYER_MAX, dtype=torch.bool),
    )
    out.parent.mkdir(parents=True, exist_ok=True)
    torch.onnx.export(
        model,
        dummy_inputs,
        out,
        input_names=["node_features", "adjacency", "global_features", "area_mask", "player_mask"],
        output_names=["policy", "value"],
        dynamic_axes={
            "node_features": {0: "batch"},
            "adjacency": {0: "batch"},
            "global_features": {0: "batch"},
            "area_mask": {0: "batch"},
            "player_mask": {0: "batch"},
            "policy": {0: "batch"},
            "value": {0: "batch"},
        },
        opset_version=18,
    )
    print(f"Exported ONNX model to {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
