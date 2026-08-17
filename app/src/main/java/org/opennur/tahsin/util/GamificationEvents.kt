package org.opennur.tahsin.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Jenis perayaan gamification (dialog global). */
enum class CelebrationType { LEVEL_UP, STREAK_MILESTONE, BADGE_EARNED }

/** Satu perayaan yang akan ditampilkan sebagai dialog global. */
data class CelebrationEvent(
    val type: CelebrationType,
    val level: Int = 0,
    val streak: Int = 0,
    val badgeKey: String = "",
    /** Tier badge yang baru dibuka (untuk event BADGE_EARNED). */
    val tier: Int = 0,
)

/**
 * Event bus ringan untuk perayaan gamification — tanpa DI, pola sama seperti
 * [DownloadProgress]. [GamificationHub] memposting saat naik level / streak
 * milestone / badge baru; MainActivity mengonsumsi dan menampilkan dialog.
 * StateFlow hanya menyimpan event TERBARU: cukup karena perayaan dibaca
 * segera; event lama yang tertimpa tidak lagi relevan.
 */
object GamificationEvents {

    private val _event = MutableStateFlow<CelebrationEvent?>(null)
    val event: StateFlow<CelebrationEvent?> = _event.asStateFlow()

    private var suppressed = false
    private var pending: CelebrationEvent? = null

    fun post(e: CelebrationEvent) {
        if (suppressed) pending = e else _event.value = e
    }

    /** Tahan dialog perayaan selama sesi flow agar bacaan tidak terputus. */
    fun beginSuppression() {
        suppressed = true
    }

    /** Akhiri penahanan dan tampilkan perayaan terakhir yang tertunda, bila ada. */
    fun endSuppression() {
        suppressed = false
        pending?.let {
            _event.value = it
            pending = null
        }
    }

    /** Tandai dialog sudah ditutup user. */
    fun consume() {
        _event.value = null
        pending = null
    }
}
