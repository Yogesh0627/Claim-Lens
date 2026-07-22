"""Best-effort extraction of motor-insurance fields from raw OCR text.

These are *signals*, not ground truth — OCR is noisy, so the backend/fraud engine treats them as hints
(e.g. does the registration on the RC match the policy's vehicle?), never as authoritative values.
All patterns are deliberately permissive about spacing/separators OCR tends to mangle."""

from __future__ import annotations

import re

from ..models import ExtractedFields

# Indian plates: 2 letters (state), 1-2 digits (RTO), 1-3 letters (series), 4 digits — separators vary.
_REG_RE = re.compile(
    r"\b([A-Z]{2})[\s\-]?([0-9]{1,2})[\s\-]?([A-Z]{1,3})[\s\-]?([0-9]{4})\b",
    re.IGNORECASE,
)
# BH-series: 2 digits (year) BH 4 digits 1-2 letters.
_BH_RE = re.compile(r"\b([0-9]{2})[\s\-]?BH[\s\-]?([0-9]{4})[\s\-]?([A-Z]{1,2})\b", re.IGNORECASE)

_POLICY_RE = re.compile(
    r"policy\s*(?:no|number|#)?\.?\s*[:\-]?\s*([A-Z0-9][A-Z0-9\-/]{4,})",
    re.IGNORECASE,
)
_CHASSIS_RE = re.compile(
    r"chassis\s*(?:no|number)?\.?\s*[:\-]?\s*([A-Z0-9]{6,})",
    re.IGNORECASE,
)
_ENGINE_RE = re.compile(
    r"engine\s*(?:no|number)?\.?\s*[:\-]?\s*([A-Z0-9]{5,})",
    re.IGNORECASE,
)
_DATE_RE = re.compile(
    r"\b("
    r"\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4}"  # 01/06/2024, 1-6-24
    r"|\d{4}[/\-.]\d{1,2}[/\-.]\d{1,2}"  # 2024-06-01
    r"|\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{2,4}"
    r")\b",
    re.IGNORECASE,
)
_AMOUNT_RE = re.compile(
    r"(?:₹|Rs\.?|INR)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)",
    re.IGNORECASE,
)


def _dedupe(values: list[str]) -> list[str]:
    seen: set[str] = set()
    out: list[str] = []
    for v in values:
        if v not in seen:
            seen.add(v)
            out.append(v)
    return out


def _normalise_reg(parts: tuple[str, ...]) -> str:
    return "".join(parts).upper()


def extract_registration_numbers(text: str) -> list[str]:
    regs = [_normalise_reg(m.groups()) for m in _REG_RE.finditer(text)]
    regs += [f"{m.group(1)}BH{m.group(2)}{m.group(3)}".upper() for m in _BH_RE.finditer(text)]
    return _dedupe(regs)


def _extract_group(regex: re.Pattern[str], text: str) -> list[str]:
    return _dedupe([m.group(1).upper() for m in regex.finditer(text)])


def extract_amounts(text: str) -> list[float]:
    amounts: list[float] = []
    for m in _AMOUNT_RE.finditer(text):
        raw = m.group(1).replace(",", "")
        try:
            amounts.append(float(raw))
        except ValueError:
            continue
    # dedupe while preserving order
    seen: set[float] = set()
    out: list[float] = []
    for a in amounts:
        if a not in seen:
            seen.add(a)
            out.append(a)
    return out


def extract_fields(text: str) -> ExtractedFields:
    return ExtractedFields(
        registration_numbers=extract_registration_numbers(text),
        policy_numbers=_extract_group(_POLICY_RE, text),
        chassis_numbers=_extract_group(_CHASSIS_RE, text),
        engine_numbers=_extract_group(_ENGINE_RE, text),
        dates=_dedupe([m.group(1) for m in _DATE_RE.finditer(text)]),
        amounts=extract_amounts(text),
    )
