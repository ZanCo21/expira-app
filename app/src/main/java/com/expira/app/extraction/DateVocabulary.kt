package com.expira.app.extraction

/** Month names and keywords recognised on Indonesian and English packaging (upper case). */
internal object DateVocabulary {

    /** Indonesian and English month names and abbreviations, mapped to month number. */
    val months: Map<String, Int> = buildMap {
        fun add(month: Int, vararg names: String) = names.forEach { put(it, month) }
        add(1, "JAN", "JANUARI", "JANUARY")
        add(2, "FEB", "PEB", "FEBRUARI", "FEBRUARY", "PEBRUARI")
        add(3, "MAR", "MARET", "MARCH")
        add(4, "APR", "APRIL")
        add(5, "MEI", "MAY")
        add(6, "JUN", "JUNI", "JUNE")
        add(7, "JUL", "JULI", "JULY")
        add(8, "AGU", "AGT", "AGS", "AGUSTUS", "AUG", "AUGUST")
        add(9, "SEP", "SEPT", "SEPTEMBER")
        add(10, "OKT", "OKTOBER", "OCT", "OCTOBER")
        add(11, "NOV", "NOP", "NOVEMBER", "NOPEMBER")
        add(12, "DES", "DESEMBER", "DEC", "DECEMBER")
    }

    /** Regex alternation of month names, longest first so "AGUSTUS" wins over "AGU". */
    val monthPattern: String = months.keys.sortedByDescending { it.length }.joinToString("|")

    /** Expiry keywords from CLAUDE.md. Dots and spaces are matched loosely. */
    val expiryKeywords = listOf(
        "BAIK DIGUNAKAN SEBELUM", "BEST BEFORE", "USE BY", "KEDALUWARSA", "KADALUWARSA",
        "EXPIRY", "EXP", "BBE", "BB", "E.D.", "ED",
    )

    /** Production / code keywords: their dates are never the expiry date when EXP exists. */
    val productionKeywords = listOf("TGL PROD", "PROD", "MFD", "BATCH", "LOT", "PD")
}
