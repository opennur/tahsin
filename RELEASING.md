# Rilis ke Google Play Store

Checklist lengkap untuk merilis **Tahsin Quran** (`org.opennur.tahsin`) ke Google
Play. Versi baseline rilis pertama: `versionCode = 1`, `versionName = "1.0.0"`.

## 0. Prasyarat akun

- Akun developer Google Play (biaya sekali USD 25).
- Verifikasi identitas (nama pengembang, alamat, KTP/paspor) di Play Console.
- Satu akun ini dipakai untuk testing internal, closed, dan production track.

## 1. Aset aplikasi (Play Console → Create app)

| Aset | Isi |
|---|---|
| Nama aplikasi | **Tahsin Quran** |
| Short description (≤80 char) | "Belajar membaca Al-Qur'an: mushaf Madani, tahsin STT, tajwid, dan kosakata." |
| Full description | ID + EN — ringkas README.md / README.en.md (fitur, offline-first, cakupan kata). |
| Icon 512×512 | dari adaptive icon; pastikan tepi tidak transparan di Play. |
| Feature graphic 1024×500 | opsional, disarankan. |
| Screenshot (min. 2, sarankan 8) | Tahsin (mushaf), penilaian STT, Kosakata, Kuis Tajwid, Belajar Arab, Dream BIG, Kuis Ayat, Keajaiban & Ayat Favorit. |
| Kategori | Education (primary). |
| Kontak | email developer; website opsional. |

## 2. Signing & build rilis

1. **Buat keystore SEKALI** (jangan commit ke git, simpan offline + backup):

   ```bash
   keytool -genkey -v -keystore tahsin-release.jks -keyalg RSA -keysize 2048 \
     -validity 10000 -alias tahsin
   ```

2. Isi `keystore.properties` di root proyek (sudah didukung `app/build.gradle.kts` —
   kalau file ini ada, `release` build memakai signing ini; kalau tidak ada,
   fallback ke debug signing):

   ```properties
   storeFile=tahsin-release.jks
   storePassword=***
   keyAlias=tahsin
   keyPassword=***
   ```

3. Build **AAB** (wajib untuk Play):

   ```bash
   ./gradlew bundleRelease --no-daemon
   # → app/build/outputs/bundle/release/app-release.aab
   ```

4. Aktifkan **Play App Signing** di Play Console — Google menyimpan kunci app
   signing; upload key tetap di tanganmu. Jangan pernah kehilangan keystore.

## 3. Konten, rating, dan kebijakan

- **Content rating (IARC)**: isi kuesioner — konten edukasi agama, tidak ada
  kekerasan/drugs/sex → rating **Everyone**.
- **Data safety** (wajib diisi):
  - Data dikumpulkan: **tidak ada** (aplikasi offline-first; statistik, XP,
    bookmark, dan progres tersimpan lokal di perangkat).
  - Izin yang dipakai + alasannya (sudah di `AndroidManifest.xml`):
    - `RECORD_AUDIO` — penilaian bacaan STT (diproses di perangkat);
    - `INTERNET` — unduh audio qari' & data surah (equran.id, everyayah.com, quran.com);
    - `POST_NOTIFICATIONS` — notifikasi "Ayah of the Day" & pengingat streak;
    - `SCHEDULE_EXACT_ALARM` — jadwal harian eksak; **fallback ke alarm tidak
      eksak** kalau izin tidak diberikan (tidak crash di Android 12+);
    - `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` — unduh semua audio
      saat layar mati;
    - `RECEIVE_BOOT_COMPLETED` — jadwal ulang notifikasi setelah restart;
    - `WAKE_LOCK`, `VIBRATE` — unduhan & getar umpan bacaan.
- **Privacy policy URL** — wajib (ada izin mikrofon + konten). Pakai
  `PRIVACY_POLICY.md` (hosting di GitHub Pages/repo). Jangan lupa isi juga
  bagian "penghapusan data" (tidak ada akun → semua data lokal bisa dihapus
  dengan menghapus data aplikasi).
- **Iklan & pembelian**: tidak ada, tidak ada.
- **Target audience**: Everyone.

## 4. Track rilis

1. **Internal testing** → upload AAB, tambah email tester, undang, validasi.
2. **Closed testing** (opsional) → beta terbatas.
3. **Production** → isi release notes ("What's new", ID + EN), Submit.
4. Review Google: 1–7 hari untuk akun baru; pantau email konsol.

## 5. Pasca rilis

- Pantau **Vitals** (crash/ANR) di Play Console.
- Setiap rilis baru: `versionCode` **naik +1** (jangan pernah turun), `versionName`
  versi semantik (1.0.1, 1.1.0, …).
- Tetap jalankan gate sebelum rilis:

  ```bash
  ./gradlew testDebugUnitTest assembleDebug jacocoCoreReport --no-daemon
  ```
