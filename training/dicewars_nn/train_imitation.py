from __future__ import annotations

import argparse
from pathlib import Path

from dicewars_nn.dataset import load_all
from dicewars_nn.model import ModelConfig, build_model


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Train the Dicewars neural bot from imitation data")
    parser.add_argument("data", nargs="+", help="Input .jsonl.gz training data files")
    parser.add_argument("--dry-run", action="store_true", help="Load data and build config without training")
    args = parser.parse_args(argv)

    records = load_all(Path(path) for path in args.data)
    config = ModelConfig()
    config.validate()
    if args.dry_run:
        print(f"Loaded {len(records)} records")
        print(f"Model config: {config}")
        return 0

    # Import/build torch model only when actual training is requested.
    model = build_model(config)
    raise NotImplementedError(f"training loop not implemented yet: {model.__class__.__name__}")


if __name__ == "__main__":
    raise SystemExit(main())
