#!/usr/bin/env python3
"""Validate the bundled Madani Uthmani 15-line layout contract."""

import json
import sys
from pathlib import Path


EXPECTED_PAGES = 604
EXPECTED_LINES = 15
EXPECTED_EDITION = "Madani"
EXPECTED_SCRIPT = "Uthmani"


def validate(path: Path) -> list[str]:
    errors: list[str] = []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return [f"cannot read layout manifest: {exc}"]

    if data.get("pageCount") != EXPECTED_PAGES:
        errors.append(f"pageCount must be {EXPECTED_PAGES}")
    if data.get("linesPerPage") != EXPECTED_LINES:
        errors.append(f"linesPerPage must be {EXPECTED_LINES}")
    if data.get("edition") != EXPECTED_EDITION:
        errors.append(f"edition must be {EXPECTED_EDITION}")
    if data.get("script") != EXPECTED_SCRIPT:
        errors.append(f"script must be {EXPECTED_SCRIPT}")
    if not data.get("referenceFont"):
        errors.append("referenceFont is required")
    for name in ("phone", "tablet"):
        viewport = data.get(name)
        if not isinstance(viewport, dict):
            errors.append(f"{name} viewport is required")
            continue
        for key in ("widthDp", "pageAspect", "lineHeightSp"):
            if not isinstance(viewport.get(key), (int, float)) or viewport[key] <= 0:
                errors.append(f"{name}.{key} must be positive")
    return errors


def main() -> int:
    path = Path("app/src/main/assets/quran/mushaf-layout.json")
    errors = validate(path)
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print(f"OK: {EXPECTED_PAGES} pages, {EXPECTED_LINES} lines, Madani Uthmani")
    return 0


if __name__ == "__main__":
    sys.exit(main())
