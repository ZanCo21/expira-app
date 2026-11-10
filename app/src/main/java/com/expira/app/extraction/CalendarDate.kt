package com.expira.app.extraction

/**
 * A calendar date that is always structurally valid (month 1-12, day within the month,
 * leap years respected). Implemented without java.time because minSdk 24 lacks it.
 */
class CalendarDate private constructor(val day: Int, val month: Int, val year: Int) : Comparable<CalendarDate> {

    /** Uniform output format of the app: DD-MM-YYYY. */
    fun format(): String = "%02d-%02d-%04d".format(day, month, year)

    /** Days since 0001-01-01 (proleptic Gregorian); used for day differences. */
    fun toEpochDay(): Long {
        val y = year.toLong() - 1
        var days = y * 365 + y / 4 - y / 100 + y / 400
        for (m in 1 until month) days += daysInMonth(m, year)
        return days + day - 1
    }

    fun daysUntil(other: CalendarDate): Long = other.toEpochDay() - toEpochDay()

    fun plusDays(days: Long): CalendarDate = fromEpochDay(toEpochDay() + days)

    override fun compareTo(other: CalendarDate): Int = compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

    override fun equals(other: Any?): Boolean =
        other is CalendarDate && day == other.day && month == other.month && year == other.year

    override fun hashCode(): Int = (year * 100 + month) * 100 + day

    override fun toString(): String = format()

    companion object {
        /** Returns null when the combination does not exist on the calendar. */
        fun of(day: Int, month: Int, year: Int): CalendarDate? {
            if (year < 1 || month !in 1..12) return null
            if (day !in 1..daysInMonth(month, year)) return null
            return CalendarDate(day, month, year)
        }

        /** Inverse of [toEpochDay]. */
        fun fromEpochDay(epochDay: Long): CalendarDate {
            require(epochDay >= 0) { "Date before 0001-01-01: $epochDay" }
            var year = (epochDay / DAYS_PER_YEAR_APPROX).toInt() + 1
            while (startOfYear(year + 1) <= epochDay) year++
            while (startOfYear(year) > epochDay) year--
            var remaining = epochDay - startOfYear(year)
            var month = 1
            while (remaining >= daysInMonth(month, year)) {
                remaining -= daysInMonth(month, year)
                month++
            }
            return CalendarDate(remaining.toInt() + 1, month, year)
        }

        private const val DAYS_PER_YEAR_APPROX = 365.2425

        /** 1 January always exists, so the private constructor is safe here. */
        private fun startOfYear(year: Int): Long = CalendarDate(1, 1, year).toEpochDay()

        /** Last day of the month: used for month-only dates such as "AUG 2027". */
        fun endOfMonth(month: Int, year: Int): CalendarDate? =
            if (year < 1 || month !in 1..12) null else CalendarDate(daysInMonth(month, year), month, year)

        fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

        fun daysInMonth(month: Int, year: Int): Int = when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
}
