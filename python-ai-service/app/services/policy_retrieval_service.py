from datetime import date
from app.retrieval.policy_retriever import PolicyRetriever


class PolicyRetrievalService:
    def __init__(self, retriever=None):
        self.retriever = retriever or PolicyRetriever()

    def retrieve_policy(self, benefit, amount=None, currency=None, as_of=None, tenant="Atlas", role="employee"):
        if not benefit:
            return {"match": False, "conflict": False, "policies": []}
        if isinstance(as_of, str):
            as_of = date.fromisoformat(as_of)
        policies = self.retriever.eligible_policies(tenant, role, as_of)
        matches = [p for p in policies if self._benefit_matches(benefit, p)]
        if not matches:
            return {"match": False, "conflict": False, "policies": []}
        if len(matches) > 1:
            return {"match": False, "conflict": True, "policies": matches}
        p = matches[0]
        return {
            "match": True,
            "conflict": False,
            "policy_id": p["policy_id"],
            "tenant": p["tenant"],
            "role": p["role"],
            "effective_from": p["effective_from"],
            "effective_to": p["effective_to"],
            "approval_status": p["approval_status"],
            "text": p["text"],
            "amount_exceeds_limit": False,
            "currency_matches": currency is None or currency.upper() == "INR",
            "source": p,
        }

    @staticmethod
    def _benefit_matches(benefit, policy):
        b = benefit.lower()
        text = policy["text"].lower()
        return b in text
