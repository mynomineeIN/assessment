package com.example.androidreminderapp.di

import android.content.Context
import androidx.room.Room
import com.example.androidreminderapp.data.local.ReminderDao
import com.example.androidreminderapp.data.local.ReminderDatabase
import com.example.androidreminderapp.data.repository.ReminderRepositoryImpl
import com.example.androidreminderapp.domain.repository.ReminderRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindReminderRepository(
        reminderRepositoryImpl: ReminderRepositoryImpl
    ): ReminderRepository

    companion object {
        @Provides
        @Singleton
        fun provideReminderDatabase(
            @ApplicationContext context: Context
        ): ReminderDatabase {
            return Room.databaseBuilder(
                context,
                ReminderDatabase::class.java,
                ReminderDatabase.DATABASE_NAME
            ).addMigrations(ReminderDatabase.MIGRATION_1_2).build()
        }

        @Provides
        @Singleton
        fun provideReminderDao(database: ReminderDatabase): ReminderDao {
            return database.reminderDao()
        }
    }
}
