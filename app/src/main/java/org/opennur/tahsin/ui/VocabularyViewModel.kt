package org.opennur.tahsin.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.opennur.tahsin.data.vocab.VocabCard
import org.opennur.tahsin.data.vocab.VocabDaily
import org.opennur.tahsin.data.vocab.VocabEntry
import org.opennur.tahsin.data.vocab.VocabQuizQuestion
import org.opennur.tahsin.data.vocab.VocabularyEngine
import org.opennur.tahsin.data.vocab.VocabularyRepository
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.GamificationHub
import org.opennur.tahsin.util.SettingsStore
import org.opennur.tahsin.util.TahsinAudioPlayer
import org.opennur.tahsin.util.VocabularyStatsStore
import org.opennur.tahsin.util.QuestionExposureStore
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Mode layar kosa kata. */
enum class VocabMode { CARDS, QUIZ }

/** State layar kosa kata. */
data class VocabUiState(
    val loading: Boolean = true,
    val mode: VocabMode = VocabMode.CARDS,
    val language: AppLanguage = AppLanguage.ID,
    // Mode kartu
    val session: List<VocabEntry> = emptyList(),
    val sessionIndex: Int = 0,
    val flipped: Boolean = false,
    // Mode kuis
    val question: VocabQuizQuestion? = null,
    val selected: String? = null,
    val quizCorrect: Int = 0,
    val quizTotal: Int = 0,
    val quizDone: Boolean = false,
    // Progres global
    val learnedCount: Int = 0,
    val dueCount: Int = 0,
    val message: String? = null,
) {
    /** Kartu yang sedang ditampilkan (null kalau sesi habis). */
    val current: VocabEntry? get() = session.getOrNull(sessionIndex)
    /** Jumlah kartu yang sudah dijawab di sesi ini. */
    val answeredCount: Int get() = sessionIndex
}

/**
 * Belajar kosa kata Al-Qur'an: sesi kartu (SRS) + kuis pilihan ganda.
 * Logika murni (pemilihan sesi, SRS, kuis) ada di [VocabularyEngine];
 * ViewModel hanya mengurus state, persistensi ([VocabularyStatsStore]),
 * dan pemutaran audio ([TahsinAudioPlayer]).
 */
