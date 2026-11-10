package com.expira.app.evaluation

import com.expira.app.extraction.CalendarDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluationMetricsTest {

    private fun date(d: Int, m: Int, y: Int) = checkNotNull(CalendarDate.of(d, m, y))

    @Test
    fun `identical dates are an exact match with zero CER`() {
        val result = EvaluationMetrics.compare(date(12, 8, 2027), date(12, 8, 2027))
        assertTrue(result.extractionSuccess && result.exactMatch && result.dayMatch && result.monthMatch && result.yearMatch)
        assertEquals(0.0, result.cer, 0.0)
    }

    @Test
    fun `wrong day keeps month and year matches`() {
        val result = EvaluationMetrics.compare(date(18, 8, 2027), date(12, 8, 2027))
        assertFalse(result.exactMatch)
        assertFalse(result.dayMatch)
        assertTrue(result.monthMatch && result.yearMatch)
        assertEquals(1.0 / 10, result.cer, 1e-9) // "18-08-2027" vs "12-08-2027": one substitution
    }

    @Test
    fun `no extraction counts as failure with CER 1`() {
        val result = EvaluationMetrics.compare(null, date(12, 8, 2027))
        assertFalse(result.extractionSuccess)
        assertFalse(result.exactMatch)
        assertEquals(1.0, result.cer, 0.0)
    }

    @Test
    fun `levenshtein distance`() {
        assertEquals(0, EvaluationMetrics.levenshtein("abc", "abc"))
        assertEquals(3, EvaluationMetrics.levenshtein("", "abc"))
        assertEquals(3, EvaluationMetrics.levenshtein("kitten", "sitting"))
        assertEquals(2, EvaluationMetrics.levenshtein("10-03-2027", "01-03-2027"))
    }
}
