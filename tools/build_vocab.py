#!/usr/bin/env python3
"""
Bangun skeleton `assets/quran/vocab.json` dari mushaf bundle (offline).

Cara kerja:
- Scan 114 `assets/quran/data/surah-<n>.json` (format equran.id).
- Tokenisasi tiap ayat persis seperti `ArabicNormalizer.splitWords` (Kotlin):
  pecah spasi, buang token tanpa huruf Arab, dan normalisasi tiap kata
  (strip harakat + samakan varian huruf) sebagai kunci lookup.
- Hitung frekuensi per kunci, catat contoh kemunculan PERTAMA
  (surah, ayat, indeks kata 1-based, kata berharakat, teks ayat Arab,
  transliterasi ayat, terjemahan ayat ID & EN).
- Keluarkan daftar peringkat frekuensi menurun.

Arti (translit / meaningId / meaningEn) DIKURASI MANUAL setelah skeleton
dibuat — skrip ini menaruh string kosong sebagai placeholder.
Hanya TOP_N kata terfrequent yang dikeluarkan (default 400): cukup untuk
kurasi + ruang tumbuh, dan membuat file aset tetap kecil (contoh ayat
disematkan per entri, jadi jangan keluarkan ribuan entri yang tidak
terpakai).
Setelah kurasi, jalankan ulang untuk menyegarkan statistik bila perlu
(skrip tidak menimpa arti yang sudah terisi).

Pakai:  python3 tools/build_vocab.py
"""

import json
import os
import re

# Jumlah entri terbanyak yang dikeluarkan ke vocab.json (urutan frekuensi).
TOP_N = 1200

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
QURAN_DIR = os.path.join(ROOT, "app", "src", "main", "assets", "quran", "data")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "quran", "vocab.json")

# Setara LETTERS di ArabicNormalizer (huruf dasar Al-Qur'an, tanpa ە).
LETTERS = set("ابتثجحخدذرزسشصضطظعغفقكلمنهويئةءأآإى")
# Setara MARKS di ArabicNormalizer (harakat & tanda mushaf), diperluas
# ke rentang \u064B-\u0656 (fatha..subscript alef, termasuk maddah 0653 &
# hamza atas/bawah 0654/0655) biar varian "ما" vs "مآ" jadi satu kunci.
MARKS = re.compile(r"[\u064B-\u0656\u0670\u06D6-\u06ED\u08D6-\u08ED]")


def normalize(word: str) -> str:
    s = MARKS.sub("", word)
    s = s.replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ٱ", "ا")
    s = s.replace("ى", "ي")
    s = s.replace("ة", "ه")
    s = s.replace("ء", "")
    s = s.replace("ـ", "")
    return s.strip()


def split_words(text: str):
    out = []
    for tok in text.split():
        tok = tok.strip()
        if tok and any(ch in LETTERS for ch in tok):
            out.append(tok)
    return out


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def main():
    counts = {}          # key -> jumlah kemunculan
    examples = {}        # key -> contoh pertama (dict)
    seen = set()         # key yang sudah punya contoh

    for n in range(1, 115):
        surah_path = os.path.join(QURAN_DIR, f"surah-{n}.json")
        if not os.path.exists(surah_path):
            continue
        surah = load_json(surah_path).get("data", {})
        ayahs = surah.get("ayat", [])
        if not ayahs:
            continue
        # Terjemahan EN (quran.com, urut per ayat) — opsional.
        en_path = os.path.join(QURAN_DIR, f"trans-en-{n}.json")
        en_texts = []
        if os.path.exists(en_path):
            en_texts = [t.get("text", "") for t in load_json(en_path).get("translations", [])]
        for idx, ayah in enumerate(ayahs):
            arab = ayah.get("teksArab", "")
            tokens = split_words(arab)
            for wi, token in enumerate(tokens, start=1):
                key = normalize(token)
                if not key:
                    continue
                counts[key] = counts.get(key, 0) + 1
                if key in seen:
                    continue
                seen.add(key)
                examples[key] = {
                    "surah": n,
                    "ayah": ayah.get("nomorAyat", idx + 1),
                    "word": wi,
                    "arab": token,
                    "ayahArab": arab,
                    "ayahLatin": ayah.get("teksLatin", ""),
                    "ayahId": ayah.get("teksIndonesia", ""),
                    "ayahEn": en_texts[idx] if idx < len(en_texts) else "",
                }

    ranked = sorted(counts.items(), key=lambda kv: (-kv[1], kv[0]))[:TOP_N]
    entries = [
        {
            "key": key,
            "word": examples[key]["arab"],
            "translit": "",
            "meaningId": "",
            "meaningEn": "",
            "freq": freq,
            "example": {k: v for k, v in examples[key].items() if k != "arab"},
        }
        for key, freq in ranked
    ]

    with open(OUT, "w", encoding="utf-8") as f:
        json.dump({"entries": entries}, f, ensure_ascii=False, indent=1)
        f.write("\n")

    total = sum(counts.values())
    print(f"Token: {total:,}  |  Kata unik: {len(entries):,}")
    print(f"Tulis: {OUT}")
    print("\nTop 25 (freq menurun):")
    for e in entries[:25]:
        print(f"  {e['freq']:>5}  {e['key']}")


if __name__ == "__main__":
    main()
