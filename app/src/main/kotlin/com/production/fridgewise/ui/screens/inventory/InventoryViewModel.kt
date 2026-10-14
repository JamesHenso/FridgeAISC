package com.production.fridgewise.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.FridgeRepository
import com.production.fridgewise.data.remote.dto.FridgeStatsResponse
import com.production.fridgewise.ui.components.ExpiryStatus
import com.production.fridgewise.ui.components.FoodItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class InventoryUiState(
    val fridgeId: String? = null,
    val fridgeName: String = "",
    val stats: FridgeStatsResponse? = null,
    val freezerFoods: List<FoodItemUi> = emptyList(),
    val fridgeFoods: List<FoodItemUi> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

private fun statusOf(expiry: String?, today: String): ExpiryStatus = when {
    expiry == null -> ExpiryStatus.NORMAL
    expiry <= today -> ExpiryStatus.USE_TODAY
    else -> try {
        val e = LocalDate.parse(expiry); val t = LocalDate.parse(today)
        if (!e.isAfter(t.plusDays(3))) ExpiryStatus.USE_SOON else ExpiryStatus.NORMAL
    } catch (_: Exception) { ExpiryStatus.NORMAL }
}

@HiltViewModel
class InventoryViewModel @Inject constructor(private val repo: FridgeRepository) : ViewModel() {
    private val _ui = MutableStateFlow(InventoryUiState())
    val ui: StateFlow<InventoryUiState> = _ui.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            val fridges = repo.getMyFridges().getOrElse {
                _ui.value = _ui.value.copy(isLoading = false, error = it.message); return@launch
            }
            val fridge = fridges.firstOrNull()
            if (fridge == null) {
                _ui.value = InventoryUiState(isLoading = false); return@launch
            }
            val today = LocalDate.now().toString()
            val stats = repo.getStats(fridge.id).getOrNull()
            val foods = repo.getFoods(fridge.id).getOrElse {
                _ui.value = _ui.value.copy(isLoading = false, error = it.message); return@launch
            }
            fun map(list: List<com.production.fridgewise.data.remote.dto.FoodItemResponse>) =
                list.map {
                    FoodItemUi(it.id, it.name, it.expiry_date, statusOf(it.expiry_date, today), it.qty_display, it.unit_display)
                }
            _ui.value = InventoryUiState(
                fridgeId = fridge.id, fridgeName = fridge.name, stats = stats,
                freezerFoods = map(foods.filter { it.location == "freezer" }),
                fridgeFoods = map(foods.filter { it.location != "freezer" }),
                isLoading = false
            )
        }
    }
}
