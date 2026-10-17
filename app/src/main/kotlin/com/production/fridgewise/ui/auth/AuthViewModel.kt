package com.production.fridgewise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.repository.AuthRepository
import com.production.fridgewise.domain.model.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    val accessToken: String? get() = repository.accessToken

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) = _uiState.update {
        it.copy(email = value, errorMessage = null)
    }

    fun updatePassword(value: String) = _uiState.update {
        it.copy(password = value, errorMessage = null)
    }

    fun updateConfirmPassword(value: String) = _uiState.update {
        it.copy(confirmPassword = value, errorMessage = null)
    }

    fun toggleRememberMe(value: Boolean) = _uiState.update { it.copy(rememberMe = value) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun toggleConfirmPasswordVisibility() = _uiState.update {
        it.copy(confirmPasswordVisible = !it.confirmPasswordVisible)
    }
    fun toggleTerms(value: Boolean) = _uiState.update { it.copy(agreedToTerms = value) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun signIn(onSuccess: () -> Unit) {
        val state = _uiState.value
        val validationError = when {
            state.email.isBlank() -> "Vui lòng nhập email."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches() ->
                "Email không hợp lệ."
            state.password.isBlank() -> "Vui lòng nhập mật khẩu."
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.signIn(state.email.trim(), state.password)
            _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message) }
            if (result.isSuccess) onSuccess()
        }
    }

    fun signUp(onSuccess: () -> Unit) {
        val state = _uiState.value
        val validationError = when {
            state.email.isBlank() -> "Vui lòng nhập email."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches() ->
                "Email không hợp lệ."
            state.password.length < 8 -> "Mật khẩu cần ít nhất 8 ký tự."
            state.confirmPassword != state.password -> "Mật khẩu xác nhận không khớp."
            !state.agreedToTerms -> "Vui lòng đồng ý với điều khoản và chính sách."
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.signUp(state.email.trim(), state.password)
            _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message) }
            if (result.isSuccess) onSuccess()
        }
    }
}
