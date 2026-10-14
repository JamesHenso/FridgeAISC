package com.production.fridgewise.data.repository

import com.production.fridgewise.data.remote.AuthInterceptor
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthState {
    data object Loading : AuthState
    data object Authenticated : AuthState
    data object Unauthenticated : AuthState
}

@Singleton
class AuthRepository @Inject constructor(
    private val authInterceptor: AuthInterceptor,
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow<AuthState>(
        if (getAccessToken() != null) AuthState.Authenticated else AuthState.Unauthenticated
    )
    val authState: StateFlow<AuthState> = _state.asStateFlow()

    fun getAccessToken(): String? = authInterceptor.token ?: prefs.getString("token", null)

    // ponytail: token persistence là SharedPreferences đủ dùng, DataStore khi cần reactive multi-process
    fun saveToken(token: String) {
        authInterceptor.token = token
        prefs.edit().putString("token", token).apply()
        _state.value = AuthState.Authenticated
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        if (email.isBlank() || password.isBlank()) throw IllegalArgumentException("email/password required")
        // TODO: wire Supabase GoTrue sign-in, hiện dùng token placeholder để unblock nav flow
        saveToken("dev-$email")
    }

    suspend fun signUp(email: String, password: String): Result<Unit> = signInWithEmail(email, password)

    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        if (email.isBlank()) throw IllegalArgumentException("email required")
    }

    fun signInWithGoogle(idToken: String): Result<Unit> = runCatching { saveToken(idToken) }

    fun signOut() {
        authInterceptor.token = null
        prefs.edit().remove("token").apply()
        _state.value = AuthState.Unauthenticated
    }
}
