package com.production.fridgewise.ui.screens.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.FavoriteRepository
import com.production.fridgewise.ui.components.RecipeCardUi
import com.production.fridgewise.ui.components.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoriteUiState(val favorites: List<RecipeCardUi> = emptyList(), val isLoading: Boolean = true, val error: String? = null)

@HiltViewModel
class FavoriteViewModel @Inject constructor(private val repo: FavoriteRepository) : ViewModel() {
    private val _ui = MutableStateFlow(FavoriteUiState())
    val ui: StateFlow<FavoriteUiState> = _ui.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            repo.getFavorites().onSuccess {
                _ui.value = FavoriteUiState(favorites = it.map { c -> c.toUi(isFavorite = true) }, isLoading = false)
            }.onFailure { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun removeFavorite(recipeId: String) {
        viewModelScope.launch {
            repo.unlikeRecipe(recipeId).onSuccess {
                _ui.value = _ui.value.copy(favorites = _ui.value.favorites.filter { it.id != recipeId })
            }
        }
    }
}
