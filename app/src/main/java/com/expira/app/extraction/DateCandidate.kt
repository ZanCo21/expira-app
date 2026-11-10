package com.expira.app.extraction

/** One line of OCR output, as seen by the extractor. */
data class TextLine(
    val text: String,
    /** OCR engine confidence in 0..1, or null when not reported. */
    val confidence: Float? = null,
)

/** Supported date layouts (CLAUDE.md "Format yang didukung"). */
enum class DateFormat {
    /** DD/MM/YYYY, DD-MM-YY, DD.MM.YYYY */
    DAY_MONTH_YEAR_NUMERIC,

    /** YYYY-MM-DD */
    YEAR_MONTH_DAY_NUMERIC,

    /** DD MMM YYYY, DDMMMYY */
    DAY_MONTHNAME_YEAR,

    /** MMM YYYY (day = last day of month) */
    MONTHNAME_YEAR,

    /** MM/YYYY (day = last day of month) */
    MONTH_YEAR_NUMERIC,
    ;

    val hasDay: Boolean get() = this != MONTHNAME_YEAR && this != MONTH_YEAR_NUMERIC
}

enum class KeywordKind { EXPIRY, PRODUCTION, NONE }

/** Which keyword governs a date and how close it is. */
data class KeywordContext(
    val kind: KeywordKind,
    /** The keyword text as found, e.g. "EXP". */
    val keyword: String? = null,
    /** True when the keyword is on the same line as the date, false when on the line above. */
    val sameLine: Boolean = false,
) {
    companion object {
        val NONE = KeywordContext(KeywordKind.NONE)
    }
}

data class DateCandidate(
    val date: CalendarDate,
    val format: DateFormat,
    /** The date as it appeared in the OCR text, before character correction. */
    val rawText: String,
    /** The full OCR line the date came from. */
    val sourceLine: String,
    val lineIndex: Int,
    val keyword: KeywordContext,
    /** Letter-to-digit fixes applied inside [rawText]. */
    val corrections: List<CharCorrection>,
    val ocrConfidence: Float?,
)

data class ExtractionResult(
    /** The chosen expiry date, or null when none can be taken safely. */
    val best: DateCandidate?,
    /** All valid dates found, best-ranked first (includes [best]). */
    val candidates: List<DateCandidate>,
) {
    companion object {
        val EMPTY = ExtractionResult(best = null, candidates = emptyList())
    }
}
