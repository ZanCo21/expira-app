package com.expira.app.data

import com.expira.app.domain.ScanRecord
import com.expira.app.domain.ScanTimings
import com.expira.app.extraction.CalendarDate
import com.expira.app.extraction.ConfidenceLevel
import com.expira.app.imaging.PreprocessConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Local scan history backed by Room; maps between entities and domain records. */
class ScanHistoryRepository(private val dao: ScanDao) {

    fun observeAll(): Flow<List<ScanRecord>> = dao.observeAll().map { list -> list.mapNotNull { it.toRecord() } }

    fun observeRecent(limit: Int): Flow<List<ScanRecord>> =
        dao.observeRecent(limit).map { list -> list.mapNotNull { it.toRecord() } }

    suspend fun save(record: ScanRecord): Long = dao.insert(record.toEntity())

    suspend fun getAll(): List<ScanRecord> = dao.getAll().mapNotNull { it.toRecord() }
}

private fun ScanRecord.toEntity() = ScanEntity(
    id = id,
    expiryEpochDay = expiry.toEpochDay(),
    expiryDay = expiry.day,
    expiryMonth = expiry.month,
    expiryYear = expiry.year,
    detectedDate = detectedDate?.format(),
    rawOcrText = rawOcrText,
    confidence = confidence,
    confidenceLevel = confidenceLevel?.name,
    wasCorrected = wasCorrected,
    imagePath = imagePath,
    preprocessConfig = preprocessConfig?.name,
    preprocessMs = timings?.preprocessMs,
    ocrMs = timings?.ocrMs,
    extractMs = timings?.extractMs,
    validateMs = timings?.validateMs,
    totalMs = timings?.totalMs,
    createdAt = createdAt,
)

/** Returns null for a corrupt row instead of crashing the list. */
private fun ScanEntity.toRecord(): ScanRecord? {
    val expiry = CalendarDate.of(expiryDay, expiryMonth, expiryYear) ?: return null
    return ScanRecord(
        id = id,
        expiry = expiry,
        detectedDate = detectedDate?.let(::parseStoredDate),
        rawOcrText = rawOcrText,
        confidence = confidence,
        confidenceLevel = confidenceLevel?.let { runCatching { ConfidenceLevel.valueOf(it) }.getOrNull() },
        wasCorrected = wasCorrected,
        imagePath = imagePath,
        preprocessConfig = preprocessConfig?.let { runCatching { PreprocessConfig.valueOf(it) }.getOrNull() },
        timings = if (totalMs != null) {
            ScanTimings(preprocessMs ?: 0, ocrMs ?: 0, extractMs ?: 0, validateMs ?: 0, totalMs)
        } else {
            null
        },
        createdAt = createdAt,
    )
}

/** Parses the DD-MM-YYYY format written by [CalendarDate.format]. */
internal fun parseStoredDate(text: String): CalendarDate? {
    val parts = text.split("-").mapNotNull { it.toIntOrNull() }
    return if (parts.size == 3) CalendarDate.of(parts[0], parts[1], parts[2]) else null
}
