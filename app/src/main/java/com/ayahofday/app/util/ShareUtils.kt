package com.ayahofday.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/** Membagikan teks langsung ke WhatsApp; fallback ke chooser bila WhatsApp tidak terpasang. */
fun shareToWhatsApp(context: Context, text: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val whatsappIntent = Intent(shareIntent).apply {
        setPackage("com.whatsapp")
    }
    try {
        context.startActivity(whatsappIntent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan"))
    }
}
