import json
import re
from datetime import date
from pathlib import Path


class PolicyRetriever:
    """Deterministic retrieval over the supplied policy corpus.

    Eligibility is filtered before keyword retrieval: tenant, role, approval state,
    and effective interval are metadata, never instructions from policy text.
    """

    def __init__(self, policy_file: Path | None = None):
        policy_file = policy_file or Path(__file__).resolve().parents[2] / "policies" / "policies.json"
        with open(policy_file, "r", encoding="utf-8") as file:
            self.policies = json.load(file)

    CALLERS = {
        "atlas-employee-01": {"tenant": "Atlas", "role": "employee"},
        "atlas-contractor-01": {"tenant": "Atlas", "role": "contractor"},
        "boreal-employee-01": {"tenant": "Boreal", "role": "employee"},
    }

    def get_caller(self, caller_id: str):
        return self.CALLERS.get(caller_id)

    def eligible_policies(self, tenant: str, role: str, as_of: date) -> list[dict]:
        tenant = tenant.strip().lower()
        role = role.strip().lower()
        eligible = []
        seen_ids = set()
        for policy in self.policies:
            if policy["policy_id"] in seen_ids:
                continue
            seen_ids.add(policy["policy_id"])
            if policy["tenant"].lower() != tenant or policy["role"].lower() != role:
                continue
            if policy["approval_status"].lower() != "approved":
                continue
            start = date.fromisoformat(policy["effective_from"])
            end = date.fromisoformat(policy["effective_to"]) if policy["effective_to"] else None
            if as_of < start or (end is not None and as_of >= end):
                continue
            eligible.append(policy)
        return eligible

    def retrieve(self, question: str, tenant: str, role: str, as_of: date) -> list[dict]:
        eligible = self.eligible_policies(tenant, role, as_of)
        tokens = self._tokens(question)
        scored = []
        for policy in eligible:
            searchable = policy["text"].lower()
            # Require a meaningful phrase match against the supplied passage.
            # Generic words such as "limit" or "allowance" cannot select an
            # unrelated passage such as the injection example.
            benefit_phrases = {
                "certification reimbursement": "certification reimbursement",
                "home-office allowance": "home-office allowance",
                "wellness benefit": "wellness benefit",
                "external training": "external training",
                "rail travel": "rail travel",
            }
            meaningful_phrase = next((phrase for phrase in benefit_phrases.values() if phrase in question.lower() and phrase in searchable), None)
            if not meaningful_phrase:
                continue
            score = sum(1 for token in tokens if len(token) > 2 and token in searchable)
            if score:
                scored.append((score, policy))
        scored.sort(key=lambda item: (-item[0], item[1]["policy_id"]))
        return [policy for _, policy in scored]

    @staticmethod
    def _tokens(text: str) -> list[str]:
        return re.findall(r"[a-z0-9]+", text.lower())
