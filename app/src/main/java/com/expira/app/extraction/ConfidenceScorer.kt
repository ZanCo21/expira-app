package com.expira.app.extraction

enum class ConfidenceLevel { HIGH, MEDIUM, LOW }

/**
 * Central scoring constants (CLAUDE.md "Tingkat keyakinan").
 * Thresholds and weights are initial values and must be calibrated with test data.
 */
object ScoringParams {
    const val HIGH_THRESHOLD = 0.80
    const val MEDIUM_THRESHOLD = 0.50

    /** Agreed: equal weights for the four components. */
    const val WEIGHT_OCR = 0.25
    const val WEIGHT_PATTERN = 0.25
    const val WEIGHT_KEYWORD = 0.25
    const val WEIGHT_PLAUSIBILITY = 0.25

    const val PATTERN_FULL_DATE = 1.0
    const val PATTERN_MONTH_ONLY = 0.7
    const val PATTERN_PENALTY_PER_CORRECTION = 0.1
    const val PATTERN_MIN = 0.5

    const val KEYWORD_EXPIRY_SAME_LINE = 1.0
    const val KEYWORD_EXPIRY_LINE_ABOVE = 0.8
    const val KEYWORD_NONE = 0.2
    const val KEYWORD_PRODUCTION = 0.0
}

/** Per-component scores in 0..1, kept for the evaluation log. */
data class ConfidenceBreakdown(
    /** Null when the OCR engine reported no confidence for the line. */
    val ocr: Double?,
    val pattern: Double,
    val keyword: Double,
    val plausibility: Double,
)

data class ConfidenceAssessment(
    val score: Double,
    val level: ConfidenceLevel,
    val breakdown: ConfidenceBreakdown?,
) {
    companion object {
        /** No candidate at all: shown as "Tanggal belum terbaca". */
        val NONE = ConfidenceAssessment(score = 0.0, level = ConfidenceLevel.LOW, breakdown = null)
    }
}

/**
 * Combines (1) OCR confidence, (2) date pattern quality, (3) expiry keyword near the date
 * and (4) date plausibility into one score. Without OCR confidence the remaining weights
 * are renormalised.
 */
class ConfidenceScorer(private val validator: DateValidator) {

    fun assess(candidate: DateCandidate?): ConfidenceAssessment {
        if (candidate == null) return ConfidenceAssessment.NONE
        val breakdown = ConfidenceBreakdown(
            ocr = candidate.ocrConfidence?.toDouble()?.coerceIn(0.0, 1.0),
            pattern = patternScore(candidate),
            keyword = keywordScore(candidate.keyword),
            plausibility = validator.plausibility(candidate.date),
        )
        val score = combine(breakdown)
        return ConfidenceAssessment(score, levelOf(score), breakdown)
    }

    private fun patternScore(candidate: DateCandidate): Double {
        val base = if (candidate.format.hasDay) ScoringParams.PATTERN_FULL_DATE else ScoringParams.PATTERN_MONTH_ONLY
        val penalty = candidate.corrections.size * ScoringParams.PATTERN_PENALTY_PER_CORRECTION
        return (base - penalty).coerceAtLeast(ScoringParams.PATTERN_MIN)
    }

    private fun keywordScore(keyword: KeywordContext): Double = when (keyword.kind) {
        KeywordKind.EXPIRY ->
            if (keyword.sameLine) ScoringParams.KEYWORD_EXPIRY_SAME_LINE else ScoringParams.KEYWORD_EXPIRY_LINE_ABOVE
        KeywordKind.NONE -> ScoringParams.KEYWORD_NONE
        KeywordKind.PRODUCTION -> ScoringParams.KEYWORD_PRODUCTION
    }

    private fun combine(b: ConfidenceBreakdown): Double {
        var weighted = ScoringParams.WEIGHT_PATTERN * b.pattern +
            ScoringParams.WEIGHT_KEYWORD * b.keyword +
            ScoringParams.WEIGHT_PLAUSIBILITY * b.plausibility
        var totalWeight = ScoringParams.WEIGHT_PATTERN + ScoringParams.WEIGHT_KEYWORD + ScoringParams.WEIGHT_PLAUSIBILITY
        if (b.ocr != null) {
            weighted += ScoringParams.WEIGHT_OCR * b.ocr
            totalWeight += ScoringParams.WEIGHT_OCR
        }
        return (weighted / totalWeight).coerceIn(0.0, 1.0)
    }

    companion object {
        fun levelOf(score: Double): ConfidenceLevel = when {
            score >= ScoringParams.HIGH_THRESHOLD -> ConfidenceLevel.HIGH
            score >= ScoringParams.MEDIUM_THRESHOLD -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
    }
}
