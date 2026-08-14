# CONTRIBUTING

Thank you for wanting to contribute to **Tahsin Quran**! This repo touches the
holy Qur'an — **even one wrong harakah is fatal**. Because of that, a few rules
are non-negotiable (see [Golden rules](#golden-rules)).

> 🇮🇩 **Versi Indonesia:** [CONTRIBUTING.md](CONTRIBUTING.md)

## Setting up

The project is developed in **Termux (Android)** — there is no emulator, so
only **JVM unit tests** can run. Do not add instrumented (androidTest) tests
that require an emulator.

```bash
# Install bundled data (once; needs internet)
python3 tools/fetch_quran_data.py       # 114 surahs, Arabic + ID & EN translations
bash tools/fetch_font.sh                # Uthmani font (Amiri, SIL OFL 1.1)

# Build & test
./gradlew testDebugUnitTest --no-daemon
```

## Golden rules

1. **100% coverage gate.** All correctness-critical logic — `data/**` + pure
   `util/**` + `stt/TranscriptAligner` — must be covered **100% line AND
   branch** (JaCoCo). Before a PR:

   ```bash
   ./gradlew testDebugUnitTest assembleDebug jacocoCoreReport --no-daemon
   ```

   `jacocoCoreReport` MUST be green at **100.00% line + 100.00% branch**
   (report: `app/build/reports/jacoco/core/index.html`). The full exclusion
   list lives in `app/build.gradle.kts` (repositories, `SettingsStore`,
   `FontStore`, `TahsinAudioPlayer`, `AudioDownloader`, `DownloadService`,
   `GamificationHub`, `ArabicSpeechRecognizer`, `ui/**`, `widget/**`,
   `theme/**`, `MainActivity`, synthetic `$default`).

2. **Never change bundled data halfway.** Every pipeline has a counterpart
   that must stay in sync (Python script ↔ Kotlin parser ↔ JSON under
   `assets/`):

   | Data | Pipeline | Required sync |
   |---|---|---|
   | Mushaf (Arabic + translations) | `tools/fetch_quran_data.py`, `tools/fetch_font.sh` | don't hand-edit `assets/quran/data/*.json` |
   | Madani pagination (604 pages + 30 juz) | `tools/build_pages.py` → `assets/quran/pages.json` | golden tests `MushafPagesTest`/`MushafPageComposerTest` |
   | Vocabulary (1,200 words) | `tools/build_vocab.py` → `assets/quran/vocab.json` | `VocabKey.normalize` (Kotlin) MUST match the normalizer in `build_vocab.py` exactly |
   | Learn Arabic (15 lessons) | `tools/build_lughoh.py` + `tools/lughoh_en.py` → `assets/lughoh/lessons.json` | the build FAILS if any ID text lacks an EN translation; `LughohEnTest` validates the output |

   After regenerating data, run the unit tests — the golden tests lock the
   integrity (114 surahs / 6236 ayahs, 604 pages, monotonic order, the
   basmalah rule, 15 sajdah verses, text free of ࣖ/۩ font artifacts).

3. **i18n: never hardcode UI strings.** All text goes through `AppStrings.kt`
   (a data class) — **every field must exist in both the ID and EN
   instances**. Use the existing helpers (`AppStrings.badgeTitle/Desc`,
   `AppStrings.sttErrorMessage`) or add a new field in all three places
   (class + ID + EN).

4. **No material3 — use the custom design system.** The app deliberately
   avoids material3: use `AyahColors` / `AyahTypography` / `AyahShapes` and
   the components in `ui/components/` (`AyahButton`, `AyahText`, `AyahCard`,
   `SimpleDropdown`, `AyahErrorView`, etc.). If you need a new component
   (e.g. a slider), build it custom like `FontSizeSlider` — do not import
   material3.

5. **ViewModel pattern (language).** ViewModels are Activity-scoped and
   cached — language captured in `init` goes stale after the user switches
   language in Settings. Every feature screen MUST call
   `LaunchedEffect(viewModel) { viewModel.refreshLanguage() }`, and the VM
   must read `settings.languageCode` inside functions (not only `init`).

6. **Mushaf page (Tahsin) — read the renderer rules before touching.**
   - Navigation uses `pageIndex` (0..603); `surahNumber`/`ayahIndex` = the
     **ACTIVE ayah** (the STT practice target), not navigation.
   - Each surah's text renders **flowing** (`SurahFlowBlock`), not per ayah.
   - Verse-end & sajdah badges are **drawn** (circle + Arabic-Indic digit) —
     do NOT use the ۝/ࣖ glyphs (they render as "[]" boxes in the runtime
     font).
   - The active-ayah highlight is drawn **behind the text** (`drawBehind`) —
     do not use `Modifier.clip(...).background(...)`, which cuts harakah/waqf
     signs.
   - **Flow mode is permanently removed** — do not bring it back. Its
     replacement is the audio playback mode `AudioPlaybackMode { AYAH,
     CONTINUOUS, REPEAT }`.

## Writing tests

- Every new pure function in `data/**` / pure `util/**` needs unit tests
  covering **all branches** (100% branch coverage).
- The mushaf golden tests (`MushafPagesTest`, `MushafPageComposerTest`,
  `MushafIntegrityTest`) lock the actual bundled data — if you change
  `pages.json` or a parser, these tests tell you "no".
- Use clear test names, e.g. `"page 3 starts at 2:6"` — avoid `:` inside
  test names (invalid for JVM tests).

## PR workflow

1. Pick an issue/task, create a branch.
2. Implement + unit tests (see [Golden rules](#golden-rules)).
3. Run the full gate:

   ```bash
   ./gradlew testDebugUnitTest assembleDebug jacocoCoreReport --no-daemon
   ```

   Required: BUILD SUCCESSFUL, all tests green, **CORE LINE 100.00%** and
   **CORE BRANCH 100.00%**.
4. If you touched data/UI that changes behavior or content, update
   `README.md` + `README.en.md` (and this CONTRIBUTING if the rules change).
5. Open a PR with a short description: what changed, why, and the gate result.

## Reporting bugs

Include: reproduction steps, expected vs actual behavior, APK version, and —
if relevant — the affected surah/ayah/page. Harakah/text bugs are the highest
priority (fatal for a holy book).

---

Made with ❤️ by [OpenNur Project (FOSS)](https://github.com/opennur/opennur).
