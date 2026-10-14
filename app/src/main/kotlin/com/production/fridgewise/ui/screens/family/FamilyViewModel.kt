package com.production.fridgewise.ui.screens.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.remote.dto.MemberResponse
import com.production.fridgewise.data.repository.FridgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyUiState(
    val members: List<MemberResponse> = emptyList(),
    val inviteCode: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class FamilyViewModel @Inject constructor(private val repo: FridgeRepository) : ViewModel() {
    private val _ui = MutableStateFlow(FamilyUiState())
    val ui: StateFlow<FamilyUiState> = _ui.asStateFlow()
    var fridgeId: String? = null

    fun load(fridgeId: String) {
        this.fridgeId = fridgeId
        refresh()
    }

    fun refresh() {
        val fid = fridgeId ?: return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            repo.getMembers(fid).onSuccess { _ui.value = _ui.value.copy(members = it, isLoading = false) }
                .onFailure { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun joinWithCode(code: String) {
        viewModelScope.launch {
            repo.joinFridge(code).onSuccess { refresh() }
                .onFailure { _ui.value = _ui.value.copy(error = it.message) }
        }
    }
}
