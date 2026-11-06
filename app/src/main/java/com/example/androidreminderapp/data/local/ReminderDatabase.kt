package com.example.androidreminderapp.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ReminderEntity::class], version = 2, exportSchema = false)
@TypeConverters(ReminderConverters::class)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "reminder_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reminders ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE reminders SET cloudId = lower(hex(randomblob(16))) WHERE cloudId = ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_reminders_cloudId ON reminders(cloudId)")
            }
        }
    }
}
