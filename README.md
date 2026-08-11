# Tahsin Quran

Aplikasi Android untuk **muroja'ah & latihan baca Al-Qur'an**: mushaf kata-per-kata
dengan penilaian bacaan real-time lewat mikrofon, pewarnaan huruf tajwid, audio
qari per ayat + per kata, dan **mode flow** untuk muroja'ah berkelanjutan tanpa
melihat layar.

> ⚠️ **Batasan jujur** — aplikasi ini adalah alat bantu latihan, **bukan pengganti guru**.
> STT (speech-to-text) hanya membaca *teks* ucapan: aplikasi bisa menilai kata
> terlewat/salah susun/salah huruf, tapi **tidak bisa menilai makhraj atau
> panjang-pendek harakat**. Deteksi tajwid bersifat rule-based ("peta hukum"
> dari teks ber-tashkeel), bukan analisis audio.

## Fitur

- 📖 **Mushaf kata-per-kata** (susunan RTL) — semua 114 surah dari equran.id.
- 🎙️ **Penilaian real-time**: kata berubah hijau (benar) / merah (salah) / kuning
  (sedang dibaca) saat kamu membaca ke mikrofon (SpeechRecognizer `ar-SA`).
- 🎨 **Warna tajwid** (nyala default, bisa dimatikan): mad (merah), ghunnah (hijau),
  qalqalah (biru), ikhfa' (abu-abu), iqlab (ungu), idgham (oranye), lam jalalah (teal).
- 🔁 **Mode Flow (muroja'ah)**: kalau satu ayat selesai benar, otomatis lanjut ke
  ayat berikutnya + mikrofon menyala lagi; beep sukses/gagal biar bisa tanpa lihat layar.
- 🔊 **Audio contoh**: Minshawy Murattal per ayat + audio per kata (qurancdn wbw),
  diunduh in-app (per surah / semua), offline setelah terunduh.
- 📂 **Manajemen audio terunduh**: lihat ukuran, hapus per surah / hapus semua.
- 🔤 **Jenis font Arab**: Utsmani (default, Amiri — lisensi OFL, diunduh otomatis),
  Indopak (siap; taruh TTF di `filesDir/fonts/indopak.ttf`), Android.
- 🌙 **Dark mode**, ukuran font A−/A+, dropdown surah & ayat (bukan tab).
- 🔍 **Panel kata**: ketuk kata → hukum tajwid + penjelasan + putar audio kata.

## Arsitektur & stack

- **Kotlin + Jetpack Compose** — **tanpa Material 3** (custom design system di `theme/`).
- compileSdk 34, targetSdk 34, minSdk 26 · AGP 8.4.0 · Kotlin 2.0.20 · Gradle 8.6 · Java 17.
- Tanpa Room/Hilt/Retrofit/Navigation — DI manual (`ViewModel` + factory), Gson untuk JSON.
- **Offline-first**: konten surah & audio di-cache ke `filesDir/` setelah diunduh.

```
app/src/main/java/com/tahsin/app/
├── data/quran/     # model Surah/Ayah + repository (equran.id, cache filesDir)
├── data/tajwid/    # TajwidEngine (rule-based) + TajwidColorizer (span warna)
├── stt/            # ArabicSpeechRecognizer + TranscriptAligner (Levenshtein)
├── ui/             # TahsinScreen, TahsinViewModel, AudioManagerScreen
├── util/           # AudioDownloader, AudioUrls, TahsinAudioPlayer, FontStore, SettingsStore
└── theme/          # Colors, Typography, Shapes, ArabicFont (custom design system)
```

## Build (Termux)

Prasyarat: Android SDK (android-34), JDK 17, `gradle.properties` dengan override
`android.aapt2FromMavenOverride`/`android.aidlExecutable` sesuai setup Termux.

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
| Teks Arab + terjemahan | [equran.id API](https://equran.id/apidev) | Digunakan non-komersial |
| Audio ayat (Minshawy Murattal) | [everyayah.com](https://everyayah.com) | Tersedia untuk umum |
| Audio per kata (wbw) | audio.qurancdn.com | Tersedia untuk umum |
| Font Amiri (Utsmani) | [Google Fonts](https://fonts.google.com/specimen/Amiri) | SIL OFL 1.1 |

## Izin aplikasi

- `RECORD_AUDIO` — penilaian bacaan (diminta saat pertama kali mic ditekan).
- `INTERNET` — unduh surah/audio/font (semua konten di-cache setelahnya).

---

Dibuat dengan ❤️ oleh Lutfian Dwi Cahyono.
