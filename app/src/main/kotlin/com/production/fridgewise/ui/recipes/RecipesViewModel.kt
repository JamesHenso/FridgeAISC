package com.production.fridgewise.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.RecipesApiException
import com.production.fridgewise.data.repository.RecipesRepository
import com.production.fridgewise.domain.model.RecipesUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipesViewModel(
    private val token: String,
    private val fridgeId: String?,
    private val repository: RecipesRepository = RecipesRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var favoritesJob: Job? = null

    fun refresh() {
        load()
        loadFavorites()
    }

    fun search(query: String) {
        _uiState.update { it.copy(query = query) }
        load(debounce = true)
    }

    private fun load(debounce: Boolean = false) {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                if (debounce) delay(300)
                val recipes = repository.recipes(token, fridgeId, _uiState.value.query)
                _uiState.update { it.copy(recipes = recipes, isLoading = false) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Không thể tải công thức. Vui lòng thử lại.",
                    needsSignIn = it.needsSignIn || e.isUnauthorized()) }
            }
        }
    }

    private fun loadFavorites() {
        if (favoritesJob?.isActive == true || _uiState.value.updatingFavorites.isNotEmpty()) return
        _uiState.update { it.copy(favoritesReady = false) }
        favoritesJob = viewModelScope.launch {
            try {
                val favorites = repository.favorites(token)
                _uiState.update { it.copy(favorites = favorites, favoritesReady = true) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Không thể tải yêu thích. Hãy thử tải lại.",
                    needsSignIn = it.needsSignIn || e.isUnauthorized()) }
            }
        }
    }

    fun toggleFavorite(id: String) {
        val state = _uiState.value
        if (!state.favoritesReady || id in state.updatingFavorites) return
        val favorite = id !in state.favorites
        _uiState.update { it.copy(updatingFavorites = it.updatingFavorites + id) }
        viewModelScope.launch {
            try {
                repository.setFavorite(token, id, favorite)
                _uiState.update { it.copy(favorites = if (favorite) it.favorites + id else it.favorites - id) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(message = e.message ?: "Không thể cập nhật yêu thích.",
                    needsSignIn = it.needsSignIn || e.isUnauthorized()) }
            } finally {
                _uiState.update { it.copy(updatingFavorites = it.updatingFavorites - id) }
            }
        }
    }
    fun clearMessage() { _uiState.update { it.copy(message = null) } }
    private fun Exception.isUnauthorized() = this is RecipesApiException && statusCode == 401
}
