package com.example.androidreminderapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.androidreminderapp.domain.model.Reminder

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String?,
    val startDate: Long,
    val timeInMillis: Long,
    val repeatFrequency: String, // DAILY, WEEKLY, MONTHLY, YEARLY, NONE
    val selectedWeekDays: List<Int>, // e.g. [1, 2, 3, 4, 5, 6, 7]
    val repeatUntil: Long?,
    val alarmSound: String,
    val snoozeMinutes: Int,
    val isActive: Boolean,
    val isCompleted: Boolean
) {
    fun toDomain(): Reminder {
        return Reminder(
            id = id,
            title = title,
            description = description,
            startDate = startDate,
            timeInMillis = timeInMillis,
            repeatFrequency = repeatFrequency,
            selectedWeekDays = selectedWeekDays,
            repeatUntil = repeatUntil,
            alarmSound = alarmSound,
            snoozeMinutes = snoozeMinutes,
            isActive = isActive,
            isCompleted = isCompleted
        )
    }

    companion object {
        fun fromDomain(reminder: Reminder): ReminderEntity {
            return ReminderEntity(
                id = reminder.id,
                title = reminder.title,
                description = reminder.description,
                startDate = reminder.startDate,
                timeInMillis = reminder.timeInMillis,
                repeatFrequency = reminder.repeatFrequency,
                selectedWeekDays = reminder.selectedWeekDays,
                repeatUntil = reminder.repeatUntil,
                alarmSound = reminder.alarmSound,
                snoozeMinutes = reminder.snoozeMinutes,
                isActive = reminder.isActive,
                isCompleted = reminder.isCompleted
            )
        }
    }
}
