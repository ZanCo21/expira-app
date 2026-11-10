package com.expira.app.domain

import com.expira.app.extraction.CalendarDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpiryStatusTest {

    private val today = checkNotNull(CalendarDate.of(10, 11, 2026))

    private fun statusOn(d: Int, m: Int, y: Int) = ExpiryStatusCalculator.of(checkNotNull(CalendarDate.of(d, m, y)), today)

    @Test
    fun `more than 30 days left is safe`() {
        assertEquals(ExpiryInfo(ExpiryStatus.SAFE, 275), statusOn(12, 8, 2027))
        assertEquals(ExpiryStatus.SAFE, statusOn(11, 12, 2026).status) // 31 days
    }

    @Test
    fun `0 to 30 days left is near`() {
        assertEquals(ExpiryInfo(ExpiryStatus.NEAR, 30), statusOn(10, 12, 2026))
        assertEquals(ExpiryInfo(ExpiryStatus.NEAR, 23), statusOn(3, 12, 2026))
        assertEquals(ExpiryInfo(ExpiryStatus.NEAR, 0), statusOn(10, 11, 2026)) // expires today
    }

    @Test
    fun `past date is expired`() {
        assertEquals(ExpiryInfo(ExpiryStatus.EXPIRED, -1), statusOn(9, 11, 2026))
        assertEquals(ExpiryStatus.EXPIRED, statusOn(20, 10, 2026).status)
    }

    @Test
    fun `system today is a valid date`() {
        SystemTodayProvider.today() // throws if invalid
    }
}
