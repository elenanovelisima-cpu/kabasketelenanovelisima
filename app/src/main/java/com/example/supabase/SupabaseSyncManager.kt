package com.example.supabase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object SupabaseSyncManager {
    private const val TAG = "SupabaseSync"
    private const val PREFS_QUEUE = "supabase_offline_queue"
    private const val KEY_PENDING_SESSIONS = "pending_sessions"
    private const val KEY_PENDING_SCORES = "pending_scores"

    private val scope = CoroutineScope(Dispatchers.IO)
    private var prefs: SharedPreferences? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow<String?>("Listo para sincronizar")
    val lastSyncStatus: StateFlow<String?> = _lastSyncStatus.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_QUEUE, Context.MODE_PRIVATE)
            SupabaseAuthManager.init(context)
            updatePendingCount()
        }
    }

    private fun updatePendingCount() {
        val p = prefs ?: return
        val sessArr = JSONArray(p.getString(KEY_PENDING_SESSIONS, "[]"))
        val scoreArr = JSONArray(p.getString(KEY_PENDING_SCORES, "[]"))
        _pendingCount.value = sessArr.length() + scoreArr.length()
    }

    /**
     * Registra una sesión de entrenamiento completa.
     * Si hay usuario autenticado, intenta enviarlo directamente a Supabase;
     * si falla o está sin conexión, lo encola localmente.
     */
    fun recordTrainingSession(
        sessionType: String,
        durationSec: Int,
        totalShots: Int,
        makes: Int,
        accuracyPct: Float,
        avgReleaseSpeed: Float = 0f,
        avgAngle: Float = 0f,
        shots: List<SessionShotPayload> = emptyList()
    ) {
        com.example.stats.PlayerStatsManager.addShootingSession(totalShots, makes)
        val user = SupabaseAuthManager.currentUser.value
        val userId = user?.id ?: "local_user_${System.currentTimeMillis()}"

        val sessionPayload = TrainingSessionPayload(
            userId = userId,
            sessionType = sessionType,
            durationSec = durationSec,
            totalShots = totalShots,
            makes = makes,
            accuracyPct = accuracyPct,
            avgReleaseSpeed = avgReleaseSpeed,
            avgAngle = avgAngle
        )

        scope.launch {
            if (user != null) {
                _isSyncing.value = true
                val result = SupabaseClient.insertTrainingSession(sessionPayload, shots, user.accessToken)
                _isSyncing.value = false
                if (result.isSuccess) {
                    _lastSyncStatus.value = "Sesión subida a Supabase con éxito"
                    Log.d(TAG, "Sesión sincronizada directamente con Supabase")
                    return@launch
                } else {
                    Log.w(TAG, "Fallo al enviar a Supabase, guardando en cola offline")
                }
            }
            // Guardar en cola offline
            enqueueSession(sessionPayload, shots)
        }
    }

    /**
     * Registra la puntuación de un minijuego (Reaction Points, Dribble Combo, Defend Zone).
     */
    fun recordMinigameScore(
        gameMode: String,
        score: Int,
        crossoversOrHits: Int = 0,
        streak: Int = 0,
        stars: Int = 0
    ) {
        com.example.stats.PlayerStatsManager.addMinigameScore(gameMode, score, crossoversOrHits)
        val user = SupabaseAuthManager.currentUser.value
        val userId = user?.id ?: "local_user_${System.currentTimeMillis()}"

        val payload = MinigameScorePayload(
            userId = userId,
            gameMode = gameMode,
            score = score,
            crossoversOrHits = crossoversOrHits,
            streak = streak,
            stars = stars
        )

        scope.launch {
            if (user != null) {
                _isSyncing.value = true
                val result = SupabaseClient.insertMinigameScore(payload, user.accessToken)
                _isSyncing.value = false
                if (result.isSuccess) {
                    _lastSyncStatus.value = "Récord de $gameMode guardado en la nube"
                    Log.d(TAG, "Récord sincronizado con Supabase")
                    return@launch
                }
            }
            enqueueScore(payload)
        }
    }

    private fun enqueueSession(session: TrainingSessionPayload, shots: List<SessionShotPayload>) {
        val p = prefs ?: return
        try {
            val sessArr = JSONArray(p.getString(KEY_PENDING_SESSIONS, "[]"))
            val item = JSONObject().apply {
                put("session", session.toJson())
                val shotsArr = JSONArray()
                for (s in shots) shotsArr.put(s.toJson())
                put("shots", shotsArr)
            }
            sessArr.put(item)
            p.edit().putString(KEY_PENDING_SESSIONS, sessArr.toString()).apply()
            updatePendingCount()
            _lastSyncStatus.value = "Guardado localmente (${sessArr.length()} pendientes)"
        } catch (e: Exception) {
            Log.e(TAG, "Error encolando sesión", e)
        }
    }

    private fun enqueueScore(score: MinigameScorePayload) {
        val p = prefs ?: return
        try {
            val scoreArr = JSONArray(p.getString(KEY_PENDING_SCORES, "[]"))
            scoreArr.put(score.toJson())
            p.edit().putString(KEY_PENDING_SCORES, scoreArr.toString()).apply()
            updatePendingCount()
            _lastSyncStatus.value = "Guardado localmente (${scoreArr.length()} pendientes)"
        } catch (e: Exception) {
            Log.e(TAG, "Error encolando score", e)
        }
    }

    /**
     * Sube todos los entrenamientos y récords pendientes en cola a Supabase
     */
    fun syncPendingNow(onComplete: (Boolean, Int) -> Unit = { _, _ -> }) {
        val user = SupabaseAuthManager.currentUser.value
        if (user == null) {
            _lastSyncStatus.value = "Inicia sesión para subir a la nube"
            onComplete(false, _pendingCount.value)
            return
        }

        scope.launch {
            _isSyncing.value = true
            val p = prefs ?: run {
                _isSyncing.value = false
                return@launch
            }

            var uploadedCount = 0
            try {
                // 1. Sesiones
                val sessStr = p.getString(KEY_PENDING_SESSIONS, "[]")
                val sessArr = JSONArray(sessStr)
                val remainingSess = JSONArray()

                for (i in 0 until sessArr.length()) {
                    val obj = sessArr.getJSONObject(i)
                    val rawSession = TrainingSessionPayload.fromJson(obj.getJSONObject("session"))
                    val session = rawSession.copy(userId = user.id)

                    val shotsList = mutableListOf<SessionShotPayload>()
                    val rawShotsArr = obj.optJSONArray("shots")
                    if (rawShotsArr != null) {
                        for (j in 0 until rawShotsArr.length()) {
                            val rawShot = SessionShotPayload.fromJson(rawShotsArr.getJSONObject(j))
                            shotsList.add(rawShot.copy(userId = user.id))
                        }
                    }

                    val res = SupabaseClient.insertTrainingSession(session, shotsList, user.accessToken)
                    if (res.isSuccess) {
                        uploadedCount++
                    } else {
                        remainingSess.put(obj)
                    }
                }
                p.edit().putString(KEY_PENDING_SESSIONS, remainingSess.toString()).apply()

                // 2. Scores
                val scoreStr = p.getString(KEY_PENDING_SCORES, "[]")
                val scoreArr = JSONArray(scoreStr)
                val remainingScores = JSONArray()

                for (i in 0 until scoreArr.length()) {
                    val rawScore = MinigameScorePayload.fromJson(scoreArr.getJSONObject(i))
                    val score = rawScore.copy(userId = user.id)
                    val res = SupabaseClient.insertMinigameScore(score, user.accessToken)
                    if (res.isSuccess) {
                        uploadedCount++
                    } else {
                        remainingScores.put(scoreArr.getJSONObject(i))
                    }
                }
                p.edit().putString(KEY_PENDING_SCORES, remainingScores.toString()).apply()

                updatePendingCount()
                _lastSyncStatus.value = if (uploadedCount > 0) "¡$uploadedCount elementos sincronizados!" else "Todo al día"
                onComplete(true, uploadedCount)
            } catch (e: Exception) {
                Log.e(TAG, "Error durante syncPendingNow", e)
                _lastSyncStatus.value = "Error al sincronizar: ${e.message}"
                onComplete(false, 0)
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
