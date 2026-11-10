package com.expira.app.imaging

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameCropTest {

    @Test
    fun `maps frame fractions to pixels without margin`() {
        val rect = FrameCrop.toPixels(NormalizedRect(0.25f, 0.4f, 0.75f, 0.6f), 1000, 2000, margin = 0f)
        assertEquals(PixelRect(250, 800, 750, 1200), rect)
    }

    @Test
    fun `adds margin proportional to frame size`() {
        val rect = FrameCrop.toPixels(NormalizedRect(0.25f, 0.4f, 0.75f, 0.6f), 1000, 2000, margin = 0.1f)
        // frame is 0.5 wide and 0.2 tall: margin 0.05 horizontally, 0.02 vertically
        assertEquals(PixelRect(200, 760, 800, 1240), rect)
    }

    @Test
    fun `clamps to image bounds when frame touches the edge`() {
        val rect = FrameCrop.toPixels(NormalizedRect(0f, 0f, 1f, 1f), 640, 480, margin = 0.2f)
        assertEquals(PixelRect(0, 0, 640, 480), rect)
    }

    @Test
    fun `never returns an empty rect for a tiny frame`() {
        val rect = FrameCrop.toPixels(NormalizedRect(0.5f, 0.5f, 0.5001f, 0.5001f), 100, 100, margin = 0f)
        assertEquals(1, rect.width)
        assertEquals(1, rect.height)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an empty image`() {
        FrameCrop.toPixels(NormalizedRect(0.1f, 0.1f, 0.9f, 0.9f), 0, 100)
    }
}
