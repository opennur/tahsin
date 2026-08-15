# Keamanan & Performa — Fondasi

Dokumen ini merangkum fondasi keamanan & performa yang aktif di repositori,
plus cara menjalankan audit keamanan eksternal (MobSF & SecDroid).

## Yang sudah aktif di aplikasi

| Area | Implementasi | Lokasi |
|---|---|---|
| Network security | Semua trafik **HTTPS** saja (`cleartextTrafficPermitted=false`), trust anchor sistem saja (tanpa CA custom) | `app/src/main/res/xml/network_security_config.xml` + `AndroidManifest.xml` |
| Backup data | `allowBackup=false` + aturan exclude penuh untuk cloud backup & device-to-device (data pribadi: riwayat baca, bookmark, progres tidak ikut backup) | `app/src/main/res/xml/data_extraction_rules.xml`, `backup_rules.xml` |
| Obfuscation/ukuran | **R8 untuk release DIMATIKAN TOTAL** (`isMinifyEnabled=false` + `isShrinkResources=false`) — build release ber-R8 terbukti crash saat launch di perangkat (debug aman); root cause belum didiagnosis. Tanpa R8, APK lebih besar (~9.6 MB) tapi stabil. Keep rules di `proguard-rules.pro` dipertahankan (dormant) untuk re-enable setelah didiagnosis | `app/build.gradle.kts`, `app/proguard-rules.pro` |
| Penyimpanan preferensi | **Preferences DataStore** menggantikan SharedPreferences (baca konsisten, tulis serial + atomik, migrasi otomatis key lama) | `util/DataStores.kt`, `util/PreferencesStore.kt`, `util/SettingsStore.kt`, `util/AyahOfTheDayManager.kt` |
| Penyimpanan data besar | JSON + `filesDir` dengan tulis atomik (tmp → rename) — tanpa database eksternal, data tetap di internal storage | `util/*Store.kt` |
| Audio download safety | File audio ditulis ke `.mp3.part`, dilanjutkan dengan HTTP Range, divalidasi panjangnya, lalu dipromosikan lewat rename atomik; antrean surah disimpan di `pending-downloads.json` | `util/AudioDownloader.kt`, `ui/TahsinViewModel.kt` |
| Audio | Hanya URL HTTPS (`everyayah.com`, `audio.qurancdn.com`) | `util/AudioUrls.kt` |

## Audit keamanan otomatis — MobSF (CI)

Workflow `.github/workflows/security.yml` menjalankan **MobSF** (Mobile Security
Framework) terhadap APK release:

- Dipicu otomatis saat **push tag `v*`** atau manual via
  **Actions → "Security — MobSF scan" → Run workflow**.
- Hasil: laporan (PDF + JSON) diunduh sebagai artifact `mobsf-report`.

MobSF memeriksa: manifest/izin berlebihan, komponen yang bisa diekspor,
penyimpanan tidak aman, hardcoded secret, TLS/cleartext, WebView, dll.
Laporan di `mobsf-report/` berisi skor & temuan per kategori.

> Catatan: image Docker MobSF besar (±3–4 GB) dan scan ±5–10 menit, jadi
> sengaja TIDAK dijalankan per-PR. Untuk audit lokal:
> `docker run -p 8000:8000 opensecurity/mobsf:latest` → buka
> `http://localhost:8000`, upload APK hasil `./gradlew assembleRelease`.

## Audit on-device — SecDroid

**SecDroid** (scanner keamanan Android yang berjalan di perangkat) dipakai
sebagai langkah QA manual sebelum rilis:

1. Instal SecDroid (F-Droid/APK) di perangkat uji.
2. Instal APK release hasil `./gradlew assembleRelease`.
3. Jalankan scan penuh di SecDroid, periksa temuan yang relevan (izin,
   komponen terekspos, penyimpanan).

## Checklist pra-rilis

- [ ] `./gradlew testDebugUnitTest detekt lintDebug assembleRelease` hijau
- [ ] `python3 tools/validate_quran_content.py` — 6.236 ayat cocok setelah
  pembersihan dengan sumber resmi Kemenag/LPMQ dan manifest ditinjau
- [ ] `python3 tools/validate_quran_fields.py` — field Arab, Latin, Indonesia,
  karakter, kutip, dan urutan ayat bersih
- [ ] Hak/atribusi semua terjemahan dan audio sudah dicatat di
  `docs/CONTENT_PROVENANCE.en.md`
- [ ] `docs/TAJWID_REVIEW.md` sudah mendapat sign-off ahli berkualifikasi
- [ ] MobSF (CI tag rilis) tidak punya temuan severity **high** yang baru
- [ ] SecDroid: tidak ada komponen terekspos yang tidak disengaja
  (`android:exported` sudah diatur eksplisit di semua komponen)
- [ ] APK release diuji di perangkat nyata (migrasi DataStore dari
  SharedPreferences lama diverifikasi sekali di perangkat lama)
