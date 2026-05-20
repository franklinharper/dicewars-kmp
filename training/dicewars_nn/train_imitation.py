from __future__ import annotations

import argparse
import json
import math
import sys
from pathlib import Path

from dicewars_nn.batch import make_batch
from dicewars_nn.dataset import load_all
from dicewars_nn.model import ModelConfig, build_model


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Train the Dicewars neural bot from imitation data")
    parser.add_argument("data", nargs="+", help="Input .jsonl.gz training data files")
    parser.add_argument("--dry-run", action="store_true", help="Load data and build config without training")
    parser.add_argument("--epochs", type=int, default=10, help="Number of training epochs")
    parser.add_argument("--batch-size", type=int, default=64, help="Training batch size")
    parser.add_argument("--lr", type=float, default=1e-3, help="Learning rate")
    parser.add_argument("--val-split", type=float, default=0.1, help="Validation split fraction")
    parser.add_argument("--checkpoint-dir", default="checkpoints", help="Directory to save checkpoints")
    parser.add_argument("--export-onnx", default=None, help="Export final model to this ONNX path")
    args = parser.parse_args(argv)

    import torch
    import torch.nn.functional as F
    from torch import optim

    records = load_all(Path(p) for p in args.data)
    config = ModelConfig()
    config.validate()

    if args.dry_run:
        print(f"Loaded {len(records)} records")
        print(f"Model config: {config}")
        return 0

    # Filter to policy records (policy_weight > 0) for supervised training
    policy_records = [r for r in records if r.policy_weight > 0]
    if not policy_records:
        print("No policy records found (policy_weight > 0 required)", file=sys.stderr)
        return 1

    print(f"Training on {len(policy_records)} policy records, {args.epochs} epochs, batch_size={args.batch_size}")

    # Train/val split
    perm = torch.randperm(len(policy_records), generator=torch.Generator().manual_seed(42))
    val_count = max(1, int(len(policy_records) * args.val_split))
    val_indices = set(perm[:val_count].tolist())
    train_records = [r for i, r in enumerate(policy_records) if i not in val_indices]
    val_records = [r for i, r in enumerate(policy_records) if i in val_indices]
    print(f"Train: {len(train_records)}, Val: {len(val_records)}")

    device = torch.device("cpu")
    model = build_model(config).to(device)
    optimizer = optim.AdamW(model.parameters(), lr=args.lr, weight_decay=1e-4)

    best_val_loss = float("inf")
    checkpoint_dir = Path(args.checkpoint_dir)
    checkpoint_dir.mkdir(parents=True, exist_ok=True)

    for epoch in range(args.epochs):
        train_loss, train_policy_loss, train_value_loss = train_epoch(
            model, optimizer, train_records, args.batch_size, device,
        )
        val_loss, val_policy_loss, val_value_loss = evaluate(
            model, val_records, args.batch_size, device,
        )

        print(
            f"Epoch {epoch + 1}/{args.epochs} "
            f"train_loss={train_loss:.4f} (policy={train_policy_loss:.4f}, value={train_value_loss:.4f}) "
            f"val_loss={val_loss:.4f} (policy={val_policy_loss:.4f}, value={val_value_loss:.4f})"
        )

        if val_loss < best_val_loss:
            best_val_loss = val_loss
            best_path = checkpoint_dir / "best.pt"
            torch.save({
                "epoch": epoch + 1,
                "model_state_dict": model.state_dict(),
                "optimizer_state_dict": optimizer.state_dict(),
                "val_loss": val_loss,
                "config": {
                    "node_feature_count": config.node_feature_count,
                    "global_feature_count": config.global_feature_count,
                    "hidden_size": config.hidden_size,
                    "message_passing_layers": config.message_passing_layers,
                    "action_count": config.action_count,
                },
            }, best_path)
            print(f"  Saved best model: {best_path} (val_loss={val_loss:.4f})")

    # Export ONNX if requested
    if args.export_onnx:
        from dicewars_nn.export_onnx import export_model
        export_model(model, config, Path(args.export_onnx))
        print(f"Exported ONNX model to {args.export_onnx}")

    return 0


