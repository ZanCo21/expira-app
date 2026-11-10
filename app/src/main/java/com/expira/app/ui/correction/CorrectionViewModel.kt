package com.expira.app.ui.correction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expira.app.ExpiraApplication
import com.expira.app.domain.CorrectDateUseCase
import com.expira.app.domain.ExpiryInfo
import com.expira.app.domain.ExpiryStatusCalculator
import com.expira.app.domain.ScanSessionStore
import com.expira.app.domain.SystemTodayProvider
import com.expira.app.domain.TodayProvider
import com.expira.app.extraction.CalendarDate
import com.expira.app.extraction.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DateField(val maxLength: Int) { DAY(2), MONTH(2), YEAR(4) }

data class CorrectionUiState(
    val ocrText: String = "",
    val day: String = "",
    val month: String = "",
    val year: String = "",
    /** Null while a field is still empty. */
    val validation: ValidationResult? = null,
    val preview: CalendarDate? = null,
    val expiry: ExpiryInfo? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val sessionMissing: Boolean = false,
) {
    val canSave: Boolean get() = validation is ValidationResult.Valid && !isSaving
}

/** Figma "Koreksi Manual": DD / MM / YYYY input with live validation and preview. */
class CorrectionViewModel(
    private val sessionStore: ScanSessionStore,
    private val correctDate: CorrectDateUseCase,
    private val todayProvider: TodayProvider,
    prefill: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState(prefill))
    val uiState: StateFlow<CorrectionUiState> = _uiState.asStateFlow()

    private fun initialState(prefill: Boolean): CorrectionUiState {
        val outcome = sessionStore.current.value?.outcome ?: return CorrectionUiState(sessionMissing = true)
        val best = outcome.extraction.best
        val ocrText = best?.sourceLine?.trim() ?: outcome.rawText.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
        val state = CorrectionUiState(ocrText = ocrText.orEmpty())
        val date = best?.date.takeIf { prefill } ?: return state
        return validated(state.copy(day = "%02d".format(date.day), month = "%02d".format(date.month), year = date.year.toString()))
    }

    fun onFieldChange(field: DateField, value: String) {
        val digits = value.filter(Char::isDigit).take(field.maxLength)
        _uiState.update { state ->
            validated(
                when (field) {
                    DateField.DAY -> state.copy(day = digits)
                    DateField.MONTH -> state.copy(month = digits)
                    DateField.YEAR -> state.copy(year = digits)
                },
            )
        }
    }

    private fun validated(state: CorrectionUiState): CorrectionUiState {
        val complete = state.day.isNotEmpty() && state.month.isNotEmpty() && state.year.length == DateField.YEAR.maxLength
        if (!complete) return state.copy(validation = null, preview = null, expiry = null)
        val result = correctDate.validate(state.day.toIntOrNull(), state.month.toIntOrNull(), state.year.toIntOrNull())
        val date = (result as? ValidationResult.Valid)?.date
        return state.copy(
            validation = result,
            preview = date,
            expiry = date?.let { ExpiryStatusCalculator.of(it, todayProvider.today()) },
        )
    }

    fun save() {
        val state = _uiState.value
        val session = sessionStore.current.value
        if (session == null) {
            _uiState.update { it.copy(sessionMissing = true) }
            return
        }
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val id = correctDate.save(session, state.day.toIntOrNull(), state.month.toIntOrNull(), state.year.toIntOrNull())
            if (id != null) sessionStore.clear()
            _uiState.update { it.copy(isSaving = false, saved = id != null) }
        }
    }

    companion object {
        const val ARG_PREFILL = "prefill"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiraApplication
                val prefill = createSavedStateHandle().get<Boolean>(ARG_PREFILL) ?: false
                CorrectionViewModel(
                    app.container.scanSessionStore,
                    app.container.correctDateUseCase,
                    SystemTodayProvider,
                    prefill,
                )
            }
        }
    }
}
