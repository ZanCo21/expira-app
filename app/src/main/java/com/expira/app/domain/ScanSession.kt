package com.expira.app.domain

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The scan currently being reviewed (result, confirmation, failure or correction screen). */
data class ScanSession(
    /** Upright, downscaled input image; also saved with the history entry. */
    val image: Bitmap,
    val outcome: ScanOutcome,
)

/**
 * Holds the scan under review in memory between screens. It is not persisted: after
 * process death the review screens find no session and return to the home screen.
 */
class ScanSessionStore {
    private val _current = MutableStateFlow<ScanSession?>(null)
    val current: StateFlow<ScanSession?> = _current.asStateFlow()

    fun start(session: ScanSession) {
        _current.value = session
    }

    fun clear() {
        _current.value = null
    }
}
