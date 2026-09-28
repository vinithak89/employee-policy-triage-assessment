from datetime import date
import pytest
from app.retrieval.policy_retriever import PolicyRetriever
from app.services.answer_service import AnswerService
from app.providers.offline_provider import ProviderTimeout, ProviderUnavailable, MalformedProviderOutput


def test_current_atlas_certification_limit_and_evidence():
    result = AnswerService().answer("What is my annual certification reimbursement limit?", "Atlas", "employee", date(2026, 9, 21))
    assert result["status"] == "ANSWERED"
    assert result["answer"] == "The annual certification reimbursement limit for employees is INR 25000."
    assert result["citations"] == [{"chunk_id": "atlas-cert-current", "quote": result["answer"]}]


def test_historical_and_future_effective_intervals():
    service = AnswerService()
    old = service.answer("certification reimbursement limit", "Atlas", "employee", date(2026, 5, 31))
    future = service.answer("certification reimbursement limit", "Atlas", "employee", date(2027, 1, 2))
    assert old["citations"][0]["chunk_id"] == "atlas-cert-historical"
    assert future["citations"][0]["chunk_id"] == "atlas-cert-future"


def test_home_office_conflict_is_not_resolved():
    result = AnswerService().answer("What is my home-office allowance?", "Atlas", "employee", date(2026, 9, 21))
    assert result["status"] == "CONFLICT"
    assert {c["chunk_id"] for c in result["citations"]} == {"atlas-home-office-a", "atlas-home-office-b"}


def test_ineligible_tenant_never_reaches_response():
    result = AnswerService().answer("certification reimbursement limit", "Atlas", "employee", date(2026, 9, 21))
    ids = {c["chunk_id"] for c in result["citations"]}
    assert "boreal-cert-current" not in ids
    assert "atlas-cert-draft" not in ids
    assert "atlas-injection-example" not in ids


def test_contractor_gets_contractor_policy():
    result = AnswerService().answer("certification reimbursement limit", "Atlas", "contractor", date(2026, 9, 21))
    assert result["citations"][0]["chunk_id"] == "atlas-cert-contractor"


class TimeoutProvider:
    def generate(self, question, policies): raise ProviderTimeout()
class UnavailableProvider:
    def generate(self, question, policies): raise ProviderUnavailable()
class MalformedProvider:
    def generate(self, question, policies): return {"status": "BOGUS"}


@pytest.mark.parametrize("provider,error", [(TimeoutProvider(), ProviderTimeout), (UnavailableProvider(), ProviderUnavailable), (MalformedProvider(), MalformedProviderOutput)])
def test_provider_failures_are_controllable(provider, error):
    service = AnswerService(provider=provider)
    with pytest.raises(error):
        service.answer("certification reimbursement limit", "Atlas", "employee", date(2026, 9, 21))


def test_duplicate_policy_record_does_not_change_result(tmp_path):
    import json
    source = json.loads((__import__('pathlib').Path('policies/policies.json')).read_text())
    source.append(dict(next(p for p in source if p['policy_id'] == 'atlas-cert-current')))
    path = tmp_path / 'policies.json'
    path.write_text(json.dumps(source))
    service = AnswerService(retriever=PolicyRetriever(path))
    result = service.answer("certification reimbursement limit", "Atlas", "employee", date(2026, 9, 21))
    assert result["status"] == "ANSWERED"
    assert result["citations"][0]["chunk_id"] == "atlas-cert-current"
