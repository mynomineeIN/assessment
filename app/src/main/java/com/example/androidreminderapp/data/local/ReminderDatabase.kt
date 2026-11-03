package com.example.androidreminderapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
@TypeConverters(ReminderConverters::class)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "reminder_database"
    }
}
