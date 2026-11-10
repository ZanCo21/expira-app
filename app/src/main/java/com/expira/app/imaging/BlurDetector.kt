package com.expira.app.imaging

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

/** Sharpness estimate with the variance of the Laplacian: lower means blurrier. */
object BlurDetector {

    fun isBlurry(score: Double): Boolean = score < PreprocessParams.BLUR_THRESHOLD

    fun score(bitmap: Bitmap): Double {
        val rgba = Mat()
        val gray = Mat()
        return try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            score(gray)
        } finally {
            rgba.release()
            gray.release()
        }
    }

    /** @param gray single-channel 8-bit image */
    fun score(gray: Mat): Double {
        if (gray.empty()) return 0.0
        val laplacian = Mat()
        val mean = MatOfDouble()
        val stdDev = MatOfDouble()
        return try {
            Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
            Core.meanStdDev(laplacian, mean, stdDev)
            val sd = stdDev.toArray().firstOrNull() ?: 0.0
            sd * sd
        } finally {
            laplacian.release()
            mean.release()
            stdDev.release()
        }
    }

    /** Scores a region of a camera luma (Y) plane without copying the whole frame. */
    fun scoreLuma(buffer: ByteBuffer, rowStride: Int, region: PixelRect): Double {
        val gray = Mat(region.height, region.width, CvType.CV_8UC1)
        return try {
            val row = ByteArray(region.width)
            for (y in 0 until region.height) {
                buffer.position((region.top + y) * rowStride + region.left)
                buffer.get(row, 0, region.width)
                gray.put(y, 0, row)
            }
            score(gray)
        } finally {
            gray.release()
        }
    }
}
