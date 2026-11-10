package com.expira.app.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrCharCorrectorTest {

    private fun fix(text: String) = OcrCharCorrector.correct(text).text

    @Test
    fun `replaces letter O with zero between digits and separators`() {
        val result = OcrCharCorrector.correct("EXP 1O.O3.27")
        assertEquals("EXP 10.03.27", result.text)
        assertEquals(2, result.corrections.size)
        assertTrue(result.corrections.all { it.from == 'O' && it.to == '0' })
    }

    @Test
    fun `replaces I and l with one, S with five, B with eight`() {
        assertEquals("12/08/2027", fix("l2/O8/2O27"))
        assertEquals("15-11-26", fix("1S-II-26"))
        assertEquals("18.05.2027", fix("1B.0S.2027"))
    }

    @Test
    fun `keeps keywords and words untouched`() {
        assertEquals("BB 12/08/27", fix("BB 12/08/27"))
        assertEquals("BEST BEFORE 12.08.27", fix("BEST BEFORE 12.08.27"))
        assertEquals("EXP 12 OKT 2027", fix("EXP 12 OKT 2027"))
        assertEquals("SUSU COKELAT", fix("SUSU COKELAT"))
    }

    @Test
    fun `does not touch month name glued to digits`() {
        assertEquals("10OKT2026", fix("10OKT2026"))
        assertEquals("12SEP27", fix("12SEP27"))
    }

    @Test
    fun `corrects year digits after a month name`() {
        assertEquals("MEI 2027", fix("MEI 2O27"))
    }

    @Test
    fun `leaves runs without any real digit alone`() {
        assertEquals("SOS", fix("SOS"))
        assertEquals("O.O", fix("O.O"))
    }

    @Test
    fun `output length always equals input length`() {
        val input = "MFD lO/O1/26 EXP 1O-O1-2O27"
        assertEquals(input.length, fix(input).length)
    }
}
