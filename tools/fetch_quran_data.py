#!/usr/bin/env python3
"""Unduh isi Al-Qur'an (ayat Arab + terjemahan ID & EN) untuk di-bundle offline
ke dalam APK. Setelah dijalankan, aplikasi siap dipakai tanpa internet untuk
membaca mushaf/terjemahan/tajwid; hanya AUDIO yang tetap perlu diunduh.

Output (masuk ke APK lewat assets):
  app/src/main/assets/quran/data/surah-<n>.json     → respons equran.id yang
                                                      sudah dibersihkan
                                                      (Arab + terjemahan Indonesia)
  app/src/main/assets/quran/data/trans-en-<n>.json  → terjemahan EN (quran.com,
                                                     resource 20, HTML dibersihkan)

Cara pakai (Termux):
  python3 tools/fetch_quran_data.py            # unduh yang belum ada (idempoten)
  python3 tools/fetch_quran_data.py --force    # unduh ulang SEMUA

Sumber:
  - https://equran.id/api/v2/surat/{nomor}
  - https://api.quran.com/api/v4/quran/translations/20?chapter_number={nomor}
"""

import concurrent.futures
import json
import os
import re
import sys
import urllib.request

from quran_text_cleaner import clean_payload

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSET_DIR = os.path.join(BASE, "app", "src", "main", "assets", "quran", "data")
os.makedirs(ASSET_DIR, exist_ok=True)

EQURAN_URL = "https://equran.id/api/v2/surat/{n}"
QURANCOM_URL = "https://api.quran.com/api/v4/quran/translations/20?chapter_number={n}"
TOTAL_SURAH = 114
WORKERS = 8

SUP_RE = re.compile(r"(?is)<sup[^>]*>.*?</sup>")
TAG_RE = re.compile(r"(?is)<[^>]*>")
WS_RE = re.compile(r"\s+")


def http_get(url: str, tries: int = 3) -> str:
    last = None
    for attempt in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "ayah-of-the-day-builder/1.0"})
            with urllib.request.urlopen(req, timeout=30) as resp:
                return resp.read().decode("utf-8")
        except Exception as exc:  # noqa: BLE001 — coba ulang, jaringan tidak stabil
            last = exc
            if attempt == tries - 1:
                raise
    raise RuntimeError(f"gagal: {last}")


def strip_html(s: str) -> str:
    """Sama persis dengan stripHtml() di QuranRepository.kt:
    buang footnote <sup ...>N</sup> + tag lain, rapikan spasi."""
    return WS_RE.sub(" ", TAG_RE.sub("", SUP_RE.sub("", s))).strip()


def fetch_equran(n: int) -> None:
    path = os.path.join(ASSET_DIR, f"surah-{n}.json")
    if os.path.exists(path) and not FORCE:
        return
    raw = http_get(EQURAN_URL.format(n=n))
    payload = clean_payload(json.loads(raw))
    with open(path, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, separators=(",", ":"))
    print(f"  [equran.id] surah {n:3d} → surah-{n}.json ({os.path.getsize(path)} byte)")


def fetch_en(n: int) -> None:
    path = os.path.join(ASSET_DIR, f"trans-en-{n}.json")
    if os.path.exists(path) and not FORCE:
        return
    raw = http_get(QURANCOM_URL.format(n=n))
    data = json.loads(raw)
    items = data.get("translations", [])
    # Urutkan berdasarkan nomor ayat di verse_key ("1:2" → 2) agar aman.
    items.sort(key=lambda it: int(str(it.get("verse_key", "0:0")).split(":")[-1]))
    cleaned = [
        {"resource_id": int(it.get("resource_id", 20)), "text": strip_html(it.get("text", ""))}
        for it in items
    ]
    with open(path, "w", encoding="utf-8") as f:
        json.dump({"translations": cleaned}, f, ensure_ascii=False)
    print(f"  [quran.com] surah {n:3d} → trans-en-{n}.json ({len(cleaned)} ayat)")


def main() -> None:
    global FORCE
    FORCE = "--force" in sys.argv
    print(f"Folder tujuan: {ASSET_DIR}")
    print("Mengunduh 114 surah (Arab+ID dari equran.id, EN dari quran.com)…")
    with concurrent.futures.ThreadPoolExecutor(max_workers=WORKERS) as pool:
        futs = []
        for n in range(1, TOTAL_SURAH + 1):
            futs.append(pool.submit(fetch_equran, n))
            futs.append(pool.submit(fetch_en, n))
        failed = 0
        for fut in concurrent.futures.as_completed(futs):
            try:
                fut.result()
            except Exception as exc:  # noqa: BLE001
                failed += 1
                print(f"  ✗ gagal: {exc}", file=sys.stderr)
    total = 0
    for f in os.listdir(ASSET_DIR):
        total += os.path.getsize(os.path.join(ASSET_DIR, f))
    print(f"Selesai. {failed} berkas gagal. Total {total / 1_000_000:.1f} MB di assets/quran/data/")
    if failed:
        sys.exit(1)


if __name__ == "__main__":
    main()
