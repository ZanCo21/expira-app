package com.expira.app.domain

import com.expira.app.extraction.ExtractionResult
import com.expira.app.imaging.ConditionalStep
import com.expira.app.imaging.PreprocessConfig
import com.expira.app.ocr.OcrResult

/** Pipeline stages, in the order shown on the "Memproses" screen. */
enum class ScanStage { PREPROCESS, OCR, EXTRACT, VALIDATE }

enum class ScanFailure { IMAGE_UNREADABLE, OCR_FAILED }

/** Progress events emitted while one image goes through the pipeline. */
sealed interface ScanEvent {
    data class StageStarted(val stage: ScanStage) : ScanEvent
    data class StageFinished(val stage: ScanStage, val durationMs: Long) : ScanEvent
    data class Completed(val outcome: ScanOutcome) : ScanEvent
    data class Failed(val reason: ScanFailure) : ScanEvent
}

/** Result of running OCR + extraction on one preprocessing variant. */
data class VariantResult(
    val config: PreprocessConfig,
    val appliedSteps: Set<ConditionalStep>,
    val preprocessMs: Long,
    val ocrMs: Long,
    val extractMs: Long,
    val ocr: OcrResult,
    val extraction: ExtractionResult,
)

data class ScanOutcome(
    /** Variance of Laplacian of the input image, or null when OpenCV is unavailable. */
    val blurScore: Double?,
    /** Variants in the order they ran (later ones may be skipped after a strong result). */
    val variants: List<VariantResult>,
    /** The variant whose candidate was chosen, or null when no variant found a date. */
    val chosen: VariantResult?,
    val preprocessMs: Long,
    val ocrMs: Long,
    val extractMs: Long,
    val totalMs: Long,
) {
    val extraction: ExtractionResult get() = chosen?.extraction ?: ExtractionResult.EMPTY

    /** OCR text of the chosen variant, or of the first variant when nothing was found. */
    val rawText: String get() = (chosen ?: variants.firstOrNull())?.ocr?.text.orEmpty()
}
