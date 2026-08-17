package org.opennur.tahsin.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.opennur.tahsin.data.lughoh.LughohRepository
import org.opennur.tahsin.data.quran.AssetQuranRepository
import org.opennur.tahsin.data.quran.QuranRepository
import org.opennur.tahsin.data.vocab.VocabularyRepository
import org.opennur.tahsin.stt.ArabicSpeechRecognizer
import org.opennur.tahsin.util.AudioDownloader
import org.opennur.tahsin.util.BackupManager
import org.opennur.tahsin.util.BookmarkStore
import org.opennur.tahsin.util.DreamBigProgressStore
import org.opennur.tahsin.util.FontStore
import org.opennur.tahsin.util.GamificationStore
import org.opennur.tahsin.util.LearningPlanStore
import org.opennur.tahsin.util.LughohProgressStore
import org.opennur.tahsin.util.ReadingHistoryStore
import org.opennur.tahsin.util.ReadingStatsStore
import org.opennur.tahsin.util.SettingsSource
import org.opennur.tahsin.util.SettingsStore
import org.opennur.tahsin.util.StatsStores
import org.opennur.tahsin.util.TahsinAudioPlayer
import org.opennur.tahsin.util.VocabularyStatsStore
import javax.inject.Singleton

/**
 * Grafik dependensi aplikasi. ViewModel memakai `@HiltViewModel` + `@Inject
 * constructor`; kelas-kelas yang butuh Context/konstruksi manual disediakan
 * di sini (singleton — aman karena store file/aset read-write aman
 * lintas-instance dan dirancang thread-safe).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    /**
     * Binding Context tanpa qualifier (application context) — dipakai
     * parameter ViewModel yang bertipe [Context] langsung.
     */
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideQuranRepository(@ApplicationContext context: Context): QuranRepository =
        AssetQuranRepository(context)

    @Provides
    @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context): SettingsStore =
        SettingsStore(context)

    /** SettingsStore sebagai [SettingsSource] (VM yang tak peduli implementasi). */
    @Provides
    @Singleton
    fun provideSettingsSource(store: SettingsStore): SettingsSource = store

    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        settings: SettingsStore,
    ): BackupManager = BackupManager.create(context, settings)

    @Provides
    @Singleton
    fun provideAudioDownloader(
        @ApplicationContext context: Context,
        settings: SettingsStore,
    ): AudioDownloader = AudioDownloader(context, settings)

    @Provides
    @Singleton
    fun provideTahsinAudioPlayer(
        @ApplicationContext context: Context,
        settings: SettingsStore,
    ): TahsinAudioPlayer = TahsinAudioPlayer(context, settings)

    @Provides
    @Singleton
    fun provideArabicSpeechRecognizer(@ApplicationContext context: Context): ArabicSpeechRecognizer =
        ArabicSpeechRecognizer(context)

    @Provides
    @Singleton
    fun provideVocabularyRepository(@ApplicationContext context: Context): VocabularyRepository =
        VocabularyRepository(context)

    @Provides
    @Singleton
    fun provideLughohRepository(@ApplicationContext context: Context): LughohRepository =
        LughohRepository(context)

    @Provides
    @Singleton
    fun provideFontStore(@ApplicationContext context: Context): FontStore = FontStore(context)

    // ---- Store persistensi (pola Gson + filesDir, lihat tiap kelas) ----

    @Provides
    @Singleton
    fun provideBookmarkStore(@ApplicationContext context: Context): BookmarkStore =
        BookmarkStore.fromContext(context)

    @Provides
    @Singleton
    fun provideReadingStatsStore(@ApplicationContext context: Context): ReadingStatsStore =
        ReadingStatsStore.fromContext(context)

    @Provides
    @Singleton
    fun provideVocabularyStatsStore(@ApplicationContext context: Context): VocabularyStatsStore =
        VocabularyStatsStore.fromContext(context)

    @Provides
    @Singleton
    fun provideDreamBigProgressStore(@ApplicationContext context: Context): DreamBigProgressStore =
        DreamBigProgressStore.fromContext(context)

    @Provides
    @Singleton
    fun provideLughohProgressStore(@ApplicationContext context: Context): LughohProgressStore =
        LughohProgressStore.fromContext(context)

    @Provides
    @Singleton
    fun provideGamificationStore(@ApplicationContext context: Context): GamificationStore =
        GamificationStore.fromContext(context)

    @Provides
    @Singleton
    fun provideReadingHistoryStore(@ApplicationContext context: Context): ReadingHistoryStore =
        ReadingHistoryStore.fromContext(context)

    @Provides
    @Singleton
    fun provideStatsStores(
        readingStats: ReadingStatsStore,
        vocabularyStats: VocabularyStatsStore,
        dreamBig: DreamBigProgressStore,
        lughoh: LughohProgressStore,
        gamification: GamificationStore,
        readingHistory: ReadingHistoryStore,
        learningPlan: LearningPlanStore,
    ): StatsStores = StatsStores(
        readingStats = readingStats,
        vocabularyStats = vocabularyStats,
        dreamBig = dreamBig,
        lughoh = lughoh,
        gamification = gamification,
        readingHistory = readingHistory,
        learningPlan = learningPlan,
    )
}
