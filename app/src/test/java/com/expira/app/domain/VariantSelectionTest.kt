package com.expira.app.domain

import com.expira.app.extraction.DateExtractor
import com.expira.app.extraction.TextLine
import com.expira.app.imaging.PreprocessConfig
import com.expira.app.ocr.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VariantSelectionTest {

    private val extractor = DateExtractor()

    private fun variant(config: PreprocessConfig, vararg lines: String) = VariantResult(
        config = config,
        appliedSteps = emptySet(),
        preprocessMs = 0,
        ocrMs = 0,
        extractMs = 0,
        ocr = OcrResult(lines.joinToString("\n"), emptyList()),
        extraction = extractor.extract(lines.map { TextLine(it) }),
    )

    @Test
    fun `variant with expiry keyword beats variant without keyword`() {
        val plain = variant(PreprocessConfig.P0, "12/08/2027")
        val keyed = variant(PreprocessConfig.P3, "EXP 12/08/2027")
        assertEquals(PreprocessConfig.P3, VariantSelection.choose(listOf(plain, keyed))?.config)
    }

    @Test
    fun `earlier variant wins a tie`() {
        val first = variant(PreprocessConfig.P0, "EXP 12/08/2027")
        val second = variant(PreprocessConfig.P1, "EXP 12/08/2027")
        assertEquals(PreprocessConfig.P0, VariantSelection.choose(listOf(first, second))?.config)
    }

    @Test
    fun `variants without any date give no choice`() {
        val empty = variant(PreprocessConfig.P0, "SUSU UHT")
        val production = variant(PreprocessConfig.P1, "MFD 12/08/2026")
        assertNull(VariantSelection.choose(listOf(empty, production)))
        assertNull(VariantSelection.choose(emptyList()))
    }

    @Test
    fun `strong result needs same-line expiry keyword and full date`() {
        assertTrue(VariantSelection.isStrong(variant(PreprocessConfig.P0, "EXP 12/08/2027")))
        assertFalse(VariantSelection.isStrong(variant(PreprocessConfig.P0, "EXP AUG 2027")))
        assertFalse(VariantSelection.isStrong(variant(PreprocessConfig.P0, "12/08/2027")))
        assertFalse(VariantSelection.isStrong(variant(PreprocessConfig.P0, "EXP", "12/08/2027")))
    }
}
