"""API tests via FastAPI's TestClient."""

import io

from fastapi.testclient import TestClient
from PIL import Image, ImageDraw

from app.main import app

client = TestClient(app)


def _png(seed: int = 1) -> bytes:
    img = Image.new("RGB", (200, 200), "white")
    draw = ImageDraw.Draw(img)
    draw.rectangle([20 + seed, 20, 160, 140], outline="black", width=3)
    draw.text((30, 60), f"claim doc {seed}", fill="black")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


def test_health_reports_per_capability_readiness():
    res = client.get("/health")
    assert res.status_code == 200
    body = res.json()
    assert body["status"] == "UP"
    assert body["opencv"] is True
    assert body["imageHash"] is True
    assert body["orb"] is True
    assert body["syntheticModel"] is True
    assert body["version"] == "1.0.0"


def test_analyze_returns_hashes_and_unknown_exif():
    res = client.post("/analyze", files={"file": ("photo.png", _png(), "image/png")})
    assert res.status_code == 200
    body = res.json()
    assert body["width"] == 200 and body["height"] == 200
    assert len(body["hashes"]["phash"]) > 0
    # Generated PNG has no EXIF -> UNKNOWN, never suspicious.
    assert body["exif"]["state"] == "UNKNOWN"
    assert 0.0 <= body["synthetic"]["score"] <= 1.0


def test_analyze_rejects_empty():
    res = client.post("/analyze", files={"file": ("empty.png", b"", "image/png")})
    assert res.status_code == 400


def test_compare_same_image_is_duplicate():
    png = _png(7)
    res = client.post(
        "/compare",
        files={
            "file_a": ("a.png", png, "image/png"),
            "file_b": ("b.png", png, "image/png"),
        },
    )
    assert res.status_code == 200
    body = res.json()
    assert body["phash_hamming"] == 0
    assert body["duplicate_candidate"] is True
