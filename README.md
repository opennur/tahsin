# Tahsin Quran

Aplikasi Android untuk **muroja'ah & latihan baca Al-Qur'an**: mushaf dengan gaya
khat Utsmani, penilaian bacaan real-time lewat mikrofon, pewarnaan huruf tajwid,
audio qari per ayat + per kata, dan **mode flow** untuk muroja'ah berkelanjutan
tanpa melihat layar.

> ⚠️ **Batasan jujur** — aplikasi ini adalah alat bantu latihan, **bukan pengganti guru**.
> STT (speech-to-text) hanya membaca *teks* ucapan: aplikasi bisa menilai kata
> terlewat/salah susun/salah huruf, tapi **tidak bisa menilai makhraj atau
> panjang-pendek harakat**. Deteksi tajwid bersifat rule-based ("peta hukum"
> dari teks ber-tashkeel), bukan analisis audio.

## Fitur

- 📖 **Mushaf gaya mushaf asli** — kata tersambung, susunan RTL, semua 114 surah,
  **offline bawaan** (Arab + terjemahan ID/EN di-bundle ke APK).
- 🎙️ **Penilaian real-time**: kata berubah hijau (benar) / merah (salah) / kuning
  (sedang dibaca) saat kamu membaca ke mikrofon (SpeechRecognizer `ar-SA`).
- 🎨 **Warna tajwid** (nyala default, bisa dimatikan di drawer): mad (merah),
  ghunnah (hijau), qalqalah (biru), ikhfa' (abu-abu), iqlab (ungu), idgham (oranye),
  lam jalalah (teal).
- 🔁 **Mode Flow (muroja'ah)**: kalau satu ayat selesai benar, otomatis lanjut ke
  ayat berikutnya + mikrofon menyala lagi; **bunyi gagal ganda + getar** saat ada
  kesalahan, beep sukses saat ayat tuntas — bisa muroja'ah tanpa lihat layar.
- 👆 **Gesture**: **geser layar** (mushaf, terjemahan, maupun background) ke
  kanan/kiri untuk ganti ayat (RTL: kanan = ayat berikutnya); petunjuk geser bisa
  ditutup permanen lewat tombol ✕.
- 🧭 **Navigasi satu baris**: `[‹ next] [surah ▾] [Ayat (n) ▾] [› prev]` — label
  surah/ayat ter-truncate otomatis (ellipsis) supaya selalu muat satu layar.
- 🔍 **Panel kata**: ketuk kata di mushaf → hukum tajwid + penjelasan + putar
  audio kata; tombolnya berubah jadi **⏹ Stop** saat kata sedang diputar
  (terpisah dari tombol Dengar ayat — bebas race).
- 🔊 **Audio contoh**: Minshawy Murattal per ayat + audio per kata (qurancdn wbw),
  diunduh in-app per surah / **semua surah** (tanpa estimasi), dengan **progress
  bar di footer** + nama surah yang sedang diunduh; unduhan latar belakang
  (foreground service) setelah user mengizinkan.
- 📂 **Manajemen audio terunduh**: ukuran per surah, hapus per surah / hapus
  semua (dengan konfirmasi), **kartu progres live** saat ada unduhan berjalan,
  dan **cache daftar** — membuka layar lagi instan tanpa pemindaian ulang.
- 🌙 **Dark mode** (tombol di header), **ganti bahasa ID/EN** (tombol di header),
  drawer kanan via tombol ⚙ untuk pengaturan lain.
- 🗓️ **Widget + notifikasi "Ayah of the Day"** — satu ayat berganti setiap hari
  (deterministik per tanggal, offline dari bundel aset). Widget home screen
  ringkas: terjemahan saja; notifikasi menampilkan teks Arab + terjemahan.
  Ketuk widget/notifikasi → aplikasi terbuka tepat di ayat itu; update harian
  via AlarmManager (+ reschedule saat boot), bisa dimatikan di drawer pengaturan.
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
- **Toggle** "🗓️ Notifikasi Harian" di drawer ⚙ (default nyala; pada Android 13+
  izin notifikasi diminta saat user menghidupkannya).

## Arsitektur & stack

- **Kotlin + Jetpack Compose** — **tanpa Material 3** (custom design system di `theme/`).
- compileSdk 35, targetSdk 35 (edge-to-edge wajib), minSdk 26 · AGP 8.4.0 · Kotlin 2.0.20 · Gradle 8.6 · Java 17.
- Tanpa Room/Hilt/Retrofit/Navigation — DI manual (`ViewModel` + factory), Gson untuk JSON.
- **Offline-first**: konten surah & terjemahan di-bundle ke APK (siap tanpa
  internet); audio diunduh in-app dan di-cache ke `filesDir/`; daftar audio
  manajemen ikut di-cache.

```
app/src/main/java/com/tahsin/app/
├── data/quran/     # model Surah/Ayah + repository (aset bundle → cache → equran.id)
├── data/tajwid/    # TajwidEngine (rule-based) + TajwidColorizer (span warna)
├── stt/            # ArabicSpeechRecognizer + TranscriptAligner (Levenshtein)
├── ui/             # TahsinScreen, TahsinViewModel, AudioManagerScreen(+VM)
├── widget/         # AyahOfTheDayWidget (AppWidgetProvider) + alarm harian/notifikasi
├── util/           # AudioDownloader, AudioUrls, TahsinAudioPlayer (PlaySource),
│                   #   DownloadProgress, DownloadService, FontStore, SettingsStore,
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
- `fonts/uthmani.ttf` — font Amiri (SIL OFL 1.1)

Tanpa script ini aplikasi tetap berfungsi (unduh on-demand + cache di
`filesDir/`), hanya saja butuh internet saat pertama kali membuka surah/font.

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
`ArabicNormalizer`, `QuranParser` (parsing JSON mushaf, di-extract dari
`QuranRepository` agar bisa diuji tanpa Android), dan `AyahOfTheDayManager`
(pemilihan ayat harian: batas index, lintas surah, determinisme):

```bash
./gradlew testDebugUnitTest --no-daemon
```

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
| Audio ayat (Minshawy Murattal) | [everyayah.com](https://everyayah.com) | Tersedia untuk umum |
| Audio per kata (wbw) | audio.qurancdn.com | Tersedia untuk umum |
| Font Amiri (Utsmani) | [Google Fonts](https://fonts.google.com/specimen/Amiri) | SIL OFL 1.1 |

## Izin aplikasi

- `RECORD_AUDIO` — penilaian bacaan (diminta saat pertama kali mic ditekan).
- `VIBRATE` — getar saat ada kesalahan pengucapan (umpan muroja'ah).
- `INTERNET` — unduh audio (konten mushaf sudah di-bundle).
- `POST_NOTIFICATIONS` — notifikasi unduhan latar belakang + notifikasi harian
  "Ayah of the Day" (diminta saat menghidupkan toggle di drawer).
- `WAKE_LOCK`, `FOREGROUND_SERVICE` (+ `FOREGROUND_SERVICE_DATA_SYNC`) —
  unduhan latar belakang saat layar mati, setelah user mengizinkan lewat prompt.
- `RECEIVE_BOOT_COMPLETED` — reschedule alarm harian "Ayah of the Day" setelah
  perangkat restart.

## Lisensi

Proyek ini didistribusikan di bawah **GNU General Public License v3.0** —
lihat [LICENSE](LICENSE) untuk teks lengkap.

---

Dibuat dengan ❤️ oleh Lutfian Dwi Cahyono.
