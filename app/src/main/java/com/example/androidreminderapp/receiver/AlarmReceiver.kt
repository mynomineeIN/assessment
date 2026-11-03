package com.example.androidreminderapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.androidreminderapp.data.local.ReminderDao
import com.example.androidreminderapp.domain.util.RecurrenceCalculator
import com.example.androidreminderapp.service.AlarmScheduler
import com.example.androidreminderapp.service.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderDaoEntryPoint {
    fun reminderDao(): ReminderDao
}

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        val expectedTrigger = intent.getLongExtra(AlarmScheduler.EXTRA_EXPECTED_TRIGGER, -1L)
        if (reminderId == -1L) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ReminderDaoEntryPoint::class.java
                )
                val reminderDao = entryPoint.reminderDao()
                val entity = reminderDao.getReminderById(reminderId)
                if (entity != null) {
                    val reminder = entity.toDomain()
                    val now = System.currentTimeMillis()

                    if (reminder.timeInMillis > now + 120_000) {
                        AlarmScheduler.scheduleAlarm(context, reminder, reminder.timeInMillis)
                        return@launch
                    }

                    if (expectedTrigger != -1L && reminder.timeInMillis > expectedTrigger) {
                        return@launch
                    }

                    if (reminder.isActive && !reminder.isCompleted) {
                        if (reminder.repeatFrequency != "NONE") {
                            val nextTrigger = RecurrenceCalculator.calculateNextTriggerTime(
                                reminder.timeInMillis,
                                reminder.repeatFrequency,
                                reminder.selectedWeekDays,
                                reminder.repeatUntil
                            )
                            if (nextTrigger != null && nextTrigger > now) {
                                val rows = reminderDao.claimAndAdvanceOccurrence(
                                    id = reminder.id,
                                    expectedTrigger = reminder.timeInMillis,
                                    nextTrigger = nextTrigger
                                )
                                if (rows > 0) {
                                    NotificationHelper.showReminderNotification(context, reminder.id, reminder.title, reminder.description)
                                    val updated = reminder.copy(timeInMillis = nextTrigger)
                                    val scheduled = AlarmScheduler.scheduleAlarm(context, updated, nextTrigger)
                                    if (!scheduled) {
                                        Log.e(TAG, "Failed to schedule next recurrence for reminder ID: $reminderId")
                                    }
                                }
                            } else {
                                val rows = reminderDao.claimAndCompleteOccurrence(
                                    id = reminder.id,
                                    expectedTrigger = reminder.timeInMillis
                                )
                                if (rows > 0) {
                                    NotificationHelper.showReminderNotification(context, reminder.id, reminder.title, reminder.description)
                                }
                            }
                        } else {
                            val rows = reminderDao.claimAndCompleteOccurrence(
                                id = reminder.id,
                                expectedTrigger = reminder.timeInMillis
                            )
                            if (rows > 0) {
                                NotificationHelper.showReminderNotification(context, reminder.id, reminder.title, reminder.description)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing alarm reminder ID: $reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
