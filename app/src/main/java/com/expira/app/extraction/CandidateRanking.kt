package com.expira.app.extraction

/**
 * Orders date candidates from most to least likely expiry date:
 * expiry keyword > no keyword > production keyword; then keyword on the same line,
 * full date over month-only, later date, higher OCR confidence.
 */
object CandidateRanking {

    val ORDER: Comparator<DateCandidate> = compareBy<DateCandidate> { kindRank(it.keyword.kind) }
        .thenByDescending { it.keyword.sameLine }
        .thenByDescending { it.format.hasDay }
        .thenByDescending { it.date }
        .thenByDescending { it.ocrConfidence ?: -1f }

    /**
     * A candidate strong enough that more preprocessing variants are unlikely to beat it:
     * an expiry keyword on the same line and a full day-month-year date.
     */
    fun isStrong(candidate: DateCandidate): Boolean =
        candidate.keyword.kind == KeywordKind.EXPIRY && candidate.keyword.sameLine && candidate.format.hasDay

    private fun kindRank(kind: KeywordKind): Int = when (kind) {
        KeywordKind.EXPIRY -> 0
        KeywordKind.NONE -> 1
        KeywordKind.PRODUCTION -> 2
    }
}
