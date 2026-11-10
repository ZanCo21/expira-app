package com.expira.app.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarDateTest {

    @Test
    fun `formats as DD-MM-YYYY`() {
        assertEquals("05-03-2027", CalendarDate.of(5, 3, 2027)?.format())
    }

    @Test
    fun `rejects month outside 1 to 12`() {
        assertNull(CalendarDate.of(10, 13, 2027))
        assertNull(CalendarDate.of(10, 0, 2027))
    }

    @Test
    fun `rejects day beyond month length`() {
        assertNull(CalendarDate.of(31, 4, 2027))
        assertNull(CalendarDate.of(32, 1, 2027))
        assertNull(CalendarDate.of(0, 1, 2027))
    }

    @Test
    fun `29 February only exists in leap years`() {
        assertNotNull(CalendarDate.of(29, 2, 2028))
        assertNull(CalendarDate.of(29, 2, 2027))
        assertNull(CalendarDate.of(29, 2, 2100))
        assertNotNull(CalendarDate.of(29, 2, 2000))
    }

    @Test
    fun `leap year rules`() {
        assertTrue(CalendarDate.isLeapYear(2028))
        assertFalse(CalendarDate.isLeapYear(2026))
        assertFalse(CalendarDate.isLeapYear(1900))
        assertTrue(CalendarDate.isLeapYear(2000))
    }

    @Test
    fun `end of month handles February in leap year`() {
        assertEquals("29-02-2028", CalendarDate.endOfMonth(2, 2028)?.format())
        assertEquals("28-02-2027", CalendarDate.endOfMonth(2, 2027)?.format())
        assertEquals("31-08-2027", CalendarDate.endOfMonth(8, 2027)?.format())
    }

    @Test
    fun `days between dates`() {
        val today = checkNotNull(CalendarDate.of(10, 11, 2026))
        assertEquals(275L, today.daysUntil(checkNotNull(CalendarDate.of(12, 8, 2027))))
        assertEquals(120L, today.daysUntil(checkNotNull(CalendarDate.of(10, 3, 2027))))
        assertEquals(-21L, today.daysUntil(checkNotNull(CalendarDate.of(20, 10, 2026))))
    }
}
