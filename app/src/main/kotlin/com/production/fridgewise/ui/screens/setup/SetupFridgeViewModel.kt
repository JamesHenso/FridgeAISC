package com.production.fridgewise.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(val name: String = "", val isLoading: Boolean = false, val error: String? = null)

@HiltViewModel
class SetupFridgeViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _ui = MutableStateFlow(SetupUiState())
    val ui: StateFlow<SetupUiState> = _ui.asStateFlow()

    fun onNameChange(name: String) { _ui.value = _ui.value.copy(name = name) }

    fun createFridge(onDone: () -> Unit) {
        viewModelScope.launch {
            // ponytail: tạo tủ qua Supabase REST trực tiếp (API.md PM4) để sau; stub done để unblock flow
            _ui.value = _ui.value.copy(isLoading = false)
            onDone()
        }
    }
}
