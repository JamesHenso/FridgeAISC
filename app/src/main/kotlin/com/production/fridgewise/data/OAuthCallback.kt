package com.production.fridgewise.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OAuthResult(val code: String? = null, val error: String? = null)

/** Bridges the OAuth deep link (MainActivity) to the auth ViewModel. */
object OAuthCallback {
    const val REDIRECT_URI = "com.production.fridgewise://login-callback"

    private val _result = MutableStateFlow<OAuthResult?>(null)
    val result: StateFlow<OAuthResult?> = _result.asStateFlow()

    fun deliver(value: OAuthResult) { _result.value = value }

    fun consume(): OAuthResult? = _result.value.also { _result.value = null }
}
