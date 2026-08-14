# Tahsin Quran

> 🌐 **English version:** [README.en.md](README.en.md)

Aplikasi Android untuk **muroja'ah & latihan baca Al-Qur'an**: mushaf halaman
ala mushaf Madani (teks ayat mengalir menyambung dari kanan ke kiri), penilaian
bacaan real-time lewat mikrofon, pewarnaan huruf tajwid, audio qari per ayat +
per kata, dan **mode pemutaran audio** (satu ayat / lanjut terus / ulang terus)
— plus **jalur belajar Qur'an & Bahasa Arab**: Kosakata, Kuis Tajwid, game
**Dream BIG** (arcade), kursus **Belajar Arab** (metodologi ala Durusul
Lughoh), **Kuis Ayat**, dan **Penghargaan** (XP + badge).

> ⚠️ **Batasan jujur** — aplikasi ini adalah alat bantu latihan, **bukan pengganti guru**.
> Tampilan mushaf halaman memakai paginasi Madani yang PERSIS (isi ayat per halaman
> sama dengan mushaf cetak), tapi teksnya disusun mengalir rata kanan-kiri (belum
> ada data layout 15-baris per halaman yang pixel-exact).
> STT (speech-to-text) hanya membaca *teks* ucapan: aplikasi bisa menilai kata
> terlewat/salah susun/salah huruf, tapi **tidak bisa menilai makhraj atau
> panjang-pendek harakat**. Deteksi tajwid bersifat rule-based ("peta hukum"
> dari teks ber-tashkeel), bukan analisis audio.

## Fitur

- 🧭 **Menu utama** (layar beranda): semua fitur dibuka lewat kartu menu —
  **Tahsin**, **Kosakata**, **Kuis Tajwid**, **Statistik**, **Dream BIG**,
  **Belajar Arab**, **Kuis Ayat**, **Penghargaan**, dan **Pengaturan**.
  (Pencarian ayat & Kelola Audio dipindah: 🔍 di header Tahsin, 🎵 di
  Pengaturan.)
- 📖 **Mushaf halaman ala mushaf Madani asli** — navigasi per HALAMAN (604 halaman,
  alur RTL seperti membuka mushaf cetak), **teks ayat mengalir menyambung dari
  kanan ke kiri** (ayat pendek di juz 30 tidak bertumpuk satu per baris),
  penomoran akhir ayat (lingkaran + angka Arab-Indik) **digambar menempel di
  ujung tiap ayat**, tanda **sujud tilawah ۩** di 15 tempat sujud, basmalah di
  awal surah, pembatas + nama surah saat surah baru mulai di tengah halaman,
  band header (nama surah + juz), **offline bawaan** (Arab + terjemahan ID/EN
  di-bundle ke APK). Terjemahan **tersembunyi secara default** (toggle di
  Pengaturan). Navigasi **3 dropdown**: [Surah] [Ayat] [Halaman] — label pendek
  supaya tidak terpotong "…"; kontrol ukuran huruf **slider presisi (100–250%)**.
- 🎙️ **Penilaian real-time**: kata berubah hijau (benar) / merah (salah) / kuning
  (sedang dibaca) saat kamu membaca ke mikrofon (SpeechRecognizer `ar-SA`).
- 📊 **Statistik gabungan semua challenge** (persisten): layar **Statistik**
  mengagregasi seluruh aktivitas — Tahsin (skor per ayat 0–100 & jumlah
  percobaan), **Dream BIG** (ronde & skor terbaik), **Belajar Arab** (sesi &
  skor terbaik), dan **Kosakata** (kata dikuasai). Ringkasan: **Total Sesi,
  Skor Terbaik %, Total Ronde, Kata Dikuasai** + rincian per fitur. Di layar
  utama Tahsin tetap ada info cepat "N× dicoba · skor terbaik M%".
- 🎨 **Warna tajwid** (nyala default, bisa dimatikan di menu Pengaturan): mad (merah),
  ghunnah (hijau), qalqalah (biru), ikhfa' (abu-abu), iqlab (ungu), idgham (oranye),
  lam jalalah (teal).
- ▶️ **Mode pemutaran audio** (pengganti mode flow): dropdown di samping tombol
  "Dengar" memilih **١ — ayat ini saja** (putar sekali), **→ — lanjut otomatis**
  ke ayat berikutnya seperti membaca terus (lintas halaman), atau **↻ — ulang
  terus** ayat ini. Tombol Stop membatalkan rantai kapan saja. Umpan bacaan STT
  tetap: kata berubah hijau (benar) / merah (salah) / kuning (sedang dibaca),
  beep sukses saat sempurna, bunyi + getar saat ada kesalahan.
