#!/data/data/com.termux/files/usr/bin/bash
# ---------------------------------------------------------------------------
# Unduh audio contoh bacaan ke app/src/main/assets/audio/ — supaya aplikasi
# Tahsin bisa memutar SEPENUHNYA OFFLINE:
#   1) audio per AYAT: Minshawy (Murattal) dari everyayah.com
#      → assets/audio/<surah3><ayat3>.mp3
#   2) audio per KATA: word-by-word dari quran.com (qurancdn.com)
#      → assets/audio/wbw/<surah3>_<ayat3>_<kata3>.mp3
#
# Cara pakai (dari root project ~/AyahOfTheDay):
#   bash tools/download_minshawi.sh
# lalu build ulang APK.
# ---------------------------------------------------------------------------
set -uo pipefail

DEST="app/src/main/assets/audio"
mkdir -p "$DEST" "$DEST/wbw"

# ---------- 1) Audio per ayat: Minshawy Murattal ----------
AYAT_FILES="
001001 001002 001003 001004 001005 001006 001007
103001 103002 103003
112001 112002 112003 112004
113001 113002 113003 113004 113005
114001 114002 114003 114004 114005 114006
"
AYAT_BASE="https://everyayah.com/data/Minshawy_Murattal_128kbps"

# ---------- 2) Audio per kata (quran.com wbw) ----------
# Format: <surah> <ayat> <jumlah kata>
AYAT_WORDS="
001 1 4
001 2 4
001 3 2
001 4 3
001 5 4
001 6 3
001 7 9
103 1 1
103 2 4
103 3 9
112 1 4
112 2 2
112 3 4
112 4 5
113 1 4
113 2 4
113 3 5
113 4 5
113 5 5
114 1 4
114 2 2
114 3 2
114 4 4
114 5 5
114 6 3
"
WBW_BASE="https://audio.qurancdn.com/wbw"

ok=0
fail=0

download() {
  local url="$1" out="$2"
  if curl -L --fail --silent --show-error "$url" -o "$out"; then
    ok=$((ok + 1))
  else
    echo "GAGAL $url"
    fail=$((fail + 1))
  fi
}

echo "== Audio per ayat (Minshawy Murattal) =="
for f in $AYAT_FILES; do
  download "${AYAT_BASE}/${f}.mp3" "${DEST}/${f}.mp3"
done

echo "== Audio per kata (quran.com wbw) =="
while read -r surah ayah nwords; do
  [ -z "$surah" ] && continue
  for w in $(seq 1 "$nwords"); do
    key=$(printf "%03d_%03d_%03d" "$surah" "$ayah" "$w")
    download "${WBW_BASE}/${key}.mp3" "${DEST}/wbw/${key}.mp3"
  done
done <<< "$AYAT_WORDS"

total=$(find "$DEST" -name '*.mp3' | wc -l)
echo "----------------------------------------"
echo "Selesai: $ok berhasil, $fail gagal. Total file MP3 di assets/audio: $total"
if [ "$fail" -gt 0 ]; then
  echo "Ada yang gagal — cek koneksi lalu jalankan ulang."
  exit 1
fi
echo "Langkah berikutnya: ./gradlew assembleDebug --no-daemon"
