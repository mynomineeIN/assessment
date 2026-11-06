package com.example.androidreminderapp.ui.reminder

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidreminderapp.domain.model.Reminder
import com.example.androidreminderapp.domain.repository.ReminderRepository
import com.example.androidreminderapp.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    var reminderId: Long = 0L
    var cloudId by mutableStateOf("")
    var userId by mutableStateOf("")
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var startDate by mutableStateOf(System.currentTimeMillis())
    var timeInMillis by mutableStateOf(System.currentTimeMillis())
    var repeatFrequency by mutableStateOf("NONE")
    var selectedWeekDays by mutableStateOf<List<Int>>(emptyList())
    var repeatUntil by mutableStateOf<Long?>(null)
    var alarmSound by mutableStateOf("default")
    var snoozeMinutes by mutableStateOf(5)
    var isActive by mutableStateOf(true)
    var isCompleted by mutableStateOf(false)

    var errorMessage by mutableStateOf<String?>(null)
    var isSaving by mutableStateOf(false)

    fun loadReminder(id: Long) {
        if (id <= 0L) return
        reminderId = id
        viewModelScope.launch {
            reminderRepository.getReminderById(id)?.let { reminder ->
                cloudId = reminder.cloudId
                userId = reminder.userId
                title = reminder.title
                description = reminder.description ?: ""
                startDate = reminder.startDate
                timeInMillis = reminder.timeInMillis
                repeatFrequency = reminder.repeatFrequency
                selectedWeekDays = reminder.selectedWeekDays
                repeatUntil = reminder.repeatUntil
                alarmSound = reminder.alarmSound
                snoozeMinutes = reminder.snoozeMinutes
                isActive = reminder.isActive
                isCompleted = reminder.isCompleted
            }
        }
    }

    fun saveReminder(context: Context, onSaved: () -> Unit) {
        if (isSaving) return
        if (title.isBlank()) {
            errorMessage = "Reminder title cannot be blank."
            return
        }
        if (repeatFrequency == "WEEKLY" && selectedWeekDays.isEmpty()) {
            errorMessage = "Please select at least one weekday for weekly recurrence."
            return
        }
        if (snoozeMinutes <= 0) {
            errorMessage = "Snooze duration must be greater than zero."
            return
        }
        if (repeatUntil != null && repeatUntil!! < startDate) {
            errorMessage = "Repeat Until date cannot be before the start date."
            return
        }

        isSaving = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val reminder = Reminder(
                    id = reminderId,
                    cloudId = cloudId.ifBlank { java.util.UUID.randomUUID().toString() },
                    userId = userId,
                    title = title.trim(),
                    description = description.ifBlank { null },
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

                if (reminderId == 0L) {
                    val id = reminderRepository.insertReminder(reminder)
                    val savedReminder = reminder.copy(id = id)
                    if (savedReminder.isActive) {
                        AlarmScheduler.scheduleAlarm(context, savedReminder, savedReminder.timeInMillis)
                    }
                } else {
                    reminderRepository.updateReminder(reminder)
                    if (reminder.isActive) {
                        AlarmScheduler.scheduleAlarm(context, reminder, reminder.timeInMillis)
                    } else {
                        AlarmScheduler.cancelAlarm(context, reminder.id)
                    }
                }
                onSaved()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to save reminder."
                isSaving = false
            }
        }
    }
}
