package com.expira.app.imaging

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** Loads images for processing: downscaled (avoids OOM) and upright (EXIF applied). */
interface BitmapLoader {
    /** Returns null when the image cannot be opened or decoded. */
    suspend fun load(uri: Uri): Bitmap?
}

class ContentBitmapLoader(
    private val contentResolver: ContentResolver,
    private val maxDimension: Int = MAX_DIMENSION,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BitmapLoader {

    override suspend fun load(uri: Uri): Bitmap? = withContext(dispatcher) {
        runCatching {
            val bounds = readBounds(uri) ?: return@runCatching null
            val decoded = decodeSampled(uri, bounds.first, bounds.second) ?: return@runCatching null
            val scaled = scaleToFit(decoded)
            rotateUpright(scaled, readExifRotation(uri))
        }.getOrNull()
    }

    private fun readBounds(uri: Uri): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        return if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
    }

    private fun decodeSampled(uri: Uri, width: Int, height: Int): Bitmap? {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxDimension) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun scaleToFit(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).roundToInt(),
            (bitmap.height * ratio).roundToInt(),
            true,
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun readExifRotation(uri: Uri): Int =
        contentResolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0

    private fun rotateUpright(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    companion object {
        /** Longest edge after downscaling; large enough for printed date text. */
        const val MAX_DIMENSION = 1600
    }
}
