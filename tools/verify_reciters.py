#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Verifikasi URL audio qari' (perawi) terhadap listing resmi everyayah.com.

Cek:
  1. Folder slug qari' ada di `https://everyayah.com/data/<slug>/` (listing 200).
  2. Sejumlah contoh file ayat ada (GET, cek status 200 + punya konten).
  3. Pola penamaan `{surah:03d}{ayah:03d}.mp3` benar untuk beberapa titik
     (halaman 1 = 001001, Al-Baqarah 255 = 002255, juz 30 = 114006).

Pakai:  python3 tools/verify_reciters.py
Butuh internet. Keluar 0 kalau semua qari' lolos, 1 kalau ada yang gagal.
"""

import sys
import urllib.request

SLUGS = [
    "Minshawy_Murattal_128kbps",
    "Husary_128kbps",
    "Husary_Muallim_128kbps",
    "Abdul_Basit_Murattal_192kbps",
    "Alafasy_128kbps",
    "Abdurrahmaan_As-Sudais_192kbps",
    "Hudhaify_128kbps",
]

# Contoh file per qari': (surah, ayah) — titik awal, tengah, akhir mushaf.
SAMPLES = [(1, 1), (2, 255), (36, 12), (114, 6)]


def http_ok(url: str) -> bool:
    try:
        req = urllib.request.Request(url, method="GET", headers={"User-Agent": "verify-reciters/1.0"})
        with urllib.request.urlopen(req, timeout=30) as resp:
            body = resp.read(64)
            return resp.status == 200 and len(body) > 0
    except Exception:
        return False


def main() -> int:
    failed = []
    for slug in SLUGS:
        base = f"https://everyayah.com/data/{slug}/"
        ok = http_ok(base)
        print(f"{'OK ' if ok else 'GAGAL'} folder: {base}")
        if not ok:
            failed.append(slug)
            continue
        for surah, ayah in SAMPLES:
            f = f"{surah:03d}{ayah:03d}.mp3"
            if not http_ok(base + f):
                print(f"  GAGAL file: {base}{f}")
                failed.append(f"{slug}/{f}")
            else:
                print(f"  OK  file: {f}")
    if failed:
        print(f"\nGagal: {len(failed)} entri -> {failed}")
        return 1
    print("\nSemua qari' lolos verifikasi.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
