"""The engine seam. Swapping Tesseract for Google Vision (or any future engine) is one impl of this
protocol selected by config — no caller changes. Mirrors the backend's OcrEngine interface intent."""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass

from PIL import Image


@dataclass
class PageResult:
    text: str
    confidence: float | None  # 0..100, or None when the engine gives no score


class OcrEngine(ABC):
    name: str

    @abstractmethod
    def extract_page(self, image: Image.Image) -> PageResult:
        """OCR a single page image."""

    @abstractmethod
    def available(self) -> tuple[bool, str]:
        """(is_usable, human-readable detail) — lets /health report engine readiness."""
