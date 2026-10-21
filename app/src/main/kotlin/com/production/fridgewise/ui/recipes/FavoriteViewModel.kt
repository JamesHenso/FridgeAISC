package com.production.fridgewise.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.RecipesApiException
import com.production.fridgewise.data.repository.RecipesRepository
import com.production.fridgewise.domain.model.Recipe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoriteUiState(
    val recipes: List<Recipe> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val needsSignIn: Boolean = false,
    val removing: Set<String> = emptySet()
)

class FavoriteViewModel(
    private val token: String,
    private val repository: RecipesRepository = RecipesRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(FavoriteUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun refresh() {
        loadJob?.cancel()
        _uiState.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val recipes = repository.favoriteRecipes(token)
                _uiState.update { it.copy(recipes = recipes, loading = false, needsSignIn = false) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message ?: "Không thể tải món đã lưu.",
                    needsSignIn = e is RecipesApiException && e.statusCode == 401) }
            }
        }
    }

    fun remove(id: String) {
        if (id in _uiState.value.removing) return
        _uiState.update { it.copy(removing = it.removing + id) }
        viewModelScope.launch {
            try {
                repository.setFavorite(token, id, false)
                _uiState.update { it.copy(recipes = it.recipes.filterNot { recipe -> recipe.id == id }) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Không thể bỏ lưu món ăn.",
                    needsSignIn = e is RecipesApiException && e.statusCode == 401) }
            } finally {
                _uiState.update { it.copy(removing = it.removing - id) }
            }
        }
    }
}
