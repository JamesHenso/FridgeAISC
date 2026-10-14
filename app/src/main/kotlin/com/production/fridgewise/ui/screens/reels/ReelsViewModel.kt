package com.production.fridgewise.ui.screens.reels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.FavoriteRepository
import com.production.fridgewise.data.repository.RecipeRepository
import com.production.fridgewise.ui.components.RecipeCardUi
import com.production.fridgewise.ui.components.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReelUi(
    val id: String, val name: String, val servings: Int?, val time: Int?,
    val difficulty: String?, val videoUrl: String?, val likeCount: Int?, val isLiked: Boolean = false
)

data class ReelsUiState(val reels: List<ReelUi> = emptyList(), val isLoading: Boolean = true, val error: String? = null)

@HiltViewModel
class ReelsViewModel @Inject constructor(
    private val recipeRepo: RecipeRepository,
    private val favoriteRepo: FavoriteRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(ReelsUiState())
    val ui: StateFlow<ReelsUiState> = _ui.asStateFlow()
    private val cards = mutableListOf<RecipeCardUi>()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            recipeRepo.getReels().onSuccess { list ->
                cards.clear(); cards.addAll(list.map { it.toUi() })
                _ui.value = ReelsUiState(reels = cards.map {
                    ReelUi(it.id, it.name, it.servings, it.time, it.difficulty, null, it.likeCount)
                }, isLoading = false)
            }.onFailure { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun likeReel(id: String) {
        viewModelScope.launch {
            favoriteRepo.likeRecipe(id).onSuccess {
                _ui.value = _ui.value.copy(reels = _ui.value.reels.map {
                    if (it.id == id) it.copy(isLiked = true) else it
                })
            }
        }
    }
}