def train_epoch(
    model: "torch.nn.Module",
    optimizer: "torch.optim.Optimizer",
    records: list,
    batch_size: int,
    device: "torch.device",
) -> tuple[float, float, float]:
    import torch
    import torch.nn.functional as F

    model.train()
    total_loss = 0.0
    total_policy_loss = 0.0
    total_value_loss = 0.0
    n_batches = 0

    # Shuffle
    indices = torch.randperm(len(records), generator=torch.Generator().manual_seed(42)).tolist()

    for start in range(0, len(indices), batch_size):
        batch_indices = indices[start:start + batch_size]
        batch = make_batch([records[i] for i in batch_indices])

        optimizer.zero_grad()
        policy_logits, value_pred = forward_batch(model, batch, device)

        # Policy loss: cross-entropy with legal action masking
        legal_mask = torch.tensor(batch.legal_action_mask, dtype=torch.float32, device=device)
        # Mask illegal actions to -inf before softmax
        masked_logits = policy_logits.clone()
        masked_logits[legal_mask == 0] = float("-inf")

        targets = torch.tensor(batch.policy_target, dtype=torch.long, device=device)
        policy_loss = F.cross_entropy(masked_logits, targets)

        # Value loss: MSE
        value_targets = torch.tensor(batch.value_target, dtype=torch.float32, device=device).unsqueeze(1)
        value_loss = F.mse_loss(value_pred, value_targets)

        # Combined loss (policy only, since we're filtering to policy_weight > 0)
        loss = policy_loss + 0.5 * value_loss

        loss.backward()
        optimizer.step()

        total_loss += loss.item()
        total_policy_loss += policy_loss.item()
        total_value_loss += value_loss.item()
        n_batches += 1

    avg = lambda x: x / max(n_batches, 1)
    return avg(total_loss), avg(total_policy_loss), avg(total_value_loss)


def evaluate(
    model: "torch.nn.Module",
    records: list,
    batch_size: int,
    device: "torch.device",
) -> tuple[float, float, float]:
    import torch
    import torch.nn.functional as F

    model.eval()
    total_loss = 0.0
    total_policy_loss = 0.0
    total_value_loss = 0.0
    n_batches = 0

    with torch.no_grad():
        for start in range(0, len(records), batch_size):
            batch_records = records[start:start + batch_size]
            batch = make_batch(batch_records)

            policy_logits, value_pred = forward_batch(model, batch, device)

            legal_mask = torch.tensor(batch.legal_action_mask, dtype=torch.float32, device=device)
            masked_logits = policy_logits.clone()
            masked_logits[legal_mask == 0] = float("-inf")

            targets = torch.tensor(batch.policy_target, dtype=torch.long, device=device)
            policy_loss = F.cross_entropy(masked_logits, targets)

            value_targets = torch.tensor(batch.value_target, dtype=torch.float32, device=device).unsqueeze(1)
            value_loss = F.mse_loss(value_pred, value_targets)

            loss = policy_loss + 0.5 * value_loss
            total_loss += loss.item()
            total_policy_loss += policy_loss.item()
            total_value_loss += value_loss.item()
            n_batches += 1

    avg = lambda x: x / max(n_batches, 1)
    return avg(total_loss), avg(total_policy_loss), avg(total_value_loss)


def forward_batch(model, batch, device):
    import torch

    node_features = torch.tensor(batch.node_features, dtype=torch.float32, device=device)
    adjacency = torch.tensor(batch.adjacency, dtype=torch.float32, device=device)
    global_features = torch.tensor(batch.global_features, dtype=torch.float32, device=device)
    area_mask = torch.tensor(batch.area_mask, dtype=torch.bool, device=device)
    player_mask = torch.tensor(batch.player_mask, dtype=torch.bool, device=device)

    outputs = model(node_features, adjacency, global_features, area_mask, player_mask)
    return outputs["policy"], outputs["value"]


if __name__ == "__main__":
    raise SystemExit(main())
