package com.tahsin.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tahsin.app.data.vocab.VocabCard
import com.tahsin.app.data.vocab.VocabDaily
import com.tahsin.app.data.vocab.VocabEntry
import com.tahsin.app.data.vocab.VocabQuizQuestion
import com.tahsin.app.data.vocab.VocabularyEngine
import com.tahsin.app.data.vocab.VocabularyRepository
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.SettingsStore
import com.tahsin.app.util.TahsinAudioPlayer
import com.tahsin.app.util.VocabularyStatsStore
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
class VocabularyViewModel(
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
    private var quizIndex = 0

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
        quizQueue = (s.session.ifEmpty { entries.take(5) }).shuffled(random)
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
        val reverse = random.nextBoolean()
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
        _state.update {
            it.copy(
                selected = option,
                quizCorrect = it.quizCorrect + if (correct) 1 else 0,
                quizTotal = it.quizTotal + 1,
            )
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
}

/** Factory manual DI (tanpa Hilt). */
fun vocabularyViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        VocabularyViewModel(
            repository = VocabularyRepository(app),
            store = VocabularyStatsStore(app),
            settings = SettingsStore(app),
            audioPlayer = TahsinAudioPlayer(app, SettingsStore(app)),
        )
    }
}
