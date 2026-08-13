# Tahsin Quran

An Android app for **muraja'ah & Qur'an reading practice**: a mushaf in the
original Uthmani script, real-time recitation scoring via microphone, tajwid
letter coloring, per-ayah + per-word qari audio, and a **flow mode** for
continuous muraja'ah without looking at the screen — plus **Qur'an & Arabic
learning tracks**: Vocabulary, Tajweed Quiz, the **Dream BIG** game (arcade),
and the **Learn Arabic** course (Durusul Lughoh-style methodology).

> ⚠️ **Honest limitations** — this app is a practice aid, **not a substitute for a teacher**.
> STT (speech-to-text) only reads the *text* of what you say: it can catch skipped
> words, wrong word order, or wrong letters, but it **cannot judge makhraj
> (articulation) or vowel length**. Tajwid detection is rule-based (a "rule map"
> derived from tashkeel text), not audio analysis.

## Features

- 🧭 **Main menu** (home screen): every feature is opened from a menu card —
  **Tahsin**, **Vocabulary**, **Tajweed Quiz**, **Statistics**, **Search**,
  **Audio Manager**, **Dream BIG**, **Learn Arabic**, and **Settings**.
- 📖 **Authentic mushaf style** — connected script, RTL layout, all 114 surahs,
  **offline by default** (Arabic + ID/EN translations bundled into the APK).
- 🎙️ **Real-time scoring**: words turn green (correct) / red (wrong) / yellow
  (currently being read) as you recite into the microphone (SpeechRecognizer `ar-SA`).
- 📊 **Aggregate statistics across all challenges** (persistent): the
  **Statistics** screen aggregates all activity — Tahsin (per-ayah score 0–100
  & attempt count), **Dream BIG** (rounds & best score), **Learn Arabic**
  (sessions & best score), and **Vocabulary** (words mastered). Summary:
  **Total Sessions, Best Score %, Total Rounds, Words Mastered** + a per-feature
  breakdown. The Tahsin main screen still shows the quick line "N× attempted ·
  best score M%".
- 🎨 **Tajwid colors** (on by default, toggleable in Settings): mad (red),
  ghunnah (green), qalqalah (blue), ikhfa' (gray), iqlab (purple), idgham (orange),
  lam jalalah (teal).
- 🧠 **Full tajwid engine + quiz**: on top of the existing mad wajib/jaiz and
  lam jalalah, the engine now detects **tafkhim/tarqiq** (isti'la letters & ra'),
  **mad badal/iwad/aridh lis-sukun**, and **mushaf waqaf signs** (مـ obligatory,
  لا don't stop, ج optional, صلي/قلي prefer continuing/stopping, ∴ paired) — shown
  in the word panel & error list. **📝 Tajweed Quiz** (Quiz menu) asks "what rule
  applies to this word?" from random words across the whole mushaf (4 multiple
  choice options, score, explanation) — for learning, not just coloring.
