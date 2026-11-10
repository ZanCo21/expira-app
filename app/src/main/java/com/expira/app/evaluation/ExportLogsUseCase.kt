package com.expira.app.evaluation

import android.content.ContentResolver
import android.net.Uri
import com.expira.app.data.EvaluationRepository
import com.expira.app.data.ScanHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Writes the evaluation log and the scan history as one CSV file (F-12). */
class ExportLogsUseCase(
    private val contentResolver: ContentResolver,
    private val evaluationRepository: EvaluationRepository,
    private val historyRepository: ScanHistoryRepository,
) {
    /** @return true when the file was written */
    suspend operator fun invoke(target: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val csv = CsvExport.build(evaluationRepository.getAll(), historyRepository.getAll())
            val stream = contentResolver.openOutputStream(target, "wt") ?: return@runCatching false
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(csv) }
            true
        }.getOrDefault(false)
    }

    companion object {
        const val MIME_TYPE = "text/csv"
        const val DEFAULT_FILE_NAME = "expira-log.csv"
    }
}
