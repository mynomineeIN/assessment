package com.example.androidreminderapp.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidreminderapp.domain.model.Reminder
import com.example.androidreminderapp.domain.repository.ReminderRepository
import com.example.androidreminderapp.service.AlarmScheduler
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _cloudSyncError = MutableStateFlow<String?>(null)
    val cloudSyncError: StateFlow<String?> = _cloudSyncError.asStateFlow()
    private var isSynchronizing = false

    val reminders: StateFlow<List<Reminder>> = reminderRepository.getAllReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun synchronizeCloudReminders(context: Context) {
        if (isSynchronizing) return
        isSynchronizing = true
        viewModelScope.launch {
            try {
                reminderRepository.synchronizeCloudReminders()
                reminderRepository.getActiveReminders().forEach { reminder ->
                    AlarmScheduler.scheduleAlarm(context, reminder, reminder.timeInMillis)
                }
                _cloudSyncError.value = null
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to synchronize reminders with Firestore", e)
                _cloudSyncError.value =
                    "Cloud reminders couldn't be loaded. Your device reminders are still available."
            } finally {
                isSynchronizing = false
            }
        }
    }

    fun toggleReminderStatus(context: Context, reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isActive = !reminder.isActive)
            reminderRepository.updateReminder(updated)
            if (updated.isActive) {
                AlarmScheduler.scheduleAlarm(context, updated, updated.timeInMillis)
            } else {
                AlarmScheduler.cancelAlarm(context, updated.id)
            }
        }
    }

    fun deleteReminder(context: Context, reminder: Reminder) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(context, reminder.id)
            reminderRepository.deleteReminder(reminder)
        }
    }
}
