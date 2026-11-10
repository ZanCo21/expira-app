package com.expira.app.evaluation

import com.expira.app.extraction.CalendarDate
import com.expira.app.extraction.ConfidenceLevel
import com.expira.app.imaging.ConditionalStep
import com.expira.app.imaging.PreprocessConfig

/** One evaluation row: one image processed with one preprocessing configuration. */
data class EvaluationLog(
    val id: Long = 0,
    /** Shared by the P0-P3 rows of the same test. */
    val runId: Long,
    val imageName: String,
    val config: PreprocessConfig,
    val groundTruth: CalendarDate,
    val extracted: CalendarDate?,
    val comparison: DateComparison,
    val confidence: Double?,
    val confidenceLevel: ConfidenceLevel,
    val appliedSteps: Set<ConditionalStep>,
    val blurScore: Double?,
    /** OCR line the extracted date came from, null when nothing was extracted. */
    val detectedLine: String?,
    val rawOcrText: String,
    val preprocessMs: Long,
    val ocrMs: Long,
    /** Extraction and validation together (Figma "Ekstraksi + validasi"). */
    val extractValidateMs: Long,
    val totalMs: Long,
    val createdAt: Long,
)
