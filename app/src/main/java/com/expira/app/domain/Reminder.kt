package com.expira.app.domain

import com.expira.app.extraction.CalendarDate
import java.util.Calendar
import java.util.TimeZone

/** PRD FR-21: remind 7, 3 or 1 day(s) before expiry; H-7 is the default. */
enum class ReminderOption(val daysBefore: Int) {
    H7(7),
    H3(3),
    H1(1),
    ;

    companion object {
        val DEFAULT = H7
    }
}

/** State of a history entry's reminder, stored with the entry. */
enum class ReminderStatus {
    SCHEDULED,

    /** The notification has been shown. */
    SENT,

    /** Not scheduled because the reminder day had already passed. */
    TOO_LATE,

    /** Not scheduled because notification permission was denied (Android 13+). */
    PERMISSION_DENIED,
}

data class ReminderInfo(
    val option: ReminderOption,
    val date: CalendarDate,
    val status: ReminderStatus,
)

/** The user's reminder choice for the scan under review (before it is saved). */
data class ReminderChoice(val enabled: Boolean, val option: ReminderOption = ReminderOption.DEFAULT)

sealed interface ReminderPlan {
    data class OnDate(val date: CalendarDate) : ReminderPlan

    /** The reminder day is already in the past (or the product has expired). */
    data class TooLate(val date: CalendarDate) : ReminderPlan
}

object ReminderPlanner {
    /** Agreed: notifications go out at about 09:00 local time (WorkManager, not exact). */
    const val NOTIFY_HOUR = 9

    fun plan(expiry: CalendarDate, option: ReminderOption, today: CalendarDate): ReminderPlan {
        val date = expiry.plusDays(-option.daysBefore.toLong())
        return if (date >= today) ReminderPlan.OnDate(date) else ReminderPlan.TooLate(date)
    }

    /** Milliseconds from [nowMillis] until [date] at [NOTIFY_HOUR]:00 in [zone]; 0 when already due. */
    fun delayUntil(date: CalendarDate, nowMillis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val target = Calendar.getInstance(zone).apply {
            clear()
            set(date.year, date.month - 1, date.day, NOTIFY_HOUR, 0, 0)
        }
        return (target.timeInMillis - nowMillis).coerceAtLeast(0)
    }
}
