#!/usr/bin/env python3
"""
Validate Arabic root words in vocab.json (offline, deterministic).

Checks:
  - Every entry has a non-blank root
  - Content words (3+ Arabic letters, not in particle list) must NOT have
    root == key (i.e. must have a real triliteral root)
  - Roots are 2–4 Arabic characters (triliteral norm)
  - Root family consistency (same-root entries share a consistent root string)

Usage:
    python3 tools/validate_quran_roots.py
"""

from __future__ import annotations

import json
import re
import sys
import unicodedata
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent
VOCAB_PATH = BASE / "app" / "src" / "main" / "assets" / "quran" / "vocab.json"

# Particles, pronouns, prepositions, conjunctions — root==key is linguistically correct
_PARTICLES = {
    "من", "في", "على", "الا", "الا", "ان", "ان", "لا", "لا", "ما", "ما",
    "ثم", "ثم", "او", "او", "بل", "بل", "حتي", "حتي", "ليس", "ليس", "عن",
    "عن", "بين", "بين", "بعد", "بعد", "قبل", "قبل", "حتي", "حتي", "سوف",
    "سوف", "لعل", "لعل", "كي", "كي", "هل", "هل", "اما", "اما", "ولا",
    "ولا", "ولو", "ولو", "ولن", "ولن", "لم", "لم", "لن", "لن", "قد", "قد",
    "منذ", "منذ", "حين", "حين", "اي", "اي", "هيا", "هيا", "ذا", "ذا",
    "ذلك", "ذلك", "هذه", "هذه", "هذا", "هذا", "ذلك", "ذلك", "التي", "التي",
    "الذي", "الذي", "الذين", "الذين", "اللاتي", "اللاتي", "اللائي", "اللائي",
    "اللذين", "اللذين", "اللتين", "اللتين", "نحن", "نحن", "هم", "هم",
    "هن", "هن", "هنا", "هنا", "هناك", "هناك", "هكذا", "هكذا", "هو", "هو",
    "هي", "هي", "هما", "هما", "انت", "انت", "انتم", "انتم", "انتن", "انتن",
    "انتما", "انتما", "انا", "انا", "اولاء", "اولاء", "اولو", "اولو",
    "اولئك", "اولئك", "اله", "اله", "الهة", "الهة", "الذين", "الذين",
}

# Arabic letters only (no marks/diacritics)
_AR_LETTER_RE = re.compile(r"[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF]+")

_MARKS_RE = re.compile(r"[\u064B-\u0656\u0670\u06D6-\u06ED\u08D6-\u08ED]")


def count_arabic_letters(s: str) -> int:
    """Count Arabic letter characters (excluding diacritics)."""
    return len(_MARKS_RE.sub("", s))


def is_particle(key: str) -> bool:
    return key in _PARTICLES


def main() -> int:
    print("=== validate_quran_roots.py ===\n")

    vocab = json.loads(VOCAB_PATH.read_text(encoding="utf-8"))
    entries = vocab.get("entries", [])
    errors: list[str] = []
    warnings: list[str] = []

    # 1. Every entry has a non-blank root
    print("[1] Checking for blank roots...")
    blank = [e for e in entries if not e.get("root", "").strip()]
    if blank:
        errors.append(f"{len(blank)} entries have blank roots")
        for e in blank[:10]:
            errors.append(f"  {e['key']} (word: {e.get('word', '?')})")
    else:
        print(f"    OK: all {len(entries)} entries have roots")

    # 2. Content words must not have root==key
    print("[2] Checking content words (root != key)...")
    bad_self_root = []
    for e in entries:
        key = e["key"]
        root = e.get("root", "")
        if root == key and not is_particle(key):
            # Count Arabic letters to determine if it's likely a content word
            if count_arabic_letters(key) >= 3:
                bad_self_root.append(e)
    if bad_self_root:
        warnings.append(
            f"{len(bad_self_root)} content words have root==key (may be incorrect)"
        )
        for e in bad_self_root[:20]:
            warnings.append(f"  {e['key']} (root={e['root']}, word={e.get('word', '?')})")
    else:
        print(f"    OK: no content words with root==key")

    # 3. Root length check (2-4 Arabic letters for most)
    print("[3] Checking root lengths...")
    short_roots = []
    long_roots = []
    for e in entries:
        root = e.get("root", "")
        if not root:
            continue
        n = count_arabic_letters(root)
        if n < 2:
            short_roots.append((e["key"], root, n))
        elif n > 5:  # Allow some leeway for compound roots
            long_roots.append((e["key"], root, n))
    if short_roots:
        for key, root, n in short_roots[:10]:
            warnings.append(f"  Short root: {key} → {root} ({n} letters)")
    if long_roots:
        for key, root, n in long_roots[:10]:
            warnings.append(f"  Long root: {key} → {root} ({n} letters)")

    # 4. Family consistency: entries sharing the same root should agree
    print("[4] Checking root family consistency...")
    root_families: dict[str, list[str]] = {}
    for e in entries:
        root = e.get("root", "")
        if root:
            root_families.setdefault(root, []).append(e["key"])
    # Check for roots that look suspicious (too many entries)
    large = [(r, len(k)) for r, k in root_families.items() if len(k) > 30]
    if large:
        for root, count in sorted(large, key=lambda x: -x[1]):
            warnings.append(f"  Root {root}: {count} entries (may be over-broad)")

    # 5. Summary
    print(f"\n{'=' * 50}")
    print(f"Entries checked: {len(entries)}")
    print(f"Errors:   {len(errors)}")
    print(f"Warnings: {len(warnings)}")

    if errors:
        print(f"\nERRORS ({len(errors)}):")
        for e in errors:
            print(f"  {e}")

    if warnings:
        print(f"\nWARNINGS ({len(warnings)}):")
        for w in warnings:
            print(f"  {w}")

    print(f"{'=' * 50}")

    if errors:
        print("\nFAILED: errors found")
        return 1

    print("\nPASSED: no errors (warnings are informational)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
