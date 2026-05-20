import gzip
import json
from pathlib import Path
import tempfile
import unittest

from dicewars_nn.dataset import ACTION_COUNT, load_jsonl_gz, parse_record


def valid_record():
    return {
        "schema_version": 1,
        "encoder_version": 1,
        "action_space_version": 1,
        "round_seed": 123,
        "round_number": 1,
        "action_number": 2,
        "actor_player": 0,
        "perspective_player": 1,
        "bot_id": "bully",
        "chosen_action_index": 34,
        "legal_action_mask": [34, 35, 1024],
        "policy_weight": 0.0,
        "state": {
            "node_features": [],
            "adjacency": [],
            "global_features": [],
            "area_mask": [],
            "player_mask": [],
        },
        "value_target": 0.5,
    }


class DatasetTest(unittest.TestCase):
    def test_parse_valid_record(self):
        record = parse_record(valid_record())

        self.assertEqual(1, record.schema_version)
        self.assertEqual("bully", record.bot_id)
        self.assertEqual(34, record.chosen_action_index)
        self.assertEqual((34, 35, 1024), record.legal_action_mask)
        self.assertEqual(0.5, record.value_target)

    def test_rejects_missing_required_field(self):
        payload = valid_record()
        del payload["state"]

        with self.assertRaisesRegex(ValueError, "missing required field 'state'"):
            parse_record(payload)

    def test_rejects_action_out_of_range(self):
        payload = valid_record()
        payload["chosen_action_index"] = ACTION_COUNT

        with self.assertRaisesRegex(ValueError, "chosen_action_index out of range"):
            parse_record(payload)

    def test_rejects_legal_mask_action_out_of_range(self):
        payload = valid_record()
        payload["legal_action_mask"] = [1025]

        with self.assertRaisesRegex(ValueError, "legal_action_mask contains out-of-range"):
            parse_record(payload)

    def test_rejects_value_target_out_of_range(self):
        payload = valid_record()
        payload["value_target"] = 1.5

        with self.assertRaisesRegex(ValueError, "value_target must be in"):
            parse_record(payload)

    def test_loads_jsonl_gz(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "records.jsonl.gz"
            with gzip.open(path, "wt", encoding="utf-8") as handle:
                handle.write(json.dumps(valid_record()))
                handle.write("\n")

            records = list(load_jsonl_gz(path))

        self.assertEqual(1, len(records))
        self.assertEqual("bully", records[0].bot_id)


if __name__ == "__main__":
    unittest.main()
