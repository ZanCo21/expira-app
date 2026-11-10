package com.expira.app.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateExtractorTest {

    private val extractor = DateExtractor()

    private fun extract(vararg lines: String) = extractor.extract(lines.map { TextLine(it) })

    private fun best(vararg lines: String): String? = extract(*lines).best?.date?.format()

    // --- Formats ---------------------------------------------------------------

    @Test
    fun `DD slash MM slash YYYY`() = assertEquals("12-08-2027", best("EXP 12/08/2027"))

    @Test
    fun `DD dash MM dash YY`() = assertEquals("12-08-2027", best("EXP 12-08-27"))

    @Test
    fun `DD dot MM dot YYYY`() = assertEquals("05-03-2027", best("EXP 05.03.2027"))

    @Test
    fun `single digit day and month`() = assertEquals("05-03-2027", best("EXP 5/3/2027"))

    @Test
    fun `DD MMM YYYY with English month`() = assertEquals("12-08-2027", best("BEST BEFORE 12 AUG 2027"))

    @Test
    fun `DD MMM YYYY with Indonesian month`() {
        assertEquals("12-08-2027", best("EXP 12 AGU 2027"))
        assertEquals("01-05-2027", best("EXP 01 MEI 2027"))
        assertEquals("20-10-2027", best("EXP 20 OKT 2027"))
        assertEquals("31-12-2027", best("EXP 31 DES 2027"))
    }

    @Test
    fun `full Indonesian month name`() = assertEquals("17-08-2027", best("KEDALUWARSA 17 AGUSTUS 2027"))

    @Test
    fun `DDMMMYY without separators`() {
        assertEquals("12-08-2027", best("EXP12AUG27"))
        assertEquals("10-10-2026", best("EXP 10OKT26"))
    }

    @Test
    fun `MMM YYYY uses last day of month`() {
        assertEquals("31-08-2027", best("EXP AUG 2027"))
        assertEquals("29-02-2028", best("EXP FEB 2028"))
    }

    @Test
    fun `MM slash YYYY uses last day of month`() {
        assertEquals("30-04-2027", best("EXP 04/2027"))
        assertEquals(DateFormat.MONTH_YEAR_NUMERIC, extract("EXP 04/2027").best?.format)
    }

    @Test
    fun `YYYY-MM-DD`() {
        assertEquals("12-08-2027", best("EXP 2027-08-12"))
        assertEquals(DateFormat.YEAR_MONTH_DAY_NUMERIC, extract("EXP 2027-08-12").best?.format)
    }

    @Test
    fun `lower case text is recognised`() = assertEquals("12-08-2027", best("exp 12 aug 2027"))

    // --- OCR character correction ---------------------------------------------------

    @Test
    fun `letter O read as zero is corrected and recorded`() {
        val best = extract("EXP 1O.O3.27").best
        assertEquals("10-03-2027", best?.date?.format())
        assertEquals("1O.O3.27", best?.rawText)
        assertEquals(2, best?.corrections?.size)
    }

    @Test
    fun `l and S in numeric context are corrected`() = assertEquals("15-11-2026", best("ED l5/11/2O26"))

    // --- Validation ---------------------------------------------------------------

    @Test
    fun `impossible dates are rejected, not shown`() {
        assertNull(best("EXP 31/02/2027"))
        assertNull(best("EXP 12/13/2027"))
        assertNull(best("EXP 29/02/2027"))
        assertTrue(extract("EXP 31/04/2027").candidates.isEmpty())
    }

    @Test
    fun `29 February in a leap year is accepted`() = assertEquals("29-02-2028", best("EXP 29/02/2028"))

    @Test
    fun `text without dates gives no result`() {
        assertEquals(ExtractionResult.EMPTY, extract("SUSU UHT COKELAT", "200 ML"))
        assertEquals(ExtractionResult.EMPTY, extractor.extract(emptyList()))
        assertEquals(ExtractionResult.EMPTY, extract(""))
    }

    // --- Keywords ---------------------------------------------------------------

    @Test
    fun `expiry keyword wins over production keyword on separate lines`() {
        val result = extract("MFD 12.08.26", "EXP 12.08.27")
        assertEquals("12-08-2027", result.best?.date?.format())
        assertEquals(KeywordKind.EXPIRY, result.best?.keyword?.kind)
        assertEquals(2, result.candidates.size)
    }

    @Test
    fun `expiry keyword wins even when production date is later in the text`() {
        assertEquals("01-06-2027", best("EXP 01/06/2027 PROD 01/06/2028"))
    }

    @Test
    fun `nearest preceding keyword governs each date on one line`() {
        val result = extract("MFD 01/01/26 EXP 01/01/27")
        assertEquals("01-01-2027", result.best?.date?.format())
        val production = result.candidates.first { it.date.format() == "01-01-2026" }
        assertEquals(KeywordKind.PRODUCTION, production.keyword.kind)
    }

    @Test
    fun `all expiry keyword variants are recognised`() {
        listOf(
            "EXP", "EXP.", "EXPIRY", "ED", "E.D.", "BB", "BBE", "BEST BEFORE", "USE BY",
            "KEDALUWARSA", "KADALUWARSA", "BAIK DIGUNAKAN SEBELUM",
        ).forEach { keyword ->
            val result = extract("MFD 01.01.2026 $keyword 01.01.2027")
            assertEquals("keyword $keyword", KeywordKind.EXPIRY, result.best?.keyword?.kind)
            assertEquals("keyword $keyword", "01-01-2027", result.best?.date?.format())
        }
    }

    @Test
    fun `all production keyword variants are recognised`() {
        listOf("MFD", "PROD", "PD", "TGL PROD", "BATCH", "LOT").forEach { keyword ->
            val result = extract("$keyword 01.01.2026 EXP 01.01.2027")
            val production = result.candidates.first { it.date.format() == "01-01-2026" }
            assertEquals("keyword $keyword", KeywordKind.PRODUCTION, production.keyword.kind)
        }
    }

    @Test
    fun `keyword alone on the line above governs the date below`() {
        val result = extract("BAIK DIGUNAKAN SEBELUM", "12 AGU 2027")
        assertEquals("12-08-2027", result.best?.date?.format())
        assertEquals(KeywordKind.EXPIRY, result.best?.keyword?.kind)
        assertFalse(checkNotNull(result.best).keyword.sameLine)
    }

    @Test
    fun `keyword of a previous line that already has its date does not leak`() {
        val result = extract("MFD 12.08.26", "12.08.27")
        assertEquals(KeywordKind.NONE, result.best?.keyword?.kind)
        assertEquals("12-08-2027", result.best?.date?.format())
    }

    @Test
    fun `without keywords the latest date is chosen`() {
        assertEquals("12-08-2027", best("12.08.26", "12.08.27"))
    }

    @Test
    fun `production-only dates are never chosen as expiry`() {
        val result = extract("MFD 12.08.26")
        assertNull(result.best)
        assertEquals(1, result.candidates.size)
    }

    @Test
    fun `keyword glued to the date is recognised`() {
        assertEquals(KeywordKind.EXPIRY, extract("EXP12/08/2027").best?.keyword?.kind)
    }

    @Test
    fun `keyword inside another word is ignored`() {
        // "ED" inside "MEDAN" and "PD" inside "UPDATE" are not keywords
        assertEquals(KeywordKind.NONE, extract("MEDAN 12/08/2027").best?.keyword?.kind)
        assertEquals(KeywordKind.NONE, extract("UPDATE 12/08/2027").best?.keyword?.kind)
    }

    @Test
    fun `full date ranks above month-only date with the same keyword`() {
        assertEquals("12-08-2027", best("EXP AUG 2027", "EXP 12/08/2027"))
    }

    @Test
    fun `OCR confidence is carried to the candidate`() {
        val result = extractor.extract(listOf(TextLine("EXP 12/08/2027", confidence = 0.91f)))
        assertEquals(0.91f, result.best?.ocrConfidence)
    }
}
