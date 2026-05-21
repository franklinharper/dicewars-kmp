import gzip
import json
from pathlib import Path
import tempfile
import unittest

from dicewars_nn.train_imitation import main
from test_dataset import valid_record


def _make_data_file(tmp: str, n: int = 10) -> Path:
    path = Path(tmp) / "records.jsonl.gz"
    actor_record = valid_record()
    actor_record["perspective_player"] = 0  # same as actor
    actor_record["policy_weight"] = 1.0  # actor record
    actor_record["state"] = {
        "node_features": [[0.0] * 6 for _ in range(32)],
        "adjacency": [[False] * 32 for _ in range(32)],
        "global_features": [0.0] * 4,
        "area_mask": [False] + [True] * 31,
        "player_mask": [True, True] + [False] * 6,
    }
    with gzip.open(path, "wt", encoding="utf-8") as handle:
        for _ in range(n):
            handle.write(json.dumps(actor_record))
            handle.write("\n")
    return path


class TrainImitationTest(unittest.TestCase):
    def test_dry_run_loads_data_without_requiring_torch(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = _make_data_file(tmp)
            exit_code = main([str(path), "--dry-run"])
        self.assertEqual(0, exit_code)

    def test_trains_and_exports_onnx(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = _make_data_file(tmp, n=20)
            onnx_path = Path(tmp) / "model.onnx"
            exit_code = main([
                str(path),
                "--epochs", "2",
                "--batch-size", "4",
                "--checkpoint-dir", str(Path(tmp) / "ckpt"),
                "--export-onnx", str(onnx_path),
            ])
            self.assertEqual(0, exit_code)
            self.assertTrue(onnx_path.exists(), "ONNX model should be exported")
            self.assertGreater(onnx_path.stat().st_size, 0)

    def test_resume_from_checkpoint(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = _make_data_file(tmp, n=20)
            ckpt_dir = Path(tmp) / "ckpt"
            first_exit = main([
                str(path),
                "--epochs", "1",
                "--batch-size", "4",
                "--checkpoint-dir", str(ckpt_dir),
            ])
            self.assertEqual(0, first_exit)
            resume_path = ckpt_dir / "best.pt"
            self.assertTrue(resume_path.exists(), "Checkpoint should exist after first run")

            second_exit = main([
                str(path),
                "--epochs", "2",
                "--batch-size", "4",
                "--checkpoint-dir", str(ckpt_dir),
                "--resume-checkpoint", str(resume_path),
            ])
            self.assertEqual(0, second_exit)


if __name__ == "__main__":
    unittest.main()
