from __future__ import annotations

import argparse
import os
import sys
import time
from pathlib import Path

from dicewars_nn.dataset import load_all
from dicewars_nn.model import ModelConfig, build_model


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Train the Dicewars neural bot from imitation data")
    parser.add_argument("data", nargs="+", help="Input .jsonl.gz training data files")
    parser.add_argument("--dry-run", action="store_true", help="Load data and build config without training")
    parser.add_argument("--epochs", type=int, default=10, help="Number of training epochs")
    parser.add_argument("--batch-size", type=int, default=1024, help="Training batch size")
    parser.add_argument("--lr", type=float, default=1e-3, help="Learning rate")
    parser.add_argument("--val-split", type=float, default=0.1, help="Validation split fraction")
    parser.add_argument("--checkpoint-dir", default="checkpoints", help="Directory to save checkpoints")
    parser.add_argument("--export-onnx", default=None, help="Export final model to this ONNX path")
    parser.add_argument("--threads", type=int, default=None, help="Number of PyTorch threads (default: all CPUs)")
    parser.add_argument("--filter-bot", default=None, help="Only train on records from this bot (e.g. terminator2)")
    parser.add_argument("--tensor-cache", default=None, help="Optional .pt cache path for pre-tensorized records")
    parser.add_argument("--resume-checkpoint", default=None, help="Resume training from checkpoint .pt")
    args = parser.parse_args(argv)

    import torch
    from torch import optim

    num_threads = args.threads or os.cpu_count() or 1
    torch.set_num_threads(num_threads)

    config = ModelConfig()
    config.validate()

    data_paths = [Path(p) for p in args.data]
    fingerprint = build_cache_fingerprint(data_paths, args.filter_bot)
    cache_path = Path(args.tensor_cache) if args.tensor_cache else None

    all_tensors = None
    if cache_path is not None and cache_path.exists():
        t_cache = time.time()
        cached = load_tensor_cache(cache_path)
        if cached.get("fingerprint") == fingerprint:
            all_tensors = cached["tensors"]
            print(f"Loaded tensor cache in {time.time() - t_cache:.1f}s: {cache_path}")
        else:
            print(f"Tensor cache fingerprint mismatch, rebuilding: {cache_path}")

    if all_tensors is None:
        t0 = time.time()
        records = load_all(data_paths)
        t1 = time.time()
        print(f"Loaded {len(records)} records in {t1 - t0:.1f}s")

        if args.dry_run:
            print(f"Model config: {config}")
            return 0

        # Filter to policy records (policy_weight > 0) for supervised training
        policy_records = [r for r in records if r.policy_weight > 0]
        if args.filter_bot:
            before = len(policy_records)
            policy_records = [r for r in policy_records if r.bot_id == args.filter_bot]
            print(f"Filtered to bot={args.filter_bot}: {before} -> {len(policy_records)} records")
        if not policy_records:
            print("No policy records found (policy_weight > 0 required)", file=sys.stderr)
            return 1

        print(f"Policy records: {len(policy_records)}")

        # Pre-tensorize entire dataset once
        print("Pre-tensorizing dataset...", end=" ", flush=True)
        t2 = time.time()
        all_tensors = pretensorize(policy_records, config)
        t3 = time.time()
        print(f"{t3 - t2:.1f}s")

        if cache_path is not None:
            save_tensor_cache(cache_path, all_tensors, fingerprint)
            print(f"Saved tensor cache: {cache_path}")

    elif args.dry_run:
        print(f"Model config: {config}")
        print(f"Cached policy records: {all_tensors['targets'].shape[0]}")
        return 0

    # Train/val split by index (no data copy)
    n = int(all_tensors["targets"].shape[0])
    perm = torch.randperm(n, generator=torch.Generator().manual_seed(42))
    val_count = max(1, int(n * args.val_split))
    val_idx = perm[:val_count]
    train_idx = perm[val_count:]

    # Slice into two persistent tensor dicts on CPU
    train_t = {k: v[train_idx] for k, v in all_tensors.items()}
    val_t = {k: v[val_idx] for k, v in all_tensors.items()}
    del all_tensors  # free memory

    print(f"Train: {train_idx.shape[0]}, Val: {val_idx.shape[0]}")
    print(f"Batch size: {args.batch_size}, Epochs: {args.epochs}, LR: {args.lr}")
    print(f"PyTorch threads: {torch.get_num_threads()}")
    print()

    device = torch.device("cpu")
    model = build_model(config).to(device)
    optimizer = optim.AdamW(model.parameters(), lr=args.lr, weight_decay=1e-4)

    best_val_loss = float("inf")
    best_val_epoch = 0
    start_epoch = 0
    if args.resume_checkpoint:
        resume_path = Path(args.resume_checkpoint)
        if not resume_path.exists():
            raise FileNotFoundError(f"resume checkpoint not found: {resume_path}")
        resume = torch.load(resume_path, map_location="cpu", weights_only=False)
        model.load_state_dict(resume["model_state_dict"])
        optimizer.load_state_dict(resume["optimizer_state_dict"])
        start_epoch = int(resume.get("epoch", 0))
        best_val_loss = float(resume.get("best_val_loss", resume.get("val_loss", best_val_loss)))
        best_val_epoch = int(resume.get("best_val_epoch", start_epoch))
        print(
            f"Resumed from {resume_path}: epoch={start_epoch}, "
            f"best_val_loss={best_val_loss:.4f} at epoch={best_val_epoch}"
        )

    checkpoint_dir = Path(args.checkpoint_dir)
    checkpoint_dir.mkdir(parents=True, exist_ok=True)

    if start_epoch >= args.epochs:
        print(f"Nothing to train: start_epoch={start_epoch} >= epochs={args.epochs}")

    for epoch in range(start_epoch, args.epochs):
        t_epoch = time.time()
        train_loss, train_pl, train_vl = train_epoch(
            model, optimizer, train_t, args.batch_size, device, epoch=epoch,
        )
        t_train = time.time()

        val_loss, val_pl, val_vl = evaluate(
            model, val_t, args.batch_size, device,
        )
        t_val = time.time()

        train_samples = train_idx.shape[0]
        val_samples = val_idx.shape[0]
        train_throughput = train_samples / (t_train - t_epoch)
        val_throughput = val_samples / (t_val - t_train)

        print(
            f"Epoch {epoch + 1:3d}/{args.epochs} "
            f"train={train_loss:.4f} (p={train_pl:.4f} v={train_vl:.4f}) "
            f"val={val_loss:.4f} (p={val_pl:.4f} v={val_vl:.4f}) "
            f"| train {train_throughput:,.0f} rec/s val {val_throughput:,.0f} rec/s "
            f"| {t_val - t_epoch:.1f}s"
        )

        is_new_best = val_loss < best_val_loss
        if is_new_best:
            best_val_loss = val_loss
            best_val_epoch = epoch + 1

        # Save the underlying model (unwrap compiled)
        state_model = model._orig_mod if hasattr(model, "_orig_mod") else model
        checkpoint_payload = {
            "epoch": epoch + 1,
            "model_state_dict": state_model.state_dict(),
            "optimizer_state_dict": optimizer.state_dict(),
            "val_loss": val_loss,
            "best_val_loss": best_val_loss,
            "best_val_epoch": best_val_epoch,
            "config": {
                "node_feature_count": config.node_feature_count,
                "global_feature_count": config.global_feature_count,
                "hidden_size": config.hidden_size,
                "message_passing_layers": config.message_passing_layers,
                "action_count": config.action_count,
            },
        }
        # Always persist latest progress for reliable resume.
        latest_path = checkpoint_dir / "latest.pt"
        torch.save(checkpoint_payload, latest_path)

        if is_new_best:
            best_path = checkpoint_dir / "best.pt"
            torch.save(checkpoint_payload, best_path)

    print()
    if best_val_epoch > 0:
        print(f"Best val loss {best_val_loss:.4f} achieved at epoch {best_val_epoch}")

    # Export ONNX if requested
    if args.export_onnx:
        state_model = model._orig_mod if hasattr(model, "_orig_mod") else model
        from dicewars_nn.export_onnx import export_model
        export_model(state_model, config, Path(args.export_onnx))
        print(f"Exported ONNX model to {args.export_onnx}")

    return 0