@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val app: Context,
    private val repository: VocabularyRepository,
    private val store: VocabularyStatsStore,
    private val settings: SettingsStore,
    private val audioPlayer: TahsinAudioPlayer,
) : ViewModel() {

    private val _state = MutableStateFlow(VocabUiState())
    val state: StateFlow<VocabUiState> = _state.asStateFlow()

    private val random = Random(System.currentTimeMillis())
    private var entries: List<VocabEntry> = emptyList()
    private var quizQueue: List<VocabEntry> = emptyList()
    private var quizDirections: List<Boolean> = emptyList()
    private var quizIndex = 0
    private var currentQuizId: String? = null
    private val questionHistory = QuestionExposureStore.fromContext(app)

    /** Serialisasi baca-ubah-tulis store (jawaban cepat ganda tidak boleh balapan). */
    private val storeMutex = Mutex()

    init {
        val language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        _state.value = VocabUiState(language = language)
        viewModelScope.launch {
            // Baca + parse aset di IO (file aset beberapa ratus KB).
            entries = withContext(Dispatchers.IO) { repository.curatedEntries() }
            buildSession()
        }
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }

    /** Hentikan audio kata (dipanggil UI saat layar ditutup). */
    fun stopAudio() {
        audioPlayer.stop()
    }

    /**
     * Sinkronkan bahasa terbaru dari pengaturan. VM layar fitur di-cache per
     * Activity (`viewModel(factory)` default owner = Activity), jadi bahasa
     * awal bisa basi setelah user pindah bahasa di Pengaturan — UI memanggil
     * ini setiap layar terbuka.
     */
    fun refreshLanguage() {
        val lang = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        val s = _state.value
        if (s.language == lang) return
        _state.update { it.copy(language = lang) }
        // Opsi soal dibuat dalam bahasa saat itu → regenerasi biar ikut bahasa
        // baru. Hanya kalau soal belum dijawab: yang sudah dijawab skornya
        // sudah dihitung — jangan sampai bisa dijawab ulang (XP ganda).
        val s2 = _state.value
        if (s2.mode == VocabMode.QUIZ && s2.question != null && s2.selected == null) {
            loadNextQuestion()
        }
    }

    // ---- Sesi kartu ----

    /** Bangun sesi: kartu jatuh tempo dulu, lalu kata baru (frekuensi). */
    private suspend fun buildSession() = withContext(Dispatchers.IO) {
        storeMutex.withLock {
            val stored = store.read()
            val now = System.currentTimeMillis()
            val day = VocabularyEngine.dayKey(now)
            val daily = if (stored.daily.date == day) stored.daily else VocabDaily(date = day)
            val session = VocabularyEngine.selectSession(entries, stored.cards, now, newLimit = 5, dueLimit = 10)
            _state.update {
                it.copy(
                    loading = false,
                    session = session,
                    sessionIndex = 0,
                    flipped = false,
                    learnedCount = stored.cards.values.count { card -> card.correctCount > 0 },
                    dueCount = stored.cards.values.count { card -> VocabularyEngine.isDue(card, now) },
                )
            }
            // Rollover harian: simpan tanggal baru begitu berganti hari.
            if (stored.daily.date != day) store.write(stored.copy(daily = daily))
        }
    }

    /** Balik kartu (lihat arti) / balik lagi. */
    fun flip() {
        _state.update { it.copy(flipped = !it.flipped) }
    }

    /** Jawab kartu: Ingat/Lupa → update SRS + simpan + maju ke kartu berikutnya. */
    fun answerCard(remembered: Boolean) {
        val entry = _state.value.current ?: return
        viewModelScope.launch {
            storeMutex.withLock {
                withContext(Dispatchers.IO) {
                    val stored = store.read()
                    val now = System.currentTimeMillis()
                    val day = VocabularyEngine.dayKey(now)
                    val daily = if (stored.daily.date == day) stored.daily else VocabDaily(date = day)
                    val old = stored.cards[entry.key] ?: VocabCard(entry.key)
                    val updated = if (remembered) VocabularyEngine.remember(old, now)
                    else VocabularyEngine.forget(old, now)
                    val wasNew = old.correctCount == 0 && old.wrongCount == 0
                    val newDaily = daily.copy(
                        studied = daily.studied + 1,
                        learned = daily.learned + if (remembered && wasNew) 1 else 0,
                    )
                    val cards = stored.cards + (entry.key to updated)
                    store.write(stored.copy(cards = cards, daily = newDaily))
                    // Kata baru dikuasai pertama kali (correctCount 0 → 1) = XP kata dikuasai.
                    if (remembered && wasNew) {
                        GamificationHub.award(app, Gamification.XP_WORD_MASTERED)
                    }
                    _state.update {
                        it.copy(
                            flipped = false,
                            sessionIndex = it.sessionIndex + 1,
                            learnedCount = cards.values.count { card -> card.correctCount > 0 },
                            dueCount = cards.values.count { card -> VocabularyEngine.isDue(card, now) },
                        )
                    }
                }
            }
        }
    }

    /** Putar audio kata (contoh ayat) — cache → URL wbw → fallback pesan. */
    fun playCurrentWord() {
        val entry = _state.value.current ?: return
        val ex = entry.example
        if (ex.surah <= 0) return
        // ex.word 1-based (dari build_vocab.py); TahsinAudioPlayer.playWord
        // menerima indeks 0-based (ditambah 1 di AudioUrls.wordKey).
        audioPlayer.playWord(ex.surah, ex.ayah, ex.word - 1, entry.word) {
            val msg = AppStrings.of(_state.value.language).vocabAudioUnavailable
            _state.update { it.copy(message = msg) }
        }
    }

    // ---- Kuis ----

    fun switchMode(mode: VocabMode) {
        if (mode == VocabMode.QUIZ) startQuiz()
        else _state.update { it.copy(mode = VocabMode.CARDS) }
    }

    /** Mulai kuis dari sesi kartu (acak); kalau kosong, pakai 5 kata teratas. */
    private fun startQuiz() {
        val s = _state.value
        val ids = entries.flatMap { entry -> listOf("${entry.key}|f", "${entry.key}|r") }
        val selectedIds = questionHistory.reserve(
            FEATURE,
            ids,
            5,
            LocalDate.now().toEpochDay(),
            random,
        )
        val parsed = selectedIds.mapNotNull { id ->
            val separator = id.lastIndexOf('|')
            val entry = entries.firstOrNull { it.key == id.substring(0, separator) }
            entry?.let { it to (id.substring(separator + 1) == "r") }
        }
        quizQueue = parsed.map { it.first }
        quizDirections = parsed.map { it.second }
        quizIndex = 0
        _state.update {
            it.copy(mode = VocabMode.QUIZ, question = null, selected = null, quizCorrect = 0, quizTotal = 0, quizDone = false)
        }
        loadNextQuestion()
    }

    private fun loadNextQuestion() {
        val s = _state.value
        val target = quizQueue.getOrNull(quizIndex)
        if (target == null) {
            _state.update { it.copy(question = null, quizDone = true) }
            return
        }
        // Mode depan/balik acak; skip kalau kolam pengecoh kurang.
        val reverse = quizDirections.getOrNull(quizIndex) ?: random.nextBoolean()
        currentQuizId = quizQueue.getOrNull(quizIndex)?.let { entry ->
            "${entry.key}|${if (reverse) "r" else "f"}"
        }
        val q = VocabularyEngine.makeQuiz(entries, target, s.language, reverse = reverse, random = random)
        if (q == null) {
            quizIndex++
            loadNextQuestion()
        } else {
            _state.update { it.copy(question = q, selected = null) }
        }
    }

    /** Jawab soal kuis (sekali per soal). */
    fun answerQuiz(option: String) {
        val s = _state.value
        val q = s.question ?: return
        if (s.selected != null) return
        val correct = q.options[q.correctIndex] == option
        currentQuizId?.let { id ->
            questionHistory.record(FEATURE, id, correct, LocalDate.now().toEpochDay())
        }
        _state.update {
            it.copy(
                selected = option,
                quizCorrect = it.quizCorrect + if (correct) 1 else 0,
                quizTotal = it.quizTotal + 1,
            )
        }
        if (correct) {
            viewModelScope.launch(Dispatchers.IO) {
                GamificationHub.award(app, Gamification.XP_QUIZ_CORRECT)
            }
        }
    }

    fun nextQuiz() {
        quizIndex++
        loadNextQuestion()
    }

    fun restartQuiz() = startQuiz()

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    private companion object {
        const val FEATURE = "vocabulary"
    }
}
