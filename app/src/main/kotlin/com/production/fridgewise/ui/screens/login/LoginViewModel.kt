package com.production.fridgewise.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(val isLoading: Boolean = false, val error: String? = null)

@HiltViewModel
class LoginViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {
    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _ui.value = LoginUiState(isLoading = true)
            repo.signInWithEmail(email, password)
                .onSuccess { _ui.value = LoginUiState() }
                .onFailure { _ui.value = LoginUiState(error = it.message) }
        }
    }

    fun signUp(email: String, password: String) = signInWithEmail(email, password)
    fun resetPassword(email: String) {
        viewModelScope.launch { repo.resetPassword(email) }
    }
    fun signInWithGoogle(idToken: String) {
        _ui.value = LoginUiState(isLoading = true)
        repo.signInWithGoogle(idToken)
            .onSuccess { _ui.value = LoginUiState() }
            .onFailure { _ui.value = LoginUiState(error = it.message) }
    }
}
