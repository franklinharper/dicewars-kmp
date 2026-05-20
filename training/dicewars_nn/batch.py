from __future__ import annotations

from dataclasses import dataclass

from dicewars_nn.dataset import ACTION_COUNT, TrainingRecord
from dicewars_nn.model import AREA_MAX, PLAYER_MAX


@dataclass(frozen=True)
class Batch:
    node_features: list
    adjacency: list
    global_features: list
    area_mask: list
    player_mask: list
    legal_action_mask: list
    policy_target: list[int]
    policy_weight: list[float]
    value_target: list[float]


def make_batch(records: list[TrainingRecord]) -> Batch:
    if not records:
        raise ValueError("records must not be empty")

    node_features = []
    adjacency = []
    global_features = []
    area_mask = []
    player_mask = []
    legal_action_mask = []
    policy_target = []
    policy_weight = []
    value_target = []

    for record in records:
        state = record.state
        node_features.append(_require_matrix(state, "node_features", AREA_MAX))
        adjacency.append(_require_matrix(state, "adjacency", AREA_MAX))
        global_features.append(_require_list(state, "global_features"))
        area_mask.append(_require_list(state, "area_mask", expected_len=AREA_MAX))
        player_mask.append(_require_list(state, "player_mask", expected_len=PLAYER_MAX))
        legal_action_mask.append(_dense_legal_action_mask(record.legal_action_mask))
        policy_target.append(record.chosen_action_index)
        policy_weight.append(record.policy_weight)
        value_target.append(record.value_target)

    return Batch(
        node_features=node_features,
        adjacency=adjacency,
        global_features=global_features,
        area_mask=area_mask,
        player_mask=player_mask,
        legal_action_mask=legal_action_mask,
        policy_target=policy_target,
        policy_weight=policy_weight,
        value_target=value_target,
    )


def _dense_legal_action_mask(indices: tuple[int, ...]) -> list[bool]:
    mask = [False] * ACTION_COUNT
    for index in indices:
        mask[index] = True
    return mask


def _require_matrix(state: dict, key: str, expected_rows: int) -> list:
    value = state.get(key)
    if not isinstance(value, list) or len(value) != expected_rows:
        raise ValueError(f"state.{key} must have {expected_rows} rows")
    return value


def _require_list(state: dict, key: str, expected_len: int | None = None) -> list:
    value = state.get(key)
    if not isinstance(value, list):
        raise ValueError(f"state.{key} must be a list")
    if expected_len is not None and len(value) != expected_len:
        raise ValueError(f"state.{key} must have length {expected_len}")
    return value
