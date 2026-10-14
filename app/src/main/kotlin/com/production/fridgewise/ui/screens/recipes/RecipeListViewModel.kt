package com.production.fridgewise.ui.screens.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.RecipeRepository
import com.production.fridgewise.ui.components.RecipeCardUi
import com.production.fridgewise.ui.components.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeListUiState(
    val recipes: List<RecipeCardUi> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedMethod: String? = null
)

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val repo: RecipeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val fridgeId: String? = savedStateHandle["fridgeId"]
    private val _ui = MutableStateFlow(RecipeListUiState())
    val ui: StateFlow<RecipeListUiState> = _ui.asStateFlow()

    init { loadMatched() }

    fun loadMatched() {
        val fid = fridgeId ?: return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            repo.getMatchedRecipes(fid).onSuccess { _ui.value = _ui.value.copy(isLoading = false, recipes = it.map { c -> c.toUi() }) }
                .onFailure { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun search() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            val s = _ui.value
            repo.searchRecipes(s.selectedCategory, s.selectedMethod, s.searchQuery.ifBlank { null })
                .onSuccess { _ui.value = s.copy(isLoading = false, recipes = it.map { c -> c.toUi() }) }
                .onFailure { _ui.value = s.copy(isLoading = false, error = it.message) }
        }
    }

    fun onQueryChange(q: String) { _ui.value = _ui.value.copy(searchQuery = q); debounceSearch(q) }
    fun searchNow() {
        searchJob?.cancel()
        val q = _ui.value.searchQuery
        if (q.isBlank()) loadMatched() else search()
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    private fun debounceSearch(q: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            if (q.isBlank()) loadMatched() else search()
        }
    }
    fun onCategoryChange(c: String?) { _ui.value = _ui.value.copy(selectedCategory = c); search() }
    fun onMethodChange(m: String?) { _ui.value = _ui.value.copy(selectedMethod = m); search() }
}
