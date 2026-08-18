# Kebijakan Privasi — Tahsin Quran

Terakhir diperbarui: 2026-08-15

## Ringkasan

Aplikasi **Tahsin Quran** (`org.opennur.tahsin`) adalah aplikasi belajar membaca
Al-Qur'an yang **offline-first**. Aplikasi ini **tidak mengumpulkan, tidak
menyimpan di server, dan tidak membagikan data pribadi apa pun** ke pihak
ketiga untuk tujuan iklan, analitik, atau profil pengguna.

## Data yang dikumpulkan

**Tidak ada.** Seluruh data aplikasi tersimpan **lokal di perangkat**:

- Statistik belajar, XP, level, streak, dan progres;
- Fokus belajar, penyelesaian rencana harian, dan metadata pengulangan hafalan;
- Bookmark ayat favorit;
- Pengaturan (qari', kecepatan, bahasa, ukuran huruf, dll.);
- Audio qari' yang diunduh;
- Cache data surah yang pernah diunduh.
- Metadata antrean unduhan audio dan file sementara `.mp3.part` untuk
  pemulihan setelah aplikasi mati.
- Gambar ringkasan progres hanya dibuat saat pengguna menekan tombol bagikan; gambar
  berisi agregat anonim dan tidak berisi audio atau transcript suara.

Aplikasi tidak membuat akun, tidak meminta login, dan tidak mengirim data ke
server milik pengembang.

## Izin yang digunakan

- **Mikrofon (`RECORD_AUDIO`)** — hanya untuk penilaian bacaan (tahsin) lewat
  speech-to-text. Audio tidak direkam, tidak disimpan, dan tidak dikirim ke
  server pengembang; pemrosesan memakai penyedia pengenalan suara Android.
  Penyedia tersebut dapat memiliki kebijakan jaringan dan privasi sendiri di
  luar kendali aplikasi ini.
- **Internet** — untuk mengunduh audio qari' dan data surah dari sumber publik
  (equran.id, everyayah.com, quran.com) atas permintaan pengguna.
- **Notifikasi & alarm** — notifikasi "Ayah of the Day", pengingat streak, dan
  progres unduhan; jadwal harian memakai alarm eksak dengan fallback tidak
  eksak.
- **Boot** — menjadwalkan ulang notifikasi harian setelah perangkat restart.

## Sumber data konten

Sumber lengkap, validasi teks kanonik, status hak terjemahan, dan kebutuhan
atribusi audio dicatat di
[docs/CONTENT_PROVENANCE.en.md](docs/CONTENT_PROVENANCE.en.md).

- Teks Arab dan Indonesia: **equran.id**, diaudit terhadap data resmi
  **Qur'an Kemenag/LPMQ** sebelum rilis.
- Terjemahan Inggris: **Saheeh International, resource 20, API Quran.com**.
- Audio qari': **everyayah.com**; audio per kata:
  **audio.qurancdn.com**.

## Penghapusan data

Karena semua data tersimpan lokal, pengguna dapat menghapus seluruh data dengan
membuka **Pengaturan Android → Aplikasi → Tahsin Quran → Hapus Data** (atau
menghapus aplikasi). Tidak ada data di server pengembang yang perlu dihapus.

## Kebijakan anak-anak

Aplikasi ditujukan untuk semua usia (konten edukasi agama). Sesuai dengan
kebijakan Google Play, karena aplikasi **tidak mengumpulkan data pribadi**,
persyaratan khusus "Families" tidak memengaruhi pengumpulan data apa pun.

## Perubahan kebijakan

Perubahan kebijakan akan diperbarui di halaman ini dengan tanggal revisi baru.

## Kontak

Pertanyaan tentang kebijakan privasi dapat diajukan melalui halaman aplikasi di
Google Play (bagian kontak pengembang).
