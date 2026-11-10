package com.expira.app.ui.review

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expira.app.ExpiraApplication
import com.expira.app.domain.ExpiryInfo
import com.expira.app.domain.ExpiryStatusCalculator
import com.expira.app.domain.ReminderChoice
import com.expira.app.domain.ReminderOption
import com.expira.app.domain.ReminderPlan
import com.expira.app.domain.ReminderPlanner
import com.expira.app.domain.SaveScanUseCase
import com.expira.app.domain.ScanOutcome
import com.expira.app.domain.ScanSessionStore
import com.expira.app.domain.SystemTodayProvider
import com.expira.app.domain.TodayProvider
import com.expira.app.extraction.CalendarDate
import com.expira.app.extraction.CharCorrection
import com.expira.app.imaging.NormalizedRect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ReviewEvent {
    data object Saved : ReviewEvent
    data object OpenManualInput : ReviewEvent
    data object SessionMissing : ReviewEvent
}

data class ReviewUiState(
    val image: Bitmap? = null,
    /** Where the detected date line is in [image], as fractions. */
    val highlight: NormalizedRect? = null,
    val detectedDate: CalendarDate? = null,
    val sourceLine: String = "",
    val corrections: List<CharCorrection> = emptyList(),
    val score: Double = 0.0,
    val expiry: ExpiryInfo? = null,
    /** Date options for the confirmation screen, best first. */
    val options: List<CalendarDate> = emptyList(),
    /** Index into [options]; equal to options.size when "isi manual" is selected. */
    val selected: Int = 0,
    val isSaving: Boolean = false,
    val event: ReviewEvent? = null,
    /** F-14 */
    val productName: String = "",
    /** F-13: applied when the entry is saved; null means no reminder. */
    val reminder: ReminderChoice? = null,
) {
    val isManualSelected: Boolean get() = selected == options.size
}

/** Shared by the high-confidence result screen and the medium-confidence confirmation screen. */
class ReviewViewModel(
    private val sessionStore: ScanSessionStore,
    private val saveScan: SaveScanUseCase,
    private val todayProvider: TodayProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState(todayProvider.today()))
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private fun initialState(today: CalendarDate): ReviewUiState {
        val session = sessionStore.current.value ?: return ReviewUiState(event = ReviewEvent.SessionMissing)
        val outcome = session.outcome
        val best = outcome.extraction.best
        return ReviewUiState(
            image = session.image,
            highlight = highlightOf(outcome, session.image.width, session.image.height),
            detectedDate = best?.date,
            sourceLine = best?.sourceLine?.trim().orEmpty(),
            corrections = best?.corrections.orEmpty(),
            score = outcome.assessment.score,
            expiry = best?.let { ExpiryStatusCalculator.of(it.date, today) },
            options = outcome.extraction.candidates.map { it.date }.distinct().take(MAX_OPTIONS),
            productName = session.productName,
            reminder = session.reminder,
        )
    }

    fun onProductNameChange(name: String) {
        val trimmed = name.take(MAX_NAME_LENGTH)
        sessionStore.updateProductName(trimmed)
        _uiState.update { it.copy(productName = trimmed) }
    }

    /** Stores the reminder choice; null (or disabled) removes it. */
    fun setReminder(choice: ReminderChoice?) {
        val value = choice?.takeIf { it.enabled }
        sessionStore.updateReminder(value)
        _uiState.update { it.copy(reminder = value) }
    }

    /** Reminder day for [option] relative to the detected date, for the sheet preview. */
    fun planFor(option: ReminderOption): ReminderPlan? {
        val date = _uiState.value.detectedDate ?: return null
        return ReminderPlanner.plan(date, option, todayProvider.today())
    }

    fun select(index: Int) = _uiState.update { it.copy(selected = index.coerceIn(0, it.options.size)) }

    /** Saves the selected option, or asks for manual input when "isi manual" is selected. */
    fun confirmSelection() {
        val state = _uiState.value
        if (state.isManualSelected) {
            _uiState.update { it.copy(event = ReviewEvent.OpenManualInput) }
        } else {
            state.options.getOrNull(state.selected)?.let(::save)
        }
    }

    /** Saves the detected date as is (result screen). */
    fun saveDetected() {
        _uiState.value.detectedDate?.let(::save)
    }

    private fun save(date: CalendarDate) {
        val session = sessionStore.current.value
        if (session == null) {
            _uiState.update { it.copy(event = ReviewEvent.SessionMissing) }
            return
        }
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            saveScan(session, date)
            sessionStore.clear()
            _uiState.update { it.copy(isSaving = false, event = ReviewEvent.Saved) }
        }
    }

    fun onEventHandled() = _uiState.update { it.copy(event = null) }

    companion object {
        private const val MAX_OPTIONS = 3
        private const val MAX_NAME_LENGTH = 60

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiraApplication
                ReviewViewModel(app.container.scanSessionStore, app.container.saveScanUseCase, SystemTodayProvider)
            }
        }
    }
}

/** Bounding box of the OCR line that holds the chosen date, as fractions of the image. */
internal fun highlightOf(outcome: ScanOutcome, imageWidth: Int, imageHeight: Int): NormalizedRect? {
    val best = outcome.extraction.best ?: return null
    val box = outcome.chosen?.ocr?.lines?.getOrNull(best.lineIndex)?.box ?: return null
    if (imageWidth <= 0 || imageHeight <= 0) return null
    val left = (box.left.toFloat() / imageWidth).coerceIn(0f, 1f)
    val top = (box.top.toFloat() / imageHeight).coerceIn(0f, 1f)
    val right = (box.right.toFloat() / imageWidth).coerceIn(0f, 1f)
    val bottom = (box.bottom.toFloat() / imageHeight).coerceIn(0f, 1f)
    return if (left < right && top < bottom) NormalizedRect(left, top, right, bottom) else null
}
