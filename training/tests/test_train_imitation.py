import gzip
import json
from pathlib import Path
import tempfile
import unittest

from dicewars_nn.train_imitation import main
from test_dataset import valid_record


class TrainImitationTest(unittest.TestCase):
    def test_dry_run_loads_data_without_requiring_torch(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "records.jsonl.gz"
            with gzip.open(path, "wt", encoding="utf-8") as handle:
                handle.write(json.dumps(valid_record()))
                handle.write("\n")

            exit_code = main([str(path), "--dry-run"])

        self.assertEqual(0, exit_code)


if __name__ == "__main__":
    unittest.main()
