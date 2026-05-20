from pathlib import Path
import tempfile
import unittest

from dicewars_nn.export_onnx import main as export_main
from dicewars_nn.validate_onnx import main as validate_main


class ExportValidateOnnxTest(unittest.TestCase):
    def test_export_dry_run_does_not_require_torch(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "model.onnx"
            exit_code = export_main(["--out", str(out), "--dry-run"])

        self.assertEqual(0, exit_code)

    def test_validate_dry_run_does_not_require_onnxruntime(self):
        with tempfile.TemporaryDirectory() as tmp:
            model = Path(tmp) / "model.onnx"
            model.write_bytes(b"not really onnx")
            exit_code = validate_main([str(model), "--dry-run"])

        self.assertEqual(0, exit_code)

    def test_validate_dry_run_requires_existing_model_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            model = Path(tmp) / "missing.onnx"
            with self.assertRaises(FileNotFoundError):
                validate_main([str(model), "--dry-run"])


if __name__ == "__main__":
    unittest.main()
