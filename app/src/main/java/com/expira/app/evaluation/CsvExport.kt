package com.expira.app.evaluation

import com.expira.app.domain.ScanRecord

/**
 * Builds one CSV with evaluation rows (source = evaluation) and saved scans
 * (source = scan); columns that do not apply to a source stay empty.
 * Fields follow RFC 4180 quoting; decimals use a dot.
 */
object CsvExport {

    val HEADER = listOf(
        "source", "id", "run_id", "created_at", "image", "config", "ground_truth", "result_date",
        "detected_date", "extraction_success", "exact_match", "day_match", "month_match", "year_match",
        "cer", "confidence", "confidence_level", "was_corrected", "applied_steps", "blur_score",
        "preprocess_ms", "ocr_ms", "extract_validate_ms", "total_ms", "detected_line", "raw_ocr_text",
    )

    fun build(logs: List<EvaluationLog>, scans: List<ScanRecord>): String = buildString {
        appendRow(HEADER)
        logs.forEach { appendRow(it.toFields()) }
        scans.forEach { appendRow(it.toFields()) }
    }

    private fun EvaluationLog.toFields(): List<String> = listOf(
        "evaluation", id.toString(), runId.toString(), createdAt.toString(), imageName, config.name,
        groundTruth.format(), extracted?.format().orEmpty(), extracted?.format().orEmpty(),
        comparison.extractionSuccess.flag(), comparison.exactMatch.flag(), comparison.dayMatch.flag(),
        comparison.monthMatch.flag(), comparison.yearMatch.flag(), comparison.cer.decimal(),
        confidence?.decimal().orEmpty(), confidenceLevel.name, "",
        appliedSteps.joinToString("|") { it.name }, blurScore?.decimal().orEmpty(),
        preprocessMs.toString(), ocrMs.toString(), extractValidateMs.toString(), totalMs.toString(),
        detectedLine.orEmpty(), rawOcrText,
    )

    private fun ScanRecord.toFields(): List<String> = listOf(
        "scan", id.toString(), "", createdAt.toString(), imagePath.orEmpty(), preprocessConfig?.name.orEmpty(),
        "", expiry.format(), detectedDate?.format().orEmpty(),
        (detectedDate != null).flag(), "", "", "", "", "",
        confidence?.decimal().orEmpty(), confidenceLevel?.name.orEmpty(), wasCorrected.flag(), "", "",
        timings?.preprocessMs?.toString().orEmpty(), timings?.ocrMs?.toString().orEmpty(),
        timings?.let { (it.extractMs + it.validateMs).toString() }.orEmpty(),
        timings?.totalMs?.toString().orEmpty(), "", rawOcrText,
    )

    private fun StringBuilder.appendRow(fields: List<String>) {
        append(fields.joinToString(",") { escape(it) })
        append("\r\n")
    }

    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    private fun Boolean.flag(): String = if (this) "1" else "0"

    private fun Double.decimal(): String = "%.4f".format(java.util.Locale.ROOT, this)
}
