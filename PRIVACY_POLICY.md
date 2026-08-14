# Kebijakan Privasi — Tahsin Quran

Terakhir diperbarui: 2026-08-14

## Ringkasan

Aplikasi **Tahsin Quran** (`org.opennur.tahsin`) adalah aplikasi belajar membaca
Al-Qur'an yang **offline-first**. Aplikasi ini **tidak mengumpulkan, tidak
menyimpan di server, dan tidak membagikan data pribadi apa pun** ke pihak
ketiga untuk tujuan iklan, analitik, atau profil pengguna.

## Data yang dikumpulkan

**Tidak ada.** Seluruh data aplikasi tersimpan **lokal di perangkat**:

- Statistik belajar, XP, level, streak, dan progres;
- Bookmark ayat favorit;
- Pengaturan (qari', kecepatan, bahasa, ukuran huruf, dll.);
- Audio qari' yang diunduh;
- Cache data surah yang pernah diunduh.

Aplikasi tidak membuat akun, tidak meminta login, dan tidak mengirim data ke
server milik pengembang.

## Izin yang digunakan

- **Mikrofon (`RECORD_AUDIO`)** — hanya untuk penilaian bacaan (tahsin) lewat
  speech-to-text. Audio tidak direkam, tidak disimpan, dan tidak dikirim ke
  server pengembang; pemrosesan memakai layanan pengenalan suara perangkat
  (Android SpeechRecognizer).
- **Internet** — untuk mengunduh audio qari' dan data surah dari sumber publik
  (equran.id, everyayah.com, quran.com) atas permintaan pengguna.
- **Notifikasi & alarm** — notifikasi "Ayah of the Day", pengingat streak, dan
  progres unduhan; jadwal harian memakai alarm eksak dengan fallback tidak
  eksak.
- **Boot** — menjadwalkan ulang notifikasi harian setelah perangkat restart.

## Sumber data konten

- Teks Al-Qur'an & terjemahan: **equran.id** dan **quran.com** (Sahih
  International untuk Inggris, Kemenag untuk Indonesia) — data dibundel dalam
  aplikasi dan tersedia offline.
- Audio qari': **everyayah.com** (diunduh atas permintaan pengguna).
- Konten "Keajaiban & Keindahan Al-Qur'an": konten edukasi dengan sumber yang
  dicantumkan di dalam aplikasi (tautan quran.com, NASA, Britannica, jurnal
  akademik, museum, dll.).

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
