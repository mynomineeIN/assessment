package com.example.androidreminderapp.data.repository

import android.util.Log
import com.example.androidreminderapp.data.local.ReminderEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderCloudStore @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "ReminderCloudStore"
    }

    fun save(userId: String, reminder: ReminderEntity) {
        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .document(reminder.cloudId)
            .set(reminder.toCloudMap())
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to save reminder ${reminder.cloudId} to Firestore", error)
            }
    }

    fun delete(userId: String, cloudId: String) {
        firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .document(cloudId)
            .delete()
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to delete reminder $cloudId from Firestore", error)
            }
    }

    suspend fun load(userId: String): List<ReminderEntity> {
        return firestore.collection("users")
            .document(userId)
            .collection("reminders")
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                val title = document.getString("title") ?: return@mapNotNull null
                ReminderEntity(
                    cloudId = document.id,
                    userId = userId,
                    title = title,
                    description = document.getString("description"),
                    startDate = document.getLong("startDate") ?: return@mapNotNull null,
                    timeInMillis = document.getLong("timeInMillis") ?: return@mapNotNull null,
                    repeatFrequency = document.getString("repeatFrequency") ?: "NONE",
                    selectedWeekDays = document.get("selectedWeekDays")
                        .asIntList(),
                    repeatUntil = document.getLong("repeatUntil"),
                    alarmSound = document.getString("alarmSound") ?: "default",
                    snoozeMinutes = document.getLong("snoozeMinutes")?.toInt() ?: 5,
                    isActive = document.getBoolean("isActive") ?: true,
                    isCompleted = document.getBoolean("isCompleted") ?: false
                )
            }
    }

    private fun ReminderEntity.toCloudMap(): Map<String, Any?> = mapOf(
        "cloudId" to cloudId,
        "userId" to userId,
        "title" to title,
        "description" to description,
        "startDate" to startDate,
        "timeInMillis" to timeInMillis,
        "repeatFrequency" to repeatFrequency,
        "selectedWeekDays" to selectedWeekDays,
        "repeatUntil" to repeatUntil,
        "alarmSound" to alarmSound,
        "snoozeMinutes" to snoozeMinutes,
        "isActive" to isActive,
        "isCompleted" to isCompleted
    )

    private fun Any?.asIntList(): List<Int> =
        (this as? List<*>)
            ?.mapNotNull { (it as? Number)?.toInt() }
            .orEmpty()
}
