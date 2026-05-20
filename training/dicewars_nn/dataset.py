from __future__ import annotations

from dataclasses import dataclass
import gzip
import json
from pathlib import Path
from typing import Iterable, Iterator, Sequence


REQUIRED_FIELDS = (
    "schema_version",
    "encoder_version",
    "action_space_version",
    "round_seed",
    "round_number",
    "action_number",
    "actor_player",
    "perspective_player",
    "bot_id",
    "chosen_action_index",
    "legal_action_mask",
    "policy_weight",
    "state",
    "value_target",
)

ACTION_COUNT = 1025


@dataclass(frozen=True)
class TrainingRecord:
    schema_version: int
    encoder_version: int
    action_space_version: int
    round_seed: int
    round_number: int
    action_number: int
    actor_player: int
    perspective_player: int
    bot_id: str
    chosen_action_index: int
    legal_action_mask: tuple[int, ...]
    policy_weight: float
    state: dict
    value_target: float


def load_jsonl_gz(path: str | Path) -> Iterator[TrainingRecord]:
    with gzip.open(path, "rt", encoding="utf-8") as handle:
        for line_number, line in enumerate(handle, start=1):
            line = line.strip()
            if not line:
                continue
            try:
                payload = json.loads(line)
            except json.JSONDecodeError as exc:
                raise ValueError(f"invalid JSON on line {line_number}: {exc}") from exc
            yield parse_record(payload, line_number=line_number)


def parse_record(payload: dict, *, line_number: int | None = None) -> TrainingRecord:
    missing = [field for field in REQUIRED_FIELDS if field not in payload]
    if missing:
        prefix = f"line {line_number}: " if line_number is not None else ""
        raise ValueError(f"{prefix}missing required field {missing[0]!r}")

    chosen_action_index = _require_int(payload, "chosen_action_index")
    if not 0 <= chosen_action_index < ACTION_COUNT:
        raise ValueError("chosen_action_index out of range")

    legal_action_mask = _require_int_sequence(payload, "legal_action_mask")
    if any(index < 0 or index >= ACTION_COUNT for index in legal_action_mask):
        raise ValueError("legal_action_mask contains out-of-range action index")

    value_target = float(payload["value_target"])
    if not 0.0 <= value_target <= 1.0:
        raise ValueError("value_target must be in [0, 1]")

    policy_weight = float(payload["policy_weight"])
    if policy_weight not in (0.0, 1.0):
        raise ValueError("policy_weight must be 0.0 or 1.0")

    return TrainingRecord(
        schema_version=_require_int(payload, "schema_version"),
        encoder_version=_require_int(payload, "encoder_version"),
        action_space_version=_require_int(payload, "action_space_version"),
        round_seed=_require_int(payload, "round_seed"),
        round_number=_require_int(payload, "round_number"),
        action_number=_require_int(payload, "action_number"),
        actor_player=_require_int(payload, "actor_player"),
        perspective_player=_require_int(payload, "perspective_player"),
        bot_id=_require_str(payload, "bot_id"),
        chosen_action_index=chosen_action_index,
        legal_action_mask=tuple(legal_action_mask),
        policy_weight=policy_weight,
        state=_require_dict(payload, "state"),
        value_target=value_target,
    )


def load_all(paths: Iterable[str | Path]) -> list[TrainingRecord]:
    records: list[TrainingRecord] = []
    for path in paths:
        records.extend(load_jsonl_gz(path))
    return records


def _require_int(payload: dict, key: str) -> int:
    value = payload[key]
    if not isinstance(value, int):
        raise ValueError(f"{key} must be an int")
    return value


def _require_str(payload: dict, key: str) -> str:
    value = payload[key]
    if not isinstance(value, str) or not value:
        raise ValueError(f"{key} must be a non-empty string")
    return value


def _require_dict(payload: dict, key: str) -> dict:
    value = payload[key]
    if not isinstance(value, dict):
        raise ValueError(f"{key} must be an object")
    return value


def _require_int_sequence(payload: dict, key: str) -> Sequence[int]:
    value = payload[key]
    if not isinstance(value, list) or any(not isinstance(item, int) for item in value):
        raise ValueError(f"{key} must be a list of ints")
    return value
