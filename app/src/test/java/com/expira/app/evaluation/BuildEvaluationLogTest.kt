package com.expira.app.evaluation

import com.expira.app.domain.ScanOutcome
import com.expira.app.domain.VariantResult
import com.expira.app.extraction.CalendarDate
import com.expira.app.extraction.ConfidenceLevel
import com.expira.app.extraction.ConfidenceScorer
import com.expira.app.extraction.DateExtractor
import com.expira.app.extraction.DateValidator
import com.expira.app.extraction.TextLine
import com.expira.app.imaging.ConditionalStep
import com.expira.app.imaging.PreprocessConfig
import com.expira.app.ocr.OcrResult
import com.expira.app.ui.evaluation.parseGroundTruth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildEvaluationLogTest {

    private val today = checkNotNull(CalendarDate.of(10, 11, 2026))
    private val groundTruth = checkNotNull(CalendarDate.of(12, 8, 2027))

    private fun outcome(text: String): ScanOutcome {
        val validator = DateValidator(today)
        val extraction = DateExtractor(validator).extract(text.lines().map { TextLine(it) })
        val variant = VariantResult(
            config = PreprocessConfig.P3,
            appliedSteps = setOf(ConditionalStep.DESKEW),
            preprocessMs = 120,
            ocrMs = 480,
            extractMs = 10,
            ocr = OcrResult(text, emptyList()),
            extraction = extraction,
        )
        return ScanOutcome(
            scannedOn = today,
            blurScore = 300.0,
            variants = listOf(variant),
            chosen = variant.takeIf { extraction.best != null },
            assessment = ConfidenceScorer(validator).assess(extraction.best),
            preprocessMs = 120,
            ocrMs = 480,
            extractMs = 10,
            validateMs = 2,
            totalMs = 612,
        )
    }

    @Test
    fun `matching result is logged with timings and applied steps`() {
        val log = buildEvaluationLog(1, "a.jpg", PreprocessConfig.P3, groundTruth, outcome("MFD 12.08.26\nEXP 12.08.27"), 5)
        assertTrue(log.comparison.exactMatch)
        assertEquals("EXP 12.08.27", log.detectedLine)
        assertEquals(setOf(ConditionalStep.DESKEW), log.appliedSteps)
        assertEquals(12L, log.extractValidateMs)
        assertEquals(612L, log.totalMs)
        assertEquals(ConfidenceLevel.HIGH, log.confidenceLevel)
    }

    @Test
    fun `missing outcome is logged as failed extraction`() {
        val log = buildEvaluationLog(1, "a.jpg", PreprocessConfig.P0, groundTruth, null, 5)
        assertFalse(log.comparison.extractionSuccess)
        assertNull(log.extracted)
        assertNull(log.confidence)
        assertEquals(0L, log.totalMs)
    }

    @Test
    fun `ground truth input accepts DD-MM-YYYY and rejects impossible dates`() {
        assertEquals(groundTruth, parseGroundTruth("12-08-2027"))
        assertEquals(groundTruth, parseGroundTruth(" 12/08/2027 "))
        assertEquals(groundTruth, parseGroundTruth("12.8.2027"))
        assertNull(parseGroundTruth("31-02-2027"))
        assertNull(parseGroundTruth("12-08-27"))
        assertNull(parseGroundTruth(""))
    }
}
