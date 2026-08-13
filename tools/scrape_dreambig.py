#!/usr/bin/env python3
"""Scrape & normalize YouTube auto-subtitles (en ASR) for the
"Dream BIG: 10-Day Live Arabic Intensive" playlist (Bayyinah / Nouman Ali Khan).

Pipeline (mengikuti pola tools/build_vocab.py):
  1. fetch_playlist_index() — innertube browse API, parse lockupViewModel
     -> daftar (videoId, judul, day, part).
  2. download_subtitles()   — yt-dlp --skip-download --write-auto-subs
     (sub-langs en.*, format json3) ke build/dreambig_raw/.
  3. normalize()            — json3 -> app/src/main/assets/dreambig/:
     index.json (indeks video) + transcripts/<videoId>.json (segmen).

Catatan konvensi:
  - Script HANYA mengekstrak data mentah (per baris caption ASR); tidak ada
    pengelompokan/paragraf — semua logika presentasi ada di sisi Kotlin
    (hindari aturan mirror ganda seperti vocab).
  - Idempoten: file transkrip yang sudah ada dilewati kecuali --force.

Usage:
  python3 tools/scrape_dreambig.py             # jalankan (skip yang sudah ada)
  python3 tools/scrape_dreambig.py --force     # unduh ulang + normalisasi ulang
  python3 tools/scrape_dreambig.py --index-only
"""

import argparse
import json
import os
import re
import subprocess
import sys
import urllib.request

PLAYLIST_ID = "PLutdSTmJ7bAIApzbo3C9vu1eWsMh2ZyUj"
PLAYLIST_TITLE = "Dream BIG: 10-Day Live Arabic Intensive"
SOURCE_LABEL = "youtube-auto-subs (en, asr)"

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "assets", "dreambig")
TRANSCRIPTS_DIR = os.path.join(OUT_DIR, "transcripts")
INDEX_PATH = os.path.join(OUT_DIR, "index.json")
RAW_DIR = os.path.join(ROOT, "build", "dreambig_raw")

YTDLP = "yt-dlp"
CLIENT = {"clientName": "WEB", "clientVersion": "2.20240101.00.00", "hl": "en"}

# Baris penanda ASR YouTube (terlokalisasi jadi "[Foreign language]" dll.) — dilewati.
MARKER_LINES = {"foreign", "music", "applause", "laughter", "noise", "inaudible"}

DAY_RE = re.compile(r"\bDay\s*(\d{1,2})\b", re.IGNORECASE)
PART_RE = re.compile(r"\bPart\s*(\d{1,2})\b", re.IGNORECASE)


# ---------------------------------------------------------------------------
# 1. Indeks playlist via innertube browse API
# ---------------------------------------------------------------------------

