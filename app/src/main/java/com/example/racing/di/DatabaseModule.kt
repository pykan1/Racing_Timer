package com.example.racing.di

import android.content.Context
import androidx.room.Room
import com.example.racing.data.local.AppDatabase
import com.example.racing.data.local.repository.CircleRepository
import com.example.racing.data.local.repository.DriverRepository
import com.example.racing.data.local.repository.RaceDriverCrossRefRepository
import com.example.racing.data.local.repository.RaceRepository
import com.example.racing.data.local.repositoryImpl.StoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {


    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    fun provideDataStore(@ApplicationContext context: Context): StoreManager {
        return StoreManager(context)
    }

    @Provides
    fun provideRaceDao(database: AppDatabase): RaceRepository {
        return database.raceDao()
    }

    @Provides
    fun provideLapDao(database: AppDatabase): CircleRepository {
        return database.lapDao()
    }

    @Provides
    fun provideDriverDao(database: AppDatabase): DriverRepository {
        return database.driverDao()
    }

    @Provides
    fun provideRaceDriverCrossRefDao(database: AppDatabase): RaceDriverCrossRefRepository {
        return database.raceDriverCrossRefDao()
    }

}