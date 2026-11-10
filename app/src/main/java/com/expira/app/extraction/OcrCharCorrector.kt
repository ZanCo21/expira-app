package com.expira.app.extraction

data class CharCorrection(val index: Int, val from: Char, val to: Char)

data class CorrectedText(val text: String, val corrections: List<CharCorrection>)

/**
 * Fixes common OCR letter/digit confusions, but only in a numeric context:
 * O->0, I/l->1, S->5, B->8.
 *
 * A confusable letter is replaced only when it sits in a run of digits, confusables and
 * date separators that contains at least one real digit, and it does not touch any other
 * letter. So "1O.O3.27" becomes "10.03.27", while "BB", "BEST", "OKT" and "EXP" stay as is.
 * The output always has the same length as the input.
 */
object OcrCharCorrector {

    private val replacements = mapOf('O' to '0', 'o' to '0', 'I' to '1', 'l' to '1', 'S' to '5', 'B' to '8')
    private const val SEPARATORS = "/.-"

    fun correct(line: String): CorrectedText {
        val chars = line.toCharArray()
        val corrections = mutableListOf<CharCorrection>()
        var start = 0
        while (start < chars.size) {
            if (!isRunChar(chars[start])) {
                start++
                continue
            }
            var end = start
            while (end < chars.size && isRunChar(chars[end])) end++
            if ((start until end).any { chars[it].isDigit() }) {
                fixRun(chars, start, end, corrections)
            }
            start = end
        }
        return CorrectedText(String(chars), corrections)
    }

    private fun fixRun(chars: CharArray, start: Int, end: Int, corrections: MutableList<CharCorrection>) {
        for (i in start until end) {
            val replacement = replacements[chars[i]] ?: continue
            if (touchesOtherLetter(chars, i)) continue
            corrections += CharCorrection(i, chars[i], replacement)
            chars[i] = replacement
        }
    }

    private fun touchesOtherLetter(chars: CharArray, index: Int): Boolean {
        val left = chars.getOrNull(index - 1)
        val right = chars.getOrNull(index + 1)
        return isOtherLetter(left) || isOtherLetter(right)
    }

    private fun isOtherLetter(c: Char?): Boolean = c != null && c.isLetter() && c !in replacements

    private fun isRunChar(c: Char): Boolean = c.isDigit() || c in replacements || c in SEPARATORS
}
