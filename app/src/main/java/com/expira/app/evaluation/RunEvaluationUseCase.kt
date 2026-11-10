package com.expira.app.evaluation

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.createBitmap
import com.expira.app.data.EvaluationRepository
import com.expira.app.domain.ScanDateUseCase
import com.expira.app.domain.ScanEvent
import com.expira.app.domain.ScanOutcome
import com.expira.app.domain.ScanStrategy
import com.expira.app.extraction.CalendarDate
import com.expira.app.extraction.ConfidenceLevel
import com.expira.app.imaging.BitmapLoader
import com.expira.app.imaging.PreprocessConfig
import com.expira.app.ocr.OcrEngine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull

/**
 * Evaluation mode: runs P0, P1, P2 and P3 on the same image, compares each result
 * with the ground truth and stores one log row per configuration.
 */
class RunEvaluationUseCase(
    private val bitmapLoader: BitmapLoader,
    private val scanDate: ScanDateUseCase,
    private val ocrEngine: OcrEngine,
    private val repository: EvaluationRepository,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var isWarmedUp = false

    /** @return the stored rows, or null when the image cannot be read */
    suspend operator fun invoke(
        imageUri: Uri,
        imageName: String,
        groundTruth: CalendarDate,
        onConfigStarted: (PreprocessConfig) -> Unit = {},
    ): List<EvaluationLog>? {
        val bitmap = bitmapLoader.load(imageUri) ?: return null
        warmUpOcr()
        val runId = now()
        val logs = PreprocessConfig.entries.map { config ->
            onConfigStarted(config)
            val outcome = scanDate(bitmap, ScanStrategy.Single(config))
                .filterIsInstance<ScanEvent.Completed>()
                .firstOrNull()
                ?.outcome
            buildEvaluationLog(runId, imageName, config, groundTruth, outcome, now())
        }
        repository.saveRun(logs)
        return logs
    }

    /**
     * The first ML Kit call also loads the model. Doing one unmeasured call first keeps
     * that one-off cost out of the P0 timing, so configurations are compared fairly.
     */
    private suspend fun warmUpOcr() {
        if (isWarmedUp) return
        runCatching { ocrEngine.recognize(blankBitmap()) }
        isWarmedUp = true
    }

    private fun blankBitmap(): Bitmap = createBitmap(WARM_UP_SIZE, WARM_UP_SIZE).apply { eraseColor(Color.WHITE) }

    private companion object {
        const val WARM_UP_SIZE = 64
    }
}

/** A failed OCR run (no outcome) is still logged as a failed extraction. */
internal fun buildEvaluationLog(
    runId: Long,
    imageName: String,
    config: PreprocessConfig,
    groundTruth: CalendarDate,
    outcome: ScanOutcome?,
    createdAt: Long,
): EvaluationLog {
    val extracted = outcome?.extraction?.best?.date
    val assessment = outcome?.assessment
    return EvaluationLog(
        runId = runId,
        imageName = imageName,
        config = config,
        groundTruth = groundTruth,
        extracted = extracted,
        comparison = EvaluationMetrics.compare(extracted, groundTruth),
        confidence = assessment?.takeIf { it.breakdown != null }?.score,
        confidenceLevel = assessment?.level ?: ConfidenceLevel.LOW,
        appliedSteps = outcome?.chosen?.appliedSteps.orEmpty(),
        blurScore = outcome?.blurScore,
        detectedLine = outcome?.extraction?.best?.sourceLine?.trim(),
        rawOcrText = outcome?.rawText.orEmpty(),
        preprocessMs = outcome?.preprocessMs ?: 0,
        ocrMs = outcome?.ocrMs ?: 0,
        extractValidateMs = (outcome?.extractMs ?: 0) + (outcome?.validateMs ?: 0),
        totalMs = outcome?.totalMs ?: 0,
        createdAt = createdAt,
    )
}
