package com.production.fridgewise.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.AuthRepository
import com.production.fridgewise.data.repository.FridgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(val fridgeName: String = "", val isLoading: Boolean = false, val error: String? = null)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val fridgeRepo: FridgeRepository,
    private val authRepo: AuthRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()
    var fridgeId: String? = null

    fun renameFridge(newName: String) {
        val fid = fridgeId ?: return
        viewModelScope.launch {
            fridgeRepo.renameFridge(fid, newName)
                .onSuccess { _ui.value = _ui.value.copy(fridgeName = newName) }
                .onFailure { _ui.value = _ui.value.copy(error = it.message) }
        }
    }

    fun logout() = authRepo.signOut()
}
