package com.example.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun getAuthToken(providedToken: String?): String {
        return providedToken
            ?: SupabaseAuthManager.currentUser.value?.accessToken
            ?: SupabaseConfig.anonKey
    }

    /**
     * Guarda o actualiza el perfil en la tabla 'profiles'
     */
    suspend fun createOrUpdateProfile(
        profile: UserProfile,
        authToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken(authToken)
            val url = "${SupabaseConfig.restUrl}/profiles"
            val body = profile.toJson().toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Perfil guardado con éxito: ${profile.id}")
                    Result.success(Unit)
                } else {
                    val err = response.body?.string().orEmpty()
                    Log.e(TAG, "Error guardando perfil HTTP ${response.code}: $err")
                    Result.failure(Exception("Error en perfil ($err)"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en createOrUpdateProfile", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene el perfil de un usuario por su UUID
     */
    suspend fun getProfile(
        userId: String,
        authToken: String? = null
    ): Result<UserProfile?> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken(authToken)
            val url = "${SupabaseConfig.restUrl}/profiles?id=eq.$userId&limit=1"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val arr = JSONArray(body)
                    if (arr.length() > 0) {
                        Result.success(UserProfile.fromJson(arr.getJSONObject(0)))
                    } else {
                        Result.success(null)
                    }
                } else {
                    Result.failure(Exception("Error obteniendo perfil: $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en getProfile", e)
            Result.failure(e)
        }
    }

    /**
     * Guarda una sesión de entrenamiento completa y sus tiros asociados en Supabase
     */
    suspend fun insertTrainingSession(
        session: TrainingSessionPayload,
        shots: List<SessionShotPayload> = emptyList(),
        authToken: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken(authToken)
            val sessionUrl = "${SupabaseConfig.restUrl}/training_sessions"
            val sessionBody = session.toJson().toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(sessionUrl)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(sessionBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Error insertando sesión: HTTP ${response.code}: $respStr")
                    return@withContext Result.failure(Exception("Error insertando sesión ($respStr)"))
                }

                var createdSessionId = session.id ?: ""
                try {
                    val arr = JSONArray(respStr)
                    if (arr.length() > 0) {
                        createdSessionId = arr.getJSONObject(0).optString("id", createdSessionId)
                    }
                } catch (_: Exception) {}

                // Si hay tiros individuales, guardarlos en bloque en session_shots
                if (shots.isNotEmpty() && createdSessionId.isNotBlank()) {
                    insertSessionShots(createdSessionId, shots, token)
                }

                Log.d(TAG, "Sesión de entrenamiento insertada correctamente: $createdSessionId")
                Result.success(createdSessionId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en insertTrainingSession", e)
            Result.failure(e)
        }
    }

    /**
     * Inserción en lote de tiros de una sesión
     */
    private fun insertSessionShots(
        sessionId: String,
        shots: List<SessionShotPayload>,
        token: String
    ) {
        try {
            val shotsUrl = "${SupabaseConfig.restUrl}/session_shots"
            val jsonArray = JSONArray()
            for (s in shots) {
                val shotJson = s.copy(sessionId = sessionId).toJson()
                jsonArray.put(shotJson)
            }

            val request = Request.Builder()
                .url(shotsUrl)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(jsonArray.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Aviso al guardar tiros individuales: HTTP ${resp.code}")
                } else {
                    Log.d(TAG, "${shots.size} tiros guardados en Supabase")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error insertando session_shots", e)
        }
    }

    /**
     * Guarda la puntuación de un minijuego (Reaction Points, Dribble Combo, Defend Zone)
     */
    suspend fun insertMinigameScore(
        score: MinigameScorePayload,
        authToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken(authToken)
            val url = "${SupabaseConfig.restUrl}/minigame_scores"
            val body = score.toJson().toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Minigame score subido con éxito: ${score.gameMode} -> ${score.score}")
                    Result.success(Unit)
                } else {
                    val err = response.body?.string().orEmpty()
                    Log.e(TAG, "Error subiendo minigame score: $err")
                    Result.failure(Exception("Error subiendo record ($err)"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en insertMinigameScore", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene el ranking global de minijuegos desde Supabase
     */
    suspend fun getLeaderboard(
        gameMode: String? = null,
        limit: Int = 15
    ): Result<List<LeaderboardItem>> = withContext(Dispatchers.IO) {
        try {
            val modeFilter = if (!gameMode.isNullOrBlank()) "&game_mode=eq.$gameMode" else ""
            val url = "${SupabaseConfig.restUrl}/minigame_scores?select=id,score,stars,game_mode,user_id,profiles(username,team)$modeFilter&order=score.desc&limit=$limit"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val arr = JSONArray(body)
                    val list = mutableListOf<LeaderboardItem>()
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        val id = item.optString("id", "$i")
                        val score = item.optInt("score", 0)
                        val stars = item.optInt("stars", 0)
                        val mode = item.optString("game_mode", "GAME")

                        var username = "Jugador #${i + 1}"
                        var team = "Streetball"
                        if (item.has("profiles") && !item.isNull("profiles")) {
                            val prof = item.optJSONObject("profiles")
                            if (prof != null) {
                                username = prof.optString("username", username)
                                team = prof.optString("team", team)
                            }
                        }

                        list.add(
                            LeaderboardItem(
                                id = id,
                                username = username,
                                team = team,
                                score = score,
                                gameMode = mode,
                                stars = stars,
                                rank = i + 1
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo leaderboard", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene el listado de sesiones del usuario
     */
    suspend fun getUserSessions(
        userId: String,
        authToken: String? = null
    ): Result<List<TrainingSessionPayload>> = withContext(Dispatchers.IO) {
        try {
            val token = getAuthToken(authToken)
            val url = "${SupabaseConfig.restUrl}/training_sessions?user_id=eq.$userId&order=created_at.desc&limit=30"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.anonKey)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val arr = JSONArray(body)
                    val list = mutableListOf<TrainingSessionPayload>()
                    for (i in 0 until arr.length()) {
                        list.add(TrainingSessionPayload.fromJson(arr.getJSONObject(i)))
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo sesiones", e)
            Result.failure(e)
        }
    }

    /**
     * Prueba la conexión con el servidor Supabase usando la URL y API Key especificadas o activas
     */
    suspend fun testConnection(
        testUrl: String? = null,
        testKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val base = if (!testUrl.isNullOrBlank()) SupabaseConfig.cleanUrl(testUrl) else SupabaseConfig.baseUrl
            val key = if (!testKey.isNullOrBlank()) testKey.trim() else SupabaseConfig.anonKey
            val url = "$base/rest/v1/profiles"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                val body = response.body?.string().orEmpty()
                when (code) {
                    200 -> Result.success("✅ ¡Conexión exitosa con Supabase! Base de datos y API online.")
                    401 -> Result.failure(Exception("⚠️ Clave API rechazada (HTTP 401). Verifica tu Anon Key."))
                    404 -> Result.failure(Exception("❌ Ruta no encontrada (HTTP 404). Verifica que la URL sea la correcta."))
                    else -> Result.failure(Exception("⚠️ Código HTTP $code: $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en testConnection", e)
            Result.failure(Exception("❌ Error al conectar: ${e.message ?: "Sin respuesta del servidor"}"))
        }
    }
}
