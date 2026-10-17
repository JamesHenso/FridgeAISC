package com.production.fridgewise.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.RecipesApiException
import com.production.fridgewise.data.repository.RecipesRepository
import com.production.fridgewise.domain.model.RecipeDetail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecipeDetailState(val loading: Boolean = true, val detail: RecipeDetail? = null,
    val error: String? = null, val needsSignIn: Boolean = false)

class RecipeDetailViewModel(private val token: String, private val recipeId: String,
    private val fridgeId: String?, private val repository: RecipesRepository = RecipesRepository()) : ViewModel() {
    private val state = MutableStateFlow(RecipeDetailState())
    val uiState = state.asStateFlow()
    private var job: Job? = null
    fun refresh() {
        job?.cancel()
        state.value = RecipeDetailState()
        job = viewModelScope.launch {
            try { state.value = RecipeDetailState(loading = false, detail = repository.detail(token, recipeId, fridgeId))
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                state.value = RecipeDetailState(loading = false, error = e.message ?: "Không tải được món ăn.",
                    needsSignIn = e is RecipesApiException && e.statusCode == 401)
            }
        }
    }
}
