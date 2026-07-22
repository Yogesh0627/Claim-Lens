"""Selects the OCR engine from config, lazily and once."""

from __future__ import annotations

from functools import lru_cache

from ..config import settings
from .base import OcrEngine


@lru_cache(maxsize=1)
def get_engine() -> OcrEngine:
    engine = settings.ocr_engine.lower().strip()
    if engine == "tesseract":
        from .tesseract_engine import TesseractEngine

        return TesseractEngine()
    if engine == "vision":
        from .vision_engine import VisionEngine

        return VisionEngine()
    raise ValueError(f"Unknown OCR_ENGINE '{settings.ocr_engine}' (expected 'tesseract' or 'vision')")
