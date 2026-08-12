# Tahsin Quran

An Android app for **muraja'ah & Qur'an reading practice**: a mushaf in the
original Uthmani script, real-time recitation scoring via microphone, tajwid
letter coloring, per-ayah + per-word qari audio, and a **flow mode** for
continuous muraja'ah without looking at the screen.

> ⚠️ **Honest limitations** — this app is a practice aid, **not a substitute for a teacher**.
> STT (speech-to-text) only reads the *text* of what you say: it can catch skipped
> words, wrong word order, or wrong letters, but it **cannot judge makhraj
> (articulation) or vowel length**. Tajwid detection is rule-based (a "rule map"
> derived from tashkeel text), not audio analysis.

## Features

- 📖 **Authentic mushaf style** — connected script, RTL layout, all 114 surahs,
  **offline by default** (Arabic + ID/EN translations bundled into the APK).
- 🎙️ **Real-time scoring**: words turn green (correct) / red (wrong) / yellow
  (currently being read) as you recite into the microphone (SpeechRecognizer `ar-SA`).
- 📊 **Statistics & error history** (persistent): every final recitation result is
  stored per ayah — score (0–100), number of attempts, and the **frequently
  wrong/missed words** (from `TranscriptAligner`). The **Statistics & History**
  screen (⚙ drawer) shows a global summary plus "frequently wrong words" per
  surah; tap a word to jump straight to that ayah to fix it. The main screen
  shows a quick line "N× attempted · best score M%".
- 🎨 **Tajwid colors** (on by default, toggleable in the drawer): mad (red),
  ghunnah (green), qalqalah (blue), ikhfa' (gray), iqlab (purple), idgham (orange),
  lam jalalah (teal).
