package org.opennur.tahsin.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.opennur.tahsin.MainActivity
import org.opennur.tahsin.R
import org.opennur.tahsin.util.AyahOfTheDay
import org.opennur.tahsin.util.AyahOfTheDayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Widget "Ayah of the Day" di home screen: satu ayat + terjemahan yang
 * berganti setiap hari (deterministik per tanggal). Ketuk widget → buka
 * aplikasi tepat di ayat itu.
 */
class AyahOfTheDayWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        AyahOfTheDayWidgetUpdater.update(context, appWidgetManager, appWidgetIds)
        // Pastikan alarm harian terpasang (juga setelah install/reboot).
        AyahOfTheDayAlarm.scheduleDaily(context)
    }
}

/** Logika update widget bersama (dipakai provider & alarm receiver). */
object AyahOfTheDayWidgetUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Update SEMUA instance widget yang terpasang (dipakai alarm harian). */
    fun updateAll(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val ids = manager.getAppWidgetIds(ComponentName(app, AyahOfTheDayWidget::class.java))
        if (ids.isEmpty()) return
        update(app, manager, ids)
    }

    fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val app = context.applicationContext
        scope.launch {
            val lang = AyahOfTheDayManager.languageOf(app)
            val ayah = withContext(Dispatchers.IO) {
                val date = LocalDate.now()
                AyahOfTheDayManager.cached(app, date, lang)
                    ?: AyahOfTheDayManager.loadAndCache(app, date, lang)
            } ?: return@launch
            val views = buildViews(app, ayah)
            ids.forEach { runCatching { manager.updateAppWidget(it, views) } }
        }
    }

    fun buildViews(context: Context, ayah: AyahOfTheDay): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_ayah_of_the_day)
        views.setTextViewText(R.id.widget_ayah_surah, "${ayah.surahName} · ${ayah.ayahNumber}")
        // Widget ringkas: hanya terjemahan (teks Arab tampil di notifikasi).
        views.setTextViewText(R.id.widget_ayah_translation, ayah.translation.ifBlank { "—" })
        views.setOnClickPendingIntent(R.id.widget_root, deepLink(context, ayah))
        return views
    }

    /** Tap widget/notifikasi → MainActivity terbuka di surah+ayat tersebut. */
    fun deepLink(context: Context, ayah: AyahOfTheDay): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_TARGET_SURAH, ayah.surahNumber)
            putExtra(MainActivity.EXTRA_TARGET_AYAH, ayah.ayahNumber)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
