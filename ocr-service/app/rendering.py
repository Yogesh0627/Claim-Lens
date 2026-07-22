"""Turn an uploaded document (image or PDF) into a list of page images for OCR."""

from __future__ import annotations

import io

from PIL import Image

from .config import settings

_IMAGE_TYPES = {"image/png", "image/jpeg", "image/jpg", "image/tiff", "image/bmp", "image/webp"}


def is_pdf(content_type: str | None, filename: str | None) -> bool:
    if content_type and "pdf" in content_type.lower():
        return True
    return bool(filename and filename.lower().endswith(".pdf"))


def render_pages(content: bytes, content_type: str | None, filename: str | None) -> list[Image.Image]:
    """Returns one PIL image per page. PDFs are rasterised with PyMuPDF (no external poppler)."""
    if is_pdf(content_type, filename):
        return _render_pdf(content)
    return [Image.open(io.BytesIO(content)).convert("RGB")]


def _render_pdf(content: bytes) -> list[Image.Image]:
    import fitz  # PyMuPDF — imported lazily so image-only installs don't pay for it

    zoom = settings.pdf_render_dpi / 72.0
    matrix = fitz.Matrix(zoom, zoom)
    images: list[Image.Image] = []
    with fitz.open(stream=content, filetype="pdf") as doc:
        for page in doc:
            if len(images) >= settings.max_pdf_pages:
                break
            pix = page.get_pixmap(matrix=matrix)
            images.append(Image.frombytes("RGB", (pix.width, pix.height), pix.samples))
    if not images:
        raise ValueError("PDF contained no renderable pages")
    return images
