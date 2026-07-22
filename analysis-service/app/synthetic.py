"""Synthetic / tampered-image soft signal.

Honest scope: no reliable detector of clean AI generation exists, so this is **always** an
investigator signal, never an auto-reject. We combine two cheap, free heuristics:
  - ELA (Error Level Analysis): recompress and diff — surfaces splicing/local editing (weak against
    clean diffusion output, but free).
  - C2PA presence: a signed provenance manifest, if present, is high-precision — reported as a note.
A proper CNN/commercial detector is the V2 swap behind this same function."""

from __future__ import annotations

import io

import numpy as np
from PIL import Image

from .models import SyntheticResult

_C2PA_MARKERS = (b"c2pa", b"jumbf", b"jumd")


def _ela_score(image: Image.Image) -> float:
    """0..1 from the spread of recompression error — higher suggests localized edits."""
    rgb = image.convert("RGB")
    buffer = io.BytesIO()
    rgb.save(buffer, format="JPEG", quality=90)
    buffer.seek(0)
    recompressed = Image.open(buffer).convert("RGB")

    original = np.asarray(rgb, dtype=np.int16)
    again = np.asarray(recompressed, dtype=np.int16)
    diff = np.abs(original - again).astype(np.float32)

    # Normalise the standard deviation of the error into a rough 0..1 band. Deliberately conservative.
    std = float(diff.std())
    return round(min(1.0, std / 12.0), 4)


def detect_synthetic(image: Image.Image, raw: bytes) -> SyntheticResult:
    lowered = raw[:65536].lower()
    has_c2pa = any(marker in lowered for marker in _C2PA_MARKERS)

    score = _ela_score(image)
    signal = score >= 0.5 or has_c2pa

    if has_c2pa:
        note = "C2PA/provenance manifest present (not parsed) — inspect provenance"
        method = "c2pa-presence+ela"
    else:
        note = "Experimental ELA heuristic - weak against clean AI generation; treat as a low-weight hint"
        method = "ela"

    return SyntheticResult(signal=signal, score=score, method=method, note=note)
