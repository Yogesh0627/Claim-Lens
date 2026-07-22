"""ORB feature-matching similarity — catches lightly-edited reuse (crop/rotate/recolour) that a
perceptual hash alone can miss. ORB, not SIFT: faster, free, good enough for V1."""

from __future__ import annotations

import cv2
import numpy as np
from PIL import Image

_orb = cv2.ORB_create(nfeatures=1000)
_matcher = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=False)


def _to_gray(image: Image.Image) -> np.ndarray:
    return cv2.cvtColor(np.array(image.convert("RGB")), cv2.COLOR_RGB2GRAY)


def orb_similarity(image_a: Image.Image, image_b: Image.Image) -> tuple[float, int]:
    """Returns (similarity 0..1, good-match count). Lowe's ratio test filters weak matches."""
    gray_a, gray_b = _to_gray(image_a), _to_gray(image_b)
    kp_a, des_a = _orb.detectAndCompute(gray_a, None)
    kp_b, des_b = _orb.detectAndCompute(gray_b, None)
    if des_a is None or des_b is None or len(kp_a) == 0 or len(kp_b) == 0:
        return 0.0, 0

    matches = _matcher.knnMatch(des_a, des_b, k=2)
    good = 0
    for pair in matches:
        if len(pair) == 2:
            m, n = pair
            if m.distance < 0.75 * n.distance:
                good += 1

    denom = max(1, min(len(kp_a), len(kp_b)))
    similarity = min(1.0, good / denom)
    return round(similarity, 4), good
