package com.example.androidreminderapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.androidreminderapp.domain.util.RecurrenceCalculator
import com.example.androidreminderapp.service.AlarmScheduler
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        ReminderDaoEntryPoint::class.java
                    )
                    val reminderDao = entryPoint.reminderDao()
                    val activeEntities = reminderDao.getActiveReminders()
                    val now = System.currentTimeMillis()

                    for (entity in activeEntities) {
                        val reminder = entity.toDomain()
                        if (reminder.isCompleted || !reminder.isActive) continue

                        var trigger: Long? = reminder.timeInMillis
                        if (trigger != null && trigger <= now) {
                            if (reminder.repeatFrequency != "NONE") {
                                var iterations = 0
                                val maxIterations = 1000
                                while (trigger != null && trigger <= now && iterations < maxIterations) {
                                    trigger = RecurrenceCalculator.calculateNextTriggerTime(
                                        trigger,
                                        reminder.repeatFrequency,
                                        reminder.selectedWeekDays,
                                        reminder.repeatUntil
                                    )
                                    iterations++
                                }
                                if (iterations >= maxIterations) {
                                    trigger = null
                                }
                            } else {
                                trigger = null
                            }
                        }

                        if (trigger != null) {
                            val updated = reminder.copy(
                                timeInMillis = trigger
                            )
                            reminderDao.updateReminder(com.example.androidreminderapp.data.local.ReminderEntity.fromDomain(updated))
                            val scheduled = AlarmScheduler.scheduleAlarm(context, updated, trigger)
                            if (!scheduled) {
                                Log.e(TAG, "Failed to reschedule alarm for reminder ID: ${reminder.id}")
                            }
                        } else {
                            val completed = reminder.copy(isCompleted = true, isActive = false)
                            reminderDao.updateReminder(com.example.androidreminderapp.data.local.ReminderEntity.fromDomain(completed))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on boot/time change", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
