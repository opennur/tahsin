#!/usr/bin/env python3
"""
Fetch real Arabic roots from the Quranic Arabic Corpus (QAC) morphology v0.4
and apply them to vocab.json.

Source: Quranic Arabic Corpus (Kais Dukes, University of Leeds)
        https://corpus.quran.com — GNU General Public License
        Version 0.4 — 49,968 tokens with ROOT field (Buckwalter)
        Downloaded from: cltk/arabic_morphology_quranic-corpus (GitHub mirror)

Usage:
    python3 tools/fetch_quran_roots.py            # fetch + apply
    python3 tools/fetch_quran_roots.py --check     # dry-run: report coverage only
    python3 tools/fetch_quran_roots.py --force      # re-download even if cached
"""

from __future__ import annotations

import json
import os
import re
import sys
import unicodedata
import urllib.request
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent
VOCAB_PATH = BASE / "app" / "src" / "main" / "assets" / "quran" / "vocab.json"
CACHE_PATH = BASE / "tools" / "quran-roots.json"

QAC_MORPHOLOGY_URL = (
    "https://raw.githubusercontent.com/cltk/arabic_morphology_quranic-corpus"
    "/master/quranic-corpus-morphology-0.4.txt"
)

# Buckwalter transliteration → Arabic Unicode
_BW_TO_AR = {
    "A": "\u0627", "b": "\u0628", "t": "\u062a", "v": "\u062b", "j": "\u062c",
    "H": "\u062d", "x": "\u062e", "d": "\u062f", "*": "\u0630", "r": "\u0631",
    "z": "\u0632", "s": "\u0633", "$": "\u0634", "S": "\u0635", "D": "\u0636",
    "T": "\u0637", "Z": "\u0638", "E": "\u0639", "g": "\u063a", "f": "\u0641",
    "q": "\u0642", "k": "\u0643", "l": "\u0644", "m": "\u0645", "n": "\u0646",
    "h": "\u0647", "w": "\u0648", "Y": "\u0649", "y": "\u064a", "o": "\u0652",
    "a": "\u064e", "u": "\u064f", "i": "\u0650", "~": "\u0651", "`": "\u0670",
    "{": "\u0671", "^": "\u0671", "F": "\u064b", "N": "\u064c", "K": "\u064d",
    "C": "\u0621", "X": "\u0629", "P": "\u067e", "J": "\u0686", "V": "\u06a4",
    "G": "\u06c1", "_": "\u0640",
}

# Diacritics stripped by VocabKey.normalize() in the app
_MARKS_RE = re.compile(r"[\u064B-\u0656\u0670\u06D6-\u06ED\u08D6-\u08ED]")


def bw_to_arabic(s: str) -> str:
    """Convert Buckwalter transliteration to Arabic Unicode."""
    return "".join(_BW_TO_AR.get(ch, ch) for ch in s)


def normalize(s: str) -> str:
    """Mirror VocabKey.normalize() from the Android app exactly."""
    s = unicodedata.normalize("NFKC", s)
    s = _MARKS_RE.sub("", s)
    s = s.replace("\u0623", "\u0627").replace("\u0625", "\u0627")
    s = s.replace("\u0622", "\u0627").replace("\u0671", "\u0627")
    s = s.replace("\u0649", "\u064a")
    s = s.replace("\u0629", "\u0647")
    s = s.replace("\u0621", "")
    s = s.replace("\u0640", "")
    return s.strip()


def download_morphology(url: str, dest: Path, force: bool = False) -> str:
    """Download QAC morphology file, return content as string."""
    if dest.exists() and not force:
        print(f"  Using cached: {dest}")
        return dest.read_text(encoding="utf-8")
    print(f"  Downloading: {url}")
    req = urllib.request.Request(url, headers={"User-Agent": "fetch_quran_roots/1.0"})
    with urllib.request.urlopen(req, timeout=120) as resp:
        content = resp.read().decode("utf-8")
    dest.write_text(content, encoding="utf-8")
    print(f"  Saved: {dest} ({len(content)} bytes)")
    return content


def parse_form_root_map(content: str) -> dict[str, str]:
    """
    Parse QAC morphology and build full-token → root map.

    Each line: LOCATION  FORM  TAG  FEATURES
    LOCATION: (surah:ayah:token:segment) e.g. (1:1:1:1)
    FORM: Buckwalter transliteration of that segment
    FEATURES: contains ROOT:xxx for STEM rows

    Strategy: group rows by (surah, ayah, token), concatenate all segment
    FORMs in order to get the full word, extract ROOT from the STEM row.
    """
    tokens: dict[tuple[int, int, int], dict] = {}

    for line in content.splitlines():
        if line.startswith("#") or "\t" not in line:
            continue
        parts = line.rstrip("\n").split("\t")
        if len(parts) < 4:
            continue
        loc, form, tag, feats = parts

        nums = re.findall(r"\d+", loc)
        if len(nums) < 4:
            continue
        key = (int(nums[0]), int(nums[1]), int(nums[2]))  # (surah, ayah, token)
        seg = int(nums[3])

        t = tokens.setdefault(key, {"segments": {}, "root": None})
        t["segments"][seg] = bw_to_arabic(form)

        m = re.search(r"ROOT:([A-Za-z~{^]+)", feats)
        if m:
            t["root"] = normalize(bw_to_arabic(m.group(1)))

    form_root: dict[str, str] = {}
    for t in tokens.values():
        if not t["root"]:
            continue
        full = "".join(t["segments"][s] for s in sorted(t["segments"]))
        nk = normalize(full)
        if nk:
            form_root.setdefault(nk, t["root"])

    return form_root


