#!/usr/bin/env bash
# Build APK debug & salin ke folder Download (Termux).
# Pakai: bash build-debug.sh   (atau chmod +x build-debug.sh lalu ./build-debug.sh)
set -euo pipefail
cd "$(dirname "$0")"

echo "==> Membangun APK debug..."
./gradlew assembleDebug --no-daemon

OUT="app/build/outputs/apk/debug/app-debug.apk"
DEST="$HOME/storage/downloads/ayah-of-the-day.apk"
cp "$OUT" "$DEST"
echo "✅ APK debug selesai: $DEST"
