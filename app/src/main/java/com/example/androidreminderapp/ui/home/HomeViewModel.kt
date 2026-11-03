package com.example.androidreminderapp.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidreminderapp.domain.model.Reminder
import com.example.androidreminderapp.domain.repository.AuthRepository
import com.example.androidreminderapp.domain.repository.ReminderRepository
import com.example.androidreminderapp.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = reminderRepository.getAllReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    fun logout() {
        authRepository.signOut()
    }
}
