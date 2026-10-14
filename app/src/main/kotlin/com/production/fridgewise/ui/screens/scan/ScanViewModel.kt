package com.production.fridgewise.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.remote.dto.LocationAssignment
import com.production.fridgewise.data.remote.dto.ScanItemRequest
import com.production.fridgewise.data.remote.dto.UpdateScanItemRequest
import com.production.fridgewise.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ScanStep { SCAN, CONFIRM, ASSIGN }

data class ScannedItemUi(
    val id: String? = null,
    val name: String,
    val ingredientId: String? = null,
    val qtyDisplay: Double? = null,
    val unitDisplay: String? = null,
    val purchasedAt: String? = null,
    val expiryDate: String? = null,
    val location: String? = null
)

data class ScanUiState(
    val scanId: String? = null,
    val items: List<ScannedItemUi> = emptyList(),
    val step: ScanStep = ScanStep.SCAN,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ScanViewModel @Inject constructor(private val repo: ScanRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ScanUiState())
    val ui: StateFlow<ScanUiState> = _ui.asStateFlow()

    val allAssigned: Boolean get() = _ui.value.items.isNotEmpty() && _ui.value.items.all { it.location != null }

    fun createScanFromOcr(fridgeId: String, items: List<ScannedItemUi>) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            repo.createScan(fridgeId, items.map {
                ScanItemRequest(it.name, it.ingredientId, it.qtyDisplay, it.unitDisplay, it.purchasedAt, it.expiryDate)
            }).onSuccess { res ->
                val ids = res.items
                _ui.value = ScanUiState(
                    scanId = res.scanId,
                    items = items.mapIndexed { i, item -> item.copy(id = ids.getOrNull(i)?.id ?: item.id) },
                    step = ScanStep.CONFIRM
                )
            }.onFailure { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun updateItem(itemId: String?, updates: ScannedItemUi) {
        _ui.value = _ui.value.copy(items = _ui.value.items.map { if (it.id == itemId) updates.copy(id = itemId) else it })
    }

    fun addManualItem(item: ScannedItemUi) {
        _ui.value = _ui.value.copy(items = _ui.value.items + item)
    }

    fun confirmItems() {
        val scanId = _ui.value.scanId ?: return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            repo.updateItems(scanId, _ui.value.items.mapNotNull {
                it.id?.let { id -> UpdateScanItemRequest(id, it.qtyDisplay, it.unitDisplay, it.purchasedAt, it.expiryDate) }
            }).onSuccess { _ui.value = _ui.value.copy(isLoading = false, step = ScanStep.ASSIGN) }
                .onFailure { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun assignLocation(itemId: String?, location: String) {
        _ui.value = _ui.value.copy(items = _ui.value.items.map {
            if (it.id == itemId) it.copy(location = location) else it
        })
    }

    fun commitScan(onDone: () -> Unit) {
        val scanId = _ui.value.scanId ?: return
        if (!allAssigned) {
            _ui.value = _ui.value.copy(error = "Hãy phân loại hết thực phẩm trước khi hoàn tất")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            repo.updateLocations(scanId, _ui.value.items.mapNotNull {
                it.id?.let { id -> LocationAssignment(id, it.location!!) }
            }).onSuccess {
                repo.commitScan(scanId).onSuccess { _ui.value = ScanUiState(); onDone() }
                    .onFailure { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
            }.onFailure { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
        }
    }
}
