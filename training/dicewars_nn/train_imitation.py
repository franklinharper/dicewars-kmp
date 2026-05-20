from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

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
    parser.add_argument("--threads", type=int, default=None, help="Number of PyTorch threads (default: all CPUs)")
    args = parser.parse_args(argv)

    import torch
    import torch.nn.functional as F
    from torch import optim

    num_threads = args.threads or os.cpu_count() or 1
    torch.set_num_threads(num_threads)

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
    print(f"PyTorch threads: {torch.get_num_threads()}")

    # Pre-tensorize entire dataset once
    print("Pre-tensorizing dataset...")
    tensors = pretensorize(policy_records, config)

    # Train/val split
    n = len(policy_records)
    perm = torch.randperm(n, generator=torch.Generator().manual_seed(42))
    val_count = max(1, int(n * args.val_split))
    val_idx = perm[:val_count]
    train_idx = perm[val_count:]

    train_tensors = {k: v[train_idx] for k, v in tensors.items()}
    val_tensors = {k: v[val_idx] for k, v in tensors.items()}
    print(f"Train: {len(train_idx)}, Val: {len(val_idx)}")

    device = torch.device("cpu")
    model = build_model(config).to(device)
    optimizer = optim.AdamW(model.parameters(), lr=args.lr, weight_decay=1e-4)

    best_val_loss = float("inf")
    checkpoint_dir = Path(args.checkpoint_dir)
    checkpoint_dir.mkdir(parents=True, exist_ok=True)

    for epoch in range(args.epochs):
        train_loss, train_policy_loss, train_value_loss = train_epoch(
            model, optimizer, train_tensors, args.batch_size, device,
        )
        val_loss, val_policy_loss, val_value_loss = evaluate(
            model, val_tensors, args.batch_size, device,
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


def pretensorize(records: list, config: ModelConfig) -> dict:
    """Convert all records to tensors once, avoiding per-batch Python overhead."""
    import torch
    from dicewars_nn.batch import make_batch

    # Use make_batch on the full dataset to get dense arrays
    batch = make_batch(records)

    node_features = torch.tensor(batch.node_features, dtype=torch.float32)
    adjacency = torch.tensor(batch.adjacency, dtype=torch.float32)
    global_features = torch.tensor(batch.global_features, dtype=torch.float32)
    area_mask = torch.tensor(batch.area_mask, dtype=torch.float32)
    player_mask = torch.tensor(batch.player_mask, dtype=torch.float32)

    # Dense legal action mask: [N, 1025]
    legal_mask = torch.tensor(batch.legal_action_mask, dtype=torch.float32)

    targets = torch.tensor(batch.policy_target, dtype=torch.long)
    value_targets = torch.tensor(batch.value_target, dtype=torch.float32)

    return {
        "node_features": node_features,
        "adjacency": adjacency,
        "global_features": global_features,
        "area_mask": area_mask,
        "player_mask": player_mask,
        "legal_mask": legal_mask,
        "targets": targets,
        "value_targets": value_targets,
    }


def train_epoch(
    model: "torch.nn.Module",
    optimizer: "torch.optim.Optimizer",
    tensors: dict,
    batch_size: int,
    device: "torch.device",
) -> tuple[float, float, float]:
    import torch
    import torch.nn.functional as F

    model.train()
    n = tensors["targets"].shape[0]
    perm = torch.randperm(n)
    n_batches = 0
    total_loss = 0.0
    total_policy_loss = 0.0
    total_value_loss = 0.0

    for start in range(0, n, batch_size):
        idx = perm[start:start + batch_size]

        node_features = tensors["node_features"][idx].to(device)
        adjacency = tensors["adjacency"][idx].to(device)
        global_features = tensors["global_features"][idx].to(device)
        area_mask = tensors["area_mask"][idx].to(device)
        player_mask = tensors["player_mask"][idx].to(device)
        legal_mask = tensors["legal_mask"][idx].to(device)
        targets = tensors["targets"][idx].to(device)
        value_targets = tensors["value_targets"][idx].to(device).unsqueeze(1)

        optimizer.zero_grad()
        outputs = model(node_features, adjacency, global_features, area_mask, player_mask)
        policy_logits = outputs["policy"]
        value_pred = outputs["value"]

        # Policy loss: mask illegal actions to -inf before cross-entropy
        masked_logits = policy_logits.clone()
        masked_logits[legal_mask == 0] = float("-inf")
        policy_loss = F.cross_entropy(masked_logits, targets)

        # Value loss: MSE
        value_loss = F.mse_loss(value_pred, value_targets)

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
    tensors: dict,
    batch_size: int,
    device: "torch.device",
) -> tuple[float, float, float]:
    import torch
    import torch.nn.functional as F

    model.eval()
    n = tensors["targets"].shape[0]
    n_batches = 0
    total_loss = 0.0
    total_policy_loss = 0.0
    total_value_loss = 0.0

    with torch.no_grad():
        for start in range(0, n, batch_size):
            idx = slice(start, min(start + batch_size, n))

            node_features = tensors["node_features"][idx].to(device)
            adjacency = tensors["adjacency"][idx].to(device)
            global_features = tensors["global_features"][idx].to(device)
            area_mask = tensors["area_mask"][idx].to(device)
            player_mask = tensors["player_mask"][idx].to(device)
            legal_mask = tensors["legal_mask"][idx].to(device)
            targets = tensors["targets"][idx].to(device)
            value_targets = tensors["value_targets"][idx].to(device).unsqueeze(1)

            outputs = model(node_features, adjacency, global_features, area_mask, player_mask)
            policy_logits = outputs["policy"]
            value_pred = outputs["value"]

            masked_logits = policy_logits.clone()
            masked_logits[legal_mask == 0] = float("-inf")
            policy_loss = F.cross_entropy(masked_logits, targets)

            value_loss = F.mse_loss(value_pred, value_targets)

            loss = policy_loss + 0.5 * value_loss
            total_loss += loss.item()
            total_policy_loss += policy_loss.item()
            total_value_loss += value_loss.item()
            n_batches += 1

    avg = lambda x: x / max(n_batches, 1)
    return avg(total_loss), avg(total_policy_loss), avg(total_value_loss)


if __name__ == "__main__":
    raise SystemExit(main())
