# Schema `assets/lughoh/lessons.json` — Fitur "📚 Belajar Arab"

Dokumen ini mendefinisikan skema JSON untuk data pelajaran Bahasa Arab fitur
"Belajar Arab" (metodologi ala Durusul Lughoh). Data dihasilkan oleh
`tools/build_lughoh.py` (konten sumber di `tools/lughoh_content_*.py`).

## Prinsip orisinalitas (penting)

**Tidak ada materi berhak cipta.** Durusul Lughoh dipakai HANYA sebagai inspirasi
struktur pedagogis (Muhadatsah → Mufrodat → Qawa'id → Tadribat). Seluruh dialog,
kalimat, contoh, dan latihan ditulis orisinal dari nol — bukan salinan dari kitab
Durusul Lughoh, buku pelajaran lain, maupun sumber daring mana pun. Validator di
`build_lughoh.py` tidak bisa memeriksa orisinalitas (tidak ada basis data hak
cipta), jadi ini adalah kewajiban penulis konten; jangan menyalin kalimat dari
sumber mana pun.

## Konvensi penulisan konten

- Bahasa pengantar konten: **Indonesia** (per spesifikasi: terjemahan ID).
  Krom UI (judul seksi, tombol, label) tetap bilingual ID/EN lewat `AppStrings`.
- **Terjemahan Inggris**: setiap teks Indonesia WAJIB punya terjemahan EN.
  Terjemahan ditulis di `tools/lughoh_en.py` (kunci = teks Indonesia), lalu
  `build_lughoh.py` menyisipkannya sebagai field `*En` saat build. Kalau ada
  satu teks tanpa terjemahan, build GAGAL (integritas konten — materi
  keagamaan tidak boleh salah/nyasar).
- Arab ditulis dengan **harakat penuh** (fathah/dammah/kasrah/tanwin/sukun)
  agar ramah pemula. Alif di awal kata memakai hamza duduk/berdiri sesuai kaidah.
- Transliterasi Latin konsisten:
  - vokal panjang `ā ī ū`; konsonan tebal `ṣ ḍ ṭ ẓ`; `th j ḥ kh dh sh ʿ gh`;
    hamza non-awal `'`; hamza awal kata tidak ditulis (`ana`, `anta`, `ism`).
- Nama pembicara dalam dialog memakai label Indonesia (mis. `Ahmad`, `Fatimah`).

## Struktur JSON

```jsonc
{
  "schemaVersion": 1,
  "generatedBy": "tools/build_lughoh.py",
  "generatedAt": "2026-01-01T00:00:00+00:00",   // ISO8601
  "levels": [
    {
      "id": 1,                                   // int, 1-based, unik
      "titleId": "Level 1 — Perkenalan & Kehidupan Sehari-hari",
      "titleEn": "Level 1 — Introductions & Daily Life",  // WAJIB
      "titleAr": "المُسْتَوَى الأَوَّل",
      "lessons": [ ... ]
    }
  ]
}
```

### Lesson

```jsonc
{
  "id": "1-1",                                   // "<levelId>-<lessonNo>", unik
  "titleId": "Perkenalan Diri",
  "titleEn": "Self-Introduction",               // WAJIB
  "titleAr": "التَّعْرِيفُ بِالنَّفْسِ",

  "muhadatsah": [                                // 8–10 baris dialog
    {
      "speaker": "Ahmad",                        // label ID
      "ar": "مَا اسْمُكَ؟",                       // Arab berharakat
      "latin": "Mā ismuka?",                     // transliterasi
      "id": "Siapa namamu?",                      // terjemahan Indonesia
      "en": "What is your name?"                 // WAJIB
    }
  ],

  "mufrodat": [                                  // 8–10 kata dari percakapan
    {
      "ar": "اِسْم",
      "latin": "ism",
      "id": "nama",
      "en": "name",                              // WAJIB
      "exampleAr": "مَا اسْمُكَ؟",
      "exampleLatin": "Mā ismuka?",
      "exampleId": "Siapa namamu?",
      "exampleEn": "What is your name?"          // WAJIB
    }
  ],

  "qawaid": [                                    // 2–3 kaidah tata bahasa
    {
      "titleId": "Kata tanya مَا",
      "titleEn": "The question word مَا",        // WAJIB (boleh memuat istilah Arab)
      "id": "Kata tanya مَا dipakai untuk bertanya tentang nama atau benda. Contoh dari dialog:",
      "en": "مَا is used to ask about a name or a thing. Example from the dialogue:",  // WAJIB
      "exampleAr": "مَا اسْمُكَ؟",                // WAJIB muncul di dialog
      "exampleLatin": "Mā ismuka?",
      "exampleId": "Siapa namamu?",
      "exampleEn": "What is your name?"          // WAJIB
    }
  ],

  "tadribat": [                                  // ≥1 per jenis, total ~8
    { "type": "fillBlank",        ... },
    { "type": "translateArId",    ... },
    { "type": "translateIdAr",    ... },
    { "type": "rearrange",        ... }
  ]
}
```

### Tadribat (4 jenis, semuanya tap-based)

**fillBlank** — isi titik-titik (pilih kata):

```jsonc
{
  "type": "fillBlank",
  "promptId": "Isilah titik-titik: Siapa ___?",
  "promptEn": "Fill in the blank: What ___?",   // WAJIB
  "promptAr": "مَا ____؟",
  "promptLatin": "Mā ____?",
  "options": ["اسْمُكَ", "بَيْتُكَ", "كِتَابُكَ", "قَلَمُكَ"],
  "answer": "اسْمُكَ"                            // harus ∈ options
}
```

**translateArId** — terjemahkan Arab → Inggris/Indonesia (pilih terjemahan):

```jsonc
{
  "type": "translateArId",
  "promptAr": "مَا اسْمُكَ؟",
  "promptLatin": "Mā ismuka?",
  "options": ["Siapa namamu?", "Apa kabarmu?", "Dari mana kamu?", "Di mana rumahmu?"],
  "answer": "Siapa namamu?",
  "optionsEn": ["What is your name?", "How are you?", "Where are you from?", "Where is your house?"],  // WAJIB
  "answerEn": "What is your name?"              // WAJIB, harus ∈ optionsEn
}
```

**translateIdAr** — terjemahkan Inggris/Indonesia → Arab (pilih kalimat Arab):

```jsonc
{
  "type": "translateIdAr",
  "promptId": "Senang berkenalan.",
  "promptEn": "Nice to meet you.",              // WAJIB
  "options": ["تَشَرَّفْنَا", "أَهْلًا وَسَهْلًا", "مَعَ السَّلَامَةِ", "صَبَاحَ الْخَيْرِ"],
  "answer": "تَشَرَّفْنَا"
}
```

**rearrange** — susun kata menjadi kalimat benar (ketuk kata berurutan):

```jsonc
{
  "type": "rearrange",
  "words": [                                    // URUTAN BENAR
    {"ar": "أَنَا",   "latin": "anā"},
    {"ar": "مِنْ",    "latin": "min"},
    {"ar": "إِنْدُونِيسِيَّا", "latin": "Indūnīsiyyā"},
    {"ar": "أَيْضًا", "latin": "ayḍan"}
  ],
  "answer": ["أَنَا", "مِنْ", "إِنْدُونِيسِيَّا", "أَيْضًا"]   // = ar(words) berurutan
}
```

## Aturan validasi (`build_lughoh.py`)

1. Struktur: semua field wajib non-kosong; tipe sesuai skema.
2. `id` level unik; `id` lesson unik & berpola `<level>-<no>`.
3. Field `ar` hanya boleh berisi huruf Arab + harakat + tanda baca Arab
   (، ؟ .) — tidak boleh huruf Latin. Field `latin` tidak boleh huruf Arab.
4. Transliterasi `latin` hanya karakter ASCII + `ā ī ū ṣ ḍ ṭ ẓ ḥ ʿ '`.
5. Setiap `mufrodat.ar` (ternormalisasi, harakat dibuang) muncul sebagai
   substring dalam teks dialog pelajaran tersebut (ter-normalisasi) — syarat
   "kata baru dari percakapan".
6. Setiap `qawaid.exampleAr` (ter-normalisasi) muncul sebagai substring salah
   satu baris dialog — syarat "contoh dari dialog".
7. `fillBlank`: promptAr berisi `____` tepat 1×; options ≥ 3 & unik; answer ∈ options.
8. `translateArId` / `translateIdAr`: options ≥ 3 & unik; answer ∈ options.
9. `rearrange`: `answer` == urutan `ar` dari `words`; kata tidak duplikat.
10. Tiap lesson punya ≥ 1 tadribat untuk keempat jenis (total ≥ 4, target 8).
11. **Terjemahan EN (kelengkapan wajib)**: SEMUA `titleId`, `id`, `exampleId`
    (level/lesson/muhadatsah/mufrodat/qawaid), `promptId` (fillBlank,
    translateIdAr), serta `options`/`answer` (translateArId) WAJIB punya
    pasangan EN (`titleEn`, `en`, `exampleEn`, `promptEn`, `optionsEn`,
    `answerEn`) — diambil dari `tools/lughoh_en.py`, kalau ada yang hilang
    build gagal. Field EN untuk teks Indonesia murni tidak boleh memuat huruf
    Arab; `answerEn` harus ∈ `optionsEn`. (Judul/penjelasan qawaid boleh
    memuat istilah Arab, mis. `Kata tanya مَا`.)

Normalisasi Arab = buang harakat `\u064B-\u0652`, `\u0670` + seragamkan
hamza/alif (sama seperti `ArabicNormalizer` di aplikasi).
