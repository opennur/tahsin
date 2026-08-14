# ProGuard/R8 rules untuk release build — SAAT INI DORMANT.
# R8 dimatikan total untuk stabilitas (lihat app/build.gradle.kts:
# isMinifyEnabled = false + isShrinkResources = false).
#
# File ini dipertahankan UTUH supaya R8 mudah diaktifkan kembali suatu saat —
# idealnya setelah root cause crash release didiagnosis lewat logcat.
# Selama minify mati, seluruh aturan di bawah TIDAK berpengaruh.
-dontobfuscate
-dontoptimize

# ---- Gson (refleksi + generics) ----
# Model yang diserialisasi Gson (JSON aset & store filesDir) di-reflect oleh
# nama field — pertahankan agar tidak di-rename/di-buang.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# Model mushaf & konten (assets/quran/*.json, filesDir store JSON).
-keep class org.opennur.tahsin.data.quran.** { *; }
-keep class org.opennur.tahsin.data.ayatquiz.** { *; }
-keep class org.opennur.tahsin.data.dreambig.** { *; }
-keep class org.opennur.tahsin.data.lughoh.** { *; }
-keep class org.opennur.tahsin.data.tajwid.** { *; }
-keep class org.opennur.tahsin.data.vocab.** { *; }

# Model util yang disimpan/parsing lewat Gson.
-keep class org.opennur.tahsin.util.AyahOfTheDay { *; }
-keep class org.opennur.tahsin.util.Bookmark { *; }
-keep class org.opennur.tahsin.util.DreamBigStats { *; }
-keep class org.opennur.tahsin.util.LughohStats { *; }
-keep class org.opennur.tahsin.util.GamificationStats { *; }
-keep class org.opennur.tahsin.util.ActivityResult { *; }
-keep class org.opennur.tahsin.util.AyahStats { *; }
-keep class org.opennur.tahsin.util.WordError { *; }
-keep class org.opennur.tahsin.util.ReadingHistoryEntry { *; }
-keep class org.opennur.tahsin.util.SearchableAyah { *; }

# Enum yang di-reflect lewat name()/valueOf (AppLanguage, Reciter, dll).
-keepclassmembers enum * { *; }

# ---- Komponen yang direferensikan manifest / sistem (aman walau R8 sudah
# menyimpulkan, tapi eksplisit supaya tidak ambigu) ----
-keep class org.opennur.tahsin.MainActivity { *; }
-keep class org.opennur.tahsin.widget.** { *; }
-keep class org.opennur.tahsin.util.DownloadService { *; }

# ---- Hilt (PENTING saat R8 aktif) ----
# R8 me-merge/obfuscate class Hilt yang di-generate (Hilt_TahsinApplication,
# TahsinApplication_HiltComponents, Dagger*_HiltComponents_*, *_GeneratedInjector,
# _hilt_aggregated_deps) → crash ClassNotFoundException/NoClassDefFoundError
# saat aplikasi launch. Keep eksplisit (tanpa allowshrinking) supaya hierarki
# & nama asli dipertahankan. Ini fix untuk "release crash, debug aman".
-keep class _hilt_aggregated_deps.** { *; }
-keep class dagger.hilt.** { *; }
-keep class org.opennur.tahsin.Hilt_* { *; }
-keep class org.opennur.tahsin.*_HiltComponents { *; }
-keep class org.opennur.tahsin.Dagger*_HiltComponents_* { *; }
-keep class org.opennur.tahsin.*_GeneratedInjector { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.internal.lifecycle.HiltViewModelFactory <methods>;
}

# ---- DataStore (proto) ----
-keep class androidx.datastore.** { *; }
-dontwarn org.jetbrains.annotations.**
