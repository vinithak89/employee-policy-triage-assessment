from app.providers.offline_provider import OfflineModelProvider
from app.retrieval.policy_retriever import PolicyRetriever


class AnswerService:
    def __init__(self, retriever=None, provider=None):
        self.retriever = retriever or PolicyRetriever()
        self.provider = provider or OfflineModelProvider()

    def answer(self, question: str, tenant: str, role: str, as_of):
        policies = self.retriever.retrieve(question, tenant, role, as_of)
        # Only eligible policies reach the provider.
        result = self.provider.generate(question, policies)
        required = {"status", "answer", "citations"}
        if not required.issubset(result) or result["status"] not in {"ANSWERED", "INSUFFICIENT_EVIDENCE", "CONFLICT"}:
            from app.providers.offline_provider import MalformedProviderOutput
            raise MalformedProviderOutput("Provider returned malformed output")
        return result
