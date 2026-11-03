package com.example.androidreminderapp

import com.example.androidreminderapp.domain.util.RecurrenceCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class RecurrenceTest {

    @Test
    fun testDailyRecurrence() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val expected = cal.timeInMillis

        val next = RecurrenceCalculator.calculateNextTriggerTime(current, "DAILY", emptyList(), null)
        assertEquals(expected, next)
    }

    @Test
    fun testWeeklyRecurrenceWithoutWeekdays() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = cal.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        val expected = cal.timeInMillis

        val next = RecurrenceCalculator.calculateNextTriggerTime(current, "WEEKLY", emptyList(), null)
        assertEquals(expected, next)
    }

    @Test
    fun testWeeklyRecurrenceWithWeekdays() {
        // Jan 1, 2026 is Thursday (Calendar.THURSDAY = 5)
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = cal.timeInMillis
        // Select Friday (Calendar.FRIDAY = 6)
        val next = RecurrenceCalculator.calculateNextTriggerTime(current, "WEEKLY", listOf(Calendar.FRIDAY), null)
        assertNotNull(next)
        val resultCal = Calendar.getInstance().apply { timeInMillis = next!! }
        assertEquals(Calendar.FRIDAY, resultCal.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun testMonthlyRecurrenceMonthEnd() {
        // Jan 31, 2026 + 1 month -> Feb 28, 2026
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 31, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = cal.timeInMillis
        val next = RecurrenceCalculator.calculateNextTriggerTime(current, "MONTHLY", emptyList(), null)
        assertNotNull(next)
        val resultCal = Calendar.getInstance().apply { timeInMillis = next!! }
        assertEquals(Calendar.FEBRUARY, resultCal.get(Calendar.MONTH))
        assertEquals(28, resultCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testYearlyRecurrenceLeapYear() {
        // Feb 29, 2028 (leap year) + 1 year -> Feb 28, 2029
        val cal = Calendar.getInstance().apply {
            set(2028, Calendar.FEBRUARY, 29, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = cal.timeInMillis
        val next = RecurrenceCalculator.calculateNextTriggerTime(current, "YEARLY", emptyList(), null)
        assertNotNull(next)
        val resultCal = Calendar.getInstance().apply { timeInMillis = next!! }
        assertEquals(2029, resultCal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, resultCal.get(Calendar.MONTH))
        assertEquals(28, resultCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testRepeatUntilBoundary() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = cal.timeInMillis
        val repeatUntil = current + (12 * 60 * 60 * 1000L)

        val next = RecurrenceCalculator.calculateNextTriggerTime(current, "DAILY", emptyList(), repeatUntil)
        assertNull(next)
    }

    @Test
    fun testOneTimeReminderNoneFrequency() {
        val current = System.currentTimeMillis()
        val next = RecurrenceCalculator.calculateNextTriggerTime(current, "NONE", emptyList(), null)
        assertNull(next)
    }
}
