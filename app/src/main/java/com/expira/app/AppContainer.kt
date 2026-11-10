package com.expira.app

import android.content.Context
import com.expira.app.data.EvaluationRepository
import com.expira.app.data.ExpiraDatabase
import com.expira.app.data.ReminderNotifier
import com.expira.app.data.ReminderScheduler
import com.expira.app.data.WorkManagerReminderScheduler
import com.expira.app.data.ScanHistoryRepository
import com.expira.app.domain.CorrectDateUseCase
import com.expira.app.domain.DeleteScanUseCase
import com.expira.app.domain.SaveScanUseCase
import com.expira.app.evaluation.ExportLogsUseCase
import com.expira.app.evaluation.RunEvaluationUseCase
import com.expira.app.domain.ScanDateUseCase
import com.expira.app.domain.ScanSessionStore
import com.expira.app.domain.ScheduleReminderUseCase
import com.expira.app.imaging.BitmapLoader
import com.expira.app.imaging.ContentBitmapLoader
import com.expira.app.imaging.ImagePreprocessor
import com.expira.app.imaging.ScanImageStore
import com.expira.app.ocr.MlKitOcrEngine
import com.expira.app.ocr.OcrEngine
import java.io.File

/** Manual dependency container; one instance per process, owned by [ExpiraApplication]. */
class AppContainer(context: Context, isOpenCvReady: Boolean) {
    val bitmapLoader: BitmapLoader by lazy { ContentBitmapLoader(context.contentResolver) }
    val scanImageStore: ScanImageStore by lazy { ScanImageStore(context.cacheDir) }
    private val ocrEngine: OcrEngine by lazy { MlKitOcrEngine() }
    val runEvaluationUseCase: RunEvaluationUseCase by lazy {
        RunEvaluationUseCase(bitmapLoader, scanDateUseCase, ocrEngine, evaluationRepository)
    }
    val exportLogsUseCase: ExportLogsUseCase by lazy {
        ExportLogsUseCase(context.contentResolver, evaluationRepository, historyRepository)
    }
    private val preprocessor: ImagePreprocessor? = if (isOpenCvReady) ImagePreprocessor() else null
    val scanDateUseCase: ScanDateUseCase by lazy { ScanDateUseCase(ocrEngine, preprocessor) }

    private val database: ExpiraDatabase by lazy { ExpiraDatabase.create(context) }
    val historyRepository: ScanHistoryRepository by lazy { ScanHistoryRepository(database.scanDao()) }
    val evaluationRepository: EvaluationRepository by lazy { EvaluationRepository(database.evaluationDao()) }
    val scanSessionStore = ScanSessionStore()
    val reminderNotifier: ReminderNotifier by lazy { ReminderNotifier(context) }
    private val reminderScheduler: ReminderScheduler by lazy { WorkManagerReminderScheduler(context) }
    private val scheduleReminderUseCase: ScheduleReminderUseCase by lazy {
        ScheduleReminderUseCase(historyRepository, reminderScheduler, reminderNotifier::canNotify)
    }
    val saveScanUseCase: SaveScanUseCase by lazy {
        SaveScanUseCase(historyRepository, File(context.filesDir, HISTORY_IMAGE_DIR), scheduleReminderUseCase)
    }
    val deleteScanUseCase: DeleteScanUseCase by lazy { DeleteScanUseCase(historyRepository, reminderScheduler) }
    val correctDateUseCase: CorrectDateUseCase by lazy { CorrectDateUseCase(saveScanUseCase) }

    private companion object {
        const val HISTORY_IMAGE_DIR = "history"
    }
}
