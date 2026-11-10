package com.expira.app.ui.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expira.app.ExpiraApplication
import com.expira.app.imaging.CapturedFrame
import com.expira.app.imaging.CapturedFrameCropper
import com.expira.app.imaging.NormalizedRect
import com.expira.app.imaging.ScanImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CameraUiState(
    val isFlashOn: Boolean = false,
    val isCapturing: Boolean = false,
    val captureFailed: Boolean = false,
    /** Live preview looks blurry (variance of Laplacian below the threshold). */
    val isBlurry: Boolean = false,
    /** Set once the cropped capture is saved; the screen navigates and then clears it. */
    val capturedImage: Uri? = null,
)

class CameraViewModel(
    private val imageStore: ScanImageStore,
    /** Blur detection needs OpenCV; without it the warning is simply never shown. */
    val isBlurDetectionEnabled: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun toggleFlash() = _uiState.update { it.copy(isFlashOn = !it.isFlashOn) }

    fun onCaptureStarted() = _uiState.update { it.copy(isCapturing = true, captureFailed = false) }

    fun onCaptureFailed() = _uiState.update { it.copy(isCapturing = false, captureFailed = true) }

    fun onFrameCaptured(frame: CapturedFrame, guide: NormalizedRect) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.Default) {
                runCatching { CapturedFrameCropper.crop(frame, guide)?.let(imageStore::save) }.getOrNull()
            }
            _uiState.update {
                if (uri == null) it.copy(isCapturing = false, captureFailed = true)
                else it.copy(isCapturing = false, capturedImage = uri)
            }
        }
    }

    /** Called from the analysis thread; StateFlow updates are thread-safe. */
    fun onBlurChanged(isBlurry: Boolean) = _uiState.update { it.copy(isBlurry = isBlurry) }

    fun onNavigated() = _uiState.update { it.copy(capturedImage = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiraApplication
                CameraViewModel(app.container.scanImageStore, isBlurDetectionEnabled = app.isOpenCvReady)
            }
        }
    }
}
