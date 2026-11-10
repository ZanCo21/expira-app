package com.expira.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expira.app.ExpiraApplication
import com.expira.app.data.ScanHistoryRepository
import com.expira.app.domain.SystemTodayProvider
import com.expira.app.domain.TodayProvider
import com.expira.app.ui.components.ScanRowUi
import com.expira.app.ui.history.toRows
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Supplies the "Pemindaian terakhir" rows on Beranda. */
class HomeViewModel(repository: ScanHistoryRepository, todayProvider: TodayProvider) : ViewModel() {

    val recent: StateFlow<List<ScanRowUi>> = repository.observeRecent(RECENT_COUNT)
        .map { it.toRows(todayProvider) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    companion object {
        /** Figma Beranda shows three recent scans. */
        const val RECENT_COUNT = 3
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiraApplication
                HomeViewModel(app.container.historyRepository, SystemTodayProvider)
            }
        }
    }
}
