package com.expira.app.domain

import android.graphics.Bitmap
import com.expira.app.ocr.OcrEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Runs the scan pipeline on an upright, downscaled bitmap:
 * preprocessing -> OCR -> (extraction, validation: added in Sprint 2 and 3).
 *
 * Sprint 1 runs the P0 baseline (no preprocessing).
 */
class ScanDateUseCase(
    private val ocrEngine: OcrEngine,
    private val clock: () -> Long = { System.nanoTime() / NANOS_PER_MILLI },
) {

    operator fun invoke(bitmap: Bitmap): Flow<ScanEvent> = flow {
        val start = clock()

        emit(ScanEvent.StageStarted(ScanStage.PREPROCESS))
        val preprocessStart = clock()
        val prepared = bitmap // P0 baseline
        val preprocessMs = clock() - preprocessStart
        emit(ScanEvent.StageFinished(ScanStage.PREPROCESS, preprocessMs))

        emit(ScanEvent.StageStarted(ScanStage.OCR))
        val ocrStart = clock()
        val ocr = try {
            ocrEngine.recognize(prepared)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(ScanEvent.Failed(ScanFailure.OCR_FAILED))
            return@flow
        }
        val ocrMs = clock() - ocrStart
        emit(ScanEvent.StageFinished(ScanStage.OCR, ocrMs))

        emit(
            ScanEvent.Completed(
                ScanOutcome(ocr = ocr, preprocessMs = preprocessMs, ocrMs = ocrMs, totalMs = clock() - start),
            ),
        )
    }.flowOn(Dispatchers.Default)

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