- 🔁 **Flow Mode (muraja'ah)**: when an ayah is fully correct, it automatically
  advances to the next ayah and re-enables the mic; **double error sound + vibration**
  on mistakes, success beep when an ayah is completed — practice without looking
  at the screen.
- 📖 **Qur'an Vocabulary** (Vocabulary menu): **589 curated words** from the
  whole mushaf (VocabKey mirror) — word cards with meaning + example ayah, an
  SRS system (new vs. due-for-review), a multiple-choice **quiz** mode, and a
  jump straight to the example ayah.
- 🎬 **Dream BIG** (Dream BIG menu, **arcade**): **endless** vocabulary quiz
  rounds — 10 questions shuffled from the whole curated vocabulary every round;
  best **score, streak, and rounds played** are persisted. No levels/unlocks,
  keep playing.
- 📚 **Learn Arabic** (Learn Arabic menu): a beginner Arabic course in the
  Durusul Lughoh style — **15 original lessons** (3 levels: introductions &
  daily life, activities, social life) with dialogue, vocabulary, and grammar.
  Practice is an **endless random session** (8 questions drawn from all lessons,
  shuffled options) with a best-score record; the material can still be browsed
  via the level/lesson browser. 100% original content (no copied book material).
- 🎮 **XP, Level & Streak**: every learning activity earns XP — Tahsin
  recitation (score ≥70: 5 XP, ≥90: 10 XP), correct quiz answers (2 XP),
  newly mastered vocabulary words (10 XP), Dream BIG rounds (15 XP), Learn
  Arabic sessions (10 XP). Levels follow a quadratic curve (`√(XP/100)`), a
  **daily streak** is tracked per calendar day, and a **50 XP daily goal**
  shows a progress bar on Home & Stats. Level-ups, streak milestones
  (3/7/14/30 days), and new badges are celebrated with a dialog + vibration.
- 🏅 **Badges**: **8 progressive achievements** — XP (knowledge seeker),
  streak, Tahsin recitations, perfect recitation (90+), mastered words,
  Dream BIG rounds, Arabic sessions, and completed surahs. Every badge has
  **unlimited tiers** (thresholds keep rising: 50 words, 100, 150, …) — once
  a tier unlocks, the next tier is always there to chase. The gallery shows
  the current tier + a **progress bar toward the next tier**; the latest
  badge (with its tier) appears on Home & Stats.
- 🎯 **Ayah Quiz** (Ayah Quiz menu): two multiple-choice modes over the whole
  mushaf — **Complete the Ayah** (which word completes this ayah?) and
  **Guess the Surah** (which surah is this ayah from?) — distractors drawn
  from words in the same surah / other surah names; score + XP per correct
  answer.
- 🔥 **Streak reminder** (optional, Settings menu): daily 18:00 notification
  when today's goal isn't reached yet — so your streak never silently dies.
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
- 🔊 **Sample audio**: **choose a reciter** (Settings menu): Minshawy, Husary,
  Husary Muallim, Abdul Basit, Alafasy, As-Sudais, Hudhaify (everyayah.com —
  ayah audio is stored per reciter in `filesDir/audio/<reciter>/`) + per-word
  audio (qurancdn wbw); **playback speed 0.5×–1.25×** for slow practice
  (applies live while playing). Downloaded in-app per surah / **all surahs**
  (from Settings, or the "📥 Download All — Reciter" button in Audio Manager
  when nothing is downloaded yet), with a **footer progress bar** + the name of
  the surah currently downloading; background downloads (foreground service)
  after the user grants permission.
- 📂 **Downloaded-audio management**: size per surah, delete per surah / delete
  all (with confirmation), **live progress card** while a download is running,
  and a **list cache** — reopening the screen is instant with no re-scan.
- 🌙 **Dark mode** & **ID/EN language switch** via the **Settings** menu; every
  screen has a back (←) button at the top left.
- 🗓️ **"Ayah of the Day" widget + notification** — one ayah that changes daily
  (deterministic per date, offline from the bundled assets). The home-screen
  widget is compact: translation only; the notification shows Arabic + translation.
  Tapping the widget/notification opens the app right at that ayah; daily updates
  via AlarmManager (+ reschedule on boot), toggleable in the Settings menu.
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
- **Toggle** "🗓️ Daily Notification" in the Settings menu (on by default; on
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
│                   #   + TajwidQuiz ("what rule applies to this word?" quiz)
├── data/vocab/     # VocabularyEngine (SRS + quiz) + Repository/Parser
│                   #   (589 curated words → assets/quran/vocab.json)
├── data/dreambig/  # DreamBigGame (arcade rounds); legacy Models/Parser/Repository
│                   #   (level/transcript era) = dead code kept on purpose
├── data/lughoh/    # LughohModels/Parser/Repository/Engine (15 original lessons
│                   #   → assets/lughoh/lessons.json; random practice sessions)
├── data/ayatquiz/  # AyatQuiz (Complete the Ayah) + SurahQuiz (Guess the Surah)
│                   #   — MCQs over the whole mushaf (pure, unit-tested)
├── stt/            # ArabicSpeechRecognizer + TranscriptAligner (Levenshtein)
├── ui/             # TahsinScreen/VM, AudioManagerScreen/VM, StatsScreen/VM
│                   #   (aggregate statistics across all challenges),
│                   #   SearchScreen/VM, TajwidQuizScreen/VM, VocabularyScreen/VM,
│                   #   DreamBigScreen/VM (arcade), LughohScreen/VM (arcade),
│                   #   AyatQuizScreen/VM (Ayah Quiz), BadgesScreen/VM (badges),
│                   #   GamificationViewModel (Home header),
│                   #   SettingsScreen (dark mode, language, download all)
├── widget/         # AyahOfTheDayWidget (AppWidgetProvider) + daily alarm/notification
│                   #   + StreakReminderReceiver (optional streak reminder)
├── util/           # AudioDownloader, AudioUrls, TahsinAudioPlayer (PlaySource),
│                   #   DownloadProgress, DownloadService, FontStore, SettingsStore,
│                   #   ReadingStatsStore (per-ayah reading history, JSON filesDir),
│                   #   VocabularyStatsStore, DreamBigProgressStore, LughohProgressStore,
│                   #   Achievements (8 progressive badges, unlimited tiers),
│                   #   GamificationStore/Hub/Events (XP, level, streak, celebrations),
│                   #   AyahSearch (normalized Arabic + translation search),
│                   #   Reciter (everyayah reciters + 0.5×–1.25× audio speed),
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

### Learning-content pipelines (`tools/`)

The learning features' content is **authored from scratch (original)** via
Python scripts and bundled into the APK — re-run them when content changes:

```bash
python3 tools/build_vocab.py       # 589 curated words → assets/quran/vocab.json
python3 tools/build_lughoh.py      # 15 Learn Arabic lessons → assets/lughoh/lessons.json
                                   #   (11 validation rules; --level N for per-level check)
```

- `tools/lughoh-schema.md` — schema & validation rules for the Learn Arabic data
  (vocab words must appear in the dialogue, grammar examples from the dialogue, etc.).
- `tools/lughoh_en.py` — ENGLISH translations of every Indonesian text (must be
  complete: the build fails if any is missing). Material & tadribat follow the
  app language (ID/EN).
- `tools/curate_vocab.py`, `tools/vocab_roots.py` — word curation & root analysis.
- `tools/scrape_dreambig.py`, `tools/dreambig_levels.py` — legacy Dream BIG
  pipeline (level + YouTube transcript era). **No longer used** since Dream BIG
  became arcade without levels/transcripts; the scripts and transcript assets
  (`assets/dreambig/transcripts/`) are kept in the repo.

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
`ArabicNormalizer`, `QuranParser` (mushaf JSON parsing), `AyahOfTheDayPicker`
(daily ayah selection: index boundaries, cross-surah mapping, determinism,
cache validation), the **Vocabulary** engine (`VocabularyEngine`: SRS + quiz),
**Dream BIG** (`DreamBigGame`: random rounds + stars), **Learn Arabic**
(`LughohParser`/`LughohEngine`: random sessions, option shuffling, word
arrangement), the **Ayah Quiz** (`AyatQuiz`/`SurahQuiz`: Complete-the-Ayah &
Guess-the-Surah), the **gamification** system (`GamificationStore`:
level/streak/todayXp; `Achievements`: progressive badges & tier evaluator),
and all *progress stores* (`ReadingStatsStore`, `VocabularyStatsStore`,
`DreamBigProgressStore`, `LughohProgressStore`):

```bash
./gradlew testDebugUnitTest --no-daemon
```

#### 100% coverage of the correctness-critical core (JaCoCo)

Every piece of logic that decides **text & harakah accuracy** is tested to
**100% lines AND 100% branches** (verified with JaCoCo):

```bash
./gradlew jacocoCoreReport --no-daemon
# Report: app/build/reports/jacoco/core/ (XML + HTML)
```

The "correctness core" = `data/**` (parsers, engines, models, quiz engines) +
pure `util/` (`ArabicNormalizer`, `AyahSearch`, `ReadingStats`, `Reciter`,
`AudioUrls`, `DownloadProgress`, `AppLanguage`, `Achievements`,
`GamificationStore`, `GamificationEvents`, all *progress stores*,
`AyahOfTheDayPicker`) + `stt/TranscriptAligner`. The **mushaf integrity golden
test** (`MushafIntegrityTest`) validates the actual bundled data: 114 surahs,
6,236 ayahs total, per-surah ayah counts exactly matching the standard mushaf,
and no blank texts (Arabic / ID / EN translation) anywhere — if even a single
harakah goes missing or corrupts, this test fails.

**Documented exclusions** (need an emulator/Robolectric): pure Android layers —
`ui/**`, `widget/**`, `theme/**`, `MainActivity`, `DownloadService`,
`TahsinAudioPlayer`, `ArabicSpeechRecognizer`, repositories (asset I/O),
`SettingsStore`, `FontStore`, `GamificationHub` (Context glue),
`AyahOfTheDayManager` (prefs/repository glue — its logic lives in
`AyahOfTheDayPicker`, which is 100% covered).

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
| Ayah audio (multiple reciters: Minshawy, Husary, Alafasy, etc.) | [everyayah.com](https://everyayah.com) | Publicly available |
| Per-word audio (wbw) | audio.qurancdn.com | Publicly available |
| Amiri font (Uthmani) | [Google Fonts](https://fonts.google.com/specimen/Amiri) | SIL OFL 1.1 |

## App permissions

- `RECORD_AUDIO` — recitation scoring (requested when the mic is first pressed).
- `VIBRATE` — vibration on recitation errors (muraja'ah feedback).
- `INTERNET` — audio downloads (mushaf content is bundled).
- `POST_NOTIFICATIONS` — background-download notifications + the daily
  "Ayah of the Day" notification (requested when enabling the Settings toggle).
- `WAKE_LOCK`, `FOREGROUND_SERVICE` (+ `FOREGROUND_SERVICE_DATA_SYNC`) —
  background downloads while the screen is off, after the user grants permission
  via the prompt.
- `RECEIVE_BOOT_COMPLETED` — reschedules the daily "Ayah of the Day" alarm after
  a device restart.

## License

This project is distributed under the **GNU General Public License v3.0** —
see [LICENSE](LICENSE) for the full text.

---

Made with ❤️ by [OpenNur Project (FOSS)](https://github.com/opennur/opennur).
