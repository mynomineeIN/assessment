package com.example.androidreminderapp.data.repository

import com.example.androidreminderapp.data.local.ReminderDao
import com.example.androidreminderapp.data.local.ReminderEntity
import com.example.androidreminderapp.domain.model.Reminder
import com.example.androidreminderapp.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getReminderById(id: Long): Reminder? {
        return reminderDao.getReminderById(id)?.toDomain()
    }

    override suspend fun getActiveReminders(): List<Reminder> {
        return reminderDao.getActiveReminders().map { it.toDomain() }
    }

    override suspend fun insertReminder(reminder: Reminder): Long {
        return reminderDao.insertReminder(ReminderEntity.fromDomain(reminder))
    }

    override suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(ReminderEntity.fromDomain(reminder))
    }

    override suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.deleteReminder(ReminderEntity.fromDomain(reminder))
    }

    override suspend fun deleteReminderById(id: Long) {
        reminderDao.deleteReminderById(id)
    }
}
