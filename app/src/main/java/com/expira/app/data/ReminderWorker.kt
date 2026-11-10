package com.expira.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expira.app.ExpiraApplication
import com.expira.app.domain.ExpiryStatusCalculator
import com.expira.app.domain.ReminderStatus
import com.expira.app.domain.SystemTodayProvider

/** Shows the expiry notification for one history entry, then marks the reminder as sent. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scanId = inputData.getLong(KEY_SCAN_ID, -1)
        val container = (applicationContext as? ExpiraApplication)?.container ?: return Result.failure()
        val repository = container.historyRepository
        val record = repository.getById(scanId) ?: return Result.success() // entry was deleted
        val reminder = record.reminder?.takeIf { it.status == ReminderStatus.SCHEDULED } ?: return Result.success()

        val daysLeft = ExpiryStatusCalculator.of(record.expiry, SystemTodayProvider.today()).daysRemaining
        val shown = container.reminderNotifier.show(record.id, record.productName, record.expiry, daysLeft)
        val status = if (shown) ReminderStatus.SENT else ReminderStatus.PERMISSION_DENIED
        repository.updateReminder(record.id, reminder.copy(status = status))
        return Result.success()
    }

    companion object {
        const val KEY_SCAN_ID = "scan_id"
        const val TAG = "expiry-reminder"
    }
}
