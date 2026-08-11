package com.tahsin.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Layanan latar depan untuk unduhan audio: menjaga proses tetap hidup saat
 * layar mati / aplikasi di latar belakang supaya unduhan tidak gagal.
 *
 * Service ini TIDAK mengunduh sendiri — hanya "life-keeping" + menampilkan
 * notifikasi progres (orchestrasi unduhan tetap di ViewModel).
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
        instance = this
        startForeground(NOTIFICATION_ID, buildNotification(lastDone, lastTotal, lastLabel))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(lastDone, lastTotal, lastLabel))
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
