package com.expira.app.ui.processing

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expira.app.ExpiraApplication
import com.expira.app.domain.ScanDateUseCase
import com.expira.app.domain.ScanFailure
import com.expira.app.imaging.BitmapLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProcessingViewModel(
    private val imageUri: Uri,
    private val bitmapLoader: BitmapLoader,
    private val scanDate: ScanDateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { process() }
    }

    private suspend fun process() {
        val bitmap = bitmapLoader.load(imageUri)
        if (bitmap == null) {
            _uiState.update { it.copy(failure = ScanFailure.IMAGE_UNREADABLE) }
            return
        }
        _uiState.update { it.copy(preview = bitmap) }
        scanDate(bitmap).collect { event -> _uiState.update { it.reduce(event) } }
    }

    companion object {
        const val ARG_IMAGE_URI = "uri"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiraApplication
                val uri = checkNotNull(createSavedStateHandle().get<String>(ARG_IMAGE_URI)).toUri()
                ProcessingViewModel(uri, app.container.bitmapLoader, app.container.scanDateUseCase)
            }
        }
    }
}