def build_cache_fingerprint(data_paths: list[Path], filter_bot: str | None) -> dict:
    return {
        "files": [
            {
                "path": str(path.resolve()),
                "size": path.stat().st_size,
                "mtime_ns": path.stat().st_mtime_ns,
            }
            for path in data_paths
        ],
        "filter_bot": filter_bot,
        "schema": 1,
    }


def load_tensor_cache(cache_path: Path) -> dict:
    import torch

    return torch.load(cache_path, map_location="cpu", weights_only=False)


def save_tensor_cache(cache_path: Path, tensors: dict, fingerprint: dict) -> None:
    import torch

    cache_path.parent.mkdir(parents=True, exist_ok=True)
    torch.save(
        {
            "fingerprint": fingerprint,
            "tensors": tensors,
        },
        cache_path,
    )


def pretensorize(records: list, config: ModelConfig) -> dict:
    """Convert all records to tensors once, avoiding per-batch Python overhead."""
    import torch
    from dicewars_nn.batch import make_batch

    batch = make_batch(records)

    return {
        "node_features": torch.tensor(batch.node_features, dtype=torch.float32),
        "adjacency": torch.tensor(batch.adjacency, dtype=torch.float32),
        "global_features": torch.tensor(batch.global_features, dtype=torch.float32),
        "area_mask": torch.tensor(batch.area_mask, dtype=torch.float32),
        "player_mask": torch.tensor(batch.player_mask, dtype=torch.float32),
        "legal_mask": torch.tensor(batch.legal_action_mask, dtype=torch.float32),
        "targets": torch.tensor(batch.policy_target, dtype=torch.long),
        "value_targets": torch.tensor(batch.value_target, dtype=torch.float32),
    }


