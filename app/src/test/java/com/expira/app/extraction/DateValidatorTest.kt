package com.expira.app.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateValidatorTest {

    private val today = checkNotNull(CalendarDate.of(10, 11, 2026))
    private val validator = DateValidator(today)

    private fun date(d: Int, m: Int, y: Int) = checkNotNull(CalendarDate.of(d, m, y))

    @Test
    fun `accepted years run from five years back to ten years ahead`() {
        assertEquals(2021, validator.minYear)
        assertEquals(2036, validator.maxYear)
        assertTrue(validator.isAcceptable(date(1, 1, 2021)))
        assertTrue(validator.isAcceptable(date(31, 12, 2036)))
        assertFalse(validator.isAcceptable(date(31, 12, 2020)))
        assertFalse(validator.isAcceptable(date(1, 1, 2037)))
    }

    @Test
    fun `manual input with a valid date passes`() {
        assertEquals(ValidationResult.Valid(date(10, 3, 2027)), validator.validate(10, 3, 2027))
    }

    @Test
    fun `manual input with impossible or missing parts is invalid`() {
        assertEquals(ValidationResult.InvalidDate, validator.validate(31, 2, 2027))
        assertEquals(ValidationResult.InvalidDate, validator.validate(10, 13, 2027))
        assertEquals(ValidationResult.InvalidDate, validator.validate(29, 2, 2027))
        assertEquals(ValidationResult.InvalidDate, validator.validate(null, 3, 2027))
    }

    @Test
    fun `manual input outside the year range is rejected with the range`() {
        assertEquals(ValidationResult.OutOfRange(2021, 2036), validator.validate(1, 1, 2040))
    }

    @Test
    fun `plausibility is full for near dates and lower for distant ones`() {
        assertEquals(1.0, validator.plausibility(date(12, 8, 2027)), 0.0)
        assertEquals(1.0, validator.plausibility(date(20, 10, 2026)), 0.0) // expired 3 weeks ago
        assertEquals(ValidationRules.LOW_PLAUSIBILITY, validator.plausibility(date(1, 1, 2023)), 0.0)
        assertEquals(ValidationRules.LOW_PLAUSIBILITY, validator.plausibility(date(1, 1, 2035)), 0.0)
        assertEquals(0.0, validator.plausibility(date(1, 1, 2040)), 0.0)
    }

    @Test
    fun `extractor drops dates outside the accepted range`() {
        val extractor = DateExtractor(validator)
        assertNull(extractor.extract(listOf(TextLine("EXP 12/08/2045"))).best)
        assertEquals("12-08-2027", extractor.extract(listOf(TextLine("EXP 12/08/2027"))).best?.date?.format())
    }
}
