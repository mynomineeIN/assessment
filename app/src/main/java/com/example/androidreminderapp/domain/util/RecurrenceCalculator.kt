package com.example.androidreminderapp.domain.util

import java.util.Calendar

object RecurrenceCalculator {

    fun calculateNextTriggerTime(
        currentTimeMillis: Long,
        frequency: String,
        selectedWeekDays: List<Int>,
        repeatUntil: Long?
    ): Long? {
        if (repeatUntil != null && currentTimeMillis > repeatUntil) {
            return null
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
        }

        val nextTime = when (frequency) {
            "DAILY" -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.timeInMillis
            }
            "WEEKLY" -> {
                if (selectedWeekDays.isEmpty()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                } else {
                    // Find next matching weekday (Calendar.SUNDAY = 1, MONDAY = 2, etc.)
                    var found = false
                    for (i in 1..7) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                        if (selectedWeekDays.contains(dayOfWeek)) {
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }
                calendar.timeInMillis
            }
            "MONTHLY" -> {
                calendar.add(Calendar.MONTH, 1)
                calendar.timeInMillis
            }
            "YEARLY" -> {
                calendar.add(Calendar.YEAR, 1)
                calendar.timeInMillis
            }
            else -> null
        }

        if (nextTime != null && repeatUntil != null && nextTime > repeatUntil) {
            return null
        }

        return nextTime
    }
}
