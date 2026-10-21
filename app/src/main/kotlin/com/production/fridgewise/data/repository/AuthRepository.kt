package com.production.fridgewise.data.repository

import com.production.fridgewise.BuildConfig
import com.production.fridgewise.data.OAuthCallback
import com.production.fridgewise.data.OAuthPkce
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Calls Supabase Auth directly and keeps the access token in memory for API requests. */
class AuthRepository(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY
) {
    @Volatile
    var accessToken: String? = null
        private set

    var userId: String? = null
        private set

    /** Display name from the OAuth provider; used as the first fridge's name. */
    var displayName: String? = null
        private set

    suspend fun signIn(email: String, password: String): Result<Unit> =
        authenticate("token?grant_type=password", email, password)

    suspend fun signUp(email: String, password: String): Result<Unit> =
        authenticate("signup", email, password)

    /** Browser URL for Google/Facebook sign-in. [verifier] is kept by the caller for [completeOAuth]. */
    fun oauthUrl(provider: String, verifier: String): String {
        val query = listOf(
            "provider" to provider.lowercase(),
            "redirect_to" to OAuthCallback.REDIRECT_URI,
            "code_challenge" to OAuthPkce.challenge(verifier),
            "code_challenge_method" to "s256"
        ).joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }
        return "${supabaseUrl.trimEnd('/')}/auth/v1/authorize?$query"
    }

    suspend fun completeOAuth(code: String, verifier: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            check(supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
                "Thiếu supabase.url hoặc supabase.anonKey trong local.properties."
            }
            val payload = JsonObject().apply {
                addProperty("auth_code", code)
                addProperty("code_verifier", verifier)
            }
            storeSession(post("token?grant_type=pkce", payload))
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun authenticate(
        endpoint: String,
        email: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            check(supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
                "Thiếu supabase.url hoặc supabase.anonKey trong local.properties."
            }
            val payload = JsonObject().apply {
                addProperty("email", email)
                addProperty("password", password)
            }
            val response = post(endpoint, payload)
            storeSession(response)
            check(endpoint == "signup" || !accessToken.isNullOrBlank()) { "Không nhận được phiên đăng nhập." }
            // Signup can succeed without a session when email confirmation is enabled.
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun post(endpoint: String, payload: JsonObject): JsonObject {
        val connection = URL("${supabaseUrl.trimEnd('/')}/auth/v1/$endpoint")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("apikey", anonKey)
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException(errorMessage(body, status))
            return JsonParser.parseString(body).asJsonObject
        } finally {
            connection.disconnect()
        }
    }

    private fun storeSession(response: JsonObject) {
        accessToken = response.get("access_token")?.takeUnless { it.isJsonNull }?.asString
        val user = response.get("user")?.takeUnless { it.isJsonNull }?.asJsonObject
        userId = user?.get("id")?.takeUnless { it.isJsonNull }?.asString
        val metadata = user?.get("user_metadata")?.takeUnless { it.isJsonNull }?.asJsonObject
        displayName = listOf("name", "full_name", "preferred_username")
            .firstNotNullOfOrNull { key ->
                metadata?.get(key)?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }
            }
            ?: user?.get("email")?.takeUnless { it.isJsonNull }?.asString?.substringBefore('@')
    }

    private fun errorMessage(body: String, status: Int): String {
        val message = runCatching {
            val json = JsonParser.parseString(body).asJsonObject
            listOf("msg", "error_description", "message", "error")
                .firstNotNullOfOrNull { key ->
                    json.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                        ?.asString?.takeIf { it.isNotBlank() }
                }
        }.getOrNull()
        return message ?: "Không thể xác thực tài khoản (HTTP $status). Vui lòng thử lại."
    }
}
