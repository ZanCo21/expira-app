package com.expira.app.imaging

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PreprocessResult(
    val bitmap: Bitmap,
    val config: PreprocessConfig,
    /** Conditional P3 steps that actually ran (always empty for P0-P2). */
    val appliedSteps: Set<ConditionalStep>,
)

/**
 * OpenCV preprocessing pipeline.
 *
 * P3 order: gray -> CLAHE -> [sharpen if blurry] -> median -> [deskew if skewed]
 * -> adaptive threshold -> [closing if strokes are fragmented, e.g. dot-matrix print].
 */
class ImagePreprocessor {

    fun process(input: Bitmap, config: PreprocessConfig, blurScore: Double? = null): PreprocessResult {
        if (config == PreprocessConfig.P0) return PreprocessResult(input, config, emptySet())
        val mats = MatScope()
        try {
            val applied = mutableSetOf<ConditionalStep>()
            var image = toGray(input, mats)
            image = clahe(image, mats)
            if (config == PreprocessConfig.P1) return result(image, config, applied)

            if (config == PreprocessConfig.P3 && BlurDetector.isBlurry(blurScore ?: BlurDetector.score(image))) {
                image = sharpen(image, mats)
                applied += ConditionalStep.SHARPEN
            }
            image = median(image, mats)
            if (config == PreprocessConfig.P3) {
                val angle = estimateSkew(image, mats)
                if (abs(angle) >= PreprocessParams.SKEW_MIN_DEGREES) {
                    image = rotate(image, angle, mats)
                    applied += ConditionalStep.DESKEW
                }
            }
            image = adaptiveThreshold(image, mats)
            if (config == PreprocessConfig.P3 && isFragmented(image, mats)) {
                image = close(image, mats)
                applied += ConditionalStep.CLOSING
            }
            return result(image, config, applied)
        } finally {
            mats.releaseAll()
        }
    }

    private fun result(gray: Mat, config: PreprocessConfig, applied: Set<ConditionalStep>): PreprocessResult {
        val rgba = Mat()
        try {
            Imgproc.cvtColor(gray, rgba, Imgproc.COLOR_GRAY2RGBA)
            val bitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(rgba, bitmap)
            return PreprocessResult(bitmap, config, applied.toSet())
        } finally {
            rgba.release()
        }
    }

    private fun toGray(bitmap: Bitmap, mats: MatScope): Mat {
        val rgba = mats.new()
        Utils.bitmapToMat(bitmap, rgba)
        return mats.new().also { Imgproc.cvtColor(rgba, it, Imgproc.COLOR_RGBA2GRAY) }
    }

    private fun clahe(gray: Mat, mats: MatScope): Mat {
        val tile = Size(PreprocessParams.CLAHE_TILE_SIZE, PreprocessParams.CLAHE_TILE_SIZE)
        val clahe = Imgproc.createCLAHE(PreprocessParams.CLAHE_CLIP_LIMIT, tile)
        return mats.new().also { clahe.apply(gray, it) }
    }

    /** Unsharp mask: original * amount - blurred * (amount - 1). */
    private fun sharpen(gray: Mat, mats: MatScope): Mat {
        val blurred = mats.new().also { Imgproc.GaussianBlur(gray, it, Size(0.0, 0.0), PreprocessParams.SHARPEN_SIGMA) }
        return mats.new().also {
            Core.addWeighted(gray, PreprocessParams.SHARPEN_AMOUNT, blurred, 1.0 - PreprocessParams.SHARPEN_AMOUNT, 0.0, it)
        }
    }

    private fun median(gray: Mat, mats: MatScope): Mat =
        mats.new().also { Imgproc.medianBlur(gray, it, PreprocessParams.MEDIAN_KERNEL) }

    private fun adaptiveThreshold(gray: Mat, mats: MatScope): Mat {
        val block = blockSizeFor(gray)
        return mats.new().also {
            Imgproc.adaptiveThreshold(
                gray, it, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,
                block, PreprocessParams.ADAPTIVE_C,
            )
        }
    }

    private fun blockSizeFor(gray: Mat): Int {
        val raw = max(PreprocessParams.ADAPTIVE_BLOCK_MIN, (min(gray.cols(), gray.rows()) * PreprocessParams.ADAPTIVE_BLOCK_FRACTION).roundToInt())
        return if (raw % 2 == 0) raw + 1 else raw
    }

