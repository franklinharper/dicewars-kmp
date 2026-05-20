from __future__ import annotations

from dataclasses import dataclass


AREA_MAX = 32
PLAYER_MAX = 8
ACTION_COUNT = AREA_MAX * AREA_MAX + 1


@dataclass(frozen=True)
class ModelConfig:
    node_feature_count: int = 6
    global_feature_count: int = 4
    hidden_size: int = 64
    message_passing_layers: int = 4
    action_count: int = ACTION_COUNT

    def validate(self) -> None:
        if self.node_feature_count <= 0:
            raise ValueError("node_feature_count must be positive")
        if self.global_feature_count <= 0:
            raise ValueError("global_feature_count must be positive")
        if self.hidden_size <= 0:
            raise ValueError("hidden_size must be positive")
        if self.message_passing_layers <= 0:
            raise ValueError("message_passing_layers must be positive")
        if self.action_count != ACTION_COUNT:
            raise ValueError(f"action_count must be {ACTION_COUNT}")


def output_shapes(batch_size: int, config: ModelConfig = ModelConfig()) -> dict[str, tuple[int, ...]]:
    config.validate()
    if batch_size <= 0:
        raise ValueError("batch_size must be positive")
    return {
        "policy": (batch_size, config.action_count),
        "value": (batch_size, 1),
    }


def build_model(config: ModelConfig = ModelConfig()):
    """Build the PyTorch model, importing torch lazily.

    The project can run dataset/CLI tests without PyTorch installed. Training
    environments should install torch before calling this function.
    """
    config.validate()
    try:
        import torch
        from torch import nn
    except ImportError as exc:
        raise RuntimeError("PyTorch is required to build the neural model") from exc

    class DicewarsPolicyValueNet(nn.Module):
        def __init__(self, cfg: ModelConfig):
            super().__init__()
            node_input = cfg.node_feature_count
            global_input = cfg.global_feature_count
            hidden = cfg.hidden_size
            self.node_proj = nn.Linear(node_input, hidden)
            self.global_proj = nn.Linear(global_input, hidden)
            self.message_layers = nn.ModuleList(nn.Linear(hidden, hidden) for _ in range(cfg.message_passing_layers))
            self.policy_head = nn.Linear(hidden * 2, cfg.action_count)
            self.value_head = nn.Linear(hidden * 2, 1)

        def forward(self, node_features, adjacency, global_features, area_mask, player_mask):
            # node_features: [B, 32, F]
            # adjacency: [B, 32, 32]
            # area_mask: [B, 32]
            x = self.node_proj(node_features)
            mask = area_mask.unsqueeze(-1).to(dtype=x.dtype)
            x = x * mask
            for layer in self.message_layers:
                degree = adjacency.sum(dim=-1, keepdim=True).clamp_min(1).to(dtype=x.dtype)
                messages = adjacency.to(dtype=x.dtype).matmul(x) / degree
                x = torch.relu(layer(x + messages)) * mask
            area_count = mask.sum(dim=1).clamp_min(1)
            pooled_nodes = x.sum(dim=1) / area_count
            global_hidden = torch.relu(self.global_proj(global_features))
            combined = torch.cat([pooled_nodes, global_hidden], dim=-1)
            return {
                "policy": self.policy_head(combined),
                "value": self.value_head(combined),
            }

    return DicewarsPolicyValueNet(config)
