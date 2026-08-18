# Schema `assets/nahwu/lessons.json`

Materi Nahwu adalah konten offline orisinal untuk pemula. Setiap teks Indonesia
memiliki pasangan Inggris, sedangkan contoh Arab memakai harakat agar mudah
dibaca. Sumber materi berada di `tools/nahwu_content.py` dan JSON dihasilkan
oleh `tools/build_nahwu.py`.

Setiap lesson memiliki `rules` dan `exercises`. Latihan `choice` memakai indeks
jawaban yang sama untuk pasangan `optionsId` dan `optionsEn`. Latihan
`rearrange` menyimpan kata dalam urutan jawaban yang benar.
