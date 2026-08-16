#!/usr/bin/env python3
"""Shared cleanup rules for the bundled Indonesian Quran data."""

from __future__ import annotations

import re
from typing import Any


WHITESPACE_RE = re.compile(r"\s+")

# These characters occur in the upstream export as formatting artifacts, not
# as Quranic letters or pause marks. The second rule also removes the stray
# kasra that is attached to the malformed ِۨ sequence.
ARABIC_ARTIFACTS = ("ِۨ", "ۨ", "ە", "۔")

ARABIC_FIELD_FIXES = {
    (11, 5): ("الصُّدُوْرِ", "الصُّدُوْرِ ۗ"),
    (11, 19): ("كفِٰرُوْنَ", "كٰفِرُوْنَ"),
    (11, 30): ("اِنْ طَرَدْتُهُمْ", "اِنْ طَرَدْتُّهُمْ"),
    (11, 32): ("فَاَ كْثَرْتَ", "فَاَكْثَرْتَ"),
}

QUOTE_CLOSERS = {
    (2, 139),
    (2, 248),
    (4, 127),
    (6, 135),
    (8, 12),
    (9, 51),
    (10, 81),
    (11, 2),
    (11, 25),
    (11, 28),
    (11, 33),
    (11, 36),
    (11, 50),
    (11, 51),
    (11, 54),
    (11, 121),
    (14, 6),
    (14, 8),
    (17, 80),
    (20, 87),
    (21, 89),
    (37, 168),
    (39, 53),
    (43, 9),
    (71, 5),
}

# Some dialogue continues into the next ayah. Re-open it there after closing
# the preceding field so every repaired boundary remains well-formed.
QUOTE_CONTINUATIONS = {
    (11, 3),
    (11, 26),
    (11, 29),
    (11, 34),
    (11, 37),
    (11, 52),
    (11, 55),
    (11, 122),
}

# Ayahs that have a stray closing quote without an opening quote — remove it.
QUOTE_REMOVALS = {
    (6, 71),
    (6, 81),
    (19, 9),
    (26, 166),
    (32, 14),
    (33, 63),
}


def normalize_spaces(value: str) -> str:
    return WHITESPACE_RE.sub(" ", value).strip()


def clean_arabic(value: str, key: tuple[int, int] | None = None) -> str:
    for artifact in ARABIC_ARTIFACTS:
        value = value.replace(artifact, "")
    if key in ARABIC_FIELD_FIXES:
        old, new = ARABIC_FIELD_FIXES[key]
        if new not in value:
            value = value.replace(old, new)
    return normalize_spaces(value)


def clean_latin(value: str) -> str:
    value = value.replace("\u0091", "‘").replace("\u00ad", "").replace("\u00b4", "‘")
    value = value.replace("yaqūlūnaftarāh", "yaqūlūna iftarāh")
    return normalize_spaces(value)


def clean_indonesian(value: str, key: tuple[int, int] | None = None) -> str:
    value = normalize_spaces(value)
    # Replace ASCII double-quotes with proper Unicode curly quotes.
    # The last " in the text is typically a closing quote; others are openers.
    if '"' in value:
        last = value.rfind('"')
        value = value[:last] + "\u201d" + value[last + 1:]
        value = value.replace('"', "\u201c")
    if key in QUOTE_CLOSERS:
        body = value[:-1] if value.endswith("\u201d") else value
        if body and body[-1] not in ".?!…":
            body += "."
        value = body + "\u201d"
    if key in QUOTE_CONTINUATIONS and not value.startswith("\u201c"):
        value = "\u201c" + value
    if key in QUOTE_REMOVALS:
        if value.endswith("\u201d"):
            value = value[:-1]
    return value


def clean_payload(payload: dict[str, Any]) -> dict[str, Any]:
    """Clean ayah fields in an equran-compatible response in place."""
    data = payload.get("data", {})
    surah = int(data.get("nomor", 0))
    for row in data.get("ayat", []):
        key = (surah, int(row.get("nomorAyat", 0)))
        row["teksArab"] = clean_arabic(str(row.get("teksArab", "")), key)
        row["teksLatin"] = clean_latin(str(row.get("teksLatin", "")))
        row["teksIndonesia"] = clean_indonesian(str(row.get("teksIndonesia", "")), key)
    return payload
