package com.production.fridgewise.ui.screens.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.remote.dto.IngredientResponse
import com.production.fridgewise.data.remote.dto.RecipeCardResponse
import com.production.fridgewise.data.remote.dto.RecipeDetailInfo
import com.production.fridgewise.data.repository.FavoriteRepository
import com.production.fridgewise.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeDetailUiState(
    val info: RecipeCardResponse? = null,
    val ingredients: List<IngredientResponse> = emptyList(),
    val detail: RecipeDetailInfo? = null,
    val matched: List<IngredientResponse> = emptyList(),
    val missing: List<IngredientResponse> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isFavorite: Boolean = false
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val recipeRepo: RecipeRepository,
    private val favoriteRepo: FavoriteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val recipeId: String = savedStateHandle["recipeId"] ?: ""
    private val fridgeId: String? = savedStateHandle["fridgeId"]
    private val _ui = MutableStateFlow(RecipeDetailUiState())
    val ui: StateFlow<RecipeDetailUiState> = _ui.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            recipeRepo.getRecipeDetail(recipeId, fridgeId)
                .onSuccess {
                    _ui.value = RecipeDetailUiState(
                        info = it.info, ingredients = it.ingredients, detail = it.detail,
                        matched = it.haveNeed?.matched.orEmpty(), missing = it.haveNeed?.missing.orEmpty(),
                        isLoading = false
                    )
                }
                .onFailure { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val fav = !_ui.value.isFavorite
            (if (fav) favoriteRepo.likeRecipe(recipeId) else favoriteRepo.unlikeRecipe(recipeId))
                .onSuccess { _ui.value = _ui.value.copy(isFavorite = fav) }
        }
    }
}
