package com.expira.app.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.expira.app.domain.ReminderPlanner
import com.expira.app.extraction.CalendarDate
import java.util.concurrent.TimeUnit

interface ReminderScheduler {
    fun schedule(scanId: Long, date: CalendarDate)
    fun cancel(scanId: Long)
}

/**
 * One unique WorkManager job per history entry. WorkManager keeps the job across
 * reboots and app restarts (FR-25) and needs no running service or exact alarm
 * (NFR-11); delivery may be delayed by a few hours under battery saving (NFR-10).
 */
class WorkManagerReminderScheduler(
    context: Context,
    private val now: () -> Long = System::currentTimeMillis,
) : ReminderScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun schedule(scanId: Long, date: CalendarDate) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(ReminderPlanner.delayUntil(date, now()), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_SCAN_ID to scanId))
            .addTag(ReminderWorker.TAG)
            .build()
        workManager.enqueueUniqueWork(workName(scanId), ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(scanId: Long) {
        workManager.cancelUniqueWork(workName(scanId))
    }

    companion object {
        fun workName(scanId: Long) = "reminder-$scanId"
    }
}