- 🧠 **Mesin tajwid lengkap + kuis**: selain mad wajib/jaiz & lam jalalah yang
  sudah ada, engine kini mendeteksi **tafkhim/tarqiq** (huruf isti'la & ra'),
  **mad badal/iwad/aridh lis-sukun**, dan **tanda waqaf mushaf** (مـ wajib,
  لا jangan berhenti, ج boleh, صلي/قلي lebih utama, ∴ berpasangan) — muncul di
  panel kata & daftar kesalahan. **📝 Kuis Tajwid** (menu Kuis) menebak "hukum
  apa pada kata ini?" dari kata acak di seluruh mushaf (4 pilihan ganda, skor,
  penjelasan) — belajar, bukan cuma pewarnaan.
- 📖 **Kosakata Qur'an** (menu Kosakata): **589 kata terkurasi** dari seluruh
  mushaf (mirror VocabKey) — kartu kata dengan arti + contoh ayat, sistem
  SRS (kata baru vs. lagi diulang), mode **quiz** pilihan ganda, dan lompat
  langsung ke ayat contoh.
- 🎬 **Dream BIG** (menu Dream BIG, **arcade**): ronde kuis kosakata **tak
  terbatas** — 10 soal diacak dari seluruh kosakata terkurasi setiap ronde;
  rekor **skor, streak, dan jumlah ronde** tersimpan. Tanpa level/unlock,
  bisa terus dimainkan.
- 📚 **Belajar Arab** (menu Belajar Arab): kursus Bahasa Arab untuk pemula ala
  metodologi Durusul Lughoh — **15 pelajaran orisinal** (3 level: perkenalan &
  kehidupan sehari-hari, aktivitas, kehidupan sosial) berisi dialog, kosakata,
  tata bahasa. Latihan berupa **sesi acak tak terbatas** (8 soal dari seluruh
  pelajaran, urutan opsi diacak) dengan rekor skor; materi tetap bisa dibaca
  lewat browser level/pelajaran. Konten 100% orisinal (tanpa salinan kitab).
- 🎮 **XP, Level & Streak**: setiap aktivitas belajar memberi XP — bacaan
  Tahsin (skor ≥70: 5 XP, ≥90: 10 XP), jawaban benar kuis (2 XP), kata
  kosakata baru dikuasai (10 XP), ronde Dream BIG (15 XP), sesi Belajar Arab
  (10 XP). Level naik dengan kurva kuadratik (`√(XP/100)`), **streak hari
  beruntun** dihitung per hari kalender, dan **target harian 50 XP** tampil
  dengan progress bar di beranda & Statistik. Naik level, capaian streak
  (3/7/14/30 hari), atau badge baru dirayakan lewat dialog + getar.
- 🏅 **Penghargaan (badges)**: **8 lencana progresif** — XP (pencari ilmu),
  streak, bacaan Tahsin, bacaan sempurna (90+), kosakata dikuasai, ronde
  Dream BIG, sesi Belajar Arab, dan surah ditamatkan. Setiap lencana punya
  **tier tak terbatas** (ambang naik terus: 50 kata, 100, 150, …) — begitu
  satu tier terbuka, masih ada tier berikutnya untuk dikejar. Layar daftar
  menampilkan tier saat ini + **progress bar menuju tier berikutnya**; badge
  terbaru (beserta tier-nya) tampil di beranda & Statistik.
- 🎯 **Kuis Ayat** (menu Kuis Ayat): dua mode pilihan ganda dari seluruh
  mushaf — **Lengkapi Ayat** (kata mana yang melengkapi ayat ini?) dan
  **Tebak Surah** (ayat ini dari surah apa?) — pengecoh dari kata dalam
  surah yang sama / nama surah lain; skor + XP per jawaban benar.
- 🔥 **Pengingat streak** (opsional, menu Pengaturan): notifikasi harian jam
  18:00 bila target harian belum tercapai — streak tidak putus diam-diam.
- 👆 **Gesture**: geser halaman mushaf ke kanan/kiri (RTL: kanan = halaman
  berikutnya, seperti membalik mushaf cetak); halaman sebelum & sesudah
  **di-pra-muat di background** supaya perpindahan mulus tanpa kilat
  "memuat surah".
