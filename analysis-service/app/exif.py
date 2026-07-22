"""EXIF extraction and the three-state consistency judgment.

CONSISTENT / INCONSISTENT / UNKNOWN. Only INCONSISTENT should raise a fraud score; **missing EXIF is
UNKNOWN, never suspicious** — WhatsApp/Instagram/Facebook strip EXIF, so absence is the norm. This is
a load-bearing edge case: penalising stripped metadata would flag most legitimate claims."""

from __future__ import annotations

import math
from datetime import datetime, timedelta

from PIL import Image
from PIL.ExifTags import GPSTAGS, TAGS

from .config import settings
from .models import ExifResult

_DATETIME_TAGS = ("DateTimeOriginal", "DateTimeDigitized", "DateTime")


def _raw_exif(image: Image.Image) -> dict:
    try:
        exif = image.getexif()
    except Exception:
        return {}
    if not exif:
        return {}
    out: dict = {}
    for tag_id, value in exif.items():
        out[TAGS.get(tag_id, tag_id)] = value
    # GPS lives in a sub-IFD.
    try:
        gps_ifd = exif.get_ifd(0x8825)
        if gps_ifd:
            out["GPSInfo"] = {GPSTAGS.get(k, k): v for k, v in gps_ifd.items()}
    except Exception:
        pass
    return out


def _parse_datetime(raw: dict) -> datetime | None:
    for tag in _DATETIME_TAGS:
        value = raw.get(tag)
        if value:
            try:
                return datetime.strptime(str(value), "%Y:%m:%d %H:%M:%S")
            except ValueError:
                continue
    return None


def _dms_to_deg(dms, ref) -> float | None:
    try:
        d, m, s = (float(x) for x in dms)
        deg = d + m / 60.0 + s / 3600.0
        if ref in ("S", "W"):
            deg = -deg
        return deg
    except Exception:
        return None


def _parse_gps(raw: dict) -> tuple[float | None, float | None]:
    gps = raw.get("GPSInfo")
    if not isinstance(gps, dict):
        return None, None
    lat = _dms_to_deg(gps.get("GPSLatitude"), gps.get("GPSLatitudeRef"))
    lon = _dms_to_deg(gps.get("GPSLongitude"), gps.get("GPSLongitudeRef"))
    return lat, lon


def _haversine_km(lat1, lon1, lat2, lon2) -> float:
    r = 6371.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def analyse_exif(
    image: Image.Image,
    incident_date: datetime | None = None,
    expected_lat: float | None = None,
    expected_lon: float | None = None,
) -> ExifResult:
    raw = _raw_exif(image)
    captured_at = _parse_datetime(raw)
    lat, lon = _parse_gps(raw)
    has_datetime = captured_at is not None
    has_gps = lat is not None and lon is not None

    reasons: list[str] = []

    # Timestamp before the incident (beyond clock-skew tolerance) => the photo predates the event.
    if has_datetime and incident_date is not None:
        skew = timedelta(hours=settings.exif_skew_hours)
        if captured_at < incident_date - skew:
            reasons.append(
                f"capture time {captured_at.isoformat()} precedes incident date "
                f"{incident_date.date().isoformat()}"
            )

    # GPS far from the reported incident location.
    if has_gps and expected_lat is not None and expected_lon is not None:
        km = _haversine_km(lat, lon, expected_lat, expected_lon)
        if km > settings.exif_gps_km_threshold:
            reasons.append(f"GPS is {km:.0f} km from the reported location")

    if reasons:
        state = "INCONSISTENT"
    elif has_datetime or has_gps:
        state = "CONSISTENT"
    else:
        state = "UNKNOWN"

    return ExifResult(
        state=state,
        has_datetime=has_datetime,
        has_gps=has_gps,
        captured_at=captured_at.isoformat() if captured_at else None,
        gps_lat=lat,
        gps_lon=lon,
        reasons=reasons,
    )
