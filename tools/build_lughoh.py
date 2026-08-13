#!/usr/bin/env python3
"""
Generator + validator untuk data fitur "📚 Belajar Arab" (metodologi ala
Durusul Lughoh). Konten ORISINAL ditulis manual di tools/lughoh_content_*.py,
skrip ini memvalidasi lalu menulis app/src/main/assets/lughoh/lessons.json.

Skema & aturan validasi: tools/lughoh-schema.md (baca dulu sebelum mengedit!).

Cara pakai:
    python3 tools/build_lughoh.py            # validasi + tulis JSON
    python3 tools/build_lughoh.py --emit X   # tulis ke path lain
    python3 tools/build_lughoh.py --check    # validasi saja, tanpa menulis

Keluar non-zero jika ada kesalahan validasi.
"""

import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUT = REPO_ROOT / "app" / "src" / "main" / "assets" / "lughoh" / "lessons.json"

# Make content modules importable regardless of CWD.
sys.path.insert(0, str(Path(__file__).resolve().parent))

from lughoh_content_l1 import LEVELS as LEVELS_L1  # noqa: E402
from lughoh_content_l2 import LEVELS as LEVELS_L2  # noqa: E402
from lughoh_content_l3 import LEVELS as LEVELS_L3  # noqa: E402

SCHEMA_VERSION = 1

# ---------------------------------------------------------------------------
# Normalisasi Arab — mirror ArabicNormalizer.normalize() di aplikasi:
# buang harakat & tanda mushaf, seragamkan hamza/alif, ratakan spasi.
# ---------------------------------------------------------------------------
HARAKAT_RE = re.compile(r"[\u064B-\u0652\u0670\u06D6-\u06ED\u08D6-\u08ED]")
HAMZA_MAP = {"أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ى": "ي", "ة": "ه", "ء": "", "ـ": ""}


def norm_ar(text: str) -> str:
    s = HARAKAT_RE.sub("", text)
    for src, dst in HAMZA_MAP.items():
        s = s.replace(src, dst)
    return s.strip()


def norm_ar_variants(text: str) -> list[str]:
    """Varian normalisasi: ta marbuta (ة→ه) bisa muncul sebagai ت dalam
    idhafah (mis. عَائِلَة → عَائِلَتِي), jadi cek kedua bentuk."""
    n = norm_ar(text)
    out = [n]
    if n.endswith("ه"):
        out.append(n[:-1] + "ت")
    return out


# ---------------------------------------------------------------------------
# Charset checks
# ---------------------------------------------------------------------------
ARABIC_RE = re.compile(r"^[\u0600-\u06FF\u0750-\u077F\s.!]+$")  # huruf+harakat+angka+punctuation
ARABIC_ANY = re.compile(r"[\u0600-\u06FF\u0750-\u077F]")
LATIN_RE = re.compile(r"^[A-Za-zāīūṣḍṭẓḥʿ'_ .,?!()\-–:;]+$")

ERRORS: list[str] = []


def err(msg: str) -> None:
    ERRORS.append(msg)


# ---------------------------------------------------------------------------
# Validator
# ---------------------------------------------------------------------------
def check_required(lesson: dict, path: str, key: str, kind: str = "str") -> bool:
    val = lesson.get(key)
    if val is None:
        err(f"{path}: field '{key}' hilang")
        return False
    if kind == "str" and not isinstance(val, str):
        err(f"{path}: field '{key}' harus string, dapat {type(val).__name__}")
        return False
    if kind == "str" and not val.strip():
        err(f"{path}: field '{key}' kosong")
        return False
    if kind == "list" and (not isinstance(val, list) or not val):
        err(f"{path}: field '{key}' harus list non-kosong")
        return False
    return True


def check_ar(lesson: dict, path: str, key: str) -> bool:
    if not check_required(lesson, path, key):
        return False
    val = lesson[key]
    # Penanda rumpang "____" pada fillBlank adalah marker UI, bukan konten.
    if not ARABIC_RE.match(val.replace("____", "")):
        err(f"{path}.{key}: bukan teks Arab valid → {val!r}")
        return False
    if re.search(r"[A-Za-z]", val):
        err(f"{path}.{key}: mengandung huruf Latin → {val!r}")
        return False
    return True


def check_latin(lesson: dict, path: str, key: str) -> bool:
    if not check_required(lesson, path, key):
        return False
    val = lesson[key]
    if ARABIC_ANY.search(val):
        err(f"{path}.{key}: transliterasi mengandung huruf Arab → {val!r}")
        return False
    # Huruf kapital berdiakritik (Ṣ, Fā, ...) disamakan ke huruf kecil dulu.
    if not LATIN_RE.match(val.lower()):
        err(f"{path}.{key}: transliterasi memuat karakter tak dikenal → {val!r}")
        return False
    return True


