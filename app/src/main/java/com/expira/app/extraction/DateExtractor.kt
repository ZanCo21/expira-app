package com.expira.app.extraction

/**
 * Finds expiry-date candidates in OCR lines and picks the most likely expiry date.
 *
 * Steps per line: numeric-context character correction -> pattern matching (8 formats,
 * Indonesian/English month names) -> calendar check (invalid dates are dropped) ->
 * keyword association (same line, or a dangling keyword on the line above).
 *
 * Selection: dates governed by an expiry keyword win. Otherwise dates without any keyword
 * are used, latest first. Dates governed by a production keyword are never chosen; they
 * stay in [ExtractionResult.candidates] for manual confirmation.
 */
class DateExtractor {

    fun extract(lines: List<TextLine>): ExtractionResult {
        val prepared = lines.map { prepare(it.text) }
        val candidates = prepared.flatMapIndexed { index, line ->
            line.dates.map { found ->
                DateCandidate(
                    date = found.date,
                    format = found.format,
                    rawText = line.original.substring(found.start, found.end),
                    sourceLine = line.original,
                    lineIndex = index,
                    keyword = keywordFor(found, line, prepared.getOrNull(index - 1)),
                    corrections = line.corrections
                        .filter { it.index in found.start until found.end }
                        .map { it.copy(index = it.index - found.start) },
                    ocrConfidence = lines[index].confidence,
                )
            }
        }
        if (candidates.isEmpty()) return ExtractionResult.EMPTY

        val ranked = candidates.sortedWith(rankOrder)
        val best = ranked.first().takeIf { it.keyword.kind != KeywordKind.PRODUCTION }
        return ExtractionResult(best = best, candidates = ranked)
    }

    private fun prepare(original: String): PreparedLine {
        val corrected = OcrCharCorrector.correct(original)
        // Char-wise upper-casing keeps indices aligned with the original line.
        val upper = String(CharArray(corrected.text.length) { corrected.text[it].uppercaseChar() })
        return PreparedLine(original, corrected.corrections, findDates(upper), findKeywords(upper))
    }

    private fun findDates(text: String): List<FoundDate> {
        val found = mutableListOf<FoundDate>()
        for (pattern in datePatterns) {
            for (match in pattern.regex.findAll(text)) {
                val range = match.range
                if (found.any { range.first < it.end && it.start <= range.last }) continue
                val date = pattern.toDate(match.groupValues) ?: continue
                found += FoundDate(range.first, range.last + 1, date, pattern.format)
            }
        }
        return found.sortedBy { it.start }
    }

    private fun findKeywords(text: String): List<KeywordHit> =
        keywordRegex.findAll(text).mapNotNull { match ->
            val kind = keywordKinds[normalizeKeyword(match.value)] ?: return@mapNotNull null
            KeywordHit(match.range.first, match.range.last + 1, kind, match.value.trim())
        }.toList()

    private fun keywordFor(date: FoundDate, line: PreparedLine, previous: PreparedLine?): KeywordContext {
        line.keywords.lastOrNull { it.end <= date.start }?.let {
            return KeywordContext(it.kind, it.text, sameLine = true)
        }
        if (previous == null) return KeywordContext.NONE
        val lastDateEnd = previous.dates.maxOfOrNull { it.end } ?: -1
        val dangling = previous.keywords.lastOrNull { it.start >= lastDateEnd } ?: return KeywordContext.NONE
        return KeywordContext(dangling.kind, dangling.text, sameLine = false)
    }

    private class PreparedLine(
        val original: String,
        val corrections: List<CharCorrection>,
        val dates: List<FoundDate>,
        val keywords: List<KeywordHit>,
    )

    private class FoundDate(val start: Int, val end: Int, val date: CalendarDate, val format: DateFormat)

    private class KeywordHit(val start: Int, val end: Int, val kind: KeywordKind, val text: String)

    private class DatePattern(
        val regex: Regex,
        val format: DateFormat,
        val toDate: (groups: List<String>) -> CalendarDate?,
    )

    private companion object {
        val MONTH = DateVocabulary.monthPattern
        const val SEP = """\s*[./\-]?\s*"""

        /** Order matters: earlier patterns claim their text span first. */
        val datePatterns = listOf(
            DatePattern(
                Regex("""(?<!\d)(\d{4})[./\-](\d{1,2})[./\-](\d{1,2})(?!\d)"""),
                DateFormat.YEAR_MONTH_DAY_NUMERIC,
            ) { g -> CalendarDate.of(g[3].toInt(), g[2].toInt(), g[1].toInt()) },
            DatePattern(
                Regex("""(?<!\d)(\d{1,2})([./\-])(\d{1,2})\2(\d{4}|\d{2})(?!\d)"""),
                DateFormat.DAY_MONTH_YEAR_NUMERIC,
            ) { g -> CalendarDate.of(g[1].toInt(), g[3].toInt(), year(g[4])) },
            DatePattern(
                Regex("""(?<!\d)(\d{1,2})$SEP($MONTH)$SEP(\d{4}|\d{2})(?!\d)"""),
                DateFormat.DAY_MONTHNAME_YEAR,
            ) { g -> month(g[2])?.let { CalendarDate.of(g[1].toInt(), it, year(g[3])) } },
            DatePattern(
                Regex("""(?<![A-Z])($MONTH)$SEP(\d{4})(?!\d)"""),
                DateFormat.MONTHNAME_YEAR,
            ) { g -> month(g[1])?.let { CalendarDate.endOfMonth(it, g[2].toInt()) } },
            DatePattern(
                Regex("""(?<![\d./\-])(\d{1,2})[./\-](\d{4})(?![\d./\-])"""),
                DateFormat.MONTH_YEAR_NUMERIC,
            ) { g -> CalendarDate.endOfMonth(g[1].toInt(), g[2].toInt()) },
        )

        val keywordKinds: Map<String, KeywordKind> =
            DateVocabulary.expiryKeywords.associate { normalizeKeyword(it) to KeywordKind.EXPIRY } +
                DateVocabulary.productionKeywords.associate { normalizeKeyword(it) to KeywordKind.PRODUCTION }

        val keywordRegex: Regex = (DateVocabulary.expiryKeywords + DateVocabulary.productionKeywords)
            .sortedByDescending { it.length }
            .joinToString("|") { keyword ->
                keyword.split(" ").joinToString("""\s+""") { word ->
                    word.filter { it != '.' }.toList().joinToString("""\.?""") + """\.?"""
                }
            }
            .let { Regex("""(?<![A-Z])(?:$it)(?![A-Z])""") }

        /** Ranking: expiry keyword > no keyword > production; then closeness, full date, later date. */
        val rankOrder: Comparator<DateCandidate> = compareBy<DateCandidate> { kindRank(it.keyword.kind) }
            .thenByDescending { it.keyword.sameLine }
            .thenByDescending { it.format.hasDay }
            .thenByDescending { it.date }
            .thenByDescending { it.ocrConfidence ?: -1f }

        fun kindRank(kind: KeywordKind): Int = when (kind) {
            KeywordKind.EXPIRY -> 0
            KeywordKind.NONE -> 1
            KeywordKind.PRODUCTION -> 2
        }

        fun normalizeKeyword(text: String): String = text.filter { it.isLetter() }

        fun year(text: String): Int = if (text.length == 2) 2000 + text.toInt() else text.toInt()

        fun month(name: String): Int? = DateVocabulary.months[name]
    }
}
