package com.expira.app.imaging

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader

@RunWith(AndroidJUnit4::class)
class ImagePreprocessorTest {

    private val preprocessor = ImagePreprocessor()

    @Before
    fun loadOpenCv() {
        assertTrue("OpenCV must load", OpenCVLoader.initLocal())
    }

    @Test
    fun p0_returnsInputUnchanged() {
        val input = SyntheticImages.text("EXP 12.08.27")
        val result = preprocessor.process(input, PreprocessConfig.P0)
        assertSame(input, result.bitmap)
        assertTrue(result.appliedSteps.isEmpty())
    }

    @Test
    fun p1_outputsGrayscaleOfSameSize() {
        val input = SyntheticImages.text("EXP 12.08.27")
        val output = preprocessor.process(input, PreprocessConfig.P1).bitmap
        assertEquals(input.width, output.width)
        assertEquals(input.height, output.height)
        output.samplePixels().forEach { assertTrue(Color.red(it) == Color.green(it) && Color.green(it) == Color.blue(it)) }
    }

    @Test
    fun p2_outputsBinaryImage() {
        val output = preprocessor.process(SyntheticImages.text("EXP 12.08.27"), PreprocessConfig.P2).bitmap
        output.samplePixels().forEach { assertTrue(Color.red(it) == 0 || Color.red(it) == 255) }
    }

    @Test
    fun p3_cleanStraightText_appliesNoConditionalStep() {
        val result = preprocessor.process(SyntheticImages.text("MFD 12.08.26", "EXP 12.08.27"), PreprocessConfig.P3)
        assertTrue("applied ${result.appliedSteps}", result.appliedSteps.isEmpty())
    }

    @Test
    fun p3_rotatedText_appliesDeskew() {
        val input = SyntheticImages.text("MFD 12.08.26", "EXP 12.08.27", rotationDegrees = 8f)
        val result = preprocessor.process(input, PreprocessConfig.P3)
        assertTrue(ConditionalStep.DESKEW in result.appliedSteps)
    }

    @Test
    fun p3_dotMatrixText_appliesClosing() {
        val input = SyntheticImages.dotMatrix(SyntheticImages.text("EXP 12.08.27"))
        val result = preprocessor.process(input, PreprocessConfig.P3)
        assertTrue(ConditionalStep.CLOSING in result.appliedSteps)
    }

    @Test
    fun p3_lowBlurScore_appliesSharpen() {
        val input = SyntheticImages.text("EXP 12.08.27")
        val result = preprocessor.process(input, PreprocessConfig.P3, blurScore = 1.0)
        assertTrue(ConditionalStep.SHARPEN in result.appliedSteps)
    }

    @Test
    fun blurDetector_ranksBlurredImageLower() {
        val sharp = SyntheticImages.text("EXP 12.08.27")
        val sharpScore = BlurDetector.score(sharp)
        val blurredScore = BlurDetector.score(SyntheticImages.blurred(sharp))
        assertTrue("sharp=$sharpScore blurred=$blurredScore", blurredScore < sharpScore)
        assertFalse(BlurDetector.isBlurry(sharpScore))
        assertTrue(BlurDetector.isBlurry(blurredScore))
    }

    @Test
    fun allConfigs_handleCompletelyBlackImageWithoutCrash() {
        val black = SyntheticImages.black()
        PreprocessConfig.entries.forEach { config ->
            val result = preprocessor.process(black, config)
            assertEquals(black.width, result.bitmap.width)
        }
        assertTrue(BlurDetector.isBlurry(BlurDetector.score(black)))
    }

    private fun Bitmap.samplePixels(step: Int = 37): List<Int> =
        (0 until height step step).flatMap { y -> (0 until width step step).map { x -> getPixel(x, y) } }
}
