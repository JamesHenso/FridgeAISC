package com.production.fridgewise.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.ScanRepository
import com.production.fridgewise.domain.model.ReceiptItem
import com.production.fridgewise.domain.model.ReceiptPlacement
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiptReviewState(
    val loading: Boolean = true,
    val stage: String = "Đang đọc hóa đơn…",
    val items: List<ReceiptItem> = emptyList(),
    val scanId: String? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val editingId: String? = null,
    val editError: String? = null,
    val placing: Boolean = false,
    val placement: ReceiptPlacement = ReceiptPlacement(),
    val committing: Boolean = false,
    val locationsSaved: Boolean = false,
    val commitError: String? = null,
    val completed: Boolean = false
)

class ReceiptReviewViewModel(
    private val token: String,
    private val fridgeId: String?,
    private val repository: ScanRepository = ScanRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiptReviewState())
    val uiState = _uiState.asStateFlow()
    private var recognized: List<ReceiptItem>? = null
    private var loadJob: Job? = null
    private var saveJob: Job? = null
    private var commitJob: Job? = null

    fun load(mimeType: String, image: () -> InputStream) {
        if (loadJob?.isActive == true || _uiState.value.scanId != null) return
        _uiState.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                require(!fridgeId.isNullOrBlank()) { "Vui lòng chọn tủ lạnh ở Inventory trước khi quét hóa đơn." }
                val items = recognized ?: repository.recognize(mimeType, image).also { recognized = it }
                if (items.isEmpty()) {
                    _uiState.update { it.copy(loading = false, items = emptyList()) }
                } else {
                    _uiState.update { it.copy(stage = "Đang chuẩn bị danh sách…") }
                    val draft = repository.createDraft(token, fridgeId, items)
                    _uiState.update { it.copy(loading = false, items = draft.items, scanId = draft.scanId) }
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(loading = false, error = e.message ?: "Không thể xử lý hóa đơn. Vui lòng thử lại.") }
            }
        }
    }
    fun edit(id: String?) { _uiState.update { it.copy(editingId = id, editError = null) } }
    fun save(item: ReceiptItem) {
        val scanId = _uiState.value.scanId ?: return
        if (_uiState.value.saving) return
        _uiState.update { it.copy(saving = true, editError = null) }
        saveJob = viewModelScope.launch {
            try {
                repository.updateItem(token, scanId, item)
                _uiState.update { state -> state.copy(items = state.items.map { if (it.id == item.id) item else it }, editingId = null) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(editError = e.message ?: "Không lưu được thay đổi.") }
            } finally { _uiState.update { it.copy(saving = false) } }
        }
    }
    fun startPlacement() {
        _uiState.update {
            if (it.scanId == null || it.items.isEmpty() || it.saving) it
            else it.copy(placing = true, editingId = null)
        }
    }
    fun backToReview() {
        _uiState.update { if (it.committing || it.locationsSaved) it else it.copy(placing = false, commitError = null) }
    }
    fun place(id: String, location: String) {
        _uiState.update {
            if (!it.placing || it.committing || it.locationsSaved) it
            else it.copy(placement = it.placement.place(it.items, id, location), commitError = null)
        }
    }
    fun undoPlacement() {
        _uiState.update {
            if (it.committing || it.locationsSaved) it
            else it.copy(placement = it.placement.undo(), commitError = null)
        }
    }
    fun finish() {
        val state = _uiState.value
        val scanId = state.scanId ?: return
        if (state.committing || state.completed || !state.placement.isComplete(state.items)) return
        _uiState.update { it.copy(committing = true, commitError = null) }
        commitJob = viewModelScope.launch {
            try {
                if (!_uiState.value.locationsSaved) {
                    repository.assignLocations(token, scanId, state.placement.locations)
                    _uiState.update { it.copy(locationsSaved = true) }
                }
                // If the response was lost, retry only commit: the server recognizes committed scans.
                repository.commit(token, scanId)
                _uiState.update { it.copy(completed = true) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(commitError = e.message ?: "Không thêm được thực phẩm. Vui lòng thử lại.") }
            } finally { _uiState.update { it.copy(committing = false) } }
        }
    }
    fun cancelPending() { loadJob?.cancel(); saveJob?.cancel(); commitJob?.cancel() }
}
