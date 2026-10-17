package com.production.fridgewise.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.InventoryApiException
import com.production.fridgewise.data.repository.InventoryRepository
import com.production.fridgewise.domain.model.InventoryStats
import com.production.fridgewise.domain.model.InventoryUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val accessToken: String,
    private val repository: InventoryRepository = InventoryRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { refresh() }

    fun refresh() = load(reloadFridges = true)
    fun selectFridge(id: String) {
        if (id == _uiState.value.selectedFridgeId) return
        _uiState.update { it.copy(selectedFridgeId = id, foods = emptyList(), stats = InventoryStats()) }
        load()
    }
    fun selectLocation(location: String?) {
        _uiState.update { it.copy(location = location) }
        load()
    }
    fun search(query: String) {
        _uiState.update { it.copy(query = query) }
        load(debounce = true)
    }

    private fun load(reloadFridges: Boolean = false, debounce: Boolean = false) {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                if (debounce) delay(300)
                val current = _uiState.value
                val fridges = if (reloadFridges || current.fridges.isEmpty()) repository.fridges(accessToken) else current.fridges
                val id = current.selectedFridgeId?.takeIf { selected -> fridges.any { it.id == selected } }
                    ?: fridges.firstOrNull()?.id
                if (id == null) {
                    _uiState.update { it.copy(fridges = fridges, selectedFridgeId = null,
                        foods = emptyList(), stats = InventoryStats(), isLoading = false) }
                } else {
                    val (loadedFoods, loadedStats) = coroutineScope {
                        val foods = async { repository.foods(accessToken, id, current.location, current.query) }
                        val stats = async { repository.stats(accessToken, id) }
                        foods.await() to stats.await()
                    }
                    _uiState.update { it.copy(fridges = fridges, selectedFridgeId = id,
                        foods = loadedFoods, stats = loadedStats, isLoading = false, needsSignIn = false) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(isLoading = false,
                    error = error.message ?: "Không thể tải Inventory. Vui lòng thử lại.",
                    needsSignIn = error is InventoryApiException && error.statusCode == 401) }
            }
        }
    }
}
