import json
from pathlib import Path
import tempfile
import unittest

from dicewars_nn.export_metadata import main, validate_metadata


ARGS = [
    "--model-id", "neuralbot-20260520-1430-a1b2c3d",
    "--git-commit", "a1b2c3d",
    "--training-config-hash", "cfg123",
    "--training-seed", "123456789",
    "--environment-version", "encoder-v1-action-v1",
    "--self-play-game-count", "250000",
    "--optimizer-settings", "adamw lr=0.001",
    "--network-architecture", "gnn hidden=64 layers=4",
    "--checkpoint-path", "checkpoints/model.pt",
    "--export-timestamp", "2026-05-20T14:30:00Z",
    "--onnx-opset", "18",
    "--quantization", "none",
    "--evaluation-tournament-results", "10k score=12345 wins=678",
]


class ExportMetadataTest(unittest.TestCase):
    def test_writes_metadata_json(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "model.metadata.json"
            exit_code = main(["--out", str(out), *ARGS])
            payload = json.loads(out.read_text(encoding="utf-8"))

        self.assertEqual(0, exit_code)
        self.assertEqual("neuralbot-20260520-1430-a1b2c3d", payload["model_id"])
        self.assertEqual(18, payload["onnx_opset"])
        self.assertIsNone(payload["map_dataset_hash"])

    def test_writes_optional_map_dataset_hash(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "model.metadata.json"
            exit_code = main(["--out", str(out), *ARGS, "--map-dataset-hash", "maps456"])
            payload = json.loads(out.read_text(encoding="utf-8"))

        self.assertEqual(0, exit_code)
        self.assertEqual("maps456", payload["map_dataset_hash"])

    def test_rejects_negative_training_seed(self):
        with self.assertRaisesRegex(ValueError, "training_seed must be non-negative"):
            validate_metadata({
                "model_id": "m",
                "git_commit": "g",
                "training_config_hash": "c",
                "training_seed": -1,
                "environment_version": "e",
                "self_play_game_count": 0,
                "optimizer_settings": "o",
                "network_architecture": "n",
                "checkpoint_path": "p",
                "export_timestamp": "t",
                "onnx_opset": 18,
                "quantization": "none",
                "evaluation_tournament_results": "r",
            })


if __name__ == "__main__":
    unittest.main()
