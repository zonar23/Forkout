package io.github.zonar23.forkout.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.zonar23.forkout.data.db.ForkoutDatabase
import io.github.zonar23.forkout.data.seed.SeedCallback
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ForkoutDatabase =
        // No destructive fallback: every schema change needs a migration.
        Room.databaseBuilder(context, ForkoutDatabase::class.java, ForkoutDatabase.NAME)
            .addCallback(SeedCallback())
            .build()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
