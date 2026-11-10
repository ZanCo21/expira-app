package com.expira.app.data

import com.expira.app.evaluation.DateComparison
import com.expira.app.evaluation.EvaluationLog
import com.expira.app.extraction.ConfidenceLevel
import com.expira.app.imaging.ConditionalStep
import com.expira.app.imaging.PreprocessConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Local evaluation log backed by Room. */
class EvaluationRepository(private val dao: EvaluationDao) {

    suspend fun saveRun(logs: List<EvaluationLog>) = dao.insertAll(logs.map { it.toEntity() })

    fun observeLatestRun(): Flow<List<EvaluationLog>> = dao.observeLatestRun().map { rows -> rows.mapNotNull { it.toLog() } }

    suspend fun getAll(): List<EvaluationLog> = dao.getAll().mapNotNull { it.toLog() }
}

private fun EvaluationLog.toEntity() = EvaluationLogEntity(
    id = id,
    runId = runId,
    imageName = imageName,
    config = config.name,
    groundTruth = groundTruth.format(),
    extractedDate = extracted?.format(),
    extractionSuccess = comparison.extractionSuccess,
    exactMatch = comparison.exactMatch,
    dayMatch = comparison.dayMatch,
    monthMatch = comparison.monthMatch,
    yearMatch = comparison.yearMatch,
    cer = comparison.cer,
    confidence = confidence,
    confidenceLevel = confidenceLevel.name,
    appliedSteps = appliedSteps.joinToString("|") { it.name },
    blurScore = blurScore,
    detectedLine = detectedLine,
    rawOcrText = rawOcrText,
    preprocessMs = preprocessMs,
    ocrMs = ocrMs,
    extractValidateMs = extractValidateMs,
    totalMs = totalMs,
    createdAt = createdAt,
)

/** Returns null for a corrupt row instead of failing the whole list. */
private fun EvaluationLogEntity.toLog(): EvaluationLog? {
    val config = runCatching { PreprocessConfig.valueOf(config) }.getOrNull() ?: return null
    val groundTruth = parseStoredDate(groundTruth) ?: return null
    return EvaluationLog(
        id = id,
        runId = runId,
        imageName = imageName,
        config = config,
        groundTruth = groundTruth,
        extracted = extractedDate?.let(::parseStoredDate),
        comparison = DateComparison(extractionSuccess, exactMatch, dayMatch, monthMatch, yearMatch, cer),
        confidence = confidence,
        confidenceLevel = runCatching { ConfidenceLevel.valueOf(confidenceLevel) }.getOrDefault(ConfidenceLevel.LOW),
        appliedSteps = appliedSteps.split("|").mapNotNull { name ->
            runCatching { ConditionalStep.valueOf(name) }.getOrNull()
        }.toSet(),
        blurScore = blurScore,
        detectedLine = detectedLine,
        rawOcrText = rawOcrText,
        preprocessMs = preprocessMs,
        ocrMs = ocrMs,
        extractValidateMs = extractValidateMs,
        totalMs = totalMs,
        createdAt = createdAt,
    )
}
