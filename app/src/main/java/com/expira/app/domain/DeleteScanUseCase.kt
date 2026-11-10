package com.expira.app.domain

import com.expira.app.data.ReminderScheduler
import com.expira.app.data.ScanHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Deletes a history entry, its stored photo and its pending reminder (AC-15). */
class DeleteScanUseCase(
    private val repository: ScanHistoryRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(scanId: Long) {
        scheduler.cancel(scanId)
        val record = repository.getById(scanId)
        repository.delete(scanId)
        record?.imagePath?.let { path -> withContext(Dispatchers.IO) { runCatching { File(path).delete() } } }
    }
}
