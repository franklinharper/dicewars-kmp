from __future__ import annotations

import argparse
from pathlib import Path

from dicewars_nn.model import ACTION_COUNT


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate an exported Dicewars ONNX model")
    parser.add_argument("model", help="ONNX model path")
    parser.add_argument("--dry-run", action="store_true", help="Validate path/config without requiring ONNX Runtime")
    args = parser.parse_args(argv)

    model_path = Path(args.model)
    if not model_path.exists():
        raise FileNotFoundError(model_path)

    if args.dry_run:
        print(f"ONNX validation dry run: {model_path}")
        print(f"Expected outputs: policy[{ACTION_COUNT}], value[1]")
        return 0

    try:
        import onnxruntime as ort
    except ImportError as exc:
        raise RuntimeError("ONNX Runtime is required for model validation") from exc

    session = ort.InferenceSession(str(model_path))
    output_names = {output.name for output in session.get_outputs()}
    if "policy" not in output_names or "value" not in output_names:
        raise ValueError(f"model outputs must include policy and value, got {sorted(output_names)}")
    print(f"ONNX model valid: {model_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
