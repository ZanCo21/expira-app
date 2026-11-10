package com.expira.app.domain

import com.expira.app.extraction.CalendarDate
import com.expira.app.extraction.ConfidenceLevel
import com.expira.app.imaging.PreprocessConfig

/** A saved scan as used by the UI and use cases. */
data class ScanRecord(
    val id: Long = 0,
    val expiry: CalendarDate,
    /** What the pipeline proposed; null when nothing was detected (manual input). */
    val detectedDate: CalendarDate?,
    val rawOcrText: String,
    val confidence: Double?,
    val confidenceLevel: ConfidenceLevel?,
    val wasCorrected: Boolean,
    val imagePath: String?,
    val preprocessConfig: PreprocessConfig?,
    val timings: ScanTimings?,
    val createdAt: Long,
)

data class ScanTimings(
    val preprocessMs: Long,
    val ocrMs: Long,
    val extractMs: Long,
    val validateMs: Long,
    val totalMs: Long,
)
