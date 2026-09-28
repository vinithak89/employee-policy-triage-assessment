class ProviderError(Exception):
    """Base class for technical model-provider failures."""


class ProviderTimeout(ProviderError):
    pass


class ProviderUnavailable(ProviderError):
    pass


class MalformedProviderOutput(ProviderError):
    pass


class OfflineModelProvider:
    """Deterministic offline model double; no network/API key is required."""

    def generate(self, question: str, policies: list[dict]) -> dict:
        if len(policies) == 0:
            return {"status": "INSUFFICIENT_EVIDENCE", "answer": None, "citations": []}
        if len(policies) > 1:
            return {
                "status": "CONFLICT",
                "answer": None,
                "citations": [{"chunk_id": p["policy_id"], "quote": p["text"]} for p in policies],
            }
        policy = policies[0]
        return {
            "status": "ANSWERED",
            "answer": policy["text"],
            "citations": [{"chunk_id": policy["policy_id"], "quote": policy["text"]}],
        }
