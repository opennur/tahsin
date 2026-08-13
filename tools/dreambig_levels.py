#!/usr/bin/env python3
"""Bangun `app/src/main/assets/dreambig/levels.json` — pembagian kosakata
terkurasi ke 10 level game "Dream BIG" (Day 1..10).

Distribusi dasar: entri vocab.json diurutkan frekuensi menurun, dipecah jadi
10 kelompok berurutan (Day 1 = kata PALING sering). Bisa di-override manual
lewat `LEVEL_WORDS_OVERRIDE` (key → daftar kunci vocab) — mengikuti pola
`CURATED_EXTRA` di curate_vocab.py.

Output `levels.json`:
  {"levels": [{"day": 1, "title": "Day 1", "wordKeys": ["من", "قال", ...]}, ...]}

Usage:
  python3 tools/dreambig_levels.py
  python3 tools/dreambig_levels.py --levels 10
"""

import argparse
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VOCAB_PATH = os.path.join(ROOT, "app", "src", "main", "assets", "quran", "vocab.json")
OUT_PATH = os.path.join(ROOT, "app", "src", "main", "assets", "dreambig", "levels.json")

DEFAULT_LEVELS = 10
MIN_WORDS_PER_LEVEL = 40

# Override manual: day → daftar kunci vocab (kosong = pakai distribusi frekuensi).
LEVEL_WORDS_OVERRIDE: dict[int, list[str]] = {}


def load_curated_entries() -> list[dict]:
    with open(VOCAB_PATH, encoding="utf-8") as f:
        data = json.load(f)
    entries = [e for e in data.get("entries", []) if e.get("meaningId") and e.get("meaningEn")]
    # Urut frekuensi menurun (paling sering di depan) — deterministik.
    return sorted(entries, key=lambda e: e.get("freq", 0), reverse=True)


def build_levels(n_levels: int) -> list[dict]:
    entries = load_curated_entries()
    if not entries:
        print("  [error] vocab.json kosong / tidak ada entri terkurasi", file=sys.stderr)
        sys.exit(1)

    # Kelompok dasar: potong berurutan (Day 1 = kata PALING sering, naik
    # bertahap) — progresif, cocok untuk jalur belajar 10 hari.
    chunk_size = (len(entries) + n_levels - 1) // n_levels
    base_chunks: list[list[str]] = [
        [e["key"] for e in entries[i * chunk_size:(i + 1) * chunk_size]]
        for i in range(n_levels)
    ]

    levels = []
    seen: set[str] = set()
    for day in range(1, n_levels + 1):
        keys = LEVEL_WORDS_OVERRIDE.get(day, base_chunks[day - 1])
        dupes = sorted(set(keys) & seen)
        if dupes:
            print(f"  [error] Day {day}: kunci duplikat antar level: {dupes}", file=sys.stderr)
            sys.exit(1)
        missing = [k for k in keys if k not in {e["key"] for e in entries}]
        if missing:
            print(f"  [error] Day {day}: kunci tidak ada di vocab.json: {missing}", file=sys.stderr)
            sys.exit(1)
        if len(keys) < MIN_WORDS_PER_LEVEL:
            print(
                f"  [warn] Day {day}: hanya {len(keys)} kata (< {MIN_WORDS_PER_LEVEL})",
                file=sys.stderr,
            )
        seen.update(keys)
        levels.append({"day": day, "title": f"Day {day}", "wordKeys": keys})

    total = sum(len(l["wordKeys"]) for l in levels)
    print(f"  {n_levels} level, {total} kata (unique={len(seen)})")
    return levels


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--levels", type=int, default=DEFAULT_LEVELS)
    args = ap.parse_args()

    print("[build] levels.json dari vocab.json...")
    levels = build_levels(args.levels)
    payload = {"levels": levels}
    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=1)
    print(f"[ok] {os.path.relpath(OUT_PATH, ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
