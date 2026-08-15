package org.opennur.tahsin.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * Layanan latar depan untuk unduhan audio: menjaga proses tetap hidup saat
 * layar mati / aplikasi di latar belakang supaya unduhan tidak gagal.
 *
 * Service ini menjaga proses tetap hidup + menampilkan notifikasi progres;
 * orchestrasi tetap di ViewModel. Jika proses mati, antrean persisten milik
 * [AudioDownloader] dipulihkan saat aplikasi dibuka kembali dan file `.part`
 * dilanjutkan tanpa mengekspos file setengah jadi ke pemutar.
 */
class DownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "audio_download"
        private const val NOTIFICATION_ID = 1001

        @Volatile private var instance: DownloadService? = null
        private var lastDone = 0
        private var lastTotal = 0
        private var lastLabel = "Mengunduh audio…"

        fun isRunning(): Boolean = instance != null

        /** Mulai foreground service (hanya boleh dari proses yang sedang foreground). */
        fun start(context: Context) {
            val ctx = context.applicationContext
            ctx.startForegroundService(Intent(ctx, DownloadService::class.java))
        }

        /** Perbarui notifikasi progres (no-op kalau service belum jalan). */
        fun updateProgress(done: Int, total: Int, label: String = lastLabel) {
            lastDone = done
            lastTotal = total
            lastLabel = label
            instance?.publish()
        }

        fun stop(context: Context) {
            val ctx = context.applicationContext
            ctx.stopService(Intent(ctx, DownloadService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        // Wake lock: cegah CPU/radio tidur saat layar mati supaya unduhan tidak macet.
        wakeLock = (getSystemService(PowerManager::class.java)).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "tahsin:download",
        ).apply { acquire(30 * 60 * 1000L) }
        instance = this
        startForegroundCompat(lastDone, lastTotal, lastLabel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat(lastDone, lastTotal, lastLabel)
        return START_NOT_STICKY
    }

    /** startForeground 3-arg (wajib di Android 14+/targetSdk 34). */
    private fun startForegroundCompat(done: Int, total: Int, label: String) {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, buildNotification(done, total, label), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(done, total, label))
        }
    }

    override fun onDestroy() {
        runCatching { wakeLock?.release() }
        wakeLock = null
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private var wakeLock: PowerManager.WakeLock? = null

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Unduhan audio", NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(done: Int, total: Int, label: String): android.app.Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(label)
            .setContentText(if (total > 0) "$done / $total berkas" else "Menyiapkan…")
            .setProgress(if (total > 0) total else 0, done, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun publish() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(lastDone, lastTotal, lastLabel))
    }
}
