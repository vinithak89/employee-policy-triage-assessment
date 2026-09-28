from typing import List
from pydantic import BaseModel, Field, field_validator
from datetime import date


class DocumentManifest(BaseModel):
    document_id: str = Field(min_length=1)
    filename: str = Field(min_length=1)


class BatchMetadata(BaseModel):
    batch_id: str = Field(min_length=1)
    as_of: date
    documents: List[DocumentManifest] = Field(min_length=1)
