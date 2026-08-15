# Tajwid Rule Review

## Status

**Pending qualified-expert sign-off.** The current engine is deterministic text
annotation, not a fatwa, teacher, or audio tajwid assessor. No release should
describe these annotations as independently certified until the review table
below has been completed.

## Scope

The implementation under review is `app/src/main/java/org/opennur/tahsin/data/tajwid/TajwidEngine.kt`.
The review must cover every branch and explanation for:

- Nun sakin and tanwin: izhar halqi, idgham bighunnah, idgham bilaghunnah, ikhfa haqiqi, and iqlab.
- Ghunnah for nun/meem mushaddad and the generic shaddah annotation.
- Mad thabi'i, mad wajib muttasil, mad jaiz munfasil, mad badal, mad iwad, and mad aridh lis-sukun.
- Qalqalah.
- Lam jalalah tafkhim and tarqiq.
- Tafkhim for isti'la letters and tafkhim/tarqiq for ra'.
- Mushaf waqaf signs: lazim, laa, jaiz, wasl aula, waqaf aula, and mu'anaqah.

## Required Review Method

1. The reviewer must be a qualified Qur'an teacher, qari, or tajwid
   instructor with documented study or certification in the riwayah used by
   the app.
2. Review the actual Uthmani asset text, not only synthetic unit-test words.
   Sample all categories across early, middle, and late surahs, including
   cross-word rules and waqaf signs.
3. For every finding, record surah, ayah, word, expected rule, observed rule,
   and the evidence or reference used to resolve it.
4. Add a regression test before changing the engine or bundled data.
5. Re-run the full correctness gate and have a second reviewer confirm changes
   affecting Arabic text or harakah.

## Sign-Off Ledger

| Area | Reviewer and qualification | Date | Evidence/tests | Status |
|---|---|---|---|---|
| Nun sakin/tanwin |  |  |  | Pending |
| Ghunnah/shaddah |  |  |  | Pending |
| Mad rules |  |  |  | Pending |
| Qalqalah |  |  |  | Pending |
| Lam jalalah |  |  |  | Pending |
| Tafkhim/tarqiq |  |  |  | Pending |
| Waqaf signs |  |  |  | Pending |
| Full-asset spot check |  |  |  | Pending |

Until this ledger is signed, user-facing copy must retain the limitation that
the engine derives a rule map from text and cannot judge makhraj, voice quality,
or vowel length from microphone audio.
