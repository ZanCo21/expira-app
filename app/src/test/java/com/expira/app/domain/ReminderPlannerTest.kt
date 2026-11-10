package com.expira.app.domain

import com.expira.app.extraction.CalendarDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderPlannerTest {

    private fun date(d: Int, m: Int, y: Int) = checkNotNull(CalendarDate.of(d, m, y))
    private val today = date(10, 11, 2026)

    @Test
    fun `reminder date is the chosen number of days before expiry`() {
        assertEquals(ReminderPlan.OnDate(date(5, 8, 2027)), ReminderPlanner.plan(date(12, 8, 2027), ReminderOption.H7, today))
        assertEquals(ReminderPlan.OnDate(date(9, 8, 2027)), ReminderPlanner.plan(date(12, 8, 2027), ReminderOption.H3, today))
        assertEquals(ReminderPlan.OnDate(date(28, 2, 2028)), ReminderPlanner.plan(date(29, 2, 2028), ReminderOption.H1, today))
    }

    @Test
    fun `reminder across a month and year boundary`() {
        assertEquals(ReminderPlan.OnDate(date(27, 12, 2026)), ReminderPlanner.plan(date(3, 1, 2027), ReminderOption.H7, today))
    }

    @Test
    fun `reminder on today is still scheduled`() {
        assertEquals(ReminderPlan.OnDate(today), ReminderPlanner.plan(date(13, 11, 2026), ReminderOption.H3, today))
    }

    @Test
    fun `reminder day already passed is too late`() {
        assertEquals(ReminderPlan.TooLate(date(5, 11, 2026)), ReminderPlanner.plan(date(12, 11, 2026), ReminderOption.H7, today))
        // Expired product
        assertEquals(ReminderPlan.TooLate(date(19, 10, 2026)), ReminderPlanner.plan(date(20, 10, 2026), ReminderOption.H1, today))
    }

    @Test
    fun `delay runs until 09-00 local time on the reminder day`() {
        val zone = TimeZone.getTimeZone("Asia/Jakarta")
        val now = Calendar.getInstance(zone).apply { clear(); set(2026, Calendar.NOVEMBER, 10, 15, 30, 0) }.timeInMillis
        val expected = (24 * 60 - (15 * 60 + 30) + 9 * 60) * 60_000L // tomorrow 09:00
        assertEquals(expected, ReminderPlanner.delayUntil(date(11, 11, 2026), now, zone))
    }

    @Test
    fun `delay is zero when 09-00 has already passed today`() {
        val zone = TimeZone.getTimeZone("Asia/Jakarta")
        val now = Calendar.getInstance(zone).apply { clear(); set(2026, Calendar.NOVEMBER, 10, 15, 0, 0) }.timeInMillis
        assertEquals(0L, ReminderPlanner.delayUntil(today, now, zone))
    }
}
