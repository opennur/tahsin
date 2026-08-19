# Security and Performance Foundations

This document summarizes the security and performance safeguards currently
enabled in the repository, along with the external audit steps for MobSF and
SecDroid.

## Active safeguards

| Area | Implementation | Location |
|---|---|---|
| Network security | **HTTPS only** (`cleartextTrafficPermitted=false`) with system trust anchors and no custom CA | `app/src/main/res/xml/network_security_config.xml` + `AndroidManifest.xml` |
| User-data backup | `allowBackup=false` plus complete cloud and device-to-device exclusion rules; reading history, bookmarks, and progress stay out of system backups | `app/src/main/res/xml/data_extraction_rules.xml`, `backup_rules.xml` |
| Obfuscation and size | **R8 is fully disabled for release** (`isMinifyEnabled=false` + `isShrinkResources=false`). Release builds previously crashed at launch on a device; the root cause is still under investigation. Without R8 the APK is larger (about 9.6 MB) but stable. Keep rules in `proguard-rules.pro` remain dormant until the issue is understood. | `app/build.gradle.kts`, `app/proguard-rules.pro` |
| Preferences | **Preferences DataStore** replaces SharedPreferences, with consistent reads, serialized atomic writes, and automatic migration of legacy keys | `util/DataStores.kt`, `util/PreferencesStore.kt`, `util/SettingsStore.kt`, `util/AyahOfTheDayManager.kt` |
| Larger local data | JSON files under `filesDir` with atomic temp-file writes; no external database is used | `util/*Store.kt` |
| Audio download safety | Audio is written to `.mp3.part`, resumed with HTTP Range, length-validated, and promoted with an atomic rename. Pending surahs are stored in `pending-downloads.json`. | `util/AudioDownloader.kt`, `ui/TahsinViewModel.kt` |
| Audio endpoints | Only HTTPS URLs are accepted (`everyayah.com`, `audio.qurancdn.com`) | `util/AudioUrls.kt` |

## Automated security audit: MobSF

The `.github/workflows/security.yml` workflow runs **MobSF** (Mobile Security
Framework) against the release APK:

- It runs automatically for a **`v*` tag**, or manually through
  **Actions → Security — MobSF scan → Run workflow**.
- PDF and JSON reports are uploaded as the `mobsf-report` artifact.

MobSF checks the manifest, permissions, exported components, insecure storage,
hardcoded secrets, TLS and cleartext settings, WebViews, and related risks. The
report in `mobsf-report/` contains the score and findings for each category.

> The MobSF Docker image is large (about 3–4 GB) and a scan takes roughly
> 5–10 minutes, so it intentionally does not run on every PR. For a local audit:
> `docker run -p 8000:8000 opensecurity/mobsf:latest`, then open
> `http://localhost:8000` and upload the APK from `./gradlew assembleRelease`.

## On-device audit: SecDroid

**SecDroid** is an Android security scanner used as a manual QA step before a
release:

1. Install SecDroid from F-Droid or its APK on a test device.
2. Install the release APK produced by `./gradlew assembleRelease`.
3. Run a full scan and review permissions, exported components, and storage.

## Pre-release checklist

- [ ] `./gradlew testDebugUnitTest assembleDebug jacocoCoreReport detekt lintDebug --no-daemon` is green.
- [ ] `python3 tools/validate_quran_content.py` confirms all 6,236 ayahs after
      cleanup against the official Kemenag/LPMQ source, and the manifest has
      been reviewed.
- [ ] `python3 tools/validate_quran_fields.py --ignore-latin --ignore-indonesian`
      confirms clean Arabic fields, characters, quotation marks, and ayah order.
- [ ] Translation rights and audio attribution are recorded in
      `docs/CONTENT_PROVENANCE.en.md`.
- [ ] `docs/TAJWID_REVIEW.md` has qualified-expert sign-off.
- [ ] The MobSF release scan has no new high-severity findings.
- [ ] SecDroid finds no accidentally exported components (`android:exported` is
      explicit on every component).
- [ ] The release APK has been tested on a real device, including one-time
      migration from legacy SharedPreferences to DataStore.
