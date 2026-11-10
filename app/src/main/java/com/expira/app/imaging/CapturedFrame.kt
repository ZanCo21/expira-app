package com.expira.app.imaging

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import kotlin.math.max

/**
 * A JPEG still from the camera, before rotation.
 *
 * @param cropRect visible viewport in buffer coordinates (matches the on-screen preview)
 * @param rotationDegrees clockwise rotation needed to make the image upright
 */
class CapturedFrame(
    val jpeg: ByteArray,
    val cropRect: PixelRect,
    val rotationDegrees: Int,
)

/** Turns a camera still into the upright region inside the on-screen frame. */
object CapturedFrameCropper {

    /** Stills larger than this (longest edge) are subsampled while decoding. */
    private const val DECODE_TARGET = 3000

    fun crop(frame: CapturedFrame, guide: NormalizedRect): Bitmap? {
        val sample = sampleSizeFor(frame.cropRect)
        val decoded = BitmapFactory.decodeByteArray(
            frame.jpeg, 0, frame.jpeg.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return null

        val visible = cropScaled(decoded, frame.cropRect, sample)
        val upright = rotate(visible, frame.rotationDegrees)
        val region = FrameCrop.toPixels(guide, upright.width, upright.height)
        val result = Bitmap.createBitmap(upright, region.left, region.top, region.width, region.height)
        if (result !== upright) upright.recycle()
        return result
    }

    private fun sampleSizeFor(rect: PixelRect): Int {
        var sample = 1
        while (max(rect.width, rect.height) / (sample * 2) >= DECODE_TARGET) sample *= 2
        return sample
    }

    private fun cropScaled(bitmap: Bitmap, rect: PixelRect, sample: Int): Bitmap {
        val left = (rect.left / sample).coerceIn(0, bitmap.width - 1)
        val top = (rect.top / sample).coerceIn(0, bitmap.height - 1)
        val width = (rect.width / sample).coerceIn(1, bitmap.width - left)
        val height = (rect.height / sample).coerceIn(1, bitmap.height - top)
        if (left == 0 && top == 0 && width == bitmap.width && height == bitmap.height) return bitmap
        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
        if (cropped !== bitmap) bitmap.recycle()
        return cropped
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}
