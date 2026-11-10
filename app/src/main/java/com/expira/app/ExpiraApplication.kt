package com.expira.app

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class ExpiraApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this, isOpenCvReady) }

    /** False when the OpenCV native library failed to load; scans then fall back to P0. */
    var isOpenCvReady: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        isOpenCvReady = OpenCVLoader.initLocal()
        if (!isOpenCvReady) Log.e(TAG, "OpenCV failed to load; preprocessing disabled")
    }

    private companion object {
        const val TAG = "ExpiraApplication"
    }
}