def check_id_text(lesson: dict, path: str, key: str) -> bool:
    if not check_required(lesson, path, key):
        return False
    val = lesson[key]
    if ARABIC_ANY.search(val):
        err(f"{path}.{key}: teks Indonesia mengandung huruf Arab → {val!r}")
        return False
    return True


def check_choices(path: str, options: list, answer: str, script: str = "id") -> None:
    """script='id' → opsi teks Indonesia (tanpa Arab); 'ar' → opsi Arab."""
    if len(options) < 3:
        err(f"{path}: options harus ≥ 3 (dapat {len(options)})")
        return
    seen: set[str] = set()
    for opt in options:
        if not isinstance(opt, str) or not opt.strip():
            err(f"{path}: ada option kosong")
            return
        if opt in seen:
            err(f"{path}: option duplikat → {opt!r}")
            return
        seen.add(opt)
        if script == "ar" and (not ARABIC_RE.match(opt) or re.search(r"[A-Za-z]", opt)):
            err(f"{path}: opsi bukan teks Arab valid → {opt!r}")
        if script == "id" and ARABIC_ANY.search(opt):
            err(f"{path}: opsi Indonesia mengandung huruf Arab → {opt!r}")
    if answer not in options:
        err(f"{path}: answer {answer!r} tidak ada di options")


def validate_lesson(level_id: int, lesson_no: int, lesson: dict) -> None:
    path = f"L{level_id}-{lesson_no}"
    lid = lesson.get("id")
    expect_id = f"{level_id}-{lesson_no}"
    if lid != expect_id:
        err(f"{path}: id lesson harus {expect_id!r}, dapat {lid!r}")

    # --- Metadata ---
    check_id_text(lesson, path, "titleId")
    check_ar(lesson, path, "titleAr")

    # --- Muhadatsah ---
    if not check_required(lesson, path, "muhadatsah", "list"):
        return
    lines = lesson["muhadatsah"]
    if not 8 <= len(lines) <= 12:
        err(f"{path}: jumlah baris dialog {len(lines)} (target 8–12)")
    dialogue_text = ""
    for i, line in enumerate(lines):
        lp = f"{path}.muhadatsah[{i}]"
        check_id_text(line, lp, "speaker")
        if check_ar(line, lp, "ar"):
            dialogue_text += " " + norm_ar(line["ar"])
        check_latin(line, lp, "latin")
        check_id_text(line, lp, "id")

    # --- Mufrodat ---
    if not check_required(lesson, path, "mufrodat", "list"):
        return
    muf = lesson["mufrodat"]
    if len(muf) < 8:
        err(f"{path}: mufrodat hanya {len(muf)} entri (target ≥ 8)")
    for i, w in enumerate(muf):
        wp = f"{path}.mufrodat[{i}]"
        check_ar(w, wp, "ar")
        check_latin(w, wp, "latin")
        check_id_text(w, wp, "id")
        check_ar(w, wp, "exampleAr")
        check_latin(w, wp, "exampleLatin")
        check_id_text(w, wp, "exampleId")
        wparts = w["ar"].split() if w.get("ar") else []
        missing = [
            p for p in wparts
            if not any(v in dialogue_text for v in norm_ar_variants(p))
        ]
        if missing:
            err(f"{wp}: kata {missing} tidak muncul di dialog pelajaran ini")

    # --- Qawa'id ---
    if not check_required(lesson, path, "qawaid", "list"):
        return
    qawa = lesson["qawaid"]
    if len(qawa) < 2:
        err(f"{path}: qawa'id hanya {len(qawa)} kaidah (target ≥ 2)")
    line_texts = [norm_ar(l["ar"]) for l in lines if l.get("ar")]
    for i, g in enumerate(qawa):
        gp = f"{path}.qawaid[{i}]"
        # Penjelasan tata bahasa memuat istilah Arab (mis. 'Kata tanya مَا'),
        # jadi hanya wajib non-kosong, bukan teks Indonesia murni.
        check_required(g, gp, "titleId")
        check_required(g, gp, "id")
        if check_ar(g, gp, "exampleAr"):
            gnorm = norm_ar(g["exampleAr"])
            if gnorm and not any(gnorm in lt for lt in line_texts):
                err(f"{gp}: contoh {g['exampleAr']!r} tidak ada di dialog")
        check_latin(g, gp, "exampleLatin")
        check_id_text(g, gp, "exampleId")

    # --- Tadribat ---
    if not check_required(lesson, path, "tadribat", "list"):
        return
    tad = lesson["tadribat"]
    types_seen: set[str] = set()
    for i, ex in enumerate(tad):
        tp = f"{path}.tadribat[{i}]"
        etype = ex.get("type")
        if etype not in ("fillBlank", "translateArId", "translateIdAr", "rearrange"):
            err(f"{tp}: type tak dikenal → {etype!r}")
            continue
        types_seen.add(etype)
        if etype == "fillBlank":
            check_id_text(ex, tp, "promptId")
            check_ar(ex, tp, "promptAr")
            check_latin(ex, tp, "promptLatin")
            prompt = ex.get("promptAr", "")
            if prompt.count("____") != 1:
                err(f"{tp}: promptAr harus memuat '____' tepat 1× → {prompt!r}")
            if isinstance(ex.get("options"), list) and isinstance(ex.get("answer"), str):
                check_choices(tp, ex["options"], ex["answer"], script="ar")  # isi rumpang Arab
            else:
                err(f"{tp}: options/answer salah tipe")
        elif etype == "translateArId":
            check_ar(ex, tp, "promptAr")
            check_latin(ex, tp, "promptLatin")
            if isinstance(ex.get("options"), list) and isinstance(ex.get("answer"), str):
                check_choices(tp, ex["options"], ex["answer"], script="id")
            else:
                err(f"{tp}: options/answer salah tipe")
        elif etype == "translateIdAr":
            check_id_text(ex, tp, "promptId")
            if isinstance(ex.get("options"), list) and isinstance(ex.get("answer"), str):
                check_choices(tp, ex["options"], ex["answer"], script="ar")
            else:
                err(f"{tp}: options/answer salah tipe")
        elif etype == "rearrange":
            words = ex.get("words")
            answer = ex.get("answer")
            if not isinstance(words, list) or len(words) < 3:
                err(f"{tp}: words harus list ≥ 3 kata")
                continue
            if not isinstance(answer, list):
                err(f"{tp}: answer harus list")
                continue
            ars = []
            for j, w in enumerate(words):
                wp = f"{tp}.words[{j}]"
                check_ar(w, wp, "ar")
                check_latin(w, wp, "latin")
                ars.append(norm_ar(w["ar"]))
            if len(set(ars)) != len(ars):
                err(f"{tp}: ada kata duplikat di words")
            if [norm_ar(a) for a in answer] != ars:
                err(f"{tp}: answer ≠ urutan ar(words)")

    for needed in ("fillBlank", "translateArId", "translateIdAr", "rearrange"):
        if needed not in types_seen:
            err(f"{path}: tidak ada tadribat jenis {needed}")


