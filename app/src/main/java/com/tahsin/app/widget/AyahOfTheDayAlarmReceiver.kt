package com.tahsin.app.widget

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tahsin.app.R
import com.tahsin.app.util.AyahOfTheDay
import com.tahsin.app.util.AyahOfTheDayManager
import com.tahsin.app.util.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/** Penjadwalan alarm harian untuk update widget + notifikasi "Ayah of the Day". */
object AyahOfTheDayAlarm {
    const val ACTION_UPDATE = "com.tahsin.app.action.UPDATE_AYAH_OF_THE_DAY"

    /** Set alarm ke tengah malam berikutnya (idempotent — menggantikan alarm lama). */
    fun scheduleDaily(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            app,
            0,
            Intent(app, AyahOfTheDayAlarmReceiver::class.java).setAction(ACTION_UPDATE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val nextMidnight = LocalDate.now().plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        am.setAndAllowWhileIdle(AlarmManager.RTC, nextMidnight, pi)
    }
}

/**
 * Menerima alarm harian (update widget + notifikasi) dan BOOT_COMPLETED
 * (reschedule alarm setelah perangkat restart).
 */
class AyahOfTheDayAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AyahOfTheDayAlarm.ACTION_UPDATE -> updateAndNotify(context)
            Intent.ACTION_BOOT_COMPLETED -> AyahOfTheDayAlarm.scheduleDaily(context)
        }
    }

    /** Kerja berat (baca aset + parse JSON) di background lewat goAsync. */
    private fun updateAndNotify(context: Context) {
        val pending = goAsync()
        val app = context.applicationContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope.launch {
            try {
                val lang = AyahOfTheDayManager.languageOf(app)
                val ayah = withContext(Dispatchers.IO) {
                    val date = LocalDate.now()
                    AyahOfTheDayManager.cached(app, date, lang)
                        ?: AyahOfTheDayManager.loadAndCache(app, date, lang)
                }
                if (ayah != null) {
                    AyahOfTheDayWidgetUpdater.updateAll(app)
                    postNotification(app, ayah)
                }
            } finally {
                // Rantai harian tetap lanjut apa pun yang terjadi (aman dipanggil ulang).
                runCatching { AyahOfTheDayAlarm.scheduleDaily(app) }
                pending.finish()
            }
        }
    }

    // ---- notifikasi harian ----

    private fun postNotification(context: Context, ayah: AyahOfTheDay) {
        val app = context.applicationContext
        if (!SettingsStore(app).ayahOfDayEnabled) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val nm = app.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Ayah of the Day", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val body = listOfNotNull(ayah.arabic, ayah.translation.ifBlank { null }).joinToString("\n\n")
        val notification = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ayah_of_the_day)
            .setContentTitle("${ayah.surahName} · ${ayah.ayahNumber}")
            .setContentText(ayah.translation.ifBlank { ayah.arabic })
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(AyahOfTheDayWidgetUpdater.deepLink(app, ayah))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "ayah_of_the_day"
        const val NOTIFICATION_ID = 1002 // jangan bentrok dengan unduhan audio (1001)
    }
}
