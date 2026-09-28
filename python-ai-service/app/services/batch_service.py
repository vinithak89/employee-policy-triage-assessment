from app.services.field_extraction_service import FieldExtractionService
from app.services.policy_retrieval_service import PolicyRetrievalService
from app.services.decision_service import DecisionService
from app.services.security_service import SecurityService
from app.utils.file_utils import calculate_file_hash, extract_text


class BatchService:
    def __init__(self, field_extraction_service=None, policy_retrieval_service=None, decision_service=None, security_service=None):
        self.field_extraction_service = field_extraction_service or FieldExtractionService()
        self.policy_retrieval_service = policy_retrieval_service or PolicyRetrievalService()
        self.decision_service = decision_service or DecisionService()
        self.security_service = security_service or SecurityService()

    def process_batch(self, batch_id, as_of, documents, tenant, role):
        results, seen_hashes = [], {}
        for document in documents:
            document_id, filename, content = document["document_id"], document["filename"], document["content"]
            if not content:
                results.append(self._failure(document_id, "EMPTY_DOCUMENT", "Document is empty")); continue
            content_hash = calculate_file_hash(content)
            if content_hash in seen_hashes:
                results.append({"document_id": document_id, "processing_status": "COMPLETED", "extracted": None, "policy": None,
                                "review_required": True, "issues": ["Exact duplicate document"], "duplicate_of": seen_hashes[content_hash],
                                "security": {"prompt_injection_detected": False}, "error": None}); continue
            seen_hashes[content_hash] = document_id
            try:
                text = extract_text(filename, content)
            except ValueError as exc:
                results.append(self._failure(document_id, "UNSUPPORTED_FILE_TYPE", str(exc))); continue
            except Exception:
                results.append(self._failure(document_id, "TEXT_EXTRACTION_FAILED", "Unable to extract document text")); continue
            if not text.strip():
                results.append(self._failure(document_id, "NO_EXTRACTED_TEXT", "No readable text could be extracted")); continue
            extracted = self.field_extraction_service.extract_fields(text)
            policy = self.policy_retrieval_service.retrieve_policy(extracted["benefit"], extracted["amount"], extracted["currency"], as_of, tenant, role)
            security_findings = self.security_service.detect_prompt_injection(text)
            decision = self.decision_service.decide(extracted, policy, security_findings, source_text=text)
            results.append({
                "document_id": document_id, "processing_status": "COMPLETED",
                "extracted": {k: extracted[k] for k in ("benefit", "amount", "currency", "reference", "field_evidence")},
                "policy": policy, "decision": decision, "security": {"prompt_injection_detected": bool(security_findings)},
                "review_required": True, "issues": decision["issues"], "duplicate_of": None, "error": None
            })
        return {"batch_id": batch_id, "summary": {"total": len(results), "completed": sum(r["processing_status"] == "COMPLETED" for r in results), "failed": sum(r["processing_status"] == "FAILED" for r in results)}, "results": results}

    @staticmethod
    def _failure(document_id, code, message):
        return {"document_id": document_id, "processing_status": "FAILED", "extracted": None, "policy": None,
                "review_required": True, "issues": [message], "duplicate_of": None,
                "security": {"prompt_injection_detected": False}, "error": {"code": code, "message": message}}
