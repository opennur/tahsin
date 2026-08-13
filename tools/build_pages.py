#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Bangun `assets/quran/pages.json` — paginasi mushaf Madani (604 halaman) +
batas 30 juz, dari metadata alquran.cloud (api.alquran.cloud/v1/quran).

Data yang diambil HANYA metadata (nomor surah, nomor ayat, page, juz) —
TIDAK mengganti teks Arab: teks tetap dari bundel equran.id
(`assets/quran/data/surah-<n>.json`). Paginasi Madani adalah fakta tetap
(604 halaman, isi ayat per halaman sama di semua mushaf Madani cetakan),
jadi file hasil layak di-commit.

Pakai:
    python3 tools/build_pages.py                 # fetch dari API → tulis pages.json
    python3 tools/build_pages.py --source FILE   # pakai hasil fetch tersimpan
    python3 tools/build_pages.py --check         # validasi saja, tanpa menulis

Validasi (gagal = exit 1): 114 surah, 6236 ayat, halaman 1..604 kontigu,
nomor halaman monoton terhadap urutan ayat, tiap ayat tepat satu kali,
30 juz dengan juz 1 dimulai 1:1.
"""

import json
import sys
import urllib.request
from pathlib import Path

API_QURAN = "https://api.alquran.cloud/v1/quran"
SCHEMA_VERSION = 1
DEFAULT_OUT = Path("app/src/main/assets/quran/pages.json")

ERRORS: list[str] = []


def err(msg: str) -> None:
    ERRORS.append(msg)
    print(f"  ✗ {msg}")


def fetch(url: str) -> dict:
    with urllib.request.urlopen(url, timeout=120) as resp:
        return json.load(resp)


def build(source: dict) -> dict:
    data = source.get("data")
    if not isinstance(data, dict) or not isinstance(data.get("surahs"), list):
        err("Respons API tidak valid (data.surahs hilang)")
        return {}

    # Kumpulkan (surah, ayat, page, juz) berurutan.
    rows: list[tuple[int, int, int, int]] = []
    for surah in data["surahs"]:
        snum = int(surah.get("number", 0))
        for a in surah.get("ayahs", []):
            rows.append((snum, int(a["numberInSurah"]), int(a["page"]), int(a["juz"])))

    if len(rows) != 6236:
        err(f"Jumlah ayat {len(rows)} (harus 6236)")
    if len({s for s, _, _, _ in rows}) != 114:
        err("Surah tidak 114")

    # Paginasi: grup per halaman, gabung rentang surah yang bersambung.
    pages: list[dict] = []
    by_page: dict[int, list[tuple[int, int]]] = {}
    for snum, anum, page, _ in rows:
        by_page.setdefault(page, []).append((snum, anum))

    prev_page = 0
    for page in sorted(by_page):
        if page != prev_page + 1:
            err(f"Halaman tidak kontigu: {prev_page} → {page}")
        prev_page = page
        segments: list[dict] = []
        for snum, anum in by_page[page]:
            if segments and segments[-1]["surah"] == snum and segments[-1]["to"] + 1 == anum:
                segments[-1]["to"] = anum
            else:
                segments.append({"surah": snum, "from": anum, "to": anum})
        pages.append({"page": page, "segments": segments})

    if prev_page != 604:
        err(f"Halaman terakhir {prev_page} (harus 604)")

    # Monoton: page tidak boleh turun seiring urutan ayat.
    last = 0
    for snum, anum, page, _ in rows:
        if page < last:
            err(f"Page turun di surah {snum} ayat {anum}: {last} → {page}")
            break
        last = page

    # Batas 30 juz: ayat pertama tiap juz.
    juz_starts: dict[int, tuple[int, int]] = {}
    for snum, anum, page, juz in rows:
        juz_starts.setdefault(juz, (snum, anum))
    if list(juz_starts) != list(range(1, 31)):
        err(f"Juz tidak 1..30: {sorted(juz_starts)}")
    if juz_starts.get(1) != (1, 1):
        err(f"Juz 1 harus mulai 1:1, dapat {juz_starts.get(1)}")

    return {
        "schemaVersion": SCHEMA_VERSION,
        "generatedBy": "tools/build_pages.py",
        "pageCount": len(pages),
        "pages": pages,
        "juzStarts": [
            {"juz": j, "surah": sn, "ayah": an} for j, (sn, an) in sorted(juz_starts.items())
        ],
    }


def emit(catalog: dict, out: Path) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    with open(out, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=1)
        f.write("\n")


def main() -> int:
    args = [a for a in sys.argv[1:]]
    check_only = "--check" in args
    source_path = None
    if "--source" in args:
        idx = args.index("--source")
        if idx + 1 < len(args):
            source_path = Path(args[idx + 1])

    if source_path is not None:
        source = json.loads(source_path.read_text(encoding="utf-8"))
    else:
        print(f"Mengunduh {API_QURAN} …")
        source = fetch(API_QURAN)

    catalog = build(source)
    if ERRORS:
        print(f"VALIDASI GAGAL — {len(ERRORS)} masalah:")
        for e in ERRORS:
            print(f"  ✗ {e}")
        return 1

    if not check_only:
        emit(catalog, DEFAULT_OUT)
        print(f"OK — {catalog['pageCount']} halaman, {len(catalog['juzStarts'])} juz → {DEFAULT_OUT}")
    else:
        print(f"OK (check saja) — {catalog['pageCount']} halaman")
    return 0


if __name__ == "__main__":
    sys.exit(main())
