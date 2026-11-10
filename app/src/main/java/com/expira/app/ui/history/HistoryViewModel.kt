package com.expira.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expira.app.ExpiraApplication
import com.expira.app.data.ScanHistoryRepository
import com.expira.app.domain.DeleteScanUseCase
import com.expira.app.domain.ExpiryStatus
import com.expira.app.domain.ExpiryStatusCalculator
import com.expira.app.domain.ReminderStatus
import com.expira.app.domain.ScanRecord
import com.expira.app.domain.SystemTodayProvider
import com.expira.app.domain.TodayProvider
import com.expira.app.ui.components.ScanRowUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Figma filter pills: Semua / Aman / Mendekati / Lewat. */
enum class HistoryFilter(val status: ExpiryStatus?) {
    ALL(null),
    SAFE(ExpiryStatus.SAFE),
    NEAR(ExpiryStatus.NEAR),
    EXPIRED(ExpiryStatus.EXPIRED),
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val filter: HistoryFilter = HistoryFilter.ALL,
    val items: List<ScanRowUi> = emptyList(),
    /** True when there are saved scans at all (to tell "no data" from "no match"). */
    val hasAnyScan: Boolean = false,
    /** Entry awaiting delete confirmation (long press). */
    val pendingDeleteId: Long? = null,
)

class HistoryViewModel(
    repository: ScanHistoryRepository,
    private val deleteScan: DeleteScanUseCase,
    private val todayProvider: TodayProvider,
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val pendingDelete = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HistoryUiState> =
        combine(repository.observeAll(), filter, pendingDelete) { records, selected, deleteId ->
            val rows = records.toRows(todayProvider)
            HistoryUiState(
                isLoading = false,
                filter = selected,
                items = rows.filter { selected.status == null || it.status == selected.status },
                hasAnyScan = rows.isNotEmpty(),
                pendingDeleteId = deleteId,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState())

    fun selectFilter(value: HistoryFilter) = filter.update { value }

    fun requestDelete(id: Long) = pendingDelete.update { id }

    fun cancelDelete() = pendingDelete.update { null }

    /** Deletes the entry, its photo and its reminder (AC-15). */
    fun confirmDelete() {
        val id = pendingDelete.value ?: return
        pendingDelete.update { null }
        viewModelScope.launch { deleteScan(id) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiraApplication
                HistoryViewModel(app.container.historyRepository, app.container.deleteScanUseCase, SystemTodayProvider)
            }
        }
    }
}

/** Status is computed against today, so it changes as dates pass. */
internal fun List<ScanRecord>.toRows(todayProvider: TodayProvider): List<ScanRowUi> {
    val today = todayProvider.today()
    return map { record ->
        ScanRowUi(
            id = record.id,
            expiry = record.expiry,
            status = ExpiryStatusCalculator.of(record.expiry, today).status,
            wasCorrected = record.wasCorrected,
            productName = record.productName,
            hasReminder = record.reminder?.status == ReminderStatus.SCHEDULED,
        )
    }
}
