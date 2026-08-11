#!/usr/bin/env bash
# Build APK release (ter-sign; fallback ke debug signing kalau keystore.properties
# tidak ada) & salin ke folder Download (Termux).
# Pakai: bash build-release.sh   (atau chmod +x build-release.sh lalu ./build-release.sh)
set -euo pipefail
cd "$(dirname "$0")"

echo "==> Membangun APK release..."
./gradlew assembleRelease --no-daemon

OUT="app/build/outputs/apk/release/app-release.apk"
DEST="$HOME/storage/downloads/ayah-of-the-day-release.apk"
cp "$OUT" "$DEST"
echo "✅ APK release selesai: $DEST"
