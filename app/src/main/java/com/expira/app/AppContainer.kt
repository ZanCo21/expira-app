package com.expira.app

import android.content.Context
import com.expira.app.data.ExpiraDatabase
import com.expira.app.data.ScanHistoryRepository
import com.expira.app.domain.CorrectDateUseCase
import com.expira.app.domain.SaveScanUseCase
import com.expira.app.domain.ScanDateUseCase
import com.expira.app.domain.ScanSessionStore
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
    private val preprocessor: ImagePreprocessor? = if (isOpenCvReady) ImagePreprocessor() else null
    val scanDateUseCase: ScanDateUseCase by lazy { ScanDateUseCase(ocrEngine, preprocessor) }

    private val database: ExpiraDatabase by lazy { ExpiraDatabase.create(context) }
    val historyRepository: ScanHistoryRepository by lazy { ScanHistoryRepository(database.scanDao()) }
    val scanSessionStore = ScanSessionStore()
    val saveScanUseCase: SaveScanUseCase by lazy {
        SaveScanUseCase(historyRepository, File(context.filesDir, HISTORY_IMAGE_DIR))
    }
    val correctDateUseCase: CorrectDateUseCase by lazy { CorrectDateUseCase(saveScanUseCase) }

    private companion object {
        const val HISTORY_IMAGE_DIR = "history"
    }
}
