package com.production.fridgewise.data.repository

import android.content.Context
import com.production.fridgewise.BuildConfig
import com.production.fridgewise.data.remote.AuthInterceptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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
    private val http: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    init {
        // cold start: nạp token đã lưu vào interceptor, nếu không mọi request đều thiếu header → 401
        authInterceptor.token = prefs.getString("token", null)
    }
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

    // GoTrue: client riêng không gắn Bearer (token cũ/fake trong interceptor sẽ làm Supabase từ chối)
    private val authHttp by lazy {
        http.newBuilder().apply { interceptors().removeAll { it is AuthInterceptor } }.build()
    }
    private fun cfg(): Pair<String, String> {
        val url = BuildConfig.SUPABASE_URL.trim()
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (url.isEmpty() || key.isEmpty()) throw IllegalStateException("Thiếu supabase.url / supabase.anonKey trong local.properties")
        return url to key
    }

    // ponytail: GoTrue REST trực tiếp qua OkHttp, khỏi thêm supabase-kt dependency cho 3 call
    private suspend fun goTrue(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val (url, key) = cfg()
        val req = Request.Builder()
            .url("$url/auth/v1/$path")
            .header("apikey", key)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        authHttp.newCall(req).execute().use { res ->
            val json = JSONObject(res.body?.string().orEmpty().ifEmpty { "{}" })
            if (!res.isSuccessful) {
                throw IllegalStateException(json.optString("msg", json.optString("message", json.optString("error_description", "Đăng nhập thất bại (${res.code})"))))
            }
            json
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        if (email.isBlank() || password.isBlank()) throw IllegalArgumentException("email/password required")
        val json = goTrue("token?grant_type=password", JSONObject().put("email", email).put("password", password))
        saveToken(json.getString("access_token"))
    }

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        if (email.isBlank() || password.isBlank()) throw IllegalArgumentException("email/password required")
        val json = goTrue("signup", JSONObject().put("email", email).put("password", password))
        // Supabase có thể yêu cầu xác nhận email → không trả session ngay
        val token = json.optString("access_token", "")
        if (token.isEmpty()) throw IllegalStateException("Đã gửi mail xác nhận — mở mail rồi đăng nhập lại")
        saveToken(token)
    }

    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        if (email.isBlank()) throw IllegalArgumentException("email required")
        goTrue("recover", JSONObject().put("email", email))
        Unit
    }

    fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        // ponytail: lấy Google ID token cần Credential Manager ở UI (Task 13 mở rộng); REST id_token grant đã sẵn
        throw UnsupportedOperationException("Đăng nhập Google chưa đấu Credential Manager — dùng Email trước")
    }

    fun signOut() {
        authInterceptor.token = null
        prefs.edit().remove("token").apply()
        _state.value = AuthState.Unauthenticated
    }
}
