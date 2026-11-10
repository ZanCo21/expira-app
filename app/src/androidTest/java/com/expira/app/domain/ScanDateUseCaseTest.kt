package com.expira.app.domain

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expira.app.imaging.ImagePreprocessor
import com.expira.app.imaging.PreprocessConfig
import com.expira.app.imaging.SyntheticImages
import com.expira.app.ocr.MlKitOcrEngine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader

/** End-to-end pipeline on synthetic images with the real ML Kit engine. */
@RunWith(AndroidJUnit4::class)
class ScanDateUseCaseTest {

    private lateinit var useCase: ScanDateUseCase

    @Before
    fun setUp() {
        assertTrue(OpenCVLoader.initLocal())
        useCase = ScanDateUseCase(MlKitOcrEngine(), ImagePreprocessor())
    }

    private fun scan(strategy: ScanStrategy, vararg lines: String): ScanOutcome = runBlocking {
        useCase(SyntheticImages.text(*lines), strategy).filterIsInstance<ScanEvent.Completed>().first().outcome
    }

    @Test
    fun readsExpiryDateAndIgnoresProductionDate() {
        val outcome = scan(ScanStrategy.DEFAULT, "MFD 12.08.26", "EXP 12.08.27")
        assertEquals("12-08-2027", outcome.extraction.best?.date?.format())
    }

    @Test
    fun strongResultOnOriginalStopsAfterFirstVariant() {
        val outcome = scan(ScanStrategy.DEFAULT, "EXP 12.08.27")
        assertEquals(listOf(PreprocessConfig.P0), outcome.variants.map { it.config })
    }

    @Test
    fun everySingleConfigurationReadsCleanText() {
        PreprocessConfig.entries.forEach { config ->
            val outcome = scan(ScanStrategy.Single(config), "EXP 12.08.27")
            assertEquals("config $config", "12-08-2027", outcome.extraction.best?.date?.format())
        }
    }

    @Test
    fun imageWithoutTextCompletesWithoutResult() = runBlocking {
        val outcome = useCase(SyntheticImages.black(), ScanStrategy.DEFAULT)
            .filterIsInstance<ScanEvent.Completed>().first().outcome
        assertNull(outcome.extraction.best)
        assertEquals(3, outcome.variants.size)
    }
}
