"""ClaimLens image-analysis service.

Stateless image fraud SIGNALS (never decisions): perceptual-hash duplicate detection, ORB similarity,
three-state EXIF, and a synthetic-image soft signal. The backend stores the returned hashes and does
cross-claim duplicate comparison itself (it owns the data); /compare offers a direct pairwise check.

  GET  /health
  POST /analyze   -> one image  -> hashes + exif + synthetic signals
  POST /compare   -> two images -> ORB similarity + perceptual-hash Hamming distance
"""

from __future__ import annotations

import io
from datetime import datetime

from fastapi import FastAPI, File, Form, Header, HTTPException, UploadFile
from PIL import Image

from .config import settings
from .exif import analyse_exif
from .hashing import compute_hashes, hamming
from .models import AnalyzeResponse, CompareResponse, HealthResponse
from .similarity import orb_similarity
from .synthetic import detect_synthetic

APP_VERSION = "1.0.0"

app = FastAPI(title="ClaimLens Analysis Service", version=APP_VERSION)


def _check_auth(token: str | None) -> None:
    if settings.analysis_shared_secret and token != settings.analysis_shared_secret:
        raise HTTPException(status_code=401, detail="Invalid or missing X-ANALYSIS-TOKEN")


async def _read_image(file: UploadFile) -> tuple[Image.Image, bytes]:
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Empty file")
    if len(content) > settings.max_upload_mb * 1024 * 1024:
        raise HTTPException(status_code=413, detail=f"File exceeds {settings.max_upload_mb} MB")
    try:
        image = Image.open(io.BytesIO(content))
        image.load()
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Not a readable image: {exc}") from exc
    return image, content


def _parse_incident_date(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        return datetime.fromisoformat(value)
    except ValueError:
        try:
            return datetime.strptime(value, "%Y-%m-%d")
        except ValueError:
            return None


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    """Per-capability readiness. Probes the actual libraries a signal needs, not just liveness."""
    opencv = orb = image_hash = synthetic = False
    try:
        import cv2

        opencv = True
        cv2.ORB_create(nfeatures=10)  # prove ORB specifically constructs, not just that cv2 imports
        orb = True
    except Exception:
        pass
    try:
        import imagehash  # noqa: F401

        image_hash = True
    except Exception:
        pass
    try:
        import numpy  # noqa: F401  (synthetic = ELA + C2PA heuristics over numpy/PIL; no ML model)

        synthetic = True
    except Exception:
        pass

    all_up = opencv and orb and image_hash and synthetic
    return HealthResponse(
        status="UP" if all_up else "DEGRADED",
        opencv=opencv,
        imageHash=image_hash,
        orb=orb,
        syntheticModel=synthetic,
        version=APP_VERSION,
    )


@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze(
    file: UploadFile = File(...),
    incident_date: str | None = Form(default=None),
    expected_lat: float | None = Form(default=None),
    expected_lon: float | None = Form(default=None),
    x_analysis_token: str | None = Header(default=None),
) -> AnalyzeResponse:
    _check_auth(x_analysis_token)
    image, content = await _read_image(file)

    warnings: list[str] = []
    exif = analyse_exif(image, _parse_incident_date(incident_date), expected_lat, expected_lon)
    if exif.state == "UNKNOWN":
        warnings.append("No EXIF timestamp/GPS - treated as UNKNOWN, not suspicious")

    return AnalyzeResponse(
        width=image.width,
        height=image.height,
        hashes=compute_hashes(image),
        exif=exif,
        synthetic=detect_synthetic(image, content),
        warnings=warnings,
    )


@app.post("/compare", response_model=CompareResponse)
async def compare(
    file_a: UploadFile = File(...),
    file_b: UploadFile = File(...),
    x_analysis_token: str | None = Header(default=None),
) -> CompareResponse:
    _check_auth(x_analysis_token)
    image_a, _ = await _read_image(file_a)
    image_b, _ = await _read_image(file_b)

    similarity, matches = orb_similarity(image_a, image_b)
    hashes_a = compute_hashes(image_a)
    hashes_b = compute_hashes(image_b)
    distance = hamming(hashes_a.phash, hashes_b.phash)

    duplicate = (
        distance <= settings.duplicate_hamming_threshold
        or similarity >= settings.orb_similarity_threshold
    )
    return CompareResponse(
        orb_similarity=similarity,
        orb_matches=matches,
        phash_hamming=distance,
        duplicate_candidate=duplicate,
    )
