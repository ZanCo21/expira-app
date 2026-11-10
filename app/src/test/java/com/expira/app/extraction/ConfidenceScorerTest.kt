package com.expira.app.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidenceScorerTest {

    private val today = checkNotNull(CalendarDate.of(10, 11, 2026))
    private val validator = DateValidator(today)
    private val extractor = DateExtractor(validator)
    private val scorer = ConfidenceScorer(validator)

    private fun assess(vararg lines: String, confidence: Float? = null): ConfidenceAssessment =
        scorer.assess(extractor.extract(lines.map { TextLine(it, confidence) }).best)

    @Test
    fun `clean expiry date with keyword is high`() {
        val result = assess("EXP 12.08.27", confidence = 0.95f)
        assertEquals(ConfidenceLevel.HIGH, result.level)
        assertEquals((0.95 + 1 + 1 + 1) / 4, result.score, 1e-6)
    }

    @Test
    fun `without OCR confidence the other three components are averaged`() {
        val result = assess("EXP 12.08.27")
        assertNull(result.breakdown?.ocr)
        assertEquals(1.0, result.score, 1e-9)
    }

    @Test
    fun `date without any keyword is not high`() {
        val result = assess("12.08.27", confidence = 0.95f)
        assertEquals(ConfidenceLevel.MEDIUM, result.level)
    }

    @Test
    fun `character corrections lower the pattern score and the total`() {
        val corrected = assess("EXP 1O.O3.27", confidence = 0.6f)
        val clean = assess("EXP 10.03.27", confidence = 0.6f)
        assertEquals(0.8, checkNotNull(corrected.breakdown).pattern, 1e-9)
        assertTrue(corrected.score < clean.score)
    }

    @Test
    fun `month-only date scores lower on pattern`() {
        val result = assess("EXP AUG 2027")
        assertEquals(ScoringParams.PATTERN_MONTH_ONLY, checkNotNull(result.breakdown).pattern, 1e-9)
    }

    @Test
    fun `keyword on the line above scores below same-line keyword`() {
        val result = assess("EXP", "12.08.27")
        assertEquals(ScoringParams.KEYWORD_EXPIRY_LINE_ABOVE, checkNotNull(result.breakdown).keyword, 1e-9)
    }

    @Test
    fun `no candidate is low with zero score`() {
        assertEquals(ConfidenceAssessment.NONE, scorer.assess(null))
        assertEquals(ConfidenceLevel.LOW, assess("SUSU UHT").level)
    }

    @Test
    fun `levels follow the central thresholds`() {
        assertEquals(ConfidenceLevel.HIGH, ConfidenceScorer.levelOf(0.80))
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceScorer.levelOf(0.7999))
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceScorer.levelOf(0.50))
        assertEquals(ConfidenceLevel.LOW, ConfidenceScorer.levelOf(0.4999))
    }
}
