import unittest

from dicewars_nn.model import ACTION_COUNT, AREA_MAX, ModelConfig, output_shapes


class ModelTest(unittest.TestCase):
    def test_action_count_matches_fixed_action_space(self):
        self.assertEqual(32 * 32 + 1, ACTION_COUNT)
        self.assertEqual(32, AREA_MAX)

    def test_output_shapes(self):
        shapes = output_shapes(batch_size=3)

        self.assertEqual((3, ACTION_COUNT), shapes["policy"])
        self.assertEqual((3, 1), shapes["value"])

    def test_rejects_invalid_batch_size(self):
        with self.assertRaisesRegex(ValueError, "batch_size must be positive"):
            output_shapes(batch_size=0)

    def test_rejects_invalid_config(self):
        with self.assertRaisesRegex(ValueError, "hidden_size must be positive"):
            ModelConfig(hidden_size=0).validate()

        with self.assertRaisesRegex(ValueError, "action_count must be"):
            ModelConfig(action_count=10).validate()


if __name__ == "__main__":
    unittest.main()
