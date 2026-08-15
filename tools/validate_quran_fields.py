#!/usr/bin/env python3
"""Validate all user-facing Quran fields in the bundled source data.

Arabic and Latin are compared with the official Kemenag/LPMQ API after the
shared cleanup rules are applied; letters, harakat, and pause marks remain
exact. The known Kemenag API misalignment at 23:78 is checked against the Arabic
ayah explicitly. Indonesian translation comparison ignores source footnote markers
and quotation punctuation, then checks quote balance separately.

Usage:
    python3 tools/validate_quran_fields.py
    python3 tools/validate_quran_fields.py --canonical-file /tmp/quran.json
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
import urllib.request
from pathlib import Path
from typing import Any

from quran_text_cleaner import (
    ARABIC_ARTIFACTS,
    clean_arabic,
    clean_indonesian,
    clean_latin,
)


BASE = Path(__file__).resolve().parent.parent
ASSET_DIR = BASE / "app" / "src" / "main" / "assets" / "quran" / "data"
CANONICAL_URL = "https://web-api.qurankemenag.net/quran-ayah?start=0&limit=7000"
EXPECTED_AYAHS = 6236
EXPECTED_SURAHS = 114

# The official API currently serves the Latin text for 23:80 under 23:78.
# Keep this exception explicit so the Arabic/Latin relationship is still gated.
MANUAL_LATIN = {
    (23, 78): "Wa huwal-lażī ansya'a lakumus-sam‘a wal-abṣāra wal-af'idata qalīlam mā tasykurūn(a).",
}


def fetch_canonical() -> dict[str, Any]:
    headers = {
        "Accept": "application/json",
        "Origin": "https://quran.kemenag.go.id",
        "Referer": "https://quran.kemenag.go.id/",
        "User-Agent": "Tahsin-Quran-field-validator/1.0",
    }
    request = urllib.request.Request(CANONICAL_URL, headers=headers)
    with urllib.request.urlopen(request, timeout=120) as response:
        return json.load(response)


def load_bundle() -> dict[tuple[int, int], dict[str, str]]:
    result: dict[tuple[int, int], dict[str, str]] = {}
    for surah in range(1, EXPECTED_SURAHS + 1):
        path = ASSET_DIR / f"surah-{surah}.json"
        payload = json.loads(path.read_text(encoding="utf-8"))
        rows = payload.get("data", {}).get("ayat", [])
        for row in rows:
            key = (surah, int(row["nomorAyat"]))
            result[key] = {
                "teksArab": str(row.get("teksArab", "")),
                "teksLatin": str(row.get("teksLatin", "")),
                "teksIndonesia": str(row.get("teksIndonesia", "")),
            }
    return result


def load_canonical(payload: dict[str, Any]) -> dict[tuple[int, int], dict[str, str]]:
    return {
        (int(row["surah_id"]), int(row["ayah"])): {
            "arabic": str(row.get("arabic", "")),
            "latin": str(row.get("latin", "")),
            "translation": str(row.get("translation", "")),
        }
        for row in payload.get("data", [])
    }


def trim(value: str) -> str:
    return value.strip()


def normalize_translation(value: str) -> str:
    value = re.sub(r"\d+\)", "", trim(value))
    value = value.replace("“", "").replace("”", "").replace('"', "")
    return re.sub(r"\s+", " ", value).strip()


def add(
    findings: list[dict[str, Any]],
    key: tuple[int, int],
    field: str,
    kind: str,
    wrong: str,
    suggestion: str,
) -> None:
    findings.append({
        "nomorSurah": key[0],
        "nomorAyat": key[1],
        "field": field,
        "jenis_kesalahan": kind,
        "teks_salah": wrong,
        "saran_perbaikan": suggestion,
    })


def check_quote_balance(
    bundle: dict[tuple[int, int], dict[str, str]],
    findings: list[dict[str, Any]],
) -> None:
    for surah in range(1, EXPECTED_SURAHS + 1):
        stack: list[tuple[tuple[int, int], str]] = []
        rows = [(key, bundle[key]["teksIndonesia"]) for key in sorted(bundle) if key[0] == surah]
        for key, text in rows:
            for char in text:
                if char == "“":
                    stack.append((key, text))
                elif char == "”":
                    if stack:
                        stack.pop()
                    else:
                        add(findings, key, "teksIndonesia", "tanda kutip penutup tanpa pembuka", "”", "")
                elif char == '"':
                    add(findings, key, "teksIndonesia", 'tanda kutip penutup memakai ASCII "', '"', "”")
        for key, text in stack:
            add(
                findings,
                key,
                "teksIndonesia",
                "tanda kutip pembuka tidak memiliki penutup",
                text[-120:],
                trim(text) + "”",
            )


def validate(bundle: dict[tuple[int, int], dict[str, str]], canonical: dict[tuple[int, int], dict[str, str]]) -> list[dict[str, Any]]:
    findings: list[dict[str, Any]] = []
    if len(bundle) != EXPECTED_AYAHS:
        add(findings, (0, 0), "nomorAyat", "jumlah ayat tidak 6236", str(len(bundle)), str(EXPECTED_AYAHS))
    if len(canonical) != EXPECTED_AYAHS:
        add(findings, (0, 0), "nomorAyat", "sumber kanonik tidak 6236 ayat", str(len(canonical)), str(EXPECTED_AYAHS))

    for key in sorted(bundle):
        row = bundle[key]
        ref = canonical.get(key)
        if ref is None:
            add(findings, key, "nomorAyat", "ayat tidak ada pada sumber kanonik", str(key[1]), "ayat yang sesuai")
            continue

        arabic = trim(row["teksArab"])
        for char in arabic:
            name = unicodedata.name(char, "UNKNOWN")
            if not char.isspace() and not name.startswith("ARABIC "):
                add(findings, key, "teksArab", "karakter non-Arab", char, "")
                break
        for artifact in ARABIC_ARTIFACTS:
            if artifact in arabic:
                add(findings, key, "teksArab", "artefak karakter Arab", artifact, "")
                break
        expected_arabic = clean_arabic(ref["arabic"], key)
        if arabic != expected_arabic:
            add(findings, key, "teksArab", "huruf atau harakat tidak sesuai mushaf kanonik", arabic, expected_arabic)

        latin = trim(row["teksLatin"])
        for char in row["teksLatin"]:
            if ord(char) < 32 or 127 <= ord(char) < 160:
                add(findings, key, "teksLatin", f"karakter kontrol/non-Latin U+{ord(char):04X}", row["teksLatin"], row["teksLatin"].replace(char, "‘"))
                break
        expected_latin = clean_latin(MANUAL_LATIN.get(key, ref["latin"]))
        if latin != expected_latin:
            kind = "transliterasi bergeser ke ayat lain" if key in {(23, 79), (23, 80), (23, 81)} else "transliterasi tidak sesuai standar Kemenag"
            add(findings, key, "teksLatin", kind, trim(row["teksLatin"]), expected_latin)

        expected_translation = clean_indonesian(ref["translation"], key)
        if normalize_translation(row["teksIndonesia"]) != normalize_translation(expected_translation):
            add(findings, key, "teksIndonesia", "teks berbeda dari terjemahan kanonik", row["teksIndonesia"], expected_translation)

    check_quote_balance(bundle, findings)
    return findings


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--canonical-file", type=Path)
    args = parser.parse_args()
    try:
        bundle = load_bundle()
        payload = json.loads(args.canonical_file.read_text(encoding="utf-8")) if args.canonical_file else fetch_canonical()
        findings = validate(bundle, load_canonical(payload))
        print(json.dumps({"hasil": findings}, ensure_ascii=False, indent=2))
        return 1 if findings else 0
    except Exception as error:  # noqa: BLE001 - command-line boundary
        print(json.dumps({"hasil": [{"jenis_kesalahan": str(error)}]}, ensure_ascii=False, indent=2))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
