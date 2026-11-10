package com.expira.app.domain

import com.expira.app.extraction.CalendarDate
import java.util.Calendar

/** Status chip on results and history (Aman / Mendekati / Kedaluwarsa). */
enum class ExpiryStatus { SAFE, NEAR, EXPIRED }

object ExpiryRules {
    /** Agreed: "Mendekati" when 0-30 days remain. */
    const val NEAR_DAYS = 30
}

data class ExpiryInfo(val status: ExpiryStatus, val daysRemaining: Long)

object ExpiryStatusCalculator {
    fun of(expiry: CalendarDate, today: CalendarDate): ExpiryInfo {
        val days = today.daysUntil(expiry)
        val status = when {
            days < 0 -> ExpiryStatus.EXPIRED
            days <= ExpiryRules.NEAR_DAYS -> ExpiryStatus.NEAR
            else -> ExpiryStatus.SAFE
        }
        return ExpiryInfo(status, days)
    }
}

/** Source of "today" so date logic stays testable. */
fun interface TodayProvider {
    fun today(): CalendarDate
}

object SystemTodayProvider : TodayProvider {
    override fun today(): CalendarDate {
        val now = Calendar.getInstance()
        return checkNotNull(
            CalendarDate.of(now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.MONTH) + 1, now.get(Calendar.YEAR)),
        )
    }
}
