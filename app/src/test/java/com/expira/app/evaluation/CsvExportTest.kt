package com.expira.app.evaluation

import com.expira.app.domain.ScanRecord
import com.expira.app.domain.ScanTimings
import com.expira.app.extraction.CalendarDate
import com.expira.app.extraction.ConfidenceLevel
import com.expira.app.imaging.ConditionalStep
import com.expira.app.imaging.PreprocessConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportTest {

    private val date = checkNotNull(CalendarDate.of(12, 8, 2027))

    private val log = EvaluationLog(
        id = 1,
        runId = 99,
        imageName = "susu, uht.jpg",
        config = PreprocessConfig.P3,
        groundTruth = date,
        extracted = date,
        comparison = EvaluationMetrics.compare(date, date),
        confidence = 0.9375,
        confidenceLevel = ConfidenceLevel.HIGH,
        appliedSteps = setOf(ConditionalStep.SHARPEN, ConditionalStep.CLOSING),
        blurScore = 87.5,
        rawOcrText = "MFD 12.08.26\nEXP \"12.08.27\"",
        preprocessMs = 120,
        ocrMs = 480,
        extractValidateMs = 12,
        totalMs = 612,
        createdAt = 1000,
    )

    private val scan = ScanRecord(
        id = 7,
        expiry = date,
        detectedDate = null,
        rawOcrText = "SUSU",
        confidence = null,
        confidenceLevel = ConfidenceLevel.LOW,
        wasCorrected = true,
        imagePath = null,
        preprocessConfig = null,
        timings = ScanTimings(1, 2, 3, 4, 10),
        createdAt = 2000,
    )

    private fun rows(csv: String) = csv.split("\r\n").filter { it.isNotEmpty() }

    @Test
    fun `header has one column per field and rows use CRLF`() {
        val csv = CsvExport.build(emptyList(), emptyList())
        assertEquals(CsvExport.HEADER.joinToString(","), rows(csv).single())
        assertTrue(csv.endsWith("\r\n"))
    }

    @Test
    fun `evaluation row quotes commas, quotes and newlines`() {
        val csv = CsvExport.build(listOf(log), emptyList())
        assertTrue(csv.contains("\"susu, uht.jpg\""))
        assertTrue(csv.contains("\"MFD 12.08.26\nEXP \"\"12.08.27\"\"\""))
        assertTrue(csv.contains(",P3,12-08-2027,12-08-2027,12-08-2027,1,1,1,1,1,0.0000,0.9375,HIGH,,SHARPEN|CLOSING,87.5000,120,480,12,612,"))
    }

    @Test
    fun `scan row records correction and combined extract time`() {
        val line = rows(CsvExport.build(emptyList(), listOf(scan)))[1]
        assertTrue(line.startsWith("scan,7,,2000,,,,12-08-2027,,0,"))
        assertTrue(line.contains(",LOW,1,,,1,2,7,10,SUSU"))
    }

    @Test
    fun `escape leaves plain fields alone`() {
        assertEquals("EXP 12.08.27", CsvExport.escape("EXP 12.08.27"))
        assertEquals("\"a\"\"b\"", CsvExport.escape("a\"b"))
    }
}
