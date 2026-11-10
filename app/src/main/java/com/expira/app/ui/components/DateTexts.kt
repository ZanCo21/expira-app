package com.expira.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.expira.app.R
import com.expira.app.extraction.CalendarDate
import kotlin.math.abs

/** "12 Agustus 2027" with Indonesian month names. */
@Composable
fun longDate(date: CalendarDate): String {
    val months = stringArrayResource(R.array.month_names)
    return "${date.day} ${months[date.month - 1]} ${date.year}"
}

/** "Sisa 275 hari", or "Lewat 3 hari" for an expired date. */
@Composable
fun remainingDaysText(daysRemaining: Long): String {
    val days = abs(daysRemaining).toInt()
    return if (daysRemaining >= 0) {
        stringResource(R.string.days_remaining, days)
    } else {
        stringResource(R.string.days_overdue, days)
    }
}
