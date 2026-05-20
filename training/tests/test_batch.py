import unittest

from dicewars_nn.batch import make_batch
from dicewars_nn.dataset import ACTION_COUNT, parse_record
from test_dataset import valid_record


def valid_record_with_full_state():
    payload = valid_record()
    payload["state"] = {
        "node_features": [[0.0] * 6 for _ in range(32)],
        "adjacency": [[False] * 32 for _ in range(32)],
        "global_features": [0.0] * 4,
        "area_mask": [False] + [True] * 31,
        "player_mask": [True, True] + [False] * 6,
    }
    return payload


class BatchTest(unittest.TestCase):
    def test_make_batch_shapes_and_targets(self):
        record = parse_record(valid_record_with_full_state())

        batch = make_batch([record])

        self.assertEqual(1, len(batch.node_features))
        self.assertEqual(32, len(batch.node_features[0]))
        self.assertEqual(32, len(batch.adjacency[0]))
        self.assertEqual(4, len(batch.global_features[0]))
        self.assertEqual(32, len(batch.area_mask[0]))
        self.assertEqual(8, len(batch.player_mask[0]))
        self.assertEqual(ACTION_COUNT, len(batch.legal_action_mask[0]))
        self.assertTrue(batch.legal_action_mask[0][34])
        self.assertTrue(batch.legal_action_mask[0][1024])
        self.assertFalse(batch.legal_action_mask[0][0])
        self.assertEqual([34], batch.policy_target)
        self.assertEqual([0.0], batch.policy_weight)
        self.assertEqual([0.5], batch.value_target)

    def test_rejects_empty_records(self):
        with self.assertRaisesRegex(ValueError, "records must not be empty"):
            make_batch([])

    def test_rejects_wrong_area_mask_length(self):
        payload = valid_record_with_full_state()
        payload["state"]["area_mask"] = [True]
        record = parse_record(payload)

        with self.assertRaisesRegex(ValueError, "state.area_mask must have length 32"):
            make_batch([record])


if __name__ == "__main__":
    unittest.main()
