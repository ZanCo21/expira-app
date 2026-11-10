package com.expira.app.imaging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

/** Synthetic packaging-like images for instrumented preprocessing tests. */
object SyntheticImages {

    fun text(
        vararg lines: String,
        width: Int = 1200,
        height: Int = 500,
        rotationDegrees: Float = 0f,
    ): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(235, 230, 220))
        canvas.rotate(rotationDegrees, width / 2f, height / 2f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 110f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        lines.forEachIndexed { index, line -> canvas.drawText(line, 80f, 190f + index * 150f, paint) }
        return bitmap
    }

    /** Keeps only a grid of small dots of the ink, like dot-matrix printing. */
    fun dotMatrix(source: Bitmap, pitch: Int = 6, dot: Int = 3): Bitmap {
        val out = createBitmap(source.width, source.height)
        val background = Color.rgb(235, 230, 220)
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val ink = Color.red(source.getPixel(x, y)) < 128
                val onDot = x % pitch < dot && y % pitch < dot
                out.setPixel(x, y, if (ink && onDot) Color.BLACK else background)
            }
        }
        return out
    }

    /** Strong blur by shrinking and enlarging again. */
    fun blurred(source: Bitmap, factor: Int = 12): Bitmap =
        source.scale(source.width / factor, source.height / factor).scale(source.width, source.height)

    fun black(width: Int = 640, height: Int = 480): Bitmap =
        createBitmap(width, height).apply { eraseColor(Color.BLACK) }
}
