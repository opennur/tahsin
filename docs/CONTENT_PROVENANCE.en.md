# Content Provenance and Licensing

Last reviewed: 2026-08-15

This document records where shipped and downloaded content comes from. Public
availability of a URL is not treated as proof that the content is freely
redistributable. A release that changes a source or adds a new reciter must
update this ledger and retain the source terms or written permission.

## Quranic Text

| Content | Production source | Independent validation | Rights status |
|---|---|---|---|
| Arabic text | [equran.id API](https://equran.id/apidev), bundled as `assets/quran/data/surah-*.json` | [Official Qur'an Kemenag/LPMQ API](https://quran.kemenag.go.id/) using `tools/validate_quran_content.py` | Attribution and current non-commercial use are documented; confirm redistribution terms before commercial distribution. |
| Indonesian translation | `teksIndonesia` field in the equran.id response | Ayah count and non-empty coverage are checked by the asset tests | Treat as source-controlled content, not public domain; obtain/record permission for the intended distribution. |
| English translation | [Saheeh International, resource 20, Quran.com API v4](https://api.quran.com) | Ayah count and non-empty coverage are checked by the asset tests | Copyright/license status must be confirmed with the rights holder; the project currently makes no public-domain claim. |

The canonical Arabic comparison is exact after trimming only transport
whitespace. It does not remove harakah, pause marks, or Unicode characters.
The validated snapshot is recorded in
`tools/quran-canonical-manifest.json`; the current bundle contains all 6,236
ayahs in 114 surahs and has the same SHA-256 text digest as the canonical
snapshot.

Run the check after every content regeneration:

```bash
python3 tools/validate_quran_content.py
python3 tools/validate_quran_fields.py --ignore-latin
```

If the canonical source identifies a clear correction, use the guarded repair
mode, inspect the diff, then run the check again:

```bash
python3 tools/validate_quran_content.py --fix --write-manifest
```

Do not hand-edit Arabic asset files. A mismatch must be resolved through the
source pipeline and recorded in the review history.

## Audio

| Content | URL pattern | Current status |
|---|---|---|
| Ayah recitations | [everyayah.com](https://everyayah.com), seven slugs listed in `util/Reciter.kt` | Files are downloaded only after user action. The listing and sample URLs are verified by `tools/verify_reciters.py`; public availability is not a license grant. Confirm attribution and redistribution terms for each reciter before release. |
| Word-by-word recitations | `https://audio.qurancdn.com/wbw/` | Downloaded only after user action. Confirm Quran.com/qurancdn terms and attribution requirements; no unrestricted-license claim is made. |

Audio is cached in the app's private `filesDir`. The app does not upload user
recordings or downloaded files. Incomplete downloads remain in private
`.mp3.part` files and are never exposed to the media player; deleting a surah
or all audio also removes pending download metadata and temporary files.

## Other Content

| Content | Source | License/status |
|---|---|---|
| Amiri/Uthmani font | [Google Fonts: Amiri](https://fonts.google.com/specimen/Amiri) | SIL Open Font License 1.1; retain the license notice. |
| Vocabulary, Learn Arabic lessons, quizzes, and app explanations | OpenNur-authored project content | GPLv3 with the project, unless a file states otherwise. |
| Madani pagination metadata | [alquran.cloud metadata API](https://alquran.cloud/api) | Metadata source attribution is retained in `tools/build_pages.py`; verify upstream terms before separate redistribution. |

## Release Gate

- [ ] `validate_quran_content.py` reports 6,236 matches after shared cleanup.
- [ ] The canonical manifest is regenerated only after reviewing the asset diff.
- [ ] Each translation has a recorded permission or applicable license.
- [ ] Each reciter and word-audio provider has recorded terms and attribution.
- [ ] Tajwid rules have qualified-expert sign-off in `TAJWID_REVIEW.md`.
- [ ] App privacy and store disclosures match the actual sources and permissions.
