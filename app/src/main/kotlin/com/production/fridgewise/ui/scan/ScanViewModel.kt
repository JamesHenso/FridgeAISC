package com.production.fridgewise.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.production.fridgewise.domain.model.ScanMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScanUiState(
    val photoUri: String? = null,
    val mode: ScanMode = ScanMode.RECEIPT,
    val labelCaptureTarget: String? = null,
    val labelPhotoUri: String? = null,
    val torch: Boolean = false,
    val isCapturing: Boolean = false,
    val error: String? = null
)

class ScanViewModel : ViewModel(), ViewModelStoreOwner {
    // Keep the review/placement session across rotation, clear it on a fresh scan.
    override val viewModelStore = ViewModelStore()
    override fun onCleared() { viewModelStore.clear() }
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState = _uiState.asStateFlow()
    var captureGeneration: Long = 0
        private set
    fun setTorch(enabled: Boolean) { _uiState.update { it.copy(torch = enabled) } }
    fun setMode(mode: ScanMode) { if (_uiState.value.photoUri == null) _uiState.update { it.copy(mode = mode) } }
    fun startLabelCapture(id: String) { _uiState.update { it.copy(labelCaptureTarget = id, labelPhotoUri = null, error = null) } }
    fun cancelLabelCapture() { _uiState.update { it.copy(labelCaptureTarget = null, labelPhotoUri = null, isCapturing = false) } }
    fun consumeLabelPhoto() { _uiState.update { it.copy(labelCaptureTarget = null, labelPhotoUri = null) } }
    fun beginCapture(): Boolean {
        if (_uiState.value.isCapturing || (_uiState.value.photoUri != null && _uiState.value.labelCaptureTarget == null)) return false
        captureGeneration++
        _uiState.update { it.copy(isCapturing = true, error = null) }
        return true
    }
    fun showPhoto(uri: String) {
        captureGeneration++
        if (_uiState.value.labelCaptureTarget != null) {
            _uiState.update { it.copy(labelPhotoUri = uri, isCapturing = false) }
        } else {
            val mode = _uiState.value.mode
            viewModelStore.clear()
            _uiState.value = ScanUiState(photoUri = uri, mode = mode)
        }
    }
    fun retake() { viewModelStore.clear(); captureGeneration++; _uiState.value = ScanUiState() }
    fun completeCapture(generation: Long, uri: String): Boolean {
        if (generation != captureGeneration || !_uiState.value.isCapturing) return false
        showPhoto(uri)
        return true
    }
    fun failCapture(generation: Long, message: String) {
        if (generation == captureGeneration && _uiState.value.isCapturing) fail(message)
    }
    fun fail(message: String) { _uiState.update { it.copy(isCapturing = false, torch = false, error = message) } }
}
