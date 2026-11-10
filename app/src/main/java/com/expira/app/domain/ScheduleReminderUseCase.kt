package com.expira.app.domain

import com.expira.app.data.ReminderScheduler
import com.expira.app.data.ScanHistoryRepository
import com.expira.app.extraction.CalendarDate

/**
 * Applies the user's reminder choice to a saved history entry (FR-20, FR-23, FR-24):
 * schedules it, or records why it was not scheduled. Any earlier job is replaced.
 */
class ScheduleReminderUseCase(
    private val repository: ScanHistoryRepository,
    private val scheduler: ReminderScheduler,
    private val canNotify: () -> Boolean,
    private val todayProvider: TodayProvider = SystemTodayProvider,
) {

    /** @return the stored reminder, or null when no reminder was requested */
    suspend operator fun invoke(scanId: Long, expiry: CalendarDate, choice: ReminderChoice?): ReminderInfo? {
        scheduler.cancel(scanId)
        if (choice == null || !choice.enabled) {
            repository.updateReminder(scanId, null)
            return null
        }
        val info = when (val plan = ReminderPlanner.plan(expiry, choice.option, todayProvider.today())) {
            is ReminderPlan.TooLate -> ReminderInfo(choice.option, plan.date, ReminderStatus.TOO_LATE)
            is ReminderPlan.OnDate -> if (canNotify()) {
                scheduler.schedule(scanId, plan.date)
                ReminderInfo(choice.option, plan.date, ReminderStatus.SCHEDULED)
            } else {
                ReminderInfo(choice.option, plan.date, ReminderStatus.PERMISSION_DENIED)
            }
        }
        repository.updateReminder(scanId, info)
        return info
    }
}
