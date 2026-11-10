package com.expira.app.domain

import com.expira.app.extraction.DateValidator
import com.expira.app.extraction.ValidationResult

/** Validates a manually entered expiry date and saves it as the final date. */
class CorrectDateUseCase(
    private val saveScan: SaveScanUseCase,
    private val todayProvider: TodayProvider = SystemTodayProvider,
) {

    fun validate(day: Int?, month: Int?, year: Int?): ValidationResult =
        DateValidator(todayProvider.today()).validate(day, month, year)

    /** Saves only a valid date; returns the new entry id, or null when the input is invalid. */
    suspend fun save(session: ScanSession, day: Int?, month: Int?, year: Int?): Long? {
        val valid = validate(day, month, year) as? ValidationResult.Valid ?: return null
        return saveScan(session, valid.date)
    }
}
