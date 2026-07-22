"""API tests via FastAPI's TestClient. The real OCR round-trip is skipped when Tesseract is absent."""

import io

import pytest
from fastapi.testclient import TestClient
from PIL import Image, ImageDraw

from app.engines.registry import get_engine
from app.main import app

client = TestClient(app)


def _engine_ready() -> bool:
    try:
        return get_engine().available()[0]
    except Exception:
        return False


def test_health_returns_engine_info():
    res = client.get("/health")
    assert res.status_code == 200
    body = res.json()
    assert body["engine"] in ("tesseract", "vision")
    assert "engine_available" in body


def test_ocr_rejects_empty_file():
    res = client.post("/ocr", files={"file": ("empty.png", b"", "image/png")})
    assert res.status_code == 400


def _text_image(text: str) -> bytes:
    img = Image.new("RGB", (900, 220), "white")
    draw = ImageDraw.Draw(img)
    draw.text((20, 60), text, fill="black")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


@pytest.mark.skipif(not _engine_ready(), reason="OCR engine (tesseract) not installed")
def test_ocr_end_to_end_extracts_registration():
    png = _text_image("Vehicle Reg MH12AB1234 Policy No MOT-2024-0098")
    res = client.post("/ocr", files={"file": ("rc.png", png, "image/png")})
    assert res.status_code == 200
    body = res.json()
    assert body["page_count"] == 1
    # OCR is imperfect, but the registration should survive on clean synthetic text.
    assert "MH12AB1234" in body["fields"]["registration_numbers"]
