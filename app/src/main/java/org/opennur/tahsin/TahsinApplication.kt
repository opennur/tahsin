package org.opennur.tahsin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application Hilt — titik masuk dependency injection.
 *
 * Terdaftar di AndroidManifest (`android:name=".TahsinApplication"`). Semua
 * ViewModel memakai [dagger.hilt.android.lifecycle.HiltViewModel] dan di-wire
 * lewat [org.opennur.tahsin.di.AppModule]; komponen Android yang perlu
 * injeksi memakai `@AndroidEntryPoint`.
 */
@HiltAndroidApp
class TahsinApplication : Application()
