package com.production.fridgewise.domain.model

data class AuthUiState(
    val email: String = "",
    val name: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val rememberMe: Boolean = true,
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val agreedToTerms: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
