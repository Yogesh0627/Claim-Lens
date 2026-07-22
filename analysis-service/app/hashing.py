"""Perceptual hashing for duplicate/near-duplicate detection.

The service returns an image's hashes; the *backend* stores them and compares a new document's hash
against other claims' hashes (cheap Hamming distance) to flag reuse — the top motor-fraud pattern.
The service also offers a direct pairwise compare via /compare."""

from __future__ import annotations

import imagehash
from PIL import Image

from .models import ImageHashes


def compute_hashes(image: Image.Image) -> ImageHashes:
    return ImageHashes(
        average=str(imagehash.average_hash(image)),
        dhash=str(imagehash.dhash(image)),
        phash=str(imagehash.phash(image)),
    )


def hamming(hash_a_hex: str, hash_b_hex: str) -> int:
    """Hamming distance between two hex-encoded perceptual hashes (0 = identical)."""
    return imagehash.hex_to_hash(hash_a_hex) - imagehash.hex_to_hash(hash_b_hex)
