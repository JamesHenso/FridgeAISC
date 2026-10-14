package com.production.fridgewise.ui.screens.storagelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.FridgeRepository
import com.production.fridgewise.ui.components.ExpiryStatus
import com.production.fridgewise.ui.components.FoodItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageListUiState(
    val title: String = "",
    val foods: List<FoodItemUi> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StorageListViewModel @Inject constructor(
    private val repo: FridgeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val location: String = savedStateHandle["location"] ?: "fridge"
    private val fridgeId: String? = savedStateHandle["fridgeId"]
    private val _ui = MutableStateFlow(StorageListUiState(title = if (location == "freezer") "Tủ đông" else "Tủ lạnh"))
    val ui: StateFlow<StorageListUiState> = _ui.asStateFlow()

    init { refresh() }

    fun refresh() {
        val fid = fridgeId ?: return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            repo.getFoods(fid, location).onSuccess { list ->
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    foods = list.map {
                        FoodItemUi(it.id, it.name, it.expiry_date, ExpiryStatus.NORMAL, it.qty_display, it.unit_display)
                    }
                )
            }.onFailure { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
        }
    }
}
