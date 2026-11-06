package com.example.androidreminderapp.domain.model

data class Reminder(
    val id: Long = 0L,
    val cloudId: String = java.util.UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String,
    val description: String? = null,
    val startDate: Long,
    val timeInMillis: Long,
    val repeatFrequency: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY, YEARLY
    val selectedWeekDays: List<Int> = emptyList(),
    val repeatUntil: Long? = null,
    val alarmSound: String = "default",
    val snoozeMinutes: Int = 5,
    val isActive: Boolean = true,
    val isCompleted: Boolean = false
)
