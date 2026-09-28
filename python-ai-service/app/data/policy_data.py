"""Compatibility loader for the supplied policy corpus.

The repository source of truth is policies/policies.json. Keeping this module
small prevents policy records from being duplicated in application logic.
"""
import json
from pathlib import Path

POLICY_FILE = Path(__file__).resolve().parents[2] / "policies" / "policies.json"
with POLICY_FILE.open("r", encoding="utf-8") as handle:
    POLICIES = json.load(handle)
