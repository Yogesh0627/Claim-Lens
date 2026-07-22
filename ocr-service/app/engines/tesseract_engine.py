"""Local, self-hosted OCR via Tesseract. Free and offline — the default engine and the local-dev
path. The tesseract binary must be installed (apt-get install tesseract-ocr, or the Windows build)."""

from __future__ import annotations

from PIL import Image

from ..config import settings
from .base import OcrEngine, PageResult


class TesseractEngine(OcrEngine):
    name = "tesseract"

    def __init__(self) -> None:
        import pytesseract

        self._pytesseract = pytesseract
        if settings.tesseract_cmd:
            pytesseract.pytesseract.tesseract_cmd = settings.tesseract_cmd

    def available(self) -> tuple[bool, str]:
        try:
            version = self._pytesseract.get_tesseract_version()
            return True, f"tesseract {version}"
        except Exception as exc:  # binary missing or not on PATH
            return False, f"tesseract binary not found: {exc}"

    def extract_page(self, image: Image.Image) -> PageResult:
        lang = settings.tesseract_lang
        text = self._pytesseract.image_to_string(image, lang=lang)
        confidence = self._page_confidence(image, lang)
        return PageResult(text=text.strip(), confidence=confidence)

    def _page_confidence(self, image: Image.Image, lang: str) -> float | None:
        """Mean per-word confidence Tesseract reports, ignoring its -1 (no-word) sentinels."""
        try:
            data = self._pytesseract.image_to_data(
                image, lang=lang, output_type=self._pytesseract.Output.DICT
            )
            scores = [float(c) for c in data.get("conf", []) if str(c) not in ("-1", "")]
            scores = [s for s in scores if s >= 0]
            return round(sum(scores) / len(scores), 2) if scores else None
        except Exception:
            return None
