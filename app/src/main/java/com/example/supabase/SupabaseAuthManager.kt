package com.example.supabase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
import java.util.concurrent.TimeUnit

object SupabaseAuthManager {
    private const val TAG = "SupabaseAuth"
    private const val PREFS_NAME = "supabase_auth_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_TEAM = "team"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _currentUser = MutableStateFlow<SupabaseUser?>(null)
    val currentUser: StateFlow<SupabaseUser?> = _currentUser.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadSavedSession()
        }
    }

    private fun loadSavedSession() {
        val p = prefs ?: return
        val userId = p.getString(KEY_USER_ID, null)
        val email = p.getString(KEY_EMAIL, null)
        val token = p.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = p.getString(KEY_REFRESH_TOKEN, null)
        val username = p.getString(KEY_USERNAME, "Jugador") ?: "Jugador"
        val team = p.getString(KEY_TEAM, "") ?: ""

        if (!userId.isNullOrBlank() && !token.isNullOrBlank() && !email.isNullOrBlank()) {
            _currentUser.value = SupabaseUser(
                id = userId,
                email = email,
                accessToken = token,
                refreshToken = refreshToken,
                username = username,
                team = team
            )
            Log.d(TAG, "Sesión de Supabase cargada para: $email")
        }
    }

    suspend fun signUp(
        email: String,
        pass: String,
        username: String,
        team: String = ""
    ): Result<SupabaseUser> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.authUrl}/signup"
            val bodyJson = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
                val meta = JSONObject().apply {
                    put("username", username.trim())
                    put("team", team.trim())
                }
                put("data", meta)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = parseErrorMessage(respStr, "Error en registro (HTTP ${response.code})")
                    return@withContext Result.failure(Exception(errMsg))
                }

                val json = JSONObject(respStr)
                val userObj = if (json.has("user")) json.getJSONObject("user") else json
                val userId = userObj.optString("id", "")
                val userEmail = userObj.optString("email", email)
                val token = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", null)

                val effectiveToken = if (token.isNotBlank()) token else SupabaseConfig.anonKey

                val newUser = SupabaseUser(
                    id = userId,
                    email = userEmail,
                    accessToken = effectiveToken,
                    refreshToken = refreshToken,
                    username = username,
                    team = team
                )

                // Guardar perfil en la tabla 'profiles' de Supabase si tenemos sesión activa
                if (token.isNotBlank()) {
                    try {
                        SupabaseClient.createOrUpdateProfile(
                            UserProfile(
                                id = userId,
                                email = userEmail,
                                username = username,
                                team = team
                            ),
                            authToken = token
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo insertar en profiles inmediatamente: ${e.message}")
                    }
                }

                saveSession(newUser)
                _currentUser.value = newUser
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en signUp", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, pass: String): Result<SupabaseUser> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.authUrl}/token?grant_type=password"
            val bodyJson = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = parseErrorMessage(respStr, "Credenciales incorrectas o error de inicio (HTTP ${response.code})")
                    return@withContext Result.failure(Exception(errMsg))
                }

                val json = JSONObject(respStr)
                val token = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", null)
                val userObj = json.getJSONObject("user")
                val userId = userObj.getString("id")
                val userEmail = userObj.optString("email", email)

                val metadata = userObj.optJSONObject("user_metadata")
                var username = metadata?.optString("username", "Jugador") ?: "Jugador"
                var team = metadata?.optString("team", "") ?: ""

                // Intentar leer perfil completo desde la tabla profiles
                val profileResult = SupabaseClient.getProfile(userId, token)
                if (profileResult.isSuccess && profileResult.getOrNull() != null) {
                    val p = profileResult.getOrNull()!!
                    if (p.username.isNotBlank()) username = p.username
                    if (!p.team.isNullOrBlank()) team = p.team
                }

                val user = SupabaseUser(
                    id = userId,
                    email = userEmail,
                    accessToken = token,
                    refreshToken = refreshToken,
                    username = username,
                    team = team
                )

                saveSession(user)
                _currentUser.value = user
                Result.success(user)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en signIn", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        prefs?.edit()?.clear()?.apply()
        _currentUser.value = null
    }

    private fun saveSession(user: SupabaseUser) {
        prefs?.edit()?.apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_EMAIL, user.email)
            putString(KEY_ACCESS_TOKEN, user.accessToken)
            putString(KEY_REFRESH_TOKEN, user.refreshToken)
            putString(KEY_USERNAME, user.username)
            putString(KEY_TEAM, user.team)
            apply()
        }
    }

    private fun parseErrorMessage(jsonStr: String, fallback: String): String {
        return try {
            val json = JSONObject(jsonStr)
            val rawMsg = when {
                json.has("msg") -> json.getString("msg")
                json.has("message") -> json.getString("message")
                json.has("error_description") -> json.getString("error_description")
                json.has("error") -> json.getString("error")
                else -> fallback
            }

            when {
                rawMsg.contains("rate limit", ignoreCase = true) || rawMsg.contains("over_email_send_rate_limit", ignoreCase = true) || rawMsg.contains("too many requests", ignoreCase = true) ->
                    "Límite de correos por hora de Supabase alcanzado. Para solucionarlo: en tu panel de Supabase ve a Authentication > Providers > Email y desactiva 'Confirm email', o intenta Iniciar Sesión si tu cuenta ya se creó."
                rawMsg.contains("Invalid path specified in request URL", ignoreCase = true) ->
                    "URL de Supabase mal configurada (incluía /rest/v1 o ruta inválida). Ya ha sido corregida automáticamente."
                rawMsg.contains("invalid", ignoreCase = true) && rawMsg.contains("email", ignoreCase = true) ->
                    "El formato del correo electrónico no es válido. Usa un email como usuario@gmail.com"
                rawMsg.contains("already registered", ignoreCase = true) || rawMsg.contains("already exists", ignoreCase = true) ->
                    "Ya existe un usuario con este correo. Inicia sesión en la pestaña 'Iniciar Sesión'."
                rawMsg.contains("invalid login credentials", ignoreCase = true) ->
                    "Credenciales incorrectas. Comprueba el correo y la contraseña."
                rawMsg.contains("password should be at least", ignoreCase = true) ->
                    "La contraseña debe tener un mínimo de 6 caracteres."
                rawMsg.contains("Email not confirmed", ignoreCase = true) ->
                    "Debes confirmar tu correo en el enlace que te envió Supabase antes de iniciar sesión."
                else -> rawMsg
            }
        } catch (_: Exception) {
            fallback
        }
    }
}
