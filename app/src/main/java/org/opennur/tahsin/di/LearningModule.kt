package org.opennur.tahsin.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.opennur.tahsin.util.LearningPlanStore
import org.opennur.tahsin.util.MemorizationStore

/** Persistence providers for the guided learning and memorization tracks. */
@Module
@InstallIn(SingletonComponent::class)
object LearningModule {

    @Provides
    @Singleton
    fun provideLearningPlanStore(@ApplicationContext context: Context): LearningPlanStore =
        LearningPlanStore.fromContext(context)

    @Provides
    @Singleton
    fun provideMemorizationStore(@ApplicationContext context: Context): MemorizationStore =
        MemorizationStore.fromContext(context)
}
