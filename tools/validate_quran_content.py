#!/usr/bin/env python3
"""Validate bundled Arabic ayahs against the official Kemenag/LPMQ source.

The app's Arabic bundle is generated from equran.id. This validator deliberately
uses a different source: the official Qur'an Kemenag/LPMQ API. Comparison is an
exact Unicode comparison after trimming transport whitespace; no harakah or
pause-mark normalization is performed.

Usage:
    python3 tools/validate_quran_content.py
    python3 tools/validate_quran_content.py --fix --write-manifest
    python3 tools/validate_quran_content.py --canonical-file /tmp/quran.json

``--fix`` changes only the ``teksArab`` value for an ayah whose canonical value
is unambiguous. It preserves the original one-line API JSON and never rewrites
translations or metadata. Review the resulting diff before committing.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import time
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


BASE = Path(__file__).resolve().parent.parent
ASSET_DIR = BASE / "app" / "src" / "main" / "assets" / "quran" / "data"
MANIFEST_PATH = BASE / "tools" / "quran-canonical-manifest.json"
CANONICAL_URL = "https://web-api.qurankemenag.net/quran-ayah?start=0&limit=7000"
EXPECTED_AYAHS = 6236
EXPECTED_SURAHS = 114


def fetch_canonical() -> dict[str, Any]:
    """Fetch the full Kemenag/LPMQ response with conservative retries."""
    headers = {
        "Accept": "application/json",
        "Origin": "https://quran.kemenag.go.id",
        "Referer": "https://quran.kemenag.go.id/",
        "User-Agent": "Tahsin-Quran-content-validator/1.0",
    }
    last_error: Exception | None = None
    for attempt in range(3):
        try:
            request = urllib.request.Request(CANONICAL_URL, headers=headers)
            with urllib.request.urlopen(request, timeout=120) as response:
                return json.load(response)
        except Exception as error:  # noqa: BLE001 - retry network failures
            last_error = error
            if attempt < 2:
                time.sleep(2**attempt)
    raise RuntimeError(f"cannot fetch canonical Quran source: {last_error}")


def load_json(path: Path) -> dict[str, Any]:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as error:  # noqa: BLE001 - report the asset path
        raise RuntimeError(f"invalid JSON: {path}: {error}") from error


def load_local() -> dict[tuple[int, int], dict[str, Any]]:
    """Load every bundled ayah keyed by (surah, ayah)."""
    result: dict[tuple[int, int], dict[str, Any]] = {}
    for surah in range(1, EXPECTED_SURAHS + 1):
        path = ASSET_DIR / f"surah-{surah}.json"
        payload = load_json(path)
        rows = payload.get("data", {}).get("ayat", [])
        if not isinstance(rows, list):
            raise RuntimeError(f"{path}: data.ayat is not a list")
        for row in rows:
            key = (surah, int(row.get("nomorAyat", 0)))
            if key in result:
                raise RuntimeError(f"duplicate local ayah {key[0]}:{key[1]}")
            result[key] = {
                "arabic": str(row.get("teksArab", "")).strip(),
                "path": path,
            }
    return result


def load_canonical(payload: dict[str, Any]) -> dict[tuple[int, int], dict[str, str]]:
    rows = payload.get("data")
    if not isinstance(rows, list):
        raise RuntimeError("canonical response has no data list")
    result: dict[tuple[int, int], dict[str, str]] = {}
    for row in rows:
        try:
            key = (int(row["surah_id"]), int(row["ayah"]))
            arabic = str(row["arabic"]).strip()
        except (KeyError, TypeError, ValueError) as error:
            raise RuntimeError(f"invalid canonical ayah row: {row!r}") from error
        if key in result:
            raise RuntimeError(f"duplicate canonical ayah {key[0]}:{key[1]}")
        result[key] = {"arabic": arabic}
    return result


def replace_arabic(path: Path, old: str, new: str) -> None:
    """Replace one JSON string without reformatting the generated API file."""
    old_json = json.dumps(old, ensure_ascii=False)
    new_json = json.dumps(new, ensure_ascii=False)
    pattern = re.compile(r'("teksArab"\s*:\s*)' + re.escape(old_json))
    raw = path.read_text(encoding="utf-8")
    updated, count = pattern.subn(lambda match: match.group(1) + new_json, raw, count=1)
    if count != 1:
        raise RuntimeError(f"could not locate one teksArab value to repair in {path}")
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(updated, encoding="utf-8")
    os.replace(temporary, path)


def text_hash(rows: dict[tuple[int, int], dict[str, str]]) -> str:
    digest = hashlib.sha256()
    for (surah, ayah), row in sorted(rows.items()):
        digest.update(f"{surah}:{ayah}|{row['arabic']}\n".encode("utf-8"))
    return digest.hexdigest()


def compare(
    local: dict[tuple[int, int], dict[str, Any]],
    canonical: dict[tuple[int, int], dict[str, str]],
) -> list[tuple[tuple[int, int], dict[str, Any], dict[str, str]]]:
    mismatches = []
    for key in sorted(set(local) | set(canonical)):
        local_row = local.get(key, {"arabic": "", "path": ASSET_DIR})
        canonical_row = canonical.get(key, {"arabic": ""})
        if local_row["arabic"] != canonical_row["arabic"]:
            mismatches.append((key, local_row, canonical_row))
    return mismatches


def write_manifest(canonical: dict[tuple[int, int], dict[str, str]], local: dict[tuple[int, int], dict[str, Any]]) -> None:
    manifest = {
        "schemaVersion": 1,
        "source": "Qur'an Kemenag / LPMQ official API",
        "sourceUrl": CANONICAL_URL,
        "comparison": "exact Unicode Arabic text after trim",
        "surahs": EXPECTED_SURAHS,
        "ayahs": EXPECTED_AYAHS,
        "canonicalTextSha256": text_hash(canonical),
        "bundledTextSha256": text_hash(local),
        "validatedAt": datetime.now(timezone.utc).isoformat(),
    }
    temporary = MANIFEST_PATH.with_name(MANIFEST_PATH.name + ".tmp")
    temporary.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    os.replace(temporary, MANIFEST_PATH)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--canonical-file", type=Path, help="use a saved canonical API response instead of the network")
    parser.add_argument("--fix", action="store_true", help="repair exact canonical Arabic mismatches in generated assets")
    parser.add_argument("--write-manifest", action="store_true", help="write tools/quran-canonical-manifest.json after a clean comparison")
    args = parser.parse_args()

    try:
        payload = (
            load_json(args.canonical_file)
            if args.canonical_file
            else fetch_canonical()
        )
        canonical = load_canonical(payload)
        local = load_local()
        if len(canonical) != EXPECTED_AYAHS:
            raise RuntimeError(f"canonical source has {len(canonical)} ayahs; expected {EXPECTED_AYAHS}")
        if len(local) != EXPECTED_AYAHS:
            raise RuntimeError(f"bundled assets have {len(local)} ayahs; expected {EXPECTED_AYAHS}")

        mismatches = compare(local, canonical)
        if mismatches and args.fix:
            for key, local_row, canonical_row in mismatches:
                if key not in local or "path" not in local_row:
                    continue
                replace_arabic(local_row["path"], local_row["arabic"], canonical_row["arabic"])
            local = load_local()
            mismatches = compare(local, canonical)

        if mismatches:
            print(f"FAIL: {len(mismatches)} ayah mismatches", file=sys.stderr)
            for (surah, ayah), local_row, canonical_row in mismatches[:20]:
                print(f"  {surah}:{ayah} ({local_row['path'].name})", file=sys.stderr)
                print(f"    bundled:   {local_row['arabic']}", file=sys.stderr)
                print(f"    canonical: {canonical_row['arabic']}", file=sys.stderr)
            if len(mismatches) > 20:
                print(f"  ... {len(mismatches) - 20} more", file=sys.stderr)
            return 1

        print(f"OK: {len(local)} ayahs across {EXPECTED_SURAHS} surahs match Kemenag/LPMQ exactly")
        print(f"SHA-256: {text_hash(local)}")
        if args.write_manifest:
            write_manifest(canonical, local)
            print(f"Manifest: {MANIFEST_PATH}")
        return 0
    except Exception as error:  # noqa: BLE001 - command-line error boundary
        print(f"FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
