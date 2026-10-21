package com.production.fridgewise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.fridgewise.data.OAuthCallback
import com.production.fridgewise.data.OAuthPkce
import com.production.fridgewise.data.repository.AuthRepository
import com.production.fridgewise.data.repository.InventoryRepository
import com.production.fridgewise.domain.model.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository(),
    private val inventory: InventoryRepository = InventoryRepository()
) : ViewModel() {
    val userId: String? get() = repository.userId
    val accessToken: String? get() = repository.accessToken

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateName(value: String) = _uiState.update {
        it.copy(name = value, errorMessage = null)
    }

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

    // ponytail: verifier lives in memory; if Android kills the app while the browser is open,
    // the user must tap the provider button again. Persist it if that shows up in practice.
    private var oauthVerifier: String? = null

    /** Opens the provider's consent page. [openBrowser] receives the Supabase authorize URL. */
    fun signInWithProvider(provider: String, openBrowser: (String) -> Unit) {
        val verifier = OAuthPkce.verifier()
        oauthVerifier = verifier
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { openBrowser(repository.oauthUrl(provider, verifier)) }
            .onFailure {
                oauthVerifier = null
                _uiState.update { it.copy(isLoading = false, errorMessage = "Không mở được trình duyệt.") }
            }
    }

    /** Called when the browser redirects back through the app's deep link. */
    fun completeSocialSignIn(onSuccess: () -> Unit) {
        val callback = OAuthCallback.consume() ?: return
        val verifier = oauthVerifier
        oauthVerifier = null
        if (callback.error != null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Đăng nhập bị huỷ: ${callback.error}") }
            return
        }
        val code = callback.code
        if (code.isNullOrBlank() || verifier.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Phiên đăng nhập đã hết hạn. Vui lòng thử lại.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.completeOAuth(code, verifier)
            if (result.isFailure) {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message) }
                return@launch
            }
            val token = repository.accessToken.orEmpty()
            val name = repository.displayName?.trim().orEmpty().ifBlank { "Tủ lạnh của tôi" }
            val fridge = runCatching {
                // OAuth is both sign-in and sign-up, so only create a fridge for a brand-new account.
                if (inventory.fridges(token).isEmpty()) inventory.createFridge(token, name)
            }
            _uiState.update {
                it.copy(isLoading = false, errorMessage = fridge.exceptionOrNull()?.let { e ->
                    "Đã đăng nhập nhưng chưa tải được tủ: ${e.message}"
                })
            }
            if (fridge.isSuccess) onSuccess()
        }
    }

    /** Stops the spinner if the user left the browser without completing OAuth. */
    fun onAppResumed() {
        if (_uiState.value.isLoading && oauthVerifier != null && OAuthCallback.result.value == null) {
            oauthVerifier = null
            _uiState.update { it.copy(isLoading = false) }
        }
    }

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
            state.name.isBlank() -> "Vui lòng nhập tên tủ lạnh."
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
            if (result.isFailure) {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message) }
                return@launch
            }
            // Signup without a session means Supabase is waiting for email confirmation; no fridge yet.
            val token = repository.accessToken
            if (token.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Kiểm tra email để xác nhận tài khoản.") }
                return@launch
            }
            val fridge = runCatching {
                // Reuses the same guard as social sign-in so a retried signup never creates a duplicate fridge.
                if (inventory.fridges(token).isEmpty()) inventory.createFridge(token, state.name.trim())
            }
            _uiState.update {
                it.copy(isLoading = false, errorMessage = fridge.exceptionOrNull()?.let { e ->
                    "Tài khoản đã tạo nhưng chưa tạo được tủ: ${e.message}"
                })
            }
            if (fridge.isSuccess) onSuccess()
        }
    }
}
