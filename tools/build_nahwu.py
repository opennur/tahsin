#!/usr/bin/env python3
"""Validate and generate the offline Nahwu course asset."""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app" / "src" / "main" / "assets" / "nahwu" / "lessons.json"
sys.path.insert(0, str(Path(__file__).resolve().parent))
from nahwu_content import LEVELS  # noqa: E402

ARABIC = re.compile(r"^[\u0600-\u06FF\u0750-\u077F\s.!?،؛:_]+$")
ARABIC_ANY = re.compile(r"[\u0600-\u06FF\u0750-\u077F]")
ERRORS = []


def required(value, path):
    if not isinstance(value, str) or not value.strip():
        ERRORS.append(f"{path}: wajib diisi")


def arabic(value, path):
    required(value, path)
    if isinstance(value, str) and (not ARABIC.match(value) or not value.strip()):
        ERRORS.append(f"{path}: bukan teks Arab valid")


def latin(value, path):
    required(value, path)
    if isinstance(value, str) and ARABIC_ANY.search(value):
        ERRORS.append(f"{path}: mengandung huruf Arab")


def validate():
    level_ids = [level.get("id") for level in LEVELS]
    if level_ids != list(range(1, len(LEVELS) + 1)):
        ERRORS.append("id level harus berurutan")
    lesson_ids = set()
    for level in LEVELS:
        path = f"level {level.get('id')}"
        for key in ("titleId", "titleEn", "titleAr"):
            required(level.get(key), f"{path}.{key}") if key != "titleAr" else arabic(level.get(key), f"{path}.{key}")
        for lesson in level.get("lessons", []):
            lp = f"{path} lesson {lesson.get('id')}"
            expected = f"{level.get('id')}-{len([x for x in level.get('lessons', []) if x is not lesson]) + 1}"
            if lesson.get("id") in lesson_ids:
                ERRORS.append(f"{lp}: id duplikat")
            lesson_ids.add(lesson.get("id"))
            for key in ("titleId", "titleEn", "introId", "introEn"):
                required(lesson.get(key), f"{lp}.{key}")
            arabic(lesson.get("titleAr"), f"{lp}.titleAr")
            if len(lesson.get("rules", [])) < 2:
                ERRORS.append(f"{lp}: minimal dua kaidah")
            for index, rule in enumerate(lesson.get("rules", [])):
                rp = f"{lp}.rules[{index}]"
                for key in ("titleId", "titleEn", "explanationId", "explanationEn", "exampleId", "exampleEn"):
                    required(rule.get(key), f"{rp}.{key}")
                arabic(rule.get("exampleAr"), f"{rp}.exampleAr")
                latin(rule.get("exampleLatin"), f"{rp}.exampleLatin")
            if len(lesson.get("exercises", [])) < 4:
                ERRORS.append(f"{lp}: minimal empat latihan")
            seen_exercises = set()
            for index, exercise in enumerate(lesson.get("exercises", [])):
                ep = f"{lp}.exercises[{index}]"
                signature = json.dumps(exercise, ensure_ascii=False, sort_keys=True)
                if signature in seen_exercises:
                    ERRORS.append(f"{ep}: soal duplikat")
                seen_exercises.add(signature)
                required(exercise.get("promptId"), f"{ep}.promptId")
                required(exercise.get("promptEn"), f"{ep}.promptEn")
                if exercise.get("type") == "choice":
                    arabic(exercise.get("promptAr"), f"{ep}.promptAr")
                    latin(exercise.get("promptLatin"), f"{ep}.promptLatin")
                    options_id = exercise.get("optionsId", [])
                    options_en = exercise.get("optionsEn", [])
                    if len(options_id) < 3 or len(options_id) != len(options_en):
                        ERRORS.append(f"{ep}: pilihan tidak valid")
                    if exercise.get("answerIndex") not in range(len(options_id)):
                        ERRORS.append(f"{ep}: indeks jawaban tidak valid")
                elif exercise.get("type") == "rearrange":
                    words = exercise.get("words", [])
                    if len(words) < 2:
                        ERRORS.append(f"{ep}: minimal dua kata")
                    for word_index, word in enumerate(words):
                        arabic(word.get("ar"), f"{ep}.words[{word_index}].ar")
                        latin(word.get("latin"), f"{ep}.words[{word_index}].latin")
                else:
                    ERRORS.append(f"{ep}: tipe tidak dikenal")
    if ERRORS:
        raise SystemExit("\n".join(ERRORS))


def main():
    validate()
    if "--check" not in sys.argv:
        OUT.parent.mkdir(parents=True, exist_ok=True)
        OUT.write_text(json.dumps({"schemaVersion": 1, "generatedBy": "tools/build_nahwu.py", "levels": LEVELS}, ensure_ascii=False, indent=1) + "\n")
    print(f"Nahwu valid: {sum(len(level['lessons']) for level in LEVELS)} lessons")


if __name__ == "__main__":
    main()
