# CONTRIBUTING

Terima kasih sudah ingin berkontribusi ke **Tahsin Quran**! Repo ini menyentuh
kitab suci Al-Qur'an — **satu harakat yang salah pun fatal**. Karena itu ada
beberapa aturan yang tidak bisa ditawar (lihat [Aturan emas](#aturan-emas)).

> 🌐 **English version:** [CONTRIBUTING.en.md](CONTRIBUTING.en.md)

## Menyiapkan lingkungan

Proyek dikembangkan di **Termux (Android)** — tidak ada emulator, jadi hanya
**unit test JVM** yang bisa dijalankan. Jangan menambahkan test instrumented
(androidTest) yang butuh emulator.

```bash
# Install data bundel (sekali; butuh internet)
python3 tools/fetch_quran_data.py       # 114 surah Arab + terjemahan ID & EN
bash tools/fetch_font.sh                # font Utsmani (Amiri, SIL OFL 1.1)

# Build & test
./gradlew testDebugUnitTest --no-daemon
```

## Aturan emas

1. **Gate cakupan 100%.** Semua logika inti kebenaran — `data/**` + `util/**`
   murni + `stt/TranscriptAligner` — harus tercakup **100% line DAN branch**
   (JaCoCo). Sebelum PR:

   ```bash
   ./gradlew testDebugUnitTest assembleDebug jacocoCoreReport --no-daemon
   ```

   `jacocoCoreReport` WAJIB hijau di **100.00% line + 100.00% branch** (laporan:
   `app/build/reports/jacoco/core/index.html`). Daftar pengecualian lengkap ada
   di `app/build.gradle.kts` (repository, `SettingsStore`, `FontStore`,
   `TahsinAudioPlayer`, `AudioDownloader`, `DownloadService`,
   `GamificationHub`, `ArabicSpeechRecognizer`, `ui/**`, `widget/**`,
   `theme/**`, `MainActivity`, sintetis `$default`).

2. **Jangan ubah data bundel separuh.** Setiap pipeline punya pasangan yang
   harus sinkron (script Python ↔ parser Kotlin ↔ JSON di `assets/`):

   | Data | Pipeline | Sinkronisasi wajib |
   |---|---|---|
   | Mushaf (Arab + terjemahan) | `tools/fetch_quran_data.py`, `tools/fetch_font.sh` | jangan edit `assets/quran/data/*.json` manual |
   | Paginasi Madani (604 halaman + 30 juz) | `tools/build_pages.py` → `assets/quran/pages.json` | tes emas `MushafPagesTest`/`MushafPageComposerTest` |
   | Kosakata (1.200 kata) | `tools/build_vocab.py` → `assets/quran/vocab.json` | `VocabKey.normalize` (Kotlin) HARUS sama persis dengan normalisasi di `build_vocab.py` |
   | Belajar Arab (15 pelajaran) | `tools/build_lughoh.py` + `tools/lughoh_en.py` → `assets/lughoh/lessons.json` | build GAGAL jika ada teks ID tanpa terjemahan EN; `LughohEnTest` memvalidasi hasil |

   Setelah regenerasi data, jalankan unit test — tes emas mengunci integritas
   (114 surah / 6236 ayat, 604 halaman, urutan monoton, aturan basmalah,
   15 sajdah, teks tanpa artefak ࣖ/۩ dari font).

3. **i18n: jangan hardcode string UI.** Semua teks lewat `AppStrings.kt`
   (data class) — **setiap field harus ada di instance ID dan EN**. Pakai
   helper yang ada (`AppStrings.badgeTitle/Desc`, `AppStrings.sttErrorMessage`)
   atau tambahkan field baru di ketiga tempat (class + ID + EN).

4. **Tanpa material3, pakai design system sendiri.** Aplikasi sengaja tanpa
   material3: pakai `AyahColors` / `AyahTypography` / `AyahShapes` dan komponen
   di `ui/components/` (`AyahButton`, `AyahText`, `AyahCard`, `SimpleDropdown`,
   `AyahErrorView`, dst). Kalau butuh komponen baru (mis. slider), buat custom
   seperti `FontSizeSlider` — jangan impor material3.

5. **Pola ViewModel (bahasa).** ViewModel di-scope ke Activity dan di-cache —
   bahasa yang ditangkap di `init` jadi basi setelah ganti bahasa di
   Pengaturan. Setiap layar fitur WAJIB memanggil
   `LaunchedEffect(viewModel) { viewModel.refreshLanguage() }`, dan VM
   membaca `settings.languageCode` di dalam fungsi (bukan hanya `init`).

6. **Mushaf halaman (Tahsin) — baca aturan renderer sebelum menyentuh.**
   - Navigasi memakai `pageIndex` (0..603); `surahNumber`/`ayahIndex` = **ayat
     AKTIF** (target latihan STT), bukan navigasi.
   - Teks satu surah di-render **mengalir** (`SurahFlowBlock`), bukan per ayat.
   - Badge akhir ayat & sujud **digambar** (circle + angka Arab-Indik) — JANGAN
     pakai glif ۝/ࣖ (jadi kotak "[]" di font runtime).
   - Highlight ayat aktif digambar **di bawah teks** (`drawBehind`) — jangan
     pakai `Modifier.clip(...).background(...)` yang memotong harakat/waqaf.
   - **Mode flow sudah dihapus permanen** — jangan kembalikan. Penggantinya:
     mode pemutaran audio `AudioPlaybackMode { AYAH, CONTINUOUS, REPEAT }`.

## Test-Driven Development (TDD)

Proyek ini memakai alur **red → green → refactor** untuk semua perubahan
perilaku (fitur baru maupun perbaikan bug):

1. **Red** — tulis dulu test yang GAGAL untuk perilaku yang diinginkan
   (assert dulu, implementasi belum ada / belum benar). Jalankan dan pastikan
   test-nya benar-benar gagal karena alasan yang dimaksud.
2. **Green** — tulis implementasi minimal sampai test hijau. Jangan menambah
   fitur di luar yang dites.
3. **Refactor** — rapikan tanpa mengubah perilaku; jalankan ulang test + gate.

Aturan praktis:

- Setiap bug dilaporkan = tulis test yang mereproduksinya TERLEBIH DAHULU,
  baru perbaiki kodenya. Test itu menjadi regresi permanen.
- Test dulu, kode kemudian — bukan sebaliknya. Kalau kamu menulis
  implementasi dan test setelahnya, tunda sebentar dan tulis ulang test-nya
  sebagai langkah pertama pada perubahan berikutnya.
- Gate command (harus hijau sebelum PR):

  ```bash
  ./gradlew testDebugUnitTest assembleDebug jacocoCoreReport --no-daemon
  ```

## Menulis test

- Setiap fungsi murni baru di `data/**` / `util/**` murni wajib punya unit test
  yang menutup **semua cabang** (branch coverage 100%).
- Tes emas mushaf (`MushafPagesTest`, `MushafPageComposerTest`,
  `MushafIntegrityTest`) mengunci data asli yang dibundel — kalau kamu
  mengubah `pages.json` atau parser, tes ini yang bilang "jangan".
- Nama test pakai backtick/deskripsi jelas, mis. `"page 3 starts at 2:6"` —
  hindari `:` di dalam nama (tidak valid untuk test JVM).
- **Integration test** (`QuranAssetsIntegrationTest`) membaca aset bundel ASLI
  (`src/main/assets/quran/`) — kalau kamu mengubah data/pipeline, tes ini yang
  bilang "jangan".
- **Unit test ViewModel** (`FavoritesViewModelTest`, `StatsViewModelTest`)
  memakai fake `QuranRepository`/`SettingsSource` + store file temp +
  `kotlinx-coroutines-test` (`Dispatchers.setMain(UnconfinedTestDispatcher())`
  di `@Before`, `resetMain()` di `@After`). Jangan lupa panggil `vm.refresh()`
  sebelum menunggu state (di layar dipanggil `LaunchedEffect`).

## Alur kerja PR

1. Ambil isu/task, buat branch baru.
2. Implementasi + unit test (lihat [Aturan emas](#aturan-emas)).
3. Jalankan gate penuh:

   ```bash
   ./gradlew testDebugUnitTest assembleDebug jacocoCoreReport --no-daemon
   ```

   Harus: BUILD SUCCESSFUL, semua test hijau, **CORE LINE 100.00%** dan
   **CORE BRANCH 100.00%**.
4. Kalau menyentuh data/UI yang mengubah tampilan atau konten, perbarui
   `README.md` + `README.en.md` (dan `CONTRIBUTING` ini kalau aturannya
   berubah).
5. Kirim PR dengan deskripsi singkat: apa yang diubah, kenapa, dan hasil gate.

## Melaporkan bug

Sertakan: langkah reproduksi, perilaku yang diharapkan vs aktual, versi APK,
dan — kalau relevan — surah/ayat/halaman yang bermasalah. Bug harakat/teks
adalah prioritas tertinggi (fatal untuk kitab suci).

---

Dibuat dengan ❤️ oleh [OpenNur Project (FOSS)](https://github.com/opennur/opennur).
