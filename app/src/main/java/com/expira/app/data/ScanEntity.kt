package com.expira.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One saved scan in the local history (Room table `scans`). */
@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Final expiry date (after confirmation or correction) as epoch day, for sorting and filtering. */
    @ColumnInfo(name = "expiry_epoch_day") val expiryEpochDay: Long,
    @ColumnInfo(name = "expiry_day") val expiryDay: Int,
    @ColumnInfo(name = "expiry_month") val expiryMonth: Int,
    @ColumnInfo(name = "expiry_year") val expiryYear: Int,
    /** Date proposed by the pipeline (DD-MM-YYYY), null when nothing was detected. */
    @ColumnInfo(name = "detected_date") val detectedDate: String?,
    @ColumnInfo(name = "raw_ocr_text") val rawOcrText: String,
    @ColumnInfo(name = "confidence") val confidence: Double?,
    @ColumnInfo(name = "confidence_level") val confidenceLevel: String?,
    @ColumnInfo(name = "was_corrected") val wasCorrected: Boolean,
    @ColumnInfo(name = "image_path") val imagePath: String?,
    @ColumnInfo(name = "preprocess_config") val preprocessConfig: String?,
    @ColumnInfo(name = "preprocess_ms") val preprocessMs: Long?,
    @ColumnInfo(name = "ocr_ms") val ocrMs: Long?,
    @ColumnInfo(name = "extract_ms") val extractMs: Long?,
    @ColumnInfo(name = "validate_ms") val validateMs: Long?,
    @ColumnInfo(name = "total_ms") val totalMs: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
