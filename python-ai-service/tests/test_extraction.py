from app.services.field_extraction_service import FieldExtractionService


def test_request_07_missing_amount_is_visible():
    text = "Reference: TRAIN-707\nI have already booked external training and now want reimbursement. I have not obtained manager approval. The invoice amount is not available. What policy applies?"
    result = FieldExtractionService().extract_fields(text)
    assert result["benefit"] == "external training"
    assert result["amount"] is None
    assert result["currency"] is None
    assert result["reference"] == "TRAIN-707"
    assert any("not available" in issue for issue in result["issues"])
