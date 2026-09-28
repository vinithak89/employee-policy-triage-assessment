from typing import List
from fastapi import APIRouter, File, Form, Header, HTTPException, UploadFile
from app.models.batch_models import BatchMetadata
from app.validators.batch_validator import validate_manifest, validate_uploaded_files
from app.services.batch_service import BatchService

router = APIRouter()
batch_service = BatchService()


@router.post("/batches")
async def process_batch(
    metadata: str = Form(...),
    files: List[UploadFile] = File(...),
    x_caller_tenant: str = Header(...),
    x_caller_role: str = Header(...),
):
    try:
        batch_metadata = BatchMetadata.model_validate_json(metadata)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Invalid metadata: {exc}")
    error = validate_manifest(batch_metadata.documents) or validate_uploaded_files(batch_metadata.documents, files)
    if error:
        raise HTTPException(status_code=400, detail=error)
    uploaded_files = {file.filename: file for file in files}
    batch_files = []
    for document in batch_metadata.documents:
        batch_files.append({"document_id": document.document_id, "filename": document.filename, "content": await uploaded_files[document.filename].read()})
    return batch_service.process_batch(batch_metadata.batch_id, batch_metadata.as_of, batch_files, x_caller_tenant, x_caller_role)
