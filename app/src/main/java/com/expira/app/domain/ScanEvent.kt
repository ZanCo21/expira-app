package com.expira.app.domain

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

data class ScanOutcome(
    val ocr: OcrResult,
    val preprocessMs: Long,
    val ocrMs: Long,
    val totalMs: Long,
)
