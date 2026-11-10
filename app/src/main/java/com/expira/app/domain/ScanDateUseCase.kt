package com.expira.app.domain

import android.graphics.Bitmap
import com.expira.app.extraction.ConfidenceScorer
import com.expira.app.extraction.DateExtractor
import com.expira.app.extraction.DateValidator
import com.expira.app.extraction.TextLine
import com.expira.app.imaging.BlurDetector
import com.expira.app.imaging.ConditionalStep
import com.expira.app.imaging.ImagePreprocessor
import com.expira.app.imaging.PreprocessConfig
import com.expira.app.ocr.OcrEngine
import com.expira.app.ocr.OcrResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Runs the scan pipeline on an upright, downscaled bitmap:
 * preprocessing (all variants) -> OCR + extraction per variant -> best variant
 * -> validation and confidence score.
 *
 * @param preprocessor null when OpenCV is unavailable; only P0 then runs.
 */
class ScanDateUseCase(
    private val ocrEngine: OcrEngine,
    private val preprocessor: ImagePreprocessor?,
    private val todayProvider: TodayProvider = SystemTodayProvider,
    private val clock: () -> Long = { System.nanoTime() / NANOS_PER_MILLI },
) {

    operator fun invoke(bitmap: Bitmap, strategy: ScanStrategy = ScanStrategy.DEFAULT): Flow<ScanEvent> = flow {
        val start = clock()
        val today = todayProvider.today()
        val validator = DateValidator(today)
        val extractor = DateExtractor(validator)

        emit(ScanEvent.StageStarted(ScanStage.PREPROCESS))
        val preprocessStart = clock()
        val blurScore = preprocessor?.let { runCatching { BlurDetector.score(bitmap) }.getOrNull() }
        val inputs = prepareVariants(bitmap, strategy, blurScore)
        emit(ScanEvent.StageFinished(ScanStage.PREPROCESS, clock() - preprocessStart))

        emit(ScanEvent.StageStarted(ScanStage.OCR))
        val variants = runVariants(inputs, extractor, stopEarly = strategy is ScanStrategy.MultiVariant)
        inputs.filter { it.bitmap !== bitmap }.forEach { it.bitmap.recycle() }
        if (variants.isEmpty()) {
            emit(ScanEvent.Failed(ScanFailure.OCR_FAILED))
            return@flow
        }
        val ocrMs = variants.sumOf { it.ocrMs }
        emit(ScanEvent.StageFinished(ScanStage.OCR, ocrMs))

        val extractMs = variants.sumOf { it.extractMs }
        emit(ScanEvent.StageStarted(ScanStage.EXTRACT))
        emit(ScanEvent.StageFinished(ScanStage.EXTRACT, extractMs))

        emit(ScanEvent.StageStarted(ScanStage.VALIDATE))
        val validateStart = clock()
        val chosen = VariantSelection.choose(variants)
        val assessment = ConfidenceScorer(validator).assess(chosen?.extraction?.best)
        val validateMs = clock() - validateStart
        emit(ScanEvent.StageFinished(ScanStage.VALIDATE, validateMs))

        val outcome = ScanOutcome(
            scannedOn = today,
            blurScore = blurScore,
            variants = variants,
            chosen = chosen,
            assessment = assessment,
            preprocessMs = inputs.sumOf { it.preprocessMs },
            ocrMs = ocrMs,
            extractMs = extractMs,
            validateMs = validateMs,
            totalMs = clock() - start,
        )
        emit(ScanEvent.Completed(outcome))
    }.flowOn(Dispatchers.Default)

    private fun prepareVariants(bitmap: Bitmap, strategy: ScanStrategy, blurScore: Double?): List<VariantInput> {
        val configs = if (preprocessor == null) listOf(PreprocessConfig.P0) else strategy.configs.distinct()
        return configs.mapNotNull { config ->
            val begin = clock()
            val result = runCatching { preprocessor?.process(bitmap, config, blurScore) }.getOrNull()
            when {
                config == PreprocessConfig.P0 -> VariantInput(config, bitmap, emptySet(), clock() - begin)
                result == null -> null
                else -> VariantInput(config, result.bitmap, result.appliedSteps, clock() - begin)
            }
        }
    }

    private suspend fun runVariants(
        inputs: List<VariantInput>,
        extractor: DateExtractor,
        stopEarly: Boolean,
    ): List<VariantResult> {
        val results = mutableListOf<VariantResult>()
        for (input in inputs) {
            val variant = runVariant(input, extractor) ?: continue
            results += variant
            if (stopEarly && VariantSelection.isStrong(variant)) break
        }
        return results
    }

    /** Returns null when OCR fails on this variant; other variants may still succeed. */
    private suspend fun runVariant(input: VariantInput, extractor: DateExtractor): VariantResult? {
        val ocrStart = clock()
        val ocr = try {
            ocrEngine.recognize(input.bitmap)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
        val ocrMs = clock() - ocrStart
        val extractStart = clock()
        val extraction = extractor.extract(ocr.toTextLines())
        return VariantResult(
            config = input.config,
            appliedSteps = input.appliedSteps,
            preprocessMs = input.preprocessMs,
            ocrMs = ocrMs,
            extractMs = clock() - extractStart,
            ocr = ocr,
            extraction = extraction,
        )
    }

    private fun OcrResult.toTextLines(): List<TextLine> = lines.map { TextLine(it.text, it.confidence) }

    private class VariantInput(
        val config: PreprocessConfig,
        val bitmap: Bitmap,
        val appliedSteps: Set<ConditionalStep>,
        val preprocessMs: Long,
    )

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