def train_epoch(
    model: "torch.nn.Module",
    optimizer: "torch.optim.Optimizer",
    tensors: dict,
    batch_size: int,
    device: "torch.device",
    epoch: int = 0,
) -> tuple[float, float, float]:
    import torch
    import torch.nn.functional as F

    model.train()
    n = tensors["targets"].shape[0]
    perm = torch.randperm(n)
    n_batches = (n + batch_size - 1) // batch_size
    total_loss = 0.0
    total_policy_loss = 0.0
    total_value_loss = 0.0
    t_start = time.time()
    log_interval = max(1, n_batches // 5)  # log ~5 times per epoch

    for i in range(n_batches):
        start = i * batch_size
        idx = perm[start:start + batch_size]

        node_features = tensors["node_features"][idx]
        adjacency = tensors["adjacency"][idx]
        global_features = tensors["global_features"][idx]
        area_mask = tensors["area_mask"][idx]
        player_mask = tensors["player_mask"][idx]
        legal_mask = tensors["legal_mask"][idx]
        targets = tensors["targets"][idx]
        value_targets = tensors["value_targets"][idx].unsqueeze(1)

        optimizer.zero_grad(set_to_none=True)
        outputs = model(node_features, adjacency, global_features, area_mask, player_mask)
        policy_logits = outputs["policy"]
        value_pred = outputs["value"]

        # Policy loss: mask illegal actions to -inf before cross-entropy
        # Use torch.where instead of clone+mask for better performance
        masked_logits = torch.where(legal_mask.bool(), policy_logits, float("-inf"))
        policy_loss = F.cross_entropy(masked_logits, targets)

        value_loss = F.mse_loss(value_pred, value_targets)

        loss = policy_loss + 0.5 * value_loss
        loss.backward()
        optimizer.step()

        total_loss += loss.item()
        total_policy_loss += policy_loss.item()
        total_value_loss += value_loss.item()

        if (i + 1) % log_interval == 0:
            elapsed = time.time() - t_start
            done = (i + 1) * batch_size
            avg_l = total_loss / (i + 1)
            throughput = done / elapsed
            print(
                f"  epoch {epoch + 1} batch {i + 1}/{n_batches} "
                f"loss={avg_l:.4f} "
                f"{throughput:,.0f} rec/s "
                f"({done}/{n})"
            )

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
    n_batches = (n + batch_size - 1) // batch_size
    total_loss = 0.0
    total_policy_loss = 0.0
    total_value_loss = 0.0

    with torch.no_grad():
        for start in range(0, n, batch_size):
            idx = slice(start, min(start + batch_size, n))

            node_features = tensors["node_features"][idx]
            adjacency = tensors["adjacency"][idx]
            global_features = tensors["global_features"][idx]
            area_mask = tensors["area_mask"][idx]
            player_mask = tensors["player_mask"][idx]
            legal_mask = tensors["legal_mask"][idx]
            targets = tensors["targets"][idx]
            value_targets = tensors["value_targets"][idx].unsqueeze(1)

            outputs = model(node_features, adjacency, global_features, area_mask, player_mask)
            policy_logits = outputs["policy"]
            value_pred = outputs["value"]

            masked_logits = torch.where(legal_mask.bool(), policy_logits, float("-inf"))
            policy_loss = F.cross_entropy(masked_logits, targets)

            value_loss = F.mse_loss(value_pred, value_targets)

            loss = policy_loss + 0.5 * value_loss
            total_loss += loss.item()
            total_policy_loss += policy_loss.item()
            total_value_loss += value_loss.item()

    avg = lambda x: x / max(n_batches, 1)
    return avg(total_loss), avg(total_policy_loss), avg(total_value_loss)


if __name__ == "__main__":
    raise SystemExit(main())