def validate_levels(levels: list[dict], expect_sequence: bool = True) -> None:
    ids = [lv.get("id") for lv in levels]
    if expect_sequence and ids != list(range(1, len(levels) + 1)):
        err(f"Level ids harus urut 1..{len(levels)}, dapat {ids}")
    for lv in levels:
        lid = lv.get("id")
        path = f"Level {lid}"
        check_id_text(lv, path, "titleId")
        check_ar(lv, path, "titleAr")
        if not check_required(lv, path, "lessons", "list"):
            continue
        seen: set[str] = set()
        for no, lesson in enumerate(lv["lessons"], start=1):
            if not isinstance(lesson, dict):
                err(f"{path}: lesson bukan dict")
                continue
            if lesson.get("id") in seen:
                err(f"{path}: id lesson duplikat → {lesson.get('id')!r}")
            seen.add(lesson.get("id"))
            validate_lesson(lid, no, lesson)


# ---------------------------------------------------------------------------
# Emit
# ---------------------------------------------------------------------------
def emit(levels: list[dict], out: Path) -> None:
    catalog = {
        "schemaVersion": SCHEMA_VERSION,
        "generatedBy": "tools/build_lughoh.py",
        "levels": levels,
    }
    out.parent.mkdir(parents=True, exist_ok=True)
    with open(out, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=1)
        f.write("\n")


def main() -> int:
    args = [a for a in sys.argv[1:]]
    check_only = "--check" in args
    out = DEFAULT_OUT
    if "--emit" in args:
        idx = args.index("--emit")
        if idx + 1 < len(args):
            out = Path(args[idx + 1])

    levels = LEVELS_L1 + LEVELS_L2 + LEVELS_L3
    level_filter = None
    if "--level" in args:
        idx = args.index("--level")
        if idx + 1 < len(args):
            try:
                level_filter = int(args[idx + 1])
            except ValueError:
                print("--level harus angka")
                return 2
        if level_filter is not None:
            levels = [lv for lv in levels if lv["id"] == level_filter]
            check_only = True  # jangan menulis file parsial saat filter aktif
    validate_levels(levels, expect_sequence=level_filter is None)

    n_lessons = sum(len(lv["lessons"]) for lv in levels)
    if ERRORS:
        print(f"VALIDASI GAGAL — {len(ERRORS)} masalah:")
        for e in ERRORS:
            print(f"  ✗ {e}")
        return 1

    if not check_only:
        emit(levels, out)
        print(f"OK — {len(levels)} level, {n_lessons} lesson → {out}")
    else:
        print(f"OK (check saja) — {len(levels)} level, {n_lessons} lesson")
    return 0


if __name__ == "__main__":
    sys.exit(main())
