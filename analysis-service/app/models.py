"""Request/response models for the analysis API."""

from __future__ import annotations

from pydantic import BaseModel, Field


class ImageHashes(BaseModel):
    average: str
    dhash: str
    phash: str


class ExifResult(BaseModel):
    # Three-state, and load-bearing: only INCONSISTENT should raise a fraud score. Missing EXIF is
    # UNKNOWN (social apps strip it — absence is the norm, not suspicion).
    state: str  # CONSISTENT | INCONSISTENT | UNKNOWN
    has_datetime: bool
    has_gps: bool
    captured_at: str | None = None
    gps_lat: float | None = None
    gps_lon: float | None = None
    reasons: list[str] = Field(default_factory=list)


class SyntheticResult(BaseModel):
    # Always a soft signal — no reliable detector exists, so this never auto-rejects.
    signal: bool
    score: float  # 0..1, weighted low by the fraud engine
    method: str
    note: str


class AnalyzeResponse(BaseModel):
    width: int
    height: int
    hashes: ImageHashes
    exif: ExifResult
    synthetic: SyntheticResult
    warnings: list[str] = Field(default_factory=list)


class CompareResponse(BaseModel):
    orb_similarity: float
    orb_matches: int
    phash_hamming: int
    duplicate_candidate: bool


class HealthResponse(BaseModel):
    # Per-capability readiness so a keep-alive/monitor can see WHAT is degraded, not just up/down.
    status: str  # UP | DEGRADED
    opencv: bool
    imageHash: bool
    orb: bool
    syntheticModel: bool
    version: str