def _browse(payload: dict) -> dict:
    req = urllib.request.Request(
        "https://www.youtube.com/youtubei/v1/browse?prettyPrint=false",
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json", "User-Agent": "Mozilla/5.0"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _walk_lockups(obj: dict, out: list) -> None:
    """Kumpulkan semua lockupViewModel (layout playlist YouTube terbaru)."""
    if isinstance(obj, dict):
        lv = obj.get("lockupViewModel")
        if isinstance(lv, dict):
            out.append(lv)
        for v in obj.values():
            _walk_lockups(v, out)
    elif isinstance(obj, list):
        for item in obj:
            _walk_lockups(item, out)


def _continuation_token(obj: dict) -> str | None:
    found = []

    def walk(o):
        if isinstance(o, dict):
            cc = o.get("continuationCommand")
            if isinstance(cc, dict) and cc.get("token"):
                found.append(cc["token"])
            for v in o.values():
                walk(v)
        elif isinstance(o, list):
            for item in o:
                walk(item)

    walk(obj)
    return found[0] if found else None


def parse_video_entry(lv: dict) -> dict:
    """Extract (videoId, title, day, part) dari satu lockupViewModel."""
    content_id = lv.get("contentId") or ""
    md = lv.get("metadata") or {}
    title = ((md.get("lockupMetadataViewModel") or {}).get("title") or {}).get("content") or ""
    # Buang akhiran channel " | Nouman Ali Khan"
    title = re.split(r"\s*\|\s*", title, maxsplit=1)[0].strip()
    day_m = DAY_RE.search(title)
    part_m = PART_RE.search(title)
    return {
        "videoId": content_id,
        "title": title,
        "day": int(day_m.group(1)) if day_m else 0,
        "part": int(part_m.group(1)) if part_m else 0,
    }


def fetch_playlist_index(playlist_id: str = PLAYLIST_ID) -> list[dict]:
    videos: dict[str, dict] = {}
    token: str | None = None
    for _ in range(20):  # aman: maks 20 halaman
        payload = {
            "context": {"client": CLIENT},
            "browseId": "VL" + playlist_id,
        }
        if token:
            payload["continuation"] = token
        obj = _browse(payload)
        lockups: list[dict] = []
        _walk_lockups(obj, lockups)
        for lv in lockups:
            entry = parse_video_entry(lv)
            if entry["videoId"]:
                videos[entry["videoId"]] = entry
        token = _continuation_token(obj)
        if not token:
            break
    return sorted(videos.values(), key=lambda v: (v["day"], v["part"]))


# ---------------------------------------------------------------------------
# 2. Download subtitle via yt-dlp
# ---------------------------------------------------------------------------

def _pick_raw_subtitle(video_id: str) -> str | None:
    """Prefer track asli (en-orig), fallback en; balikin path file json3."""
    for name in (f"{video_id}.en-orig.json3", f"{video_id}.en.json3"):
        path = os.path.join(RAW_DIR, name)
        if os.path.exists(path) and os.path.getsize(path) > 0:
            return path
    return None


def download_subtitles(videos: list[dict], force: bool = False) -> list[dict]:
    os.makedirs(RAW_DIR, exist_ok=True)
    pending = []
    for v in videos:
        vid = v["videoId"]
        if not force and _pick_raw_subtitle(vid):
            pending.append(v)
            continue
        url = f"https://www.youtube.com/watch?v={vid}"
        for attempt in range(2):
            proc = subprocess.run(
                [
                    YTDLP, "--skip-download", "--write-auto-subs",
                    "--sub-langs", "en.*", "--sub-format", "json3",
                    "--no-warnings", "-o", os.path.join(RAW_DIR, "%(id)s"), url,
                ],
                capture_output=True, text=True,
            )
            if proc.returncode == 0 and _pick_raw_subtitle(vid):
                break
            print(f"  [warn] yt-dlp gagal ({attempt + 1}x): {vid}\n{proc.stderr[-400:]}",
                  file=sys.stderr)
        else:
            print(f"  [skip] subtitle tidak bisa diunduh: {vid}", file=sys.stderr)
        pending.append(v)
    return pending


# ---------------------------------------------------------------------------
# 3. Normalisasi json3 -> transkrip terkurasi
# ---------------------------------------------------------------------------

def _clean_segment_text(text: str) -> str | None:
    text = text.strip()
    if not text:
        return None
    lower = text.lower()
    if lower in MARKER_LINES:
        return None
    if text.startswith("[") and text.endswith("]"):
        return None  # penanda ASR lokal, mis. "[Foreign language]"
    return text


def normalize_transcript(video_id: str, raw_path: str) -> dict:
    with open(raw_path, encoding="utf-8") as f:
        raw = json.load(f)
    events = raw.get("events") or []
    duration_ms = None
    segments = []
    prev_text = None
    for ev in events:
        segs = ev.get("segs")
        if not segs:
            if ev.get("dDurationMs") and duration_ms is None:
                duration_ms = int(ev["dDurationMs"])  # event pertama = durasi video
            continue
        text = _clean_segment_text("".join(s.get("utf8", "") for s in segs))
        if text is None or text == prev_text:
            continue  # buang kosong/marker + duplikat berurutan (umum di ASR)
        prev_text = text
        start_ms = ev.get("tStartMs")
        if start_ms is None:
            continue
        segments.append({"startMs": int(start_ms), "durationMs": None, "text": text})

    # durationMs per segmen = jarak ke segmen berikutnya (terakhir null)
    for i in range(len(segments) - 1):
        dur = segments[i + 1]["startMs"] - segments[i]["startMs"]
        segments[i]["durationMs"] = max(dur, 0)
    return {
        "videoId": video_id,
        "source": SOURCE_LABEL,
        "durationMs": duration_ms,
        "segments": segments,
    }


def normalize_all(videos: list[dict], force: bool = False) -> list[dict]:
    os.makedirs(TRANSCRIPTS_DIR, exist_ok=True)
    done = []
    for v in videos:
        vid = v["videoId"]
        raw_path = _pick_raw_subtitle(vid)
        if raw_path is None:
            print(f"  [skip] raw subtitle tidak ada: {vid}", file=sys.stderr)
            continue
        out_path = os.path.join(TRANSCRIPTS_DIR, f"{vid}.json")
        if not force and os.path.exists(out_path):
            done.append(v)
            continue
        transcript = normalize_transcript(vid, raw_path)
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(transcript, f, ensure_ascii=False, indent=1)
        print(f"  [ok] {vid} -> {os.path.relpath(out_path, ROOT)} "
              f"({len(transcript['segments'])} segmen)")
        done.append(v)
    return done


def write_index(videos: list[dict]) -> None:
    index = {
        "playlistId": PLAYLIST_ID,
        "title": PLAYLIST_TITLE,
        "source": SOURCE_LABEL,
        "videos": [
            {
                "videoId": v["videoId"],
                "day": v["day"],
                "part": v["part"],
                "title": v["title"],
                "watchUrl": f"https://www.youtube.com/watch?v={v['videoId']}",
                # Relatif ke root assets/ (konvensi DreamBigModels.kt & repository).
                "transcript": f"dreambig/transcripts/{v['videoId']}.json",
            }
            for v in videos
        ],
    }
    with open(INDEX_PATH, "w", encoding="utf-8") as f:
        json.dump(index, f, ensure_ascii=False, indent=1)
    print(f"[ok] {os.path.relpath(INDEX_PATH, ROOT)} ({len(videos)} video)")


# ---------------------------------------------------------------------------

def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--force", action="store_true", help="unduh ulang + normalisasi ulang semua")
    ap.add_argument("--index-only", action="store_true", help="hanya tulis index.json (pakai cache raw)")
    args = ap.parse_args()

    print("[1/3] Ambil indeks playlist...")
    videos = fetch_playlist_index()
    if not videos:
        print("  [error] indeks playlist kosong", file=sys.stderr)
        return 1
    for v in videos:
        part = f" (Part {v['part']})" if v["part"] else ""
        print(f"  - Day {v['day']}{part}: {v['videoId']} — {v['title']}")

    if not args.index_only:
        print("[2/3] Download subtitle (yt-dlp)...")
        videos = download_subtitles(videos, force=args.force)
        print("[3/3] Normalisasi -> assets/dreambig/...")
        videos = normalize_all(videos, force=args.force)

    write_index(videos)
    total = sum(
        os.path.getsize(os.path.join(TRANSCRIPTS_DIR, f"{v['videoId']}.json"))
        for v in videos
        if os.path.exists(os.path.join(TRANSCRIPTS_DIR, f"{v['videoId']}.json"))
    )
    print(f"Selesai. {len(videos)} video, total transkrip {total / 1024:.0f} KiB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
