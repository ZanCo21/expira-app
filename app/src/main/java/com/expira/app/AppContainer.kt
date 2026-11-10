package com.expira.app

import android.content.Context
import com.expira.app.domain.ScanDateUseCase
import com.expira.app.imaging.BitmapLoader
import com.expira.app.imaging.ContentBitmapLoader
import com.expira.app.imaging.ScanImageStore
import com.expira.app.ocr.MlKitOcrEngine
import com.expira.app.ocr.OcrEngine

/** Manual dependency container; one instance per process, owned by [ExpiraApplication]. */
class AppContainer(context: Context) {
    val bitmapLoader: BitmapLoader by lazy { ContentBitmapLoader(context.contentResolver) }
    val scanImageStore: ScanImageStore by lazy { ScanImageStore(context.cacheDir) }
    private val ocrEngine: OcrEngine by lazy { MlKitOcrEngine() }
    val scanDateUseCase: ScanDateUseCase by lazy { ScanDateUseCase(ocrEngine) }
}
