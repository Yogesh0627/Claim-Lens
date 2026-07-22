"""ClaimLens OCR service.

Stateless HTTP OCR over a swappable engine (Tesseract | Google Vision). The backend claims OCR jobs,
sends each document here, and persists what comes back — this service owns no database and no queue.

  GET  /health         -> engine readiness
  POST /ocr            -> multipart file (+ optional document_type) -> extracted text + fields
"""

from __future__ import annotations

from fastapi import FastAPI, File, Form, Header, HTTPException, UploadFile

from .config import settings
from .engines.registry import get_engine
from .extraction.fields import extract_fields
from .models import ExtractedFields, HealthResponse, OcrResponse
from .rendering import render_pages

app = FastAPI(title="ClaimLens OCR Service", version="1.0.0")


def _check_auth(token: str | None) -> None:
    if settings.ocr_shared_secret and token != settings.ocr_shared_secret:
        raise HTTPException(status_code=401, detail="Invalid or missing X-OCR-Token")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    try:
        engine = get_engine()
        ok, detail = engine.available()
        return HealthResponse(
            status="ok" if ok else "degraded",
            engine=engine.name,
            engine_available=ok,
            detail=detail,
        )
    except Exception as exc:  # bad OCR_ENGINE, missing optional dep, etc.
        return HealthResponse(
            status="error",
            engine=settings.ocr_engine,
            engine_available=False,
            detail=str(exc),
        )


@app.post("/ocr", response_model=OcrResponse)
async def ocr(
    file: UploadFile = File(...),
    document_type: str | None = Form(default=None),
    x_ocr_token: str | None = Header(default=None),
) -> OcrResponse:
    _check_auth(x_ocr_token)

    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Empty file")
    if len(content) > settings.max_upload_mb * 1024 * 1024:
        raise HTTPException(status_code=413, detail=f"File exceeds {settings.max_upload_mb} MB")

    engine = get_engine()
    ok, detail = engine.available()
    if not ok:
        # The engine binary/credentials aren't ready — a clear 503 beats a cryptic 500.
        raise HTTPException(status_code=503, detail=f"OCR engine unavailable: {detail}")

    try:
        pages = render_pages(content, file.content_type, file.filename)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Could not read document: {exc}") from exc

    warnings: list[str] = []
    texts: list[str] = []
    confidences: list[float] = []
    for page in pages:
        result = engine.extract_page(page)
        texts.append(result.text)
        if result.confidence is not None:
            confidences.append(result.confidence)

    full_text = "\n\n".join(t for t in texts if t)
    if not full_text.strip():
        warnings.append("No text extracted — document may be blank, handwritten, or low quality")

    overall_conf = round(sum(confidences) / len(confidences), 2) if confidences else None
    fields = extract_fields(full_text) if full_text else ExtractedFields()

    return OcrResponse(
        engine=engine.name,
        document_type=document_type,
        page_count=len(pages),
        text=full_text,
        confidence=overall_conf,
        fields=fields,
        warnings=warnings,
    )
