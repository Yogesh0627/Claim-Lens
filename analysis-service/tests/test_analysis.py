"""Unit tests for the analysis primitives. All run — imagehash/opencv are pip packages, no binaries."""

from PIL import Image, ImageDraw

from app.exif import analyse_exif
from app.hashing import compute_hashes, hamming
from app.similarity import orb_similarity
from app.synthetic import detect_synthetic


def _photo(seed: int, size=(256, 256)) -> Image.Image:
    """A deterministic non-trivial image (feature-rich enough for ORB)."""
    img = Image.new("RGB", size, "white")
    draw = ImageDraw.Draw(img)
    for i in range(0, size[0], 16):
        draw.line([(i, 0), (size[0] - i, size[1])], fill=(seed % 255, (i + seed) % 255, 90), width=2)
    draw.rectangle([40 + seed % 20, 40, 200, 180], outline="black", width=3)
    return img


def test_identical_images_have_zero_hamming():
    img = _photo(1)
    a, b = compute_hashes(img), compute_hashes(img)
    assert hamming(a.phash, b.phash) == 0


def test_different_images_have_nonzero_hamming():
    assert hamming(compute_hashes(_photo(1)).phash, compute_hashes(_photo(200)).phash) > 0


def test_exif_absent_is_unknown_not_suspicious():
    result = analyse_exif(_photo(1))
    assert result.state == "UNKNOWN"
    assert result.has_datetime is False and result.has_gps is False
    assert result.reasons == []


def test_orb_self_similarity_is_high():
    img = _photo(5)
    similarity, matches = orb_similarity(img, img)
    assert similarity > 0.5 and matches > 0


def test_synthetic_returns_bounded_soft_signal():
    result = detect_synthetic(_photo(1), b"\xff\xd8\xff")
    assert 0.0 <= result.score <= 1.0
    assert isinstance(result.signal, bool)


def test_synthetic_flags_c2pa_presence():
    result = detect_synthetic(_photo(1), b"....jumbf....c2pa....")
    assert result.signal is True
    assert "c2pa" in result.method
