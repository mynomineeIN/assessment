package com.example.androidreminderapp.data.repository

import com.example.androidreminderapp.data.local.ReminderDao
import com.example.androidreminderapp.data.local.ReminderEntity
import com.example.androidreminderapp.domain.model.Reminder
import com.example.androidreminderapp.domain.repository.ReminderRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val firebaseAuth: FirebaseAuth,
    private val cloudStore: ReminderCloudStore
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> {
        val userId = firebaseAuth.currentUser?.uid ?: return emptyFlow()
        return reminderDao.getAllReminders(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getReminderById(id: Long): Reminder? {
        val userId = currentUserId()
        return reminderDao.getReminderById(id)
            ?.takeIf { it.userId == userId }
            ?.toDomain()
    }

    override suspend fun getActiveReminders(): List<Reminder> {
        return reminderDao.getActiveReminders(currentUserId()).map { it.toDomain() }
    }

    override suspend fun synchronizeCloudReminders() {
        val userId = currentUserId()
        reminderDao.getLegacyReminders().forEach { legacy ->
            val claimedReminder = legacy.copy(userId = userId)
            reminderDao.updateReminder(claimedReminder)
            cloudStore.save(userId, claimedReminder)
        }

        cloudStore.load(userId).forEach { cloudReminder ->
            val existing = reminderDao.getReminderByCloudId(cloudReminder.cloudId, userId)
            reminderDao.insertReminder(cloudReminder.copy(id = existing?.id ?: 0L))
        }
    }

    override suspend fun insertReminder(reminder: Reminder): Long {
        val entity = ReminderEntity.fromDomain(reminder.copy(userId = currentUserId()))
        val id = reminderDao.insertReminder(entity)
        cloudStore.save(entity.userId, entity.copy(id = id))
        return id
    }

    override suspend fun updateReminder(reminder: Reminder) {
        val userId = currentUserId()
        val existing = reminderDao.getReminderById(reminder.id)
        require(existing?.userId == userId) { "Cannot update a reminder owned by another account." }
        val entity = ReminderEntity.fromDomain(reminder.copy(userId = userId))
        reminderDao.updateReminder(entity)
        cloudStore.save(entity.userId, entity)
    }

    override suspend fun deleteReminder(reminder: Reminder) {
        val userId = currentUserId()
        val entity = reminderDao.getReminderById(reminder.id)
            ?.takeIf { it.userId == userId }
            ?: return
        reminderDao.deleteReminder(entity)
        cloudStore.delete(userId, entity.cloudId)
    }

    override suspend fun deleteReminderById(id: Long) {
        val userId = currentUserId()
        val entity = reminderDao.getReminderById(id)
            ?.takeIf { it.userId == userId }
            ?: return
        reminderDao.deleteReminder(entity)
        cloudStore.delete(userId, entity.cloudId)
    }

    private fun currentUserId(): String =
        requireNotNull(firebaseAuth.currentUser?.uid) { "Sign in before accessing reminders." }
}
