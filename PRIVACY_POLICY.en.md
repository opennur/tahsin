# Privacy Policy — Tahsin Quran

Last updated: 2026-08-15

## Summary

**Tahsin Quran** (`org.opennur.tahsin`) is an **offline-first** app for learning
to read the Qur'an. It **does not collect, store on any server, or share any
personal data** with third parties for advertising, analytics, or user
profiling purposes.

## Data collected

**None.** All app data is stored **locally on your device**:

- Learning stats, XP, level, streak, and progress;
- Learning goal, daily plan completion, and memorization review metadata;
- Favorite ayah bookmarks;
- Settings (reciter, speed, language, font size, etc.);
- Downloaded reciter audio;
- Cache of downloaded surah data.
- Pending audio-download metadata and temporary `.mp3.part` files used for
  crash-safe resume.

The app has no accounts, no login, and sends no data to developer servers.

## Permissions used

- **Microphone (`RECORD_AUDIO`)** — used only for recitation (tahsin) scoring
  via speech-to-text. Audio is not recorded, stored, or sent to developer
  servers; processing uses Android's speech-recognition provider. That provider
  may have its own network and privacy policy, which is outside this app's
  control.
- **Internet** — used to download reciter audio and surah data from public
  sources (equran.id, everyayah.com, quran.com) at the user's request.
- **Notifications & alarms** — "Ayah of the Day" notification, streak reminder,
  and download progress; daily scheduling uses an exact alarm with an inexact
  fallback.
- **Boot** — rescheduling daily notifications after device restart.

## Content data sources

The complete source, canonical-text validation, translation rights status, and
audio attribution requirements are maintained in
[docs/CONTENT_PROVENANCE.en.md](docs/CONTENT_PROVENANCE.en.md).

- Qur'an Arabic and Indonesian data: **equran.id**, audited against official
  **Qur'an Kemenag/LPMQ** data before release.
- English translation: **Saheeh International, resource 20, Quran.com API**.
- Reciter audio: **everyayah.com**; word-by-word audio:
  **audio.qurancdn.com**.

## Data deletion

Because all data is stored locally, users can delete everything via
**Android Settings → Apps → Tahsin Quran → Clear Data** (or by uninstalling
the app). There is no developer-server data to delete.

## Children's policy

The app is intended for all ages (religious education content). Per Google Play
policy, since the app **does not collect personal data**, the special "Families"
requirements do not affect any data collection.

## Policy changes

Policy changes will be reflected on this page with a new revision date.

## Contact

Questions about this privacy policy can be submitted through the app's page on
Google Play (developer contact section).
