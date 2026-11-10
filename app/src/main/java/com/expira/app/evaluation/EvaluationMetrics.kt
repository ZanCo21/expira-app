package com.expira.app.evaluation

import com.expira.app.extraction.CalendarDate

/** Per-image comparison of the extracted date with the ground truth. */
data class DateComparison(
    val extractionSuccess: Boolean,
    val exactMatch: Boolean,
    val dayMatch: Boolean,
    val monthMatch: Boolean,
    val yearMatch: Boolean,
    /**
     * Character error rate of the extracted date against the ground truth, both in
     * DD-MM-YYYY form (1.0 when nothing was extracted).
     */
    val cer: Double,
)

object EvaluationMetrics {

    fun compare(extracted: CalendarDate?, groundTruth: CalendarDate): DateComparison {
        if (extracted == null) {
            return DateComparison(
                extractionSuccess = false,
                exactMatch = false,
                dayMatch = false,
                monthMatch = false,
                yearMatch = false,
                cer = 1.0,
            )
        }
        return DateComparison(
            extractionSuccess = true,
            exactMatch = extracted == groundTruth,
            dayMatch = extracted.day == groundTruth.day,
            monthMatch = extracted.month == groundTruth.month,
            yearMatch = extracted.year == groundTruth.year,
            cer = characterErrorRate(extracted.format(), groundTruth.format()),
        )
    }

    /** Levenshtein distance divided by the reference length. */
    fun characterErrorRate(hypothesis: String, reference: String): Double {
        if (reference.isEmpty()) return if (hypothesis.isEmpty()) 0.0 else 1.0
        return levenshtein(hypothesis, reference).toDouble() / reference.length
    }

    fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, substitution)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
