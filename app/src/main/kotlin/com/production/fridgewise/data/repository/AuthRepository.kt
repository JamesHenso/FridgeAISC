package com.production.fridgewise.data.repository

import com.production.fridgewise.BuildConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
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

    suspend fun signIn(email: String, password: String): Result<Unit> =
        authenticate("token?grant_type=password", email, password)

    suspend fun signUp(email: String, password: String): Result<Unit> =
        authenticate("signup", email, password)

    private suspend fun authenticate(
        endpoint: String,
        email: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            check(supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
                "Thiếu supabase.url hoặc supabase.anonKey trong local.properties."
            }
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
                val payload = JsonObject().apply {
                    addProperty("email", email)
                    addProperty("password", password)
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    throw IOException(errorMessage(body, status))
                }
                val response = JsonParser.parseString(body).asJsonObject
                val token = response.get("access_token")?.takeUnless { it.isJsonNull }?.asString
                check(endpoint == "signup" || !token.isNullOrBlank()) { "Không nhận được phiên đăng nhập." }
                accessToken = token
                // Signup can succeed without a session when email confirmation is enabled.
                Result.success(Unit)
            } finally {
                connection.disconnect()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
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