- 🔁 **Flow Mode (muraja'ah)**: when an ayah is fully correct, it automatically
  advances to the next ayah and re-enables the mic; **double error sound + vibration**
  on mistakes, success beep when an ayah is completed — practice without looking
  at the screen.
- 👆 **Gestures**: **swipe** (mushaf, translation, or background) left/right to
  change ayah (RTL: right = next ayah); the swipe hint can be dismissed
  permanently with the ✕ button.
- 🧭 **Single-line navigation**: `[‹ next] [surah ▾] [Ayah (n) ▾] [› prev]` —
  surah/ayah labels auto-truncate (ellipsis) so it always fits on one screen.
- 🔍 **Ayah search** (🔍 button in the header): search by **Arabic word**
  (diacritics and hamza/ya/ta-marbuta variants are normalized automatically)
  or by **ID/EN translation keyword** across all 114 surahs — offline from the
  bundle. Tap a result to jump straight to that ayah; typing is debounced and
  the index is built once.
- 🔍 **Word panel**: tap a word in the mushaf → tajwid rule + explanation + play
  the word audio; the button becomes **⏹ Stop** while the word is playing
  (separate from the "Listen to ayah" button — race-free).
- 🔊 **Sample audio**: Minshawy Murattal per ayah + per-word audio (qurancdn wbw),
  downloaded in-app per surah / **all surahs** (no estimate), with a **footer
  progress bar** + the name of the surah currently downloading; background
  downloads (foreground service) after the user grants permission.
- 📂 **Downloaded-audio management**: size per surah, delete per surah / delete
  all (with confirmation), **live progress card** while a download is running,
  and a **list cache** — reopening the screen is instant with no re-scan.
- 🌙 **Dark mode** (header button), **ID/EN language switch** (header button),
  right drawer via ⚙ for other settings.
- 🗓️ **"Ayah of the Day" widget + notification** — one ayah that changes daily
  (deterministic per date, offline from the bundled assets). The home-screen
  widget is compact: translation only; the notification shows Arabic + translation.
  Tapping the widget/notification opens the app right at that ayah; daily updates
  via AlarmManager (+ reschedule on boot), toggleable in the settings drawer.
- 🔤 **Uthmani script (Amiri)** — font bundled into the APK, renders immediately
  with no download.

## Ayah of the Day (widget & daily notification)

The same ayah for every user throughout the day, automatically changing tomorrow:

- **Deterministic selection** (`util/AyahOfTheDayManager.kt`) — seed = epoch day
  (days since 1970-01-01) → `Random(seed)` picks one index out of the total
  6,236 ayahs → mapped to (surah, ayah) via the per-surah cumulative list.
  Stable all day and **works offline**.
- **Content** is read from the bundled mushaf assets (fallback: download), then
  cached in SharedPreferences keyed by **date + language** → widget/notification
  updates are instant with no re-parsing.
- **Home-screen widget**: compact — surah name · ayah number + **translation only**
  (the Arabic text appears in the notification).
- **Daily notification**: Arabic + translation (BigText), triggered by
  `AlarmManager` (midnight, `setAndAllowWhileIdle`); the alarm is rescheduled
  every time it fires, when the app is opened (`MainActivity.onCreate`), and on
  device restart (`BOOT_COMPLETED`).
- **Tapping the widget/notification** opens the app right at that surah/ayah
  (deep link via Intent extras; safe to trigger repeatedly, rotation-safe).
- **Toggle** "🗓️ Daily Notification" in the ⚙ drawer (on by default; on
  Android 13+ the notification permission is requested when turning it on).

## Architecture & stack

- **Kotlin + Jetpack Compose** — **no Material 3** (custom design system in `theme/`).
- compileSdk 35, targetSdk 35 (edge-to-edge required), minSdk 26 · AGP 8.4.0 · Kotlin 2.0.20 · Gradle 8.6 · Java 17.
- No Room/Hilt/Retrofit/Navigation — manual DI (`ViewModel` + factory), Gson for JSON.
- **Offline-first**: surah content & translations bundled into the APK (ready
  without internet); audio is downloaded in-app and cached in `filesDir/`; the
  audio management list is cached too.

```
app/src/main/java/com/tahsin/app/
├── data/quran/     # Surah/Ayah models + repository (asset bundle → cache → equran.id)
├── data/tajwid/    # TajwidEngine (rule-based) + TajwidColorizer (color spans)
├── stt/            # ArabicSpeechRecognizer + TranscriptAligner (Levenshtein)
├── ui/             # TahsinScreen, TahsinViewModel, AudioManagerScreen(+VM),
│                   #   StatsScreen + StatsViewModel (statistics & history),
│                   #   SearchScreen + SearchViewModel (ayah search)
├── widget/         # AyahOfTheDayWidget (AppWidgetProvider) + daily alarm/notification
├── util/           # AudioDownloader, AudioUrls, TahsinAudioPlayer (PlaySource),
│                   #   DownloadProgress, DownloadService, FontStore, SettingsStore,
│                   #   ReadingStatsStore (per-ayah reading history, JSON filesDir),
│                   #   AyahSearch (normalized Arabic + translation search),
│                   #   AyahOfTheDayManager (daily ayah selection + cache)
└── theme/          # Colors, Typography, Shapes, ArabicFont (custom design system)
```

## Bundled offline content (shipped in the APK)

Mushaf, Indonesian translation, English translation, and the Uthmani font are
**bundled into the APK** so the app works immediately without internet (only
audio is still downloaded in-app). Run once in Termux before building:

```bash
python3 tools/fetch_quran_data.py            # download 114 surahs (Arabic+ID & EN)
python3 tools/fetch_quran_data.py --force    # force re-download everything
bash tools/fetch_font.sh                     # bundle Uthmani font (Amiri/OFL)
```

Output is written to `app/src/main/assets/`:

- `quran/data/surah-<n>.json` — raw equran.id response (Arabic + Indonesian translation)
- `quran/data/trans-en-<n>.json` — English translation (quran.com, Saheeh
  International; HTML tags & `<sup>` footnotes already stripped)
- `fonts/uthmani.ttf` — Amiri font (SIL OFL 1.1)

Without these scripts the app still works (on-demand download + cache in
`filesDir/`), it just needs internet the first time you open a surah/font.

## Build (Termux)

Prerequisites: Android SDK (android-34 **and android-35**), JDK 17, and
`gradle.properties` with the `android.aapt2FromMavenOverride`/`android.aidlExecutable`
overrides matching your Termux setup.
(AGP 8.4.0 + compileSdk 35 only emits a warning — already suppressed via
`android.suppressUnsupportedCompileSdk=35` in `gradle.properties`.)

```bash
# debug APK  → ~/storage/downloads/ayah-of-the-day.apk
bash build-debug.sh

# release APK → ~/storage/downloads/ayah-of-the-day-release.apk
bash build-release.sh
```

(Or `chmod +x build-debug.sh build-release.sh` once, then `./build-debug.sh`.)

Manual, without the scripts:

```bash
./gradlew assembleDebug --no-daemon
cp app/build/outputs/apk/debug/app-debug.apk ~/storage/downloads/ayah-of-the-day.apk
```

If the android-35 platform is missing (needed for compileSdk 35):

```bash
sdkmanager "platforms;android-35"
```

### Unit tests

Pure JVM tests for `TajwidEngine`, `TranscriptAligner` (Levenshtein),
`ArabicNormalizer`, `QuranParser` (mushaf JSON parsing, extracted from
`QuranRepository` so it can be tested without Android), and `AyahOfTheDayManager`
(daily ayah selection: index boundaries, cross-surah mapping, determinism):

```bash
./gradlew testDebugUnitTest --no-daemon
```

### CI (GitHub Actions)

`.github/workflows/build.yml` runs `testDebugUnitTest` + `assembleDebug` on every
push/PR. On CI runners the Termux overrides (`aapt2`/`aidl`) are stripped from
`gradle.properties` automatically before building.

### Signing a release

Without extra configuration, the release build uses **debug signing** (fine for
personal installs). For your own keystore, create `keystore.properties` at the
project root **(do not commit it — add it to `.gitignore`)**:

```properties
storeFile=keystore/release.jks      # relative to the project root
storePassword=secret
keyAlias=tahsin
keyPassword=secret
```

## Data sources

| Data | Source | License/Status |
|---|---|---|
| Arabic text + Indonesian translation | [equran.id API](https://equran.id/apidev) | Used non-commercially |
| English translation (Saheeh Int'l) | [quran.com API v4](https://api.quran.com) | Used non-commercially |
| Ayah audio (Minshawy Murattal) | [everyayah.com](https://everyayah.com) | Publicly available |
| Per-word audio (wbw) | audio.qurancdn.com | Publicly available |
| Amiri font (Uthmani) | [Google Fonts](https://fonts.google.com/specimen/Amiri) | SIL OFL 1.1 |

## App permissions

- `RECORD_AUDIO` — recitation scoring (requested when the mic is first pressed).
- `VIBRATE` — vibration on recitation errors (muraja'ah feedback).
- `INTERNET` — audio downloads (mushaf content is bundled).
- `POST_NOTIFICATIONS` — background-download notifications + the daily
  "Ayah of the Day" notification (requested when enabling the drawer toggle).
- `WAKE_LOCK`, `FOREGROUND_SERVICE` (+ `FOREGROUND_SERVICE_DATA_SYNC`) —
  background downloads while the screen is off, after the user grants permission
  via the prompt.
- `RECEIVE_BOOT_COMPLETED` — reschedules the daily "Ayah of the Day" alarm after
  a device restart.

## License

This project is distributed under the **GNU General Public License v3.0** —
see [LICENSE](LICENSE) for the full text.

---

Made with ❤️ by Lutfian Dwi Cahyono.
