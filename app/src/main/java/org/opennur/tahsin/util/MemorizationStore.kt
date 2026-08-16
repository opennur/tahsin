package org.opennur.tahsin.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import org.opennur.tahsin.data.learning.MemorizationCard

/** JSON envelope for the local memorization queue. */
data class MemorizationSnapshot(
    val cards: List<MemorizationCard> = emptyList(),
)

/** Persists only review metadata; Qur'an text remains in the bundled repository. */
class MemorizationStore internal constructor(private val file: File) {

    companion object {
        fun fromContext(context: Context): MemorizationStore =
            MemorizationStore(File(context.applicationContext.filesDir, "memorization.json"))
    }

    private val gson = Gson()
    private val type = object : TypeToken<MemorizationSnapshot>() {}.type

    fun read(): MemorizationSnapshot = synchronized(this) {
        runCatching {
            if (!file.exists()) return@synchronized MemorizationSnapshot()
            gson.fromJson<MemorizationSnapshot>(file.readText(), type) ?: MemorizationSnapshot()
        }.getOrDefault(MemorizationSnapshot())
    }

    fun write(snapshot: MemorizationSnapshot) = synchronized(this) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(snapshot))
        }
    }

    fun upsert(card: MemorizationCard): MemorizationSnapshot = synchronized(this) {
        val current = read()
        val cards = current.cards.toMutableList()
        val index = cards.indexOfFirst { it.surah == card.surah && it.ayah == card.ayah }
        if (index >= 0) cards[index] = card else cards += card
        val updated = MemorizationSnapshot(cards)
        write(updated)
        updated
    }
}
