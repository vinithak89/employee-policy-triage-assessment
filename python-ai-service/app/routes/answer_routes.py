from fastapi import APIRouter, Header, HTTPException

from app.models.request_models import AnswerRequest, AnswerResponse
from app.services.answer_service import AnswerService
from app.providers.offline_provider import (
    ProviderTimeout,
    ProviderUnavailable,
    MalformedProviderOutput,
)

router = APIRouter()
answer_service = AnswerService()


@router.post("/answer", response_model=AnswerResponse)
def answer(
    request: AnswerRequest,
    x_caller_tenant: str = Header(...),
    x_caller_role: str = Header(...),
):
    """Answer a policy question using caller context supplied by Spring.

    Spring owns caller-id validation and resolves X-Caller-Id to tenant/role.
    Python deliberately does not accept caller identity from the JSON body.
    """
    try:
        return answer_service.answer(
            question=request.question,
            tenant=x_caller_tenant,
            role=x_caller_role,
            as_of=request.as_of,
        )
    except ProviderTimeout:
        raise HTTPException(
            status_code=504,
            detail={"code": "PROVIDER_TIMEOUT", "message": "Provider timed out"},
        )
    except ProviderUnavailable:
        raise HTTPException(
            status_code=503,
            detail={"code": "PROVIDER_UNAVAILABLE", "message": "Provider unavailable"},
        )
    except MalformedProviderOutput:
        raise HTTPException(
            status_code=502,
            detail={"code": "MALFORMED_PROVIDER_OUTPUT", "message": "Provider returned malformed output"},
        )
