package com.expira.app.ui.evaluation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expira.app.ExpiraApplication
import com.expira.app.data.EvaluationRepository
import com.expira.app.evaluation.EvaluationLog
import com.expira.app.evaluation.ExportLogsUseCase
import com.expira.app.evaluation.RunEvaluationUseCase
import com.expira.app.extraction.CalendarDate
import com.expira.app.imaging.PreprocessConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EvaluationMessage { RUN_FAILED, EXPORT_DONE, EXPORT_FAILED }

/** Ground truth entry for an image picked for testing. */
data class GroundTruthInput(val imageUri: Uri, val imageName: String, val text: String = "", val showError: Boolean = false)

data class EvaluationUiState(
    /** Figma selects P3 by default. */
    val selected: PreprocessConfig = PreprocessConfig.P3,
    /** Rows of the most recent run, by configuration. */
    val latestRun: Map<PreprocessConfig, EvaluationLog> = emptyMap(),
    val runningConfig: PreprocessConfig? = null,
    val isExporting: Boolean = false,
    val groundTruthInput: GroundTruthInput? = null,
    val message: EvaluationMessage? = null,
) {
    val isRunning: Boolean get() = runningConfig != null
    val selectedLog: EvaluationLog? get() = latestRun[selected]
}

class EvaluationViewModel(
    repository: EvaluationRepository,
    private val runEvaluation: RunEvaluationUseCase,
    private val exportLogs: ExportLogsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EvaluationUiState())
    val uiState: StateFlow<EvaluationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeLatestRun().collect { logs ->
                _uiState.update { state -> state.copy(latestRun = logs.associateBy { it.config }) }
            }
        }
    }

    fun select(config: PreprocessConfig) = _uiState.update { it.copy(selected = config) }

    fun onImagePicked(uri: Uri, displayName: String) =
        _uiState.update { it.copy(groundTruthInput = GroundTruthInput(uri, displayName)) }

    fun onGroundTruthChange(text: String) = _uiState.update { state ->
        state.copy(groundTruthInput = state.groundTruthInput?.copy(text = text.take(MAX_INPUT), showError = false))
    }

    fun dismissGroundTruth() = _uiState.update { it.copy(groundTruthInput = null) }

    fun startRun() {
        val input = _uiState.value.groundTruthInput ?: return
        val groundTruth = parseGroundTruth(input.text)
        if (groundTruth == null) {
            _uiState.update { it.copy(groundTruthInput = input.copy(showError = true)) }
            return
        }
        _uiState.update { it.copy(groundTruthInput = null, runningConfig = PreprocessConfig.P0) }
        viewModelScope.launch {
            val logs = runEvaluation(input.imageUri, input.imageName, groundTruth) { config ->
                _uiState.update { it.copy(runningConfig = config) }
            }
            _uiState.update { it.copy(runningConfig = null, message = if (logs == null) EvaluationMessage.RUN_FAILED else null) }
        }
    }

    fun export(target: Uri) {
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val ok = exportLogs(target)
            _uiState.update {
                it.copy(isExporting = false, message = if (ok) EvaluationMessage.EXPORT_DONE else EvaluationMessage.EXPORT_FAILED)
            }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    companion object {
        private const val MAX_INPUT = 10

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiraApplication
                EvaluationViewModel(
                    app.container.evaluationRepository,
                    app.container.runEvaluationUseCase,
                    app.container.exportLogsUseCase,
                )
            }
        }
    }
}

/** Accepts DD-MM-YYYY (also with / or .) and rejects dates that do not exist. */
internal fun parseGroundTruth(text: String): CalendarDate? {
    val match = Regex("""^\s*(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})\s*$""").find(text) ?: return null
    val (day, month, year) = match.destructured
    return CalendarDate.of(day.toInt(), month.toInt(), year.toInt())
}