# Proclitics to strip from vocab keys when matching (Arabic letters)
_PROCLITICS = ["\u0648", "\u0641", "\u0628", "\u0644", "\u0643",
               "\u0627\u0644", "\u0633"]


def lookup_root(key: str, form_root: dict[str, str]) -> str | None:
    """Look up root by direct match, then by proclitic stripping."""
    if key in form_root:
        return form_root[key]
    for p in _PROCLITICS:
        if key.startswith(p):
            r = form_root.get(key[len(p):])
            if r:
                return r
    return None


def load_manual_roots() -> dict[str, str]:
    """Load manual overrides from vocab_roots.py (same module as curate_vocab.py)."""
    roots_py = BASE / "tools" / "vocab_roots.py"
    if not roots_py.exists():
        return {}
    # Execute the file to extract ROOTS and ROOT_MEANINGS
    namespace: dict = {}
    exec(roots_py.read_text(encoding="utf-8"), namespace)  # noqa: S102
    return {normalize(k): v for k, v in namespace.get("ROOTS", {}).items()}


def apply_roots(
    entries: list[dict],
    form_root: dict[str, str],
    manual: dict[str, str],
    check_only: bool = False,
) -> tuple[list[dict], dict]:
    """
    Apply QAC roots to vocab entries.

    Priority: manual overrides > QAC match > existing root (kept)
    Returns (updated_entries, stats).
    """
    fixed = 0
    already_ok = 0
    matched_qac = 0
    matched_manual = 0
    unmatched = 0
    particle_kept = 0

    for entry in entries:
        key = entry["key"]
        old_root = entry.get("root", "")

        # Manual override takes highest priority
        if key in manual:
            new_root = manual[key]
            if new_root != old_root and not check_only:
                entry["root"] = new_root
            if new_root:
                matched_manual += 1
                if new_root != old_root:
                    fixed += 1
                else:
                    already_ok += 1
                continue

        # QAC match
        qac_root = lookup_root(key, form_root)
        if qac_root:
            if qac_root != old_root and not check_only:
                entry["root"] = qac_root
            matched_qac += 1
            if qac_root != old_root:
                fixed += 1
            else:
                already_ok += 1
            continue

        # No QAC match: keep existing root
        if old_root:
            # Check if it's a particle (root==key is OK for particles)
            if old_root == key:
                particle_kept += 1
            already_ok += 1
        else:
            unmatched += 1

    total = len(entries)
    stats = {
        "total": total,
        "matched_qac": matched_qac,
        "matched_manual": matched_manual,
        "already_ok": already_ok,
        "fixed": fixed,
        "particle_kept": particle_kept,
        "unmatched": unmatched,
        "coverage": round(100 * (matched_qac + matched_manual) / total, 1),
    }
    return entries, stats


def main() -> int:
    check_only = "--check" in sys.argv
    force = "--force" in sys.argv

    print("=== fetch_quran_roots.py ===\n")

    # 1. Download QAC morphology
    print("[1] Downloading Quranic Arabic Corpus morphology v0.4...")
    content = download_morphology(QAC_MORPHOLOGY_URL, CACHE_PATH, force=force)

    # 2. Parse form→root map
    print("[2] Parsing QAC morphology (Buckwalter → Arabic → root)...")
    form_root = parse_form_root_map(content)
    print(f"    Parsed {len(form_root)} unique Arabic form → root mappings")

    # 3. Load manual overrides
    print("[3] Loading manual overrides from vocab_roots.py...")
    manual = load_manual_roots()
    print(f"    {len(manual)} manual root overrides")

    # 4. Load vocab entries
    print("[4] Loading vocab.json...")
    vocab = json.loads(VOCAB_PATH.read_text(encoding="utf-8"))
    entries = vocab.get("entries", [])
    print(f"    {len(entries)} entries")

    # 5. Apply roots
    print("[5] Applying roots...")
    entries, stats = apply_roots(entries, form_root, manual, check_only=check_only)
    vocab["entries"] = entries

    # 6. Report
    print(f"\n{'=' * 50}")
    print(f"Total entries:      {stats['total']}")
    print(f"QAC direct match:   {stats['matched_qac']}")
    print(f"Manual overrides:   {stats['matched_manual']}")
    print(f"Already correct:    {stats['already_ok']}")
    print(f"Roots fixed:        {stats['fixed']}")
    print(f"Particles (key==root): {stats['particle_kept']}")
    print(f"Unmatched:          {stats['unmatched']}")
    print(f"Coverage:           {stats['coverage']}%")
    print(f"{'=' * 50}")

    if stats["fixed"] > 0:
        # Show sample of fixes
        print(f"\nSample fixes (first 20):")
        count = 0
        for entry in entries:
            key = entry["key"]
            qac_root = lookup_root(key, form_root) or ""
            manual_root = manual.get(key, "")
            new_root = entry.get("root", "")
            old_root = ""  # We don't track old, so show QAC vs current
            if manual_root and manual_root == new_root and count < 20:
                print(f"  {key:12s} → {new_root} (manual override)")
                count += 1

    if check_only:
        print("\n[CHECK-ONLY] No changes written.")
        return 0

    # 7. Write updated vocab.json
    print(f"\n[7] Writing updated vocab.json...")
    VOCAB_PATH.write_text(
        json.dumps(vocab, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )
    print(f"    Written to {VOCAB_PATH}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
