package com.production.fridgewise.ui.scan

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.ScanRepository
import com.production.fridgewise.domain.model.ReceiptItem
import com.production.fridgewise.domain.model.ScanMode
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiptReviewState(
    val loading: Boolean = true,
    val stage: String = "Đang đọc hóa đơn…",
    val items: List<ReceiptItem> = emptyList(),
    val excludedNames: List<String> = emptyList(),
    val merchant: String? = null,
    val receiptTotalVnd: Long? = null,
    val receiptSubtotalVnd: Long? = null,
    val receiptDiscountVnd: Long? = null,
    val reconciliationDeltaVnd: Long? = null,
    val mode: ScanMode = ScanMode.RECEIPT,
    val scanId: String? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val editingId: String? = null,
    val editError: String? = null,
    val labelSuggestion: ReceiptItem? = null,
    val labelScanning: Boolean = false,
    val committing: Boolean = false,
    val commitError: String? = null,
    val saved: Boolean = false,
    val completed: Boolean = false
)

class ReceiptReviewViewModel(
    private val token: String,
    private val fridgeId: String?,
    private val repository: ScanRepository = ScanRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiptReviewState())
    val uiState = _uiState.asStateFlow()
    private var recognized: com.production.fridgewise.domain.model.OcrScanResult? = null
    private var loadJob: Job? = null
    private var saveJob: Job? = null
    private var commitJob: Job? = null

    fun load(mode: ScanMode, mimeType: String, image: () -> InputStream) {
        if (loadJob?.isActive == true || _uiState.value.scanId != null) return
        _uiState.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                require(!fridgeId.isNullOrBlank()) { "Vui lòng chọn tủ lạnh ở Inventory trước khi quét hóa đơn." }
                val scan = recognized ?: repository.recognize(mode, mimeType, image).also { recognized = it }
                if (scan.items.isEmpty()) {
                    _uiState.update { it.copy(loading = false, items = emptyList(),
                        excludedNames = scan.excludedNames, merchant = scan.merchant,
                        receiptTotalVnd = scan.totalVnd, receiptSubtotalVnd = scan.subtotalVnd,
                        receiptDiscountVnd = scan.discountVnd,
                        reconciliationDeltaVnd = scan.reconciliationDeltaVnd, mode = scan.mode) }
                } else {
                    _uiState.update { it.copy(stage = "Đang chuẩn bị danh sách…") }
                    val draft = repository.createDraft(token, fridgeId, scan)
                    _uiState.update { it.copy(loading = false, items = draft.items, scanId = draft.scanId,
                        excludedNames = scan.excludedNames, merchant = scan.merchant,
                        receiptTotalVnd = scan.totalVnd, receiptSubtotalVnd = scan.subtotalVnd,
                        receiptDiscountVnd = scan.discountVnd,
                        reconciliationDeltaVnd = scan.reconciliationDeltaVnd,
                        mode = scan.mode) }
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(loading = false, error = e.message ?: "Không thể xử lý hóa đơn. Vui lòng thử lại.") }
            }
        }
    }
    fun edit(id: String?) { _uiState.update { it.copy(editingId = id, editError = null,
        labelSuggestion = if (id == null) null else it.labelSuggestion) } }

    fun scanLabel(id: String, mimeType: String, image: () -> InputStream) {
        if (_uiState.value.labelScanning) return
        _uiState.update { it.copy(labelScanning = true, editError = null) }
        viewModelScope.launch {
            try {
                val label = repository.recognize(ScanMode.PRODUCT, mimeType, image).items.firstOrNull()
                    ?: error("Không đọc được nhãn sản phẩm. Hãy thử ảnh rõ hơn hoặc nhập tay.")
                require(label.expiryDate != null) { "Không tìm thấy HSD trên nhãn. Hãy chụp rõ dòng HSD hoặc nhập tay." }
                _uiState.update { state ->
                    val target = state.items.firstOrNull { it.id == id } ?: return@update state
                    state.copy(labelSuggestion = target.copy(
                        expiryDate = label.expiryDate ?: target.expiryDate,
                        quantity = label.quantity ?: target.quantity,
                        unit = label.unit ?: target.unit,
                        priceVnd = label.priceVnd ?: target.priceVnd,
                        unitPriceVnd = label.unitPriceVnd ?: target.unitPriceVnd
                    ), editingId = id)
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(editError = e.message ?: "Không đọc được nhãn sản phẩm.") }
            } finally { _uiState.update { it.copy(labelScanning = false) } }
        }
    }
    fun save(item: ReceiptItem) {
        val scanId = _uiState.value.scanId ?: return
        if (_uiState.value.saving) return
        _uiState.update { it.copy(saving = true, editError = null) }
        saveJob = viewModelScope.launch {
            try {
                repository.updateItem(token, scanId, item)
                _uiState.update { state -> state.copy(items = state.items.map { if (it.id == item.id) item.copy(imageUrl = if (it.ingredientId == item.ingredientId) item.imageUrl else null) else it }, editingId = null, labelSuggestion = null) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(editError = e.message ?: "Không lưu được thay đổi.") }
            } finally { _uiState.update { it.copy(saving = false) } }
        }
    }
    fun finish() {
        val state = _uiState.value
        val scanId = state.scanId ?: return
        if (state.committing || state.completed || state.items.isEmpty() || state.items.any { it.ingredientId.isNullOrBlank() }) return
        _uiState.update { it.copy(committing = true, commitError = null, saved = false) }
        commitJob = viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            try {
                // Inventory no longer asks users to choose fridge/freezer. The backend
                // stores a compatibility default for the legacy location column.
                repository.commit(token, scanId)
                // Keep the put-away animation on screen when the network is faster than it.
                val remaining = PUT_AWAY_MIN_MILLIS - (SystemClock.elapsedRealtime() - startedAt)
                if (remaining > 0) delay(remaining)
                _uiState.update { it.copy(saved = true) }
                delay(PUT_AWAY_SUCCESS_MILLIS)
                _uiState.update { it.copy(completed = true) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(commitError = e.message ?: "Không thêm được thực phẩm. Vui lòng thử lại.") }
            } finally { _uiState.update { it.copy(committing = false) } }
        }
    }
    fun cancelPending() { loadJob?.cancel(); saveJob?.cancel(); commitJob?.cancel() }

    private companion object {
        const val PUT_AWAY_MIN_MILLIS = 1_400L
        const val PUT_AWAY_SUCCESS_MILLIS = 1_000L
    }
}
