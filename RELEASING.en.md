# Releasing to the Google Play Store

Complete checklist to release **Tahsin Quran** (`org.opennur.tahsin`) on Google
Play. First-release baseline: `versionCode = 1`, `versionName = "1.0.0"`.

## 0. Account prerequisites

- Google Play developer account (one-time USD 25).
- Identity verification (developer name, address, ID) in Play Console.
- Use the same account for internal, closed, and production tracks.

## 1. App assets (Play Console → Create app)

| Asset | Content |
|---|---|
| App name | **Tahsin Quran** |
| Short description (≤80 chars) | "Learn to read the Qur'an: Madani mushaf, STT tahsin, tajwid, and vocabulary." |
| Full description | EN + ID — condense README.en.md / README.md (features, offline-first, word coverage). |
| Icon 512×512 | from the adaptive icon; keep edges non-transparent in Play. |
| Feature graphic 1024×500 | optional, recommended. |
| Screenshots (min. 2, recommend 8) | Tahsin (mushaf), STT scoring, Vocabulary, Tajweed Quiz, Learn Arabic, Dream BIG, Ayah Quiz, Wonders & Favorite Ayahs. |
| Category | Education (primary). |
| Contact | developer email; website optional. |

## 2. Signing & release build

1. **Generate a keystore ONCE** (never commit it; keep offline + backup):

   ```bash
   keytool -genkey -v -keystore tahsin-release.jks -keyalg RSA -keysize 2048 \
     -validity 10000 -alias tahsin
   ```

2. Fill `keystore.properties` at the project root (already supported by
   `app/build.gradle.kts` — when the file exists, release builds use this
   signing; otherwise they fall back to debug signing):

   ```properties
   storeFile=tahsin-release.jks
   storePassword=***
   keyAlias=tahsin
   keyPassword=***
   ```

3. Build the **AAB** (required for Play):

   ```bash
   ./gradlew bundleRelease --no-daemon
   # → app/build/outputs/bundle/release/app-release.aab
   ```

4. Enable **Play App Signing** in Play Console — Google stores the app-signing
   key; the upload key stays with you. Never lose the keystore.

## 3. Content rating & policy

- **Content rating (IARC)**: fill the questionnaire — religious education
  content, no violence/drugs/sex → rating **Everyone**.
- **Data safety** (mandatory):
   - Data collected: **none** (offline-first app; stats, XP, bookmarks, learning
     plan, memorization metadata, and progress are stored locally on device).
  - Permissions used + reasons (already in `AndroidManifest.xml`):
    - `RECORD_AUDIO` — STT recitation scoring (processed on device);
    - `INTERNET` — reciter audio & surah data downloads (equran.id, everyayah.com, quran.com);
    - `POST_NOTIFICATIONS` — "Ayah of the Day" notification & streak reminder;
    - `SCHEDULE_EXACT_ALARM` — daily exact schedule; **falls back to an
      inexact alarm** when the permission is not granted (no crash on Android 12+);
    - `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` — downloading all
      audio while the screen is off;
    - `RECEIVE_BOOT_COMPLETED` — rescheduling notifications after reboot;
    - `WAKE_LOCK`, `VIBRATE` — downloads & reading-feedback vibration.
- **Privacy policy URL** — required (microphone permission + content). Use
  `PRIVACY_POLICY.md` (host on GitHub Pages/repo). Also fill the data-deletion
  section (no accounts → all data is local and can be wiped by clearing app data).
- **Ads & purchases**: none, none.
- **Target audience**: Everyone.

## 4. Release tracks

1. **Internal testing** → upload the AAB, add tester emails, validate.
2. **Closed testing** (optional) → limited beta.
3. **Production** → fill release notes ("What's new", EN + ID), submit.
4. Google review: 1–7 days for new accounts; watch the console email.

## 5. Content and teaching-quality gate

Before uploading an AAB:

- Run `python3 tools/validate_quran_content.py` and
  `python3 tools/validate_quran_fields.py --ignore-latin --ignore-indonesian`; all 6,236 ayahs and user-facing
  fields must pass the official Kemenag/LPMQ checks.
- Review the diff and update `tools/quran-canonical-manifest.json` only after a
  clean validation.
- Confirm the translation and audio rights ledger in
  [docs/CONTENT_PROVENANCE.en.md](docs/CONTENT_PROVENANCE.en.md), including
  attribution or written permission for every provider.
- Do not mark the release ready while any row in
  [docs/TAJWID_REVIEW.md](docs/TAJWID_REVIEW.md) is still pending qualified
  expert sign-off.

## 6. After release

- Monitor **Vitals** (crash/ANR) in Play Console.
- Each new release: bump `versionCode` **by +1** (never decrease), `versionName`
  with semantic versioning (1.0.1, 1.1.0, …).
- Always run the gate before releasing:

  ```bash
  ./gradlew testDebugUnitTest assembleDebug jacocoCoreReport --no-daemon
  ```
