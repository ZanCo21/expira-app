package com.expira.app.ui.camera

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.expira.app.BuildConfig
import com.expira.app.imaging.BlurDetector
import com.expira.app.imaging.FrameCrop
import com.expira.app.imaging.NormalizedRect
import com.expira.app.imaging.PixelRect
import java.util.concurrent.atomic.AtomicReference

/**
 * Live blur check on the preview stream: scores the luma inside the on-screen frame
 * a few times per second and reports a verdict only after it is stable.
 */
internal class BlurAnalyzer(
    private val onBlurChanged: (Boolean) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) : ImageAnalysis.Analyzer {

    /** Latest on-screen frame position; updated from the UI thread. */
    val guide = AtomicReference<NormalizedRect?>(null)

    private var lastRunAt = 0L
    private var pendingVerdict: Boolean? = null
    private var reportedVerdict: Boolean? = null

    override fun analyze(image: ImageProxy) {
        image.use { proxy ->
            val now = clock()
            if (now - lastRunAt < INTERVAL_MS) return
            lastRunAt = now
            val region = regionOf(proxy) ?: return
            val plane = proxy.planes.firstOrNull() ?: return
            val score = runCatching { BlurDetector.scoreLuma(plane.buffer, plane.rowStride, region) }.getOrNull() ?: return
            // Logged in debug builds to calibrate BLUR_THRESHOLD on real devices.
            if (BuildConfig.DEBUG) Log.d(TAG, "score=%.1f region=%s".format(score, region))
            report(BlurDetector.isBlurry(score))
        }
    }

    private fun regionOf(proxy: ImageProxy): PixelRect? {
        val upright = guide.get() ?: return null
        val crop = proxy.cropRect
        if (crop.width() <= 0 || crop.height() <= 0) return null
        val inBuffer = FrameCrop.toBufferOrientation(upright, proxy.imageInfo.rotationDegrees)
        val local = FrameCrop.toPixels(inBuffer, crop.width(), crop.height(), margin = 0f)
        return PixelRect(crop.left + local.left, crop.top + local.top, crop.left + local.right, crop.top + local.bottom)
    }

    /** Two equal verdicts in a row are needed before the banner changes. */
    private fun report(isBlurry: Boolean) {
        if (pendingVerdict != isBlurry) {
            pendingVerdict = isBlurry
            return
        }
        if (reportedVerdict != isBlurry) {
            reportedVerdict = isBlurry
            onBlurChanged(isBlurry)
        }
    }

    private companion object {
        const val INTERVAL_MS = 500L
        const val TAG = "BlurAnalyzer"
    }
}
