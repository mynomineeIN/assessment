package com.example.androidreminderapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY startDate ASC, timeInMillis ASC")
    fun getAllReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND isCompleted = 0 AND userId = :userId")
    suspend fun getActiveReminders(userId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE userId = ''")
    suspend fun getLegacyReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE cloudId = :cloudId AND userId = :userId LIMIT 1")
    suspend fun getReminderByCloudId(cloudId: String, userId: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("UPDATE reminders SET timeInMillis = :nextTrigger WHERE id = :id AND timeInMillis = :expectedTrigger AND isActive = 1 AND isCompleted = 0")
    suspend fun claimAndAdvanceOccurrence(id: Long, expectedTrigger: Long, nextTrigger: Long): Int

    @Query("UPDATE reminders SET isCompleted = 1, isActive = 0 WHERE id = :id AND timeInMillis = :expectedTrigger AND isActive = 1 AND isCompleted = 0")
    suspend fun claimAndCompleteOccurrence(id: Long, expectedTrigger: Long): Int
}
