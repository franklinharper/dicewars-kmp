"""Generate a minimal test ONNX model for OnnxNeuralModel tests.

The model accepts the 5 neural inputs (node_features, adjacency,
global_features, area_mask, player_mask) and produces fixed outputs:
  policy: all zeros (shape [1, 1025])
  value:  [[0.42]]

This lets us verify the JVM OnnxNeuralModel wiring end-to-end without
a real trained model.
"""
from __future__ import annotations

import argparse
from pathlib import Path

import onnx
from onnx import TensorProto, helper


POLICY_SIZE = 1025


def build_model() -> onnx.ModelProto:
    node_features = helper.make_tensor_value_info("node_features", TensorProto.FLOAT, [1, 32, 6])
    adjacency = helper.make_tensor_value_info("adjacency", TensorProto.FLOAT, [1, 32, 32])
    global_features = helper.make_tensor_value_info("global_features", TensorProto.FLOAT, [1, 4])
    area_mask = helper.make_tensor_value_info("area_mask", TensorProto.FLOAT, [1, 32])
    player_mask = helper.make_tensor_value_info("player_mask", TensorProto.FLOAT, [1, 8])
    policy_out = helper.make_tensor_value_info("policy", TensorProto.FLOAT, [1, POLICY_SIZE])
    value_out = helper.make_tensor_value_info("value", TensorProto.FLOAT, [1, 1])

    policy_const = helper.make_tensor("policy_const", TensorProto.FLOAT, [1, POLICY_SIZE], [0.0] * POLICY_SIZE)
    value_const = helper.make_tensor("value_const", TensorProto.FLOAT, [1, 1], [0.42])

    graph = helper.make_graph(
        nodes=[
            helper.make_node("Constant", [], ["policy"], value=policy_const),
            helper.make_node("Constant", [], ["value"], value=value_const),
        ],
        name="test_model",
        inputs=[node_features, adjacency, global_features, area_mask, player_mask],
        outputs=[policy_out, value_out],
    )
    model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 18)])
    model.ir_version = 8
    onnx.checker.check_model(model)
    return model


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    model = build_model()
    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    onnx.save(model, args.out)
    print(f"Saved test model: {args.out}")


if __name__ == "__main__":
    main()
