package com.example.supabase

import org.json.JSONObject

data class SupabaseUser(
    val id: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String? = null,
    val username: String = "",
    val team: String = "",
    val age: Int? = null,
    val avatarUrl: String? = null
)

data class UserProfile(
    val id: String,
    val email: String,
    val username: String,
    val avatarUrl: String? = null,
    val age: Int? = null,
    val team: String? = null,
    val createdAt: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("email", email)
        put("username", username)
        avatarUrl?.let { put("avatar_url", it) }
        age?.let { put("age", it) }
        team?.let { put("team", it) }
    }

    companion object {
        fun fromJson(json: JSONObject): UserProfile {
            return UserProfile(
                id = json.optString("id", ""),
                email = json.optString("email", ""),
                username = json.optString("username", "Jugador"),
                avatarUrl = json.optString("avatar_url", null),
                age = if (json.has("age") && !json.isNull("age")) json.optInt("age") else null,
                team = json.optString("team", null),
                createdAt = json.optString("created_at", null)
            )
        }
    }
}

data class TrainingSessionPayload(
    val id: String? = null,
    val userId: String,
    val sessionType: String, // 'SHOOTING', 'DRIBBLE', 'REACTION', 'DEFEND'
    val durationSec: Int,
    val totalShots: Int,
    val makes: Int,
    val accuracyPct: Float,
    val avgReleaseSpeed: Float = 0f,
    val avgAngle: Float = 0f,
    val createdAt: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        id?.let { put("id", it) }
        put("user_id", userId)
        put("session_type", sessionType)
        put("duration_sec", durationSec)
        put("total_shots", totalShots)
        put("makes", makes)
        put("accuracy_pct", accuracyPct.toDouble())
        put("avg_release_speed", avgReleaseSpeed.toDouble())
        put("avg_angle", avgAngle.toDouble())
    }

    companion object {
        fun fromJson(json: JSONObject): TrainingSessionPayload {
            return TrainingSessionPayload(
                id = json.optString("id", null),
                userId = json.optString("user_id", ""),
                sessionType = json.optString("session_type", "SHOOTING"),
                durationSec = json.optInt("duration_sec", 0),
                totalShots = json.optInt("total_shots", 0),
                makes = json.optInt("makes", 0),
                accuracyPct = json.optDouble("accuracy_pct", 0.0).toFloat(),
                avgReleaseSpeed = json.optDouble("avg_release_speed", 0.0).toFloat(),
                avgAngle = json.optDouble("avg_angle", 0.0).toFloat(),
                createdAt = json.optString("created_at", null)
            )
        }
    }
}

data class SessionShotPayload(
    val id: String? = null,
    val sessionId: String,
    val userId: String,
    val courtX: Float,
    val courtY: Float,
    val isMake: Boolean,
    val releaseSpeed: Float = 0f,
    val releaseAngle: Float = 0f,
    val createdAt: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        id?.let { put("id", it) }
        put("session_id", sessionId)
        put("user_id", userId)
        put("court_x", courtX.toDouble())
        put("court_y", courtY.toDouble())
        put("is_make", isMake)
        put("release_speed", releaseSpeed.toDouble())
        put("release_angle", releaseAngle.toDouble())
    }

    companion object {
        fun fromJson(json: JSONObject): SessionShotPayload {
            return SessionShotPayload(
                id = json.optString("id", null),
                sessionId = json.optString("session_id", ""),
                userId = json.optString("user_id", ""),
                courtX = json.optDouble("court_x", 0.0).toFloat(),
                courtY = json.optDouble("court_y", 0.0).toFloat(),
                isMake = json.optBoolean("is_make", false),
                releaseSpeed = json.optDouble("release_speed", 0.0).toFloat(),
                releaseAngle = json.optDouble("release_angle", 0.0).toFloat(),
                createdAt = json.optString("created_at", null)
            )
        }
    }
}

data class MinigameScorePayload(
    val id: String? = null,
    val userId: String,
    val gameMode: String, // 'REACTION_POINTS', 'DRIBBLE_COMBO', 'DEFEND_ZONE'
    val score: Int,
    val crossoversOrHits: Int = 0,
    val streak: Int = 0,
    val stars: Int = 0,
    val createdAt: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        id?.let { put("id", it) }
        put("user_id", userId)
        put("game_mode", gameMode)
        put("score", score)
        put("crossovers_or_hits", crossoversOrHits)
        put("streak", streak)
        put("stars", stars)
    }

    companion object {
        fun fromJson(json: JSONObject): MinigameScorePayload {
            return MinigameScorePayload(
                id = json.optString("id", null),
                userId = json.optString("user_id", ""),
                gameMode = json.optString("game_mode", ""),
                score = json.optInt("score", 0),
                crossoversOrHits = json.optInt("crossovers_or_hits", 0),
                streak = json.optInt("streak", 0),
                stars = json.optInt("stars", 0),
                createdAt = json.optString("created_at", null)
            )
        }
    }
}

data class LeaderboardItem(
    val id: String,
    val username: String,
    val team: String,
    val score: Int,
    val gameMode: String,
    val stars: Int,
    val rank: Int = 1
)
