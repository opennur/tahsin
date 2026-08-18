#!/usr/bin/env python3
"""Validate and generate the offline Shorof course asset."""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app" / "src" / "main" / "assets" / "shorof" / "lessons.json"
sys.path.insert(0, str(Path(__file__).resolve().parent))
from shorof_content import LEVELS  # noqa: E402

ARABIC = re.compile(r"^[\u0600-\u06FF\u0750-\u077F\s.!?،؛:_→]+$")
ARABIC_ANY = re.compile(r"[\u0600-\u06FF\u0750-\u077F]")
ERRORS = []


def required(value, path):
    if not isinstance(value, str) or not value.strip():
        ERRORS.append(f"{path}: wajib diisi")


def arabic(value, path):
    required(value, path)
    if isinstance(value, str) and not ARABIC.match(value):
        ERRORS.append(f"{path}: bukan teks Arab valid")


def latin(value, path):
    required(value, path)
    if isinstance(value, str) and ARABIC_ANY.search(value):
        ERRORS.append(f"{path}: mengandung huruf Arab")


def validate():
    if [level["id"] for level in LEVELS] != list(range(1, len(LEVELS) + 1)):
        ERRORS.append("id level harus berurutan")
    for level in LEVELS:
        for key in ("titleId", "titleEn"):
            required(level.get(key), f"level {level['id']}.{key}")
        arabic(level.get("titleAr"), f"level {level['id']}.titleAr")
        for lesson in level.get("lessons", []):
            path = f"lesson {lesson['id']}"
            for key in ("titleId", "titleEn", "introId", "introEn"):
                required(lesson.get(key), f"{path}.{key}")
            arabic(lesson.get("titleAr"), f"{path}.titleAr")
            if len(lesson.get("rules", [])) < 2:
                ERRORS.append(f"{path}: minimal dua kaidah")
            for i, item in enumerate(lesson.get("rules", [])):
                p = f"{path}.rules[{i}]"
                for key in ("titleId", "titleEn", "explanationId", "explanationEn", "exampleId", "exampleEn"):
                    required(item.get(key), f"{p}.{key}")
                arabic(item.get("exampleAr"), f"{p}.exampleAr")
                latin(item.get("exampleLatin"), f"{p}.exampleLatin")
            for i, pattern in enumerate(lesson.get("patterns", [])):
                p = f"{path}.patterns[{i}]"
                for key in ("root", "wazan", "exampleAr"):
                    arabic(pattern.get(key), f"{p}.{key}")
                for key in ("rootLatin", "wazanLatin", "formId", "formEn", "meaningId", "meaningEn", "exampleLatin"):
                    required(pattern.get(key), f"{p}.{key}")
                    latin(pattern.get(key), f"{p}.{key}") if key in ("rootLatin", "wazanLatin", "exampleLatin") else None
            for i, row in enumerate(lesson.get("conjugations", [])):
                p = f"{path}.conjugations[{i}]"
                arabic(row.get("pronounAr"), f"{p}.pronounAr")
                for key in ("pronounLatin", "past", "present", "imperative"):
                    latin(row.get(key), f"{p}.{key}") if key == "pronounLatin" else arabic(row.get(key), f"{p}.{key}")
            if len(lesson.get("exercises", [])) < 4:
                ERRORS.append(f"{path}: minimal empat latihan")
            for i, exercise in enumerate(lesson.get("exercises", [])):
                p = f"{path}.exercises[{i}]"
                for key in ("promptId", "promptEn"):
                    required(exercise.get(key), f"{p}.{key}")
                arabic(exercise.get("promptAr"), f"{p}.promptAr")
                latin(exercise.get("promptLatin"), f"{p}.promptLatin")
                ids, ens = exercise.get("optionsId", []), exercise.get("optionsEn", [])
                if len(ids) < 3 or len(ids) != len(ens) or exercise.get("answerIndex") not in range(len(ids)):
                    ERRORS.append(f"{p}: opsi atau jawaban tidak valid")
    if ERRORS:
        raise SystemExit("\n".join(ERRORS))


def main():
    validate()
    if "--check" not in sys.argv:
        OUT.write_text(json.dumps({"schemaVersion": 1, "generatedBy": "tools/build_shorof.py", "levels": LEVELS}, ensure_ascii=False, indent=1) + "\n")
    print(f"Shorof valid: {sum(len(level['lessons']) for level in LEVELS)} lessons")


if __name__ == "__main__":
    main()