- 🧭 **Navigasi satu baris**: `[Surah ▾] [Ayat ▾] [Halaman ▾]` — dropdown
  [Ayat] memilih ayat di dalam surah aktif (mushaf ikut pindah ke halaman
  ayat tersebut); label pendek (nama surah, nomor Arab-Indik dalam kurung)
  supaya tidak terpotong "…".
- 🔍 **Pencarian ayat** (tombol 🔍 di header): cari **kata Arab** (harakat &
  varian hamza/ya/ta marbuta dinormalisasi otomatis) atau **kata kunci
  terjemahan ID/EN** di seluruh 114 surah — offline dari bundle. Ketuk hasil
  → langsung buka ayat itu; ketikan di-debounce, indeks dibangun sekali.
- 🔍 **Panel kata**: ketuk kata di mushaf → hukum tajwid + penjelasan + putar
  audio kata; tombolnya berubah jadi **⏹ Stop** saat kata sedang diputar
  (terpisah dari tombol Dengar ayat — bebas race).
- 🔊 **Audio contoh**: **pilih qari'** (menu Pengaturan): Minshawy, Husary, Husary
  Muallim, Abdul Basit, Alafasy, As-Sudais, Hudhaify (everyayah.com — audio
  ayat tersimpan per qari' di `filesDir/audio/<qari'>/`) + audio per kata
  (qurancdn wbw); **kecepatan pemutaran 0.5×–1.25×** untuk latihan pelan-pelan
  (berlaku langsung saat sedang memutar). Diunduh in-app per surah / **semua
  surah** (dari Pengaturan atau tombol "Unduh Semua — Qari'" di Kelola Audio
  saat belum ada audio), dengan **progress bar di footer** + nama surah yang
  sedang diunduh; unduhan latar belakang (foreground service) setelah user
  mengizinkan.
- 📂 **Manajemen audio terunduh**: ukuran per surah, hapus per surah / hapus
  semua (dengan konfirmasi), **kartu progres live** saat ada unduhan berjalan,
  dan **cache daftar** — membuka layar lagi instan tanpa pemindaian ulang.
- 🌙 **Dark mode** & **ganti bahasa ID/EN** lewat menu **Pengaturan**; setiap
  layar punya tombol kembali (←) di kiri atas.
- 🗓️ **Widget + notifikasi "Ayah of the Day"** — satu ayat berganti setiap hari
  (deterministik per tanggal, offline dari bundel aset). Widget home screen
  ringkas: terjemahan saja; notifikasi menampilkan teks Arab + terjemahan.
  Ketuk widget/notifikasi → aplikasi terbuka tepat di ayat itu; update harian
  via AlarmManager (+ reschedule saat boot), bisa dimatikan di menu Pengaturan.
- 🔤 **Khat Utsmani (Amiri)** — font di-bundle ke APK, langsung tampil tanpa unduhan.

## Ayah of the Day (widget & notifikasi harian)

Satu ayat yang sama untuk semua pengguna sepanjang hari, berganti otomatis besok:

- **Pemilihan deterministik** (`util/AyahOfTheDayManager.kt`) — seed = hari epoch
  (jumlah hari sejak 1970-01-01) → `Random(seed)` memilih satu index dari total
  6.236 ayat → dipetakan ke (surah, ayat) lewat daftar kumulatif per surah.
  Stabil sepanjang hari dan **tanpa internet**.
- **Konten** dibaca dari bundel aset mushaf (fallback: unduh), lalu di-cache ke
  SharedPreferences dengan kunci **tanggal + bahasa** → update widget/notifikasi
  instan tanpa parse ulang.
- **Widget home screen**: ringkas — nama surah · nomor ayat + **terjemahan saja**
  (teks Arab tampil di notifikasi).
- **Notifikasi harian**: teks Arab + terjemahan (BigText), dipicu `AlarmManager`
  (tengah malam, `setAndAllowWhileIdle`); alarm di-reschedule tiap kali menyala,
  saat app dibuka (`MainActivity.onCreate`), dan saat perangkat restart
  (`BOOT_COMPLETED`).
- **Ketuk widget/notifikasi** → aplikasi terbuka tepat di surah/ayat itu
  (deep link via extra Intent; aman dipanggil berulang, rotation-safe).
- **Toggle** "🗓️ Notifikasi Harian" di menu Pengaturan (default nyala; pada Android 13+
  izin notifikasi diminta saat user menghidupkannya).

## Arsitektur & stack

- **Kotlin + Jetpack Compose** — **tanpa Material 3** (custom design system di `theme/`).
- compileSdk 35, targetSdk 35 (edge-to-edge wajib), minSdk 26 · AGP 8.4.0 · Kotlin 2.0.20 · Gradle 8.6 · Java 17.
- Tanpa Room/Hilt/Retrofit/Navigation — DI manual (`ViewModel` + factory), Gson untuk JSON.
- **Offline-first**: konten surah & terjemahan di-bundle ke APK (siap tanpa
  internet); audio diunduh in-app dan di-cache ke `filesDir/`; daftar audio
  manajemen ikut di-cache.

```
app/src/main/java/org/opennur/tahsin/
├── data/quran/     # model Surah/Ayah + QuranRepository (aset bundle → cache →
│                   #   equran.id) + MUSHAF: MushafPages (paginasi Madani 604
│                   #   halaman ← assets/quran/pages.json), MushafPage +
│                   #   MushafPageComposer (susun halaman), Basmalah,
│                   #   SajdahSigns (15 sujud tilawah), AyahNumbering
├── data/tajwid/    # TajwidEngine (rule-based) + TajwidColorizer (span warna)
│                   #   + TajwidQuiz (kuis "hukum apa pada kata ini?")
├── data/vocab/     # VocabularyEngine (SRS + quiz) + Repository/Parser
│                   #   (589 kata terkurasi → assets/quran/vocab.json)
├── data/dreambig/  # DreamBigGame (ronde arcade); Models/Parser/Repository
│                   #   lama (era level/transkrip) = dead code yang dipertahankan
├── data/lughoh/    # LughohModels/Parser/Repository/Engine (15 pelajaran orisinal
│                   #   → assets/lughoh/lessons.json; sesi latihan acak)
├── data/ayatquiz/  # AyatQuiz (Lengkapi Ayat) + SurahQuiz (Tebak Surah) —
│                   #   kuis pilihan ganda dari seluruh mushaf (murni, teruji)
├── stt/            # ArabicSpeechRecognizer + TranscriptAligner (Levenshtein)
├── ui/             # TahsinScreen/VM, AudioManagerScreen/VM, StatsScreen/VM
│                   #   (statistik gabungan semua challenge), SearchScreen/VM,
│                   #   TajwidQuizScreen/VM, VocabularyScreen/VM,
│                   #   DreamBigScreen/VM (arcade), LughohScreen/VM (arcade),
│                   #   AyatQuizScreen/VM (Kuis Ayat), BadgesScreen/VM
│                   #   (penghargaan), GamificationViewModel (header beranda),
│                   #   SettingsScreen (dark mode, bahasa, unduh semua)
├── widget/         # AyahOfTheDayWidget (AppWidgetProvider) + alarm harian/notifikasi
│                   #   + StreakReminderReceiver (pengingat streak, opsional)
├── util/           # AudioDownloader, AudioUrls, TahsinAudioPlayer (PlaySource),
│                   #   DownloadProgress, DownloadService, FontStore, SettingsStore,
│                   #   ReadingStatsStore (riwayat bacaan per ayat, JSON filesDir),
│                   #   VocabularyStatsStore, DreamBigProgressStore, LughohProgressStore,
│                   #   Achievements (8 badge progresif, tier tak terbatas),
│                   #   GamificationStore/Hub/Events (XP, level, streak, perayaan),
│                   #   AyahSearch (pencarian Arab ternormalisasi + terjemahan),
│                   #   Reciter (qari' everyayah + kecepatan audio 0.5×–1.25×),
│                   #   AyahOfTheDayManager (pemilihan ayat harian + cache)
└── theme/          # Colors, Typography, Shapes, ArabicFont (custom design system)
```

## Konten offline bawaan (bundle ke APK)

Mushaf, terjemahan Indonesia, terjemahan Inggris, dan font khat Utsmani
**di-bundle ke APK** sehingga aplikasi langsung siap dipakai tanpa internet
(hanya audio yang tetap diunduh di dalam aplikasi). Jalankan sekali di Termux
sebelum build:

```bash
python3 tools/fetch_quran_data.py            # unduh 114 surah (Arab+ID & EN)
python3 tools/fetch_quran_data.py --force    # kalau mau unduh ulang semua
bash tools/fetch_font.sh                     # bundle font khat Utsmani (Amiri/OFL)
```

Hasilnya ditulis ke `app/src/main/assets/`:

- `quran/data/surah-<n>.json` — respons mentah equran.id (Arab + terjemahan Indonesia)
- `quran/data/trans-en-<n>.json` — terjemahan Inggris (quran.com, Saheeh
  International; tag HTML & footnote `<sup>` sudah dibersihkan)
- `quran/pages.json` — paginasi mushaf Madani (604 halaman + 30 juz) dari
  `tools/build_pages.py` (metadata alquran.cloud; teks Arab tetap dari bundel)
- `fonts/uthmani.ttf` — font Amiri (SIL OFL 1.1)

Tanpa script ini aplikasi tetap berfungsi (unduh on-demand + cache di
`filesDir/`), hanya saja butuh internet saat pertama kali membuka surah/font.

### Pipeline konten belajar (`tools/`)

Konten fitur belajar **dibuat dari nol (orisinal)** lewat script Python dan
di-bundle ke APK — jalankan ulang saat konten diubah:

```bash
python3 tools/build_pages.py      # paginasi mushaf Madani → assets/quran/pages.json
python3 tools/build_vocab.py       # 589 kata terkurasi → assets/quran/vocab.json
python3 tools/build_lughoh.py      # 15 pelajaran Belajar Arab → assets/lughoh/lessons.json
                                   #   (validasi 11 aturan; --level N untuk cek per level)
```

- `tools/lughoh-schema.md` — skema & aturan validasi data Belajar Arab
  (mufrodat harus muncul di dialog, contoh qawa'id dari dialog, dst.).
- `tools/lughoh_en.py` — terjemahan INGGRIS semua teks Indonesia (wajib
  lengkap: kalau ada yang hilang, build gagal). Materi & tadribat tampil
  dalam bahasa aplikasi (ID/EN).
- `tools/curate_vocab.py`, `tools/vocab_roots.py` — kurasi & analisis akar kata.
- `tools/scrape_dreambig.py`, `tools/dreambig_levels.py` — pipeline lama
  Dream BIG (era level + transkrip YouTube). **Tidak lagi dipakai** sejak
  Dream BIG menjadi arcade tanpa level/transkrip; script & aset transkrip
  (`assets/dreambig/transcripts/`) dipertahankan di repo.

## Build (Termux)

Prasyarat: Android SDK (android-34 **dan android-35**), JDK 17, `gradle.properties` dengan override
`android.aapt2FromMavenOverride`/`android.aidlExecutable` sesuai setup Termux.
(AGP 8.4.0 + compileSdk 35 hanya mengeluarkan warning — sudah ditekan lewat
`android.suppressUnsupportedCompileSdk=35` di `gradle.properties`.)

```bash
# APK debug  → ~/storage/downloads/ayah-of-the-day.apk
bash build-debug.sh

# APK release → ~/storage/downloads/ayah-of-the-day-release.apk
bash build-release.sh
```

(Atau `chmod +x build-debug.sh build-release.sh` sekali, lalu `./build-debug.sh`.)

Manual tanpa script:

```bash
./gradlew assembleDebug --no-daemon
cp app/build/outputs/apk/debug/app-debug.apk ~/storage/downloads/ayah-of-the-day.apk
```

Kalau belum ada platform android-35 (untuk compileSdk 35):

```bash
sdkmanager "platforms;android-35"
```

### Unit test

Test JVM murni untuk `TajwidEngine`, `TranscriptAligner` (Levenshtein),
`ArabicNormalizer`, `QuranParser` (parsing JSON mushaf), `AyahOfTheDayPicker`
(pemilihan ayat harian: batas index, lintas surah, determinisme, validasi
cache), mesin **Kosakata** (`VocabularyEngine`: SRS + quiz), **Dream BIG**
(`DreamBigGame`: ronde acak + bintang), **Belajar Arab** (`LughohParser`/
`LughohEngine`: sesi acak, acak opsi, susun kata), **Kuis Ayat**
(`AyatQuiz`/`SurahQuiz`: soal Lengkapi Ayat & Tebak Surah), sistem
**gamification** (`GamificationStore`: level/streak/todayXp; `Achievements`:
badge progresif & evaluator tier), dan semua *progress store*
(`ReadingStatsStore`, `VocabularyStatsStore`, `DreamBigProgressStore`,
`LughohProgressStore`), dan **integritas mushaf & paginasi**
(`MushafIntegrityTest` — 114 surah / 6236 ayat dari data bundel;
`MushafPagesTest` & `MushafPageComposerTest` — tes emas paginasi Madani:
604 halaman, urutan monoton, batas 30 juz, aturan basmalah, 15 sajdah,
teks tanpa artefak ࣖ):

```bash
./gradlew testDebugUnitTest --no-daemon
```

#### Cakupan 100% inti kebenaran (JaCoCo)

Seluruh logika yang menentukan **akurasi teks & harakat** diuji sampai
**100% baris DAN 100% cabang** (verified dengan JaCoCo):

```bash
./gradlew jacocoCoreReport --no-daemon
# Laporan: app/build/reports/jacoco/core/ (XML + HTML)
```

Lingkup "inti kebenaran" = `data/**` (parser, engine, model, kuis) + util/ yang
murni (`ArabicNormalizer`, `AyahSearch`, `ReadingStats`, `Reciter`,
`AudioUrls`, `DownloadProgress`, `AppLanguage`, `Achievements`,
`GamificationStore`, `GamificationEvents`, semua *progress store*,
`AyahOfTheDayPicker`) + `stt/TranscriptAligner`. **Tes emas integritas
mushaf** (`MushafIntegrityTest`) memvalidasi data asli yang dibundel: 114
surah, total 6.236 ayat, jumlah ayat per surah persis mushaf standar, dan
tidak ada teks kosong (Arab / terjemahan ID / EN) — kalau satu harakat pun
hilang atau rusak, tes ini gagal.

Yang **dikecualikan secara terdokumentasi** (butuh emulator/Robolectric):
lapisan Android murni — `ui/**`, `widget/**`, `theme/**`, `MainActivity`,
`DownloadService`, `TahsinAudioPlayer`, `ArabicSpeechRecognizer`,
repository (I/O assets), `SettingsStore`, `FontStore`, `GamificationHub`
(glue Context), `AyahOfTheDayManager` (glue prefs/repository — logikanya ada
di `AyahOfTheDayPicker` yang diuji 100%).

### CI (GitHub Actions)

`.github/workflows/build.yml` menjalankan `testDebugUnitTest` + `assembleDebug`
pada setiap push/PR. Di runner CI, override Termux (`aapt2`/`aidl`) dihapus
otomatis dari `gradle.properties` sebelum build.

### Signing release

Tanpa konfigurasi tambahan, release memakai **debug signing** (boleh untuk install
pribadi). Untuk keystore sendiri, buat `keystore.properties` di root proyek
**(jangan di-commit / tambahkan ke `.gitignore`)**:

```properties
storeFile=keystore/release.jks      # relatif ke root proyek
storePassword=rahasia
keyAlias=tahsin
keyPassword=rahasia
```

## Sumber data

| Data | Sumber | Lisensi/Status |
|---|---|---|
| Teks Arab + terjemahan Indonesia | [equran.id API](https://equran.id/apidev) | Digunakan non-komersial |
| Terjemahan Inggris (Saheeh Int'l) | [quran.com API v4](https://api.quran.com) | Digunakan non-komersial |
| Audio ayat (banyak qari': Minshawy, Husary, Alafasy, dll.) | [everyayah.com](https://everyayah.com) | Tersedia untuk umum |
| Audio per kata (wbw) | audio.qurancdn.com | Tersedia untuk umum |
| Font Amiri (Utsmani) | [Google Fonts](https://fonts.google.com/specimen/Amiri) | SIL OFL 1.1 |

## Izin aplikasi

- `RECORD_AUDIO` — penilaian bacaan (diminta saat pertama kali mic ditekan).
- `VIBRATE` — getar saat ada kesalahan pengucapan (umpan muroja'ah).
- `INTERNET` — unduh audio (konten mushaf sudah di-bundle).
- `POST_NOTIFICATIONS` — notifikasi unduhan latar belakang + notifikasi harian
  "Ayah of the Day" (diminta saat menghidupkan toggle di menu Pengaturan).
- `WAKE_LOCK`, `FOREGROUND_SERVICE` (+ `FOREGROUND_SERVICE_DATA_SYNC`) —
  unduhan latar belakang saat layar mati, setelah user mengizinkan lewat prompt.
- `RECEIVE_BOOT_COMPLETED` — reschedule alarm harian "Ayah of the Day" setelah
  perangkat restart.

## Lisensi

Proyek ini didistribusikan di bawah **GNU General Public License v3.0** —
lihat [LICENSE](LICENSE) untuk teks lengkap.

---

Dibuat dengan ❤️ oleh [OpenNur Project (FOSS)](https://github.com/opennur/opennur).
