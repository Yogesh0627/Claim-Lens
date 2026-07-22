"""Request/response models for the OCR API."""

from __future__ import annotations

from pydantic import BaseModel, Field


class ExtractedFields(BaseModel):
    """Structured signals parsed from the raw OCR text (best-effort; any field may be absent)."""

    registration_numbers: list[str] = Field(default_factory=list)
    policy_numbers: list[str] = Field(default_factory=list)
    chassis_numbers: list[str] = Field(default_factory=list)
    engine_numbers: list[str] = Field(default_factory=list)
    dates: list[str] = Field(default_factory=list)
    amounts: list[float] = Field(default_factory=list)


class OcrResponse(BaseModel):
    engine: str
    document_type: str | None = None
    page_count: int
    text: str
    confidence: float | None = None
    fields: ExtractedFields
    warnings: list[str] = Field(default_factory=list)


class HealthResponse(BaseModel):
    status: str
    engine: str
    engine_available: bool
    detail: str | None = None
