package com.example.androidreminderapp.domain.repository

import com.example.androidreminderapp.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    suspend fun getReminderById(id: Long): Reminder?
    suspend fun getActiveReminders(): List<Reminder>
    suspend fun synchronizeCloudReminders()
    suspend fun insertReminder(reminder: Reminder): Long
    suspend fun updateReminder(reminder: Reminder)
    suspend fun deleteReminder(reminder: Reminder)
    suspend fun deleteReminderById(id: Long)
}
