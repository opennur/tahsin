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
import com.tahsin.app.MainActivity
import com.tahsin.app.R
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.Gamification
import com.tahsin.app.util.GamificationStore
import com.tahsin.app.util.SettingsStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Alarm harian pengingat streak (jam 18:00) — pola sama seperti
 * [AyahOfTheDayAlarm]: setAndAllowWhileIdle + PendingIntent broadcast.
 * Dijadwalkan saat aplikasi dibuka & saat boot; receiver mengecek toggle
 * dan progres XP hari ini sebelum memposting notifikasi.
 */
object StreakReminderAlarm {
    const val ACTION_STREAK_REMINDER = "com.tahsin.app.action.STREAK_REMINDER"

    /** Jam pengingat (waktu lokal perangkat). */
    private val REMINDER_TIME = LocalTime.of(18, 0)

    /** Set alarm ke jam 18:00 berikutnya (idempotent — menggantikan alarm lama). */
    fun scheduleDaily(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(app)
        val todayAtReminder = LocalDate.now().atTime(REMINDER_TIME)
        // Lewat jam pengingat hari ini → besok.
        val trigger = if (todayAtReminder.isAfter(LocalDateTime.now())) todayAtReminder
        else todayAtReminder.plusDays(1)
        am.setAndAllowWhileIdle(
            AlarmManager.RTC,
            trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            pi,
        )
    }

    /** Batalkan alarm pengingat (saat toggle dimatikan). */
    fun cancel(context: Context) {
        val app = context.applicationContext
        app.getSystemService(AlarmManager::class.java).cancel(pendingIntent(app))
    }

    private fun pendingIntent(app: Context): PendingIntent = PendingIntent.getBroadcast(
        app,
        1,
        Intent(app, StreakReminderReceiver::class.java).setAction(ACTION_STREAK_REMINDER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Menerima alarm pengingat streak dan BOOT_COMPLETED (reschedule). */
class StreakReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            StreakReminderAlarm.ACTION_STREAK_REMINDER -> {
                if (SettingsStore(context).streakReminderEnabled) {
                    StreakReminderAlarm.scheduleDaily(context)
                    maybeNotify(context)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                if (SettingsStore(context).streakReminderEnabled) {
                    StreakReminderAlarm.scheduleDaily(context)
                }
            }
        }
    }

    /** Post notifikasi hanya kalau streak aktif dan target harian belum tercapai. */
    private fun maybeNotify(context: Context) {
        val app = context.applicationContext
        val settings = SettingsStore(app)
        if (!settings.streakReminderEnabled) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val stats = GamificationStore.fromContext(app).read()
        if (stats.streak <= 0) return
        val today = LocalDate.now().toEpochDay()
        val todayXp = Gamification.todayXpFor(stats, today)
        if (todayXp >= Gamification.DAILY_GOAL_XP) return // target hari ini sudah tuntas

        val nm = app.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Streak Reminder",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val id = settings.languageCode == AppLanguage.ID.code
        val title = if (id) "🔥 Pertahankan streak-mu!" else "🔥 Keep your streak alive!"
        val body = if (id) {
            "Kamu sudah ${stats.streak} hari beruntun. Buka aplikasi & capai target harian " +
                "(${todayXp}/${Gamification.DAILY_GOAL_XP} XP)."
        } else {
            "You're on a ${stats.streak}-day streak. Open the app and hit today's goal " +
                "(${todayXp}/${Gamification.DAILY_GOAL_XP} XP)."
        }
        val notification = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ayah_of_the_day)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(launcherIntent(app))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun launcherIntent(app: Context): PendingIntent {
        val i = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            app,
            0,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val CHANNEL_ID = "streak_reminder"
        const val NOTIFICATION_ID = 1003 // jangan bentrok dengan yang lain (1001/1002)
    }
}
