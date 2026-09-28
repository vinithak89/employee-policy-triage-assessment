from datetime import date
from pydantic import BaseModel, Field


class AnswerRequest(BaseModel):
    question: str = Field(min_length=1)
    as_of: date


class Citation(BaseModel):
    chunk_id: str
    quote: str


class AnswerResponse(BaseModel):
    status: str
    answer: str | None
    citations: list[Citation]
