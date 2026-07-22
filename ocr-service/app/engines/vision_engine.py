"""Google Cloud Vision OCR. Higher accuracy, used for the deployed demo (generous free tier). Needs
google-cloud-vision (requirements-vision.txt) and GOOGLE_APPLICATION_CREDENTIALS. Image PII leaves
your infra when this engine is active — a deliberate trade-off documented in the plan."""

from __future__ import annotations

import io

from PIL import Image

from .base import OcrEngine, PageResult


class VisionEngine(OcrEngine):
    name = "vision"

    def __init__(self) -> None:
        from google.cloud import vision  # lazy: only imported when this engine is selected

        self._vision = vision
        self._client = vision.ImageAnnotatorClient()

    def available(self) -> tuple[bool, str]:
        try:
            # Construct-time success (client + credentials resolved) is our readiness signal.
            return self._client is not None, "google cloud vision client ready"
        except Exception as exc:
            return False, f"vision client unavailable: {exc}"

    def extract_page(self, image: Image.Image) -> PageResult:
        buffer = io.BytesIO()
        image.save(buffer, format="PNG")
        vision_image = self._vision.Image(content=buffer.getvalue())
        response = self._client.document_text_detection(image=vision_image)
        if response.error.message:
            raise RuntimeError(f"vision error: {response.error.message}")

        annotation = response.full_text_annotation
        text = annotation.text if annotation else ""
        confidence = self._mean_confidence(annotation)
        return PageResult(text=text.strip(), confidence=confidence)

    @staticmethod
    def _mean_confidence(annotation) -> float | None:
        if not annotation or not annotation.pages:
            return None
        scores = [p.confidence for p in annotation.pages if p.confidence]
        return round(sum(scores) / len(scores) * 100, 2) if scores else None
