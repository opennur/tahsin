import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // JaCoCo bawaan Gradle (bukan dari Plugin Portal — org.jacoco tidak ada di sana).
    jacoco
}

// Signing rilis: kalau ada keystore.properties di root proyek, dipakai.
// Kalau tidak ada, fallback ke debug signing (cukup untuk pemakaian pribadi).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
val releaseSigningConfigured = keystoreProps.getProperty("storeFile") != null

android {
    namespace = "org.opennur.tahsin"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.opennur.tahsin"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (releaseSigningConfigured) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // ---- Compose (BOM 2024.10.00) ----
    // Sengaja TANPA material3: semua komponen memakai custom design system.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    debugImplementation(libs.androidx.ui.tooling)

    // ---- AndroidX / Lifecycle / Coroutines ----
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // ---- JSON parsing (mushaf asset) ----
    implementation(libs.gson)

    // ---- Unit test (JVM) ----
    testImplementation(libs.junit)
}

/**
 * Laporan cakupan INTI KEBENARAN: data/ (parser, engine, model, kuis) +
 * util/ murni + stt/TranscriptAligner — lapisan tempat akurasi teks &
 * harakat Al-Qur'an ditentukan. UI/lapisan Android (repository, widget,
 * MainActivity, ui/, widget/, GamificationHub) DIKECUALIKAN secara
 * terdokumentasi (butuh emulator/instrumented test).
 *
 * Pakai: ./gradlew jacocoCoreReport
 * (menjalankan testDebugUnitTest dulu, lalu laporan XML+HTML ke
 * build/reports/jacoco/core/)
 */
tasks.register<JacocoReport>("jacocoCoreReport") {
    group = "verification"
    description = "JaCoCo: cakupan inti kebenaran (data/ + util murni + TranscriptAligner)."
    dependsOn("testDebugUnitTest")

    // Data eksekusi dari agent bawaan plugin jacoco (tiap Test task
    // diinstrumentasi otomatis → build/jacoco/<namaTask>.exec).
    executionData.from(
        fileTree(layout.buildDirectory.dir("jacoco")) {
            include("testDebugUnitTest.exec")
        },
    )

    sourceDirectories.setFrom(files("$projectDir/src/main/java"))

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            include(
                "org/opennur/tahsin/data/**",
                "org/opennur/tahsin/util/Achievements*",
                "org/opennur/tahsin/util/AppLanguage*",
                "org/opennur/tahsin/util/ArabicNormalizer*",
                "org/opennur/tahsin/util/AudioUrls*",
                "org/opennur/tahsin/util/AyahOfTheDayPicker*",
                "org/opennur/tahsin/util/AyahSearch*",
                "org/opennur/tahsin/util/BookmarkStore*",
                "org/opennur/tahsin/util/DownloadProgress*",
                "org/opennur/tahsin/util/DreamBigProgressStore*",
                "org/opennur/tahsin/util/GamificationEvents*",
                "org/opennur/tahsin/util/GamificationStore*",
                "org/opennur/tahsin/util/LughohProgressStore*",
                "org/opennur/tahsin/util/ReadingStats*",
                "org/opennur/tahsin/util/Reciter*",
                "org/opennur/tahsin/util/VocabularyStatsStore*",
                "org/opennur/tahsin/stt/TranscriptAligner*",
            )
            exclude(
                // Sintetik Kotlin (lambdas, when-mappings, default-impl).
                "**/*\$*",
                // Lapisan Android dalam data/ (assets/files) — butuh Robolectric.
                "org/opennur/tahsin/data/**/*Repository*",
            )
        },
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/core/jacocoCoreReport.xml"))
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/core/html"))
    }
}
