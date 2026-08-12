#!/usr/bin/env bash
# Unduh font khat Utsmani (Amiri, lisensi OFL) ke assets agar di-bundle ke APK —
# mushaf langsung tampil dengan gaya Utsmani tanpa unduhan pertama kali.
#
# Output: app/src/main/assets/fonts/uthmani.ttf
# Jalankan sekali di Termux sebelum build:
#   bash tools/fetch_font.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$ROOT/app/src/main/assets/fonts"
URL="https://raw.githubusercontent.com/google/fonts/main/ofl/amiri/Amiri-Regular.ttf"

mkdir -p "$DIR"
if command -v curl >/dev/null 2>&1; then
  curl -L --fail --retry 3 -o "$DIR/uthmani.ttf" "$URL"
else
  wget -O "$DIR/uthmani.ttf" "$URL"
fi

echo "OK → $DIR/uthmani.ttf ($(du -h "$DIR/uthmani.ttf" | cut -f1))"
