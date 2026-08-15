# Agent Instructions

## Project Shape

- This is a single Android module, `:app`; the application ID is `org.opennur.tahsin`.
- Use `CONTRIBUTING.en.md` for the full contribution/test rules and `README.en.md` for build/content details; scripts and CI are the executable source of truth.
- `MainActivity` is the single Compose entry point and owns the saveable screen stack; `TahsinApplication` and `di/AppModule.kt` own Hilt setup.
- Quran data flows through `AssetQuranRepository`: bundled assets first, `filesDir/quran` cache second, network fallback last. Tests must not depend on the network.
- Core parsing and feature logic lives under `data/**`, pure persistence/search helpers under `util/**`, speech alignment under `stt/**`, and screens/ViewModels under `ui/**`.
- The app deliberately has no Material 3, Room, Retrofit, or Navigation; use the custom `theme/` and `ui/components/` design system and the existing repository/store abstractions.
- Preferences are DataStore-backed through `DataStores`/`PreferencesStore`/`SettingsStore`; reuse those facades rather than opening another DataStore or using SharedPreferences directly.

## Toolchain And Commands

- Use the Gradle wrapper with Java 17, Gradle 8.6, AGP 8.4, Kotlin 2.0.20, compile/target SDK 35, and min SDK 26; local builds need Android SDK platforms 34 and 35.
- The checked-in `gradle.properties` contains Termux-only `android.aapt2FromMavenOverride` and `android.aidlExecutable` paths. CI removes them before Gradle runs; do not replace them with guessed paths.
- Run JVM/Robolectric tests with `./gradlew testDebugUnitTest --no-daemon`; instrumented `androidTest` tests are not supported in the project environment.
- Run one focused test with `./gradlew testDebugUnitTest --tests 'org.opennur.tahsin.data.quran.QuranAssetsIntegrationTest' --no-daemon`.
- The correctness gate is `./gradlew testDebugUnitTest assembleDebug jacocoCoreReport --no-daemon`; `jacocoCoreReport` must remain at 100% line and branch coverage for its configured core scope.
- Run static checks with `./gradlew detekt --no-daemon` and `./gradlew lintDebug --no-daemon`; new findings fail CI, while historical findings are in the checked-in baselines.
- `./gradlew assembleDebug --no-daemon` and `./gradlew assembleRelease --no-daemon` build without the Termux copy step; `bash build-debug.sh`/`bash build-release.sh` also copy APKs to `$HOME/storage/downloads/` and therefore assume Termux storage exists.

## Quran And Generated Content

- Treat Arabic, harakah, pause marks, transliteration, and translations as correctness-critical content. Do not hand-edit `app/src/main/assets/quran/data/*.json`.
- Regenerate Quran assets with `python3 tools/fetch_quran_data.py` (idempotent) or `--force`; it applies `tools/quran_text_cleaner.py`. After any regeneration, run both `python3 tools/validate_quran_content.py` and `python3 tools/validate_quran_fields.py`.
- `validate_quran_content.py --fix --write-manifest` is the guarded repair path; inspect the asset diff before accepting changes and only regenerate `tools/quran-canonical-manifest.json` after a clean review.
- Build or check derived content through its generator: `python3 tools/build_pages.py --check`, `python3 tools/build_vocab.py`, and `python3 tools/build_lughoh.py --check`/`python3 tools/build_lughoh.py`; read `tools/lughoh-schema.md` before editing Learn Arabic content.
- `pages.json` is Madani metadata for 604 pages and 30 juz; `build_pages.py` must not replace bundled Arabic text. The golden asset tests cover 114 surahs, 6,236 ayahs, pagination, and vocabulary/assets integrity.
- Content-source or license changes require updating `docs/CONTENT_PROVENANCE.en.md`; tajwid rule changes require a regression test and an entry in `docs/TAJWID_REVIEW.md`, whose expert sign-off is still pending.

## UI And Mushaf Invariants

- Mushaf navigation uses `pageIndex` 0..603. `surahNumber`/`ayahIndex` identify the active STT practice ayah, not the current navigation mode.
- Render surah text as the existing flowing `SurahFlowBlock`; do not revert to one ayah per row or bring back the removed flow-playback mode.
- Verse-end and sajdah markers are drawn by the renderer; do not add `۝`/`ࣖ` glyphs, which render as boxes in the runtime font. Draw the active highlight behind text so harakah and waqf signs are not clipped.
- Every user-facing string belongs in `ui/AppStrings.kt` with matching Indonesian and English fields/values; feature ViewModels must refresh language after settings changes rather than caching it only in `init`.
- ViewModels are Hilt `@HiltViewModel` classes with injected constructors; use `di/AppModule.kt` for application-context singletons and construct ViewModels directly in JVM tests, not through Hilt test setup unless the test boots the real app.
- Hilt uses kapt, not KSP, because the development environment requires linux-arm64 support. Release R8/minification remains disabled because it previously caused a launch crash; do not enable it casually.

## Verification And Release

- Data or parser changes must run the real-asset `QuranAssetsIntegrationTest` and the Quran validators; those tests read `app/src/main/assets`, not fixtures.
- New pure logic in the configured correctness core needs tests for every branch so the JaCoCo gate stays at 100% line and branch coverage.
- Releases are tagged `v*`; release builds fall back to debug signing unless an untracked root `keystore.properties` is present. Never commit keystores or signing properties.
- Use `.github/workflows/build.yml` as the CI source of truth: content validation, detekt/lint, JVM tests plus JaCoCo, then debug/release APK builds run in separate jobs.
