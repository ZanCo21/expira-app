package com.expira.app.imaging

import kotlin.math.roundToInt

/** Rectangle in fractions (0..1) of a reference area, e.g. the camera preview. */
data class NormalizedRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init {
        require(left < right && top < bottom) { "Empty rect: $this" }
    }
}

/** Rectangle in image pixels; right/bottom are exclusive. */
data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * Maps the on-screen "Bingkai Tanggal" frame onto the captured image.
 *
 * The frame is given as a fraction of the visible preview area. Because the captured
 * image is cropped to the same viewport as the preview, the same fractions apply to it.
 * A small margin is added so characters touching the frame edge are not cut off.
 */
object FrameCrop {
    const val DEFAULT_MARGIN = 0.08f

    fun toPixels(
        frame: NormalizedRect,
        imageWidth: Int,
        imageHeight: Int,
        margin: Float = DEFAULT_MARGIN,
    ): PixelRect {
        require(imageWidth > 0 && imageHeight > 0) { "Invalid image size ${imageWidth}x$imageHeight" }
        val marginX = (frame.right - frame.left) * margin
        val marginY = (frame.bottom - frame.top) * margin
        val left = ((frame.left - marginX) * imageWidth).roundToInt().coerceIn(0, imageWidth - 1)
        val top = ((frame.top - marginY) * imageHeight).roundToInt().coerceIn(0, imageHeight - 1)
        val right = ((frame.right + marginX) * imageWidth).roundToInt().coerceIn(left + 1, imageWidth)
        val bottom = ((frame.bottom + marginY) * imageHeight).roundToInt().coerceIn(top + 1, imageHeight)
        return PixelRect(left, top, right, bottom)
    }
}
