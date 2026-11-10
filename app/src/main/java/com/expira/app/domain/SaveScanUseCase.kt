package com.expira.app.domain

import android.graphics.Bitmap
import com.expira.app.data.ScanHistoryRepository
import com.expira.app.extraction.CalendarDate
import com.expira.app.extraction.ConfidenceLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Saves the reviewed scan (photo + record) to the local history. */
class SaveScanUseCase(
    private val repository: ScanHistoryRepository,
    private val imageDir: File,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** @return the id of the new history entry */
    suspend operator fun invoke(session: ScanSession, finalDate: CalendarDate): Long {
        val createdAt = now()
        val imagePath = withContext(Dispatchers.IO) {
            runCatching { storeImage(session.image, createdAt) }.getOrNull()
        }
        return repository.save(buildScanRecord(session.outcome, finalDate, imagePath, createdAt))
    }

    private fun storeImage(image: Bitmap, createdAt: Long): String {
        imageDir.mkdirs()
        val file = File(imageDir, "scan_$createdAt.jpg")
        file.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return file.absolutePath
    }

    private companion object {
        const val JPEG_QUALITY = 90
    }
}

/**
 * Builds the history record. A scan counts as corrected when the final date differs from
 * the date the pipeline proposed (or when nothing was proposed and the user typed one).
 */
fun buildScanRecord(outcome: ScanOutcome, finalDate: CalendarDate, imagePath: String?, createdAt: Long): ScanRecord {
    val detected = outcome.extraction.best?.date
    val assessment = outcome.assessment
    val hasDetection = assessment.breakdown != null
    return ScanRecord(
        expiry = finalDate,
        detectedDate = detected,
        rawOcrText = outcome.rawText,
        confidence = if (hasDetection) assessment.score else null,
        confidenceLevel = if (hasDetection) assessment.level else ConfidenceLevel.LOW,
        wasCorrected = detected != finalDate,
        imagePath = imagePath,
        preprocessConfig = outcome.chosen?.config,
        timings = ScanTimings(
            preprocessMs = outcome.preprocessMs,
            ocrMs = outcome.ocrMs,
            extractMs = outcome.extractMs,
            validateMs = outcome.validateMs,
            totalMs = outcome.totalMs,
        ),
        createdAt = createdAt,
    )
}
