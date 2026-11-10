package com.expira.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One evaluation row (image x configuration), Room table `evaluation_logs`. */
@Entity(tableName = "evaluation_logs", indices = [Index("run_id")])
data class EvaluationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "run_id") val runId: Long,
    @ColumnInfo(name = "image_name") val imageName: String,
    @ColumnInfo(name = "config") val config: String,
    @ColumnInfo(name = "ground_truth") val groundTruth: String,
    @ColumnInfo(name = "extracted_date") val extractedDate: String?,
    @ColumnInfo(name = "extraction_success") val extractionSuccess: Boolean,
    @ColumnInfo(name = "exact_match") val exactMatch: Boolean,
    @ColumnInfo(name = "day_match") val dayMatch: Boolean,
    @ColumnInfo(name = "month_match") val monthMatch: Boolean,
    @ColumnInfo(name = "year_match") val yearMatch: Boolean,
    @ColumnInfo(name = "cer") val cer: Double,
    @ColumnInfo(name = "confidence") val confidence: Double?,
    @ColumnInfo(name = "confidence_level") val confidenceLevel: String,
    /** Conditional P3 steps joined with "|", empty when none ran. */
    @ColumnInfo(name = "applied_steps") val appliedSteps: String,
    @ColumnInfo(name = "blur_score") val blurScore: Double?,
    @ColumnInfo(name = "detected_line") val detectedLine: String?,
    @ColumnInfo(name = "raw_ocr_text") val rawOcrText: String,
    @ColumnInfo(name = "preprocess_ms") val preprocessMs: Long,
    @ColumnInfo(name = "ocr_ms") val ocrMs: Long,
    @ColumnInfo(name = "extract_validate_ms") val extractValidateMs: Long,
    @ColumnInfo(name = "total_ms") val totalMs: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
