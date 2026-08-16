package org.opennur.tahsin.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.opennur.tahsin.util.GamificationReader
import org.opennur.tahsin.util.GamificationStore

/** Binds the persistent gamification store to the read-only ViewModel contract. */
@Module
@InstallIn(SingletonComponent::class)
abstract class GamificationModule {

    @Binds
    @Singleton
    abstract fun bindGamificationReader(store: GamificationStore): GamificationReader
}
