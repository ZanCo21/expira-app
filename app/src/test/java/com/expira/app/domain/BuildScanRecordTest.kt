package com.expira.app.domain

import com.expira.app.extraction.CalendarDate
import com.expira.app.extraction.ConfidenceAssessment
import com.expira.app.extraction.ConfidenceLevel
import com.expira.app.extraction.ConfidenceScorer
import com.expira.app.extraction.DateExtractor
import com.expira.app.extraction.DateValidator
import com.expira.app.extraction.TextLine
import com.expira.app.imaging.PreprocessConfig
import com.expira.app.ocr.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildScanRecordTest {

    private val today = checkNotNull(CalendarDate.of(10, 11, 2026))
    private val validator = DateValidator(today)

    private fun outcome(vararg lines: String): ScanOutcome {
        val extraction = DateExtractor(validator).extract(lines.map { TextLine(it) })
        val variant = VariantResult(
            config = PreprocessConfig.P3,
            appliedSteps = emptySet(),
            preprocessMs = 120,
            ocrMs = 480,
            extractMs = 10,
            ocr = OcrResult(lines.joinToString("\n"), emptyList()),
            extraction = extraction,
        )
        val chosen = variant.takeIf { extraction.best != null }
        return ScanOutcome(
            scannedOn = today,
            blurScore = 500.0,
            variants = listOf(variant),
            chosen = chosen,
            assessment = ConfidenceScorer(validator).assess(extraction.best),
            preprocessMs = 120,
            ocrMs = 480,
            extractMs = 10,
            validateMs = 2,
            totalMs = 612,
        )
    }

    private fun date(d: Int, m: Int, y: Int) = checkNotNull(CalendarDate.of(d, m, y))

    @Test
    fun `accepting the detected date is not a correction`() {
        val record = buildScanRecord(outcome("EXP 12.08.27"), date(12, 8, 2027), "/img.jpg", createdAt = 5)
        assertFalse(record.wasCorrected)
        assertEquals(date(12, 8, 2027), record.detectedDate)
        assertEquals(ConfidenceLevel.HIGH, record.confidenceLevel)
        assertEquals(PreprocessConfig.P3, record.preprocessConfig)
        assertEquals(612L, record.timings?.totalMs)
        assertEquals("EXP 12.08.27", record.rawOcrText)
    }

    @Test
    fun `a different final date is a correction`() {
        val record = buildScanRecord(outcome("EXP 12.08.27"), date(18, 8, 2027), null, createdAt = 5)
        assertTrue(record.wasCorrected)
        assertEquals(date(18, 8, 2027), record.expiry)
    }

    @Test
    fun `manual input after failed detection is a correction without score`() {
        val failed = outcome("SUSU UHT")
        assertEquals(ConfidenceAssessment.NONE, failed.assessment)
        val record = buildScanRecord(failed, date(1, 1, 2027), null, createdAt = 5)
        assertTrue(record.wasCorrected)
        assertNull(record.detectedDate)
        assertNull(record.confidence)
        assertNull(record.preprocessConfig)
    }
}
