package com.example.androidreminderapp.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.androidreminderapp.domain.model.Reminder
import com.example.androidreminderapp.receiver.AlarmReceiver

object AlarmScheduler {
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_EXPECTED_TRIGGER = "extra_expected_trigger"
    const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
    const val EXTRA_REMINDER_DESC = "extra_reminder_desc"

    private fun getPendingIntentRequestCode(reminderId: Long): Int {
        return (reminderId xor (reminderId ushr 32)).toInt()
    }

    fun scheduleAlarm(context: Context, reminder: Reminder, triggerAtMillis: Long): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_EXPECTED_TRIGGER, triggerAtMillis)
            putExtra(EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(EXTRA_REMINDER_DESC, reminder.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getPendingIntentRequestCode(reminder.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var scheduled = false
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            scheduled = true
        } catch (e: SecurityException) {
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                scheduled = true
            } catch (_: Exception) {
                scheduled = false
            }
        } catch (_: Exception) {
            scheduled = false
        }

        if (!scheduled) {
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                scheduled = true
            } catch (_: Exception) {
                scheduled = false
            }
        }

        return scheduled
    }

    fun cancelAlarm(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getPendingIntentRequestCode(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