    /**
     * Projection-profile skew estimate: the text angle is where the horizontal
     * projection of a binarised, downscaled copy has the highest variance.
     */
    private fun estimateSkew(gray: Mat, mats: MatScope): Double {
        val scale = min(1.0, PreprocessParams.SKEW_ANALYSIS_WIDTH.toDouble() / gray.cols())
        val small = mats.new().also { Imgproc.resize(gray, it, Size(gray.cols() * scale, gray.rows() * scale)) }
        val binary = mats.new().also {
            Imgproc.threshold(small, it, 0.0, 255.0, Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_OTSU)
        }
        var bestAngle = 0.0
        var bestScore = -1.0
        for (deg in -PreprocessParams.SKEW_MAX_DEGREES..PreprocessParams.SKEW_MAX_DEGREES step PreprocessParams.SKEW_STEP_DEGREES) {
            val rotated = rotateRaw(binary, deg.toDouble(), borderValue = 0.0)
            val score = projectionVariance(rotated)
            rotated.release()
            if (score > bestScore) {
                bestScore = score
                bestAngle = deg.toDouble()
            }
        }
        return bestAngle
    }

    private fun projectionVariance(binary: Mat): Double {
        val rows = Mat()
        val mean = MatOfDouble()
        val stdDev = MatOfDouble()
        return try {
            Core.reduce(binary, rows, 1, Core.REDUCE_SUM, CvType.CV_64F)
            Core.meanStdDev(rows, mean, stdDev)
            val sd = stdDev.toArray().firstOrNull() ?: 0.0
            sd * sd
        } finally {
            rows.release()
            mean.release()
            stdDev.release()
        }
    }

    private fun rotate(gray: Mat, angle: Double, mats: MatScope): Mat =
        rotateRaw(gray, angle, borderValue = null).also { mats.track(it) }

    /** Rotates around the centre; null border value replicates the edge pixels. */
    private fun rotateRaw(src: Mat, angle: Double, borderValue: Double?): Mat {
        val center = Point(src.cols() / 2.0, src.rows() / 2.0)
        val matrix = Imgproc.getRotationMatrix2D(center, angle, 1.0)
        val dst = Mat()
        try {
            if (borderValue == null) {
                Imgproc.warpAffine(src, dst, matrix, src.size(), Imgproc.INTER_LINEAR, Core.BORDER_REPLICATE)
            } else {
                Imgproc.warpAffine(
                    src, dst, matrix, src.size(), Imgproc.INTER_NEAREST, Core.BORDER_CONSTANT,
                    Scalar(borderValue),
                )
            }
        } finally {
            matrix.release()
        }
        return dst
    }

    /** True when most ink blobs are tiny dots (dot-matrix or broken strokes). */
    private fun isFragmented(binary: Mat, mats: MatScope): Boolean {
        val ink = mats.new().also { Core.bitwise_not(binary, it) }
        val labels = mats.new()
        val stats = mats.new()
        val centroids = mats.new()
        val count = Imgproc.connectedComponentsWithStats(ink, labels, stats, centroids)
        val components = count - 1 // label 0 is the background
        if (components < PreprocessParams.FRAGMENT_MIN_COMPONENTS) return false
        val maxDotArea = max(4.0, binary.total() * PreprocessParams.FRAGMENT_MAX_AREA_FRACTION)
        var tiny = 0
        for (label in 1 until count) {
            val area = stats.get(label, Imgproc.CC_STAT_AREA)?.firstOrNull() ?: continue
            if (area <= maxDotArea) tiny++
        }
        return tiny.toDouble() / components >= PreprocessParams.FRAGMENT_RATIO
    }

    /** Closing on the ink (dark text) joins dots into continuous strokes. */
    private fun close(binary: Mat, mats: MatScope): Mat {
        val kernel = mats.track(
            Imgproc.getStructuringElement(
                Imgproc.MORPH_ELLIPSE,
                Size(PreprocessParams.CLOSING_KERNEL.toDouble(), PreprocessParams.CLOSING_KERNEL.toDouble()),
            ),
        )
        val ink = mats.new().also { Core.bitwise_not(binary, it) }
        val closed = mats.new().also { Imgproc.morphologyEx(ink, it, Imgproc.MORPH_CLOSE, kernel) }
        return mats.new().also { Core.bitwise_not(closed, it) }
    }

    /** Collects native Mats so every intermediate is released, even on failure. */
    private class MatScope {
        private val mats = mutableListOf<Mat>()
        fun new(): Mat = track(Mat())
        fun track(mat: Mat): Mat = mat.also { mats += it }
        fun releaseAll() = mats.forEach { it.release() }
    }
}
