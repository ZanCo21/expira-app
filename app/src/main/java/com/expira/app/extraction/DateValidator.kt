package com.expira.app.extraction

/** Validation constants (initial values; calibrate with the test dataset). */
object ValidationRules {
    /** Accepted year range relative to the scan year (agreed: -5 .. +10). */
    const val YEARS_BEFORE = 5
    const val YEARS_AFTER = 10

    /** A date is fully plausible from this many days ago up to [PLAUSIBLE_FUTURE_DAYS] ahead. */
    const val PLAUSIBLE_PAST_DAYS = 365
    const val PLAUSIBLE_FUTURE_DAYS = 5 * 365 + 1

    /** Plausibility for valid dates outside the plausible window (old expiry or far future). */
    const val LOW_PLAUSIBILITY = 0.5
}

sealed interface ValidationResult {
    data class Valid(val date: CalendarDate) : ValidationResult

    /** The day/month/year combination does not exist (e.g. 31-02 or month 13). */
    data object InvalidDate : ValidationResult

    /** The date exists but its year is outside the accepted range. */
    data class OutOfRange(val minYear: Int, val maxYear: Int) : ValidationResult
}

/** Checks that an expiry date is real and reasonable relative to [today]. */
class DateValidator(private val today: CalendarDate) {

    val minYear: Int get() = today.year - ValidationRules.YEARS_BEFORE
    val maxYear: Int get() = today.year + ValidationRules.YEARS_AFTER

    fun isAcceptable(date: CalendarDate): Boolean = date.year in minYear..maxYear

    /** Validates manual input from the correction screen. */
    fun validate(day: Int?, month: Int?, year: Int?): ValidationResult {
        if (day == null || month == null || year == null) return ValidationResult.InvalidDate
        val date = CalendarDate.of(day, month, year) ?: return ValidationResult.InvalidDate
        return if (isAcceptable(date)) ValidationResult.Valid(date) else ValidationResult.OutOfRange(minYear, maxYear)
    }

    /** 1.0 inside the plausible window, [ValidationRules.LOW_PLAUSIBILITY] otherwise, 0 when not acceptable. */
    fun plausibility(date: CalendarDate): Double {
        if (!isAcceptable(date)) return 0.0
        val daysAhead = today.daysUntil(date)
        val inWindow = daysAhead in -ValidationRules.PLAUSIBLE_PAST_DAYS..ValidationRules.PLAUSIBLE_FUTURE_DAYS
        return if (inWindow) 1.0 else ValidationRules.LOW_PLAUSIBILITY
    }
}
