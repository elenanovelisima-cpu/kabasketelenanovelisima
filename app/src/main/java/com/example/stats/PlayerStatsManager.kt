package com.example.stats

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerStats(
    val playerName: String = "Miguel",
    val reactionPointsScore: Int = 0,
    val reactionPointsBest: Int = 0,
    val reactionPointsGames: Int = 0,
    val reactionPointsHits: Int = 0,
    val dribbleComboScore: Int = 0,
    val dribbleComboBest: Int = 0,
    val dribbleComboGames: Int = 0,
    val dribbleCrossovers: Int = 0,
    val defendZoneScore: Int = 0,
    val defendZoneBest: Int = 0,
    val defendZoneGames: Int = 0,
    val defendShields: Int = 0,
    val shootingShots: Int = 0,
    val shootingMakes: Int = 0,
    val shootingSessions: Int = 0,
    val shootingXp: Int = 0
) {
    val totalGamePoints: Int
        get() = reactionPointsScore + dribbleComboScore + defendZoneScore

    val totalXp: Int
        get() = totalGamePoints + shootingXp

    val shootingAccuracyPct: Int
        get() = if (shootingShots > 0) ((shootingMakes.toFloat() / shootingShots) * 100).toInt() else 0

    val rankTitle: String
        get() = when {
            totalXp >= 1500 -> "MVP"
            totalXp >= 750 -> "ALL-STAR"
            totalXp >= 250 -> "PRO"
            else -> "ROOKIE"
        }

    val nextRankXp: Int
        get() = when {
            totalXp >= 1500 -> 3000
            totalXp >= 750 -> 1500
            totalXp >= 250 -> 750
            else -> 250
        }

    val currentRankBaseXp: Int
        get() = when {
            totalXp >= 1500 -> 1500
            totalXp >= 750 -> 750
            totalXp >= 250 -> 250
            else -> 0
        }

    val rankProgress: Float
        get() {
            val range = (nextRankXp - currentRankBaseXp).coerceAtLeast(1)
            val currentInRange = (totalXp - currentRankBaseXp).coerceAtLeast(0)
            return (currentInRange.toFloat() / range.toFloat()).coerceIn(0f, 1f)
        }
}

object PlayerStatsManager {
    private const val PREFS_STATS = "player_stats_prefs"

    private const val KEY_PLAYER_NAME = "key_player_name"
    private const val KEY_REACTION_SCORE = "key_reaction_score"
    private const val KEY_REACTION_BEST = "key_reaction_best"
    private const val KEY_REACTION_GAMES = "key_reaction_games"
    private const val KEY_REACTION_HITS = "key_reaction_hits"

    private const val KEY_DRIBBLE_SCORE = "key_dribble_score"
    private const val KEY_DRIBBLE_BEST = "key_dribble_best"
    private const val KEY_DRIBBLE_GAMES = "key_dribble_games"
    private const val KEY_DRIBBLE_CROSSOVERS = "key_dribble_crossovers"

    private const val KEY_DEFEND_SCORE = "key_defend_score"
    private const val KEY_DEFEND_BEST = "key_defend_best"
    private const val KEY_DEFEND_GAMES = "key_defend_games"
    private const val KEY_DEFEND_SHIELDS = "key_defend_shields"

    private const val KEY_SHOOTING_SHOTS = "key_shooting_shots"
    private const val KEY_SHOOTING_MAKES = "key_shooting_makes"
    private const val KEY_SHOOTING_SESSIONS = "key_shooting_sessions"
    private const val KEY_SHOOTING_XP = "key_shooting_xp"

    private var prefs: SharedPreferences? = null

    private val _stats = MutableStateFlow(PlayerStats())
    val stats: StateFlow<PlayerStats> = _stats.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_STATS, Context.MODE_PRIVATE)
            loadStats()
        }
    }

    private fun loadStats() {
        val p = prefs ?: return
        _stats.value = PlayerStats(
            playerName = p.getString(KEY_PLAYER_NAME, "Miguel") ?: "Miguel",
            reactionPointsScore = p.getInt(KEY_REACTION_SCORE, 0),
            reactionPointsBest = p.getInt(KEY_REACTION_BEST, 0),
            reactionPointsGames = p.getInt(KEY_REACTION_GAMES, 0),
            reactionPointsHits = p.getInt(KEY_REACTION_HITS, 0),
            dribbleComboScore = p.getInt(KEY_DRIBBLE_SCORE, 0),
            dribbleComboBest = p.getInt(KEY_DRIBBLE_BEST, 0),
            dribbleComboGames = p.getInt(KEY_DRIBBLE_GAMES, 0),
            dribbleCrossovers = p.getInt(KEY_DRIBBLE_CROSSOVERS, 0),
            defendZoneScore = p.getInt(KEY_DEFEND_SCORE, 0),
            defendZoneBest = p.getInt(KEY_DEFEND_BEST, 0),
            defendZoneGames = p.getInt(KEY_DEFEND_GAMES, 0),
            defendShields = p.getInt(KEY_DEFEND_SHIELDS, 0),
            shootingShots = p.getInt(KEY_SHOOTING_SHOTS, 0),
            shootingMakes = p.getInt(KEY_SHOOTING_MAKES, 0),
            shootingSessions = p.getInt(KEY_SHOOTING_SESSIONS, 0),
            shootingXp = p.getInt(KEY_SHOOTING_XP, 0)
        )
    }

    fun updatePlayerName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        prefs?.edit()?.putString(KEY_PLAYER_NAME, trimmed)?.apply()
        _stats.value = _stats.value.copy(playerName = trimmed)
    }

    fun addMinigameScore(gameMode: String, score: Int, hitsOrCrossovers: Int = 0) {
        val p = prefs ?: return
        val current = _stats.value

        val updated = when (gameMode.uppercase()) {
            "REACTION_POINTS" -> {
                val newScore = current.reactionPointsScore + score
                val newBest = maxOf(current.reactionPointsBest, score)
                val newGames = current.reactionPointsGames + 1
                val newHits = current.reactionPointsHits + hitsOrCrossovers
                p.edit()
                    .putInt(KEY_REACTION_SCORE, newScore)
                    .putInt(KEY_REACTION_BEST, newBest)
                    .putInt(KEY_REACTION_GAMES, newGames)
                    .putInt(KEY_REACTION_HITS, newHits)
                    .apply()
                current.copy(
                    reactionPointsScore = newScore,
                    reactionPointsBest = newBest,
                    reactionPointsGames = newGames,
                    reactionPointsHits = newHits
                )
            }
            "DRIBBLE_COMBO" -> {
                val newScore = current.dribbleComboScore + score
                val newBest = maxOf(current.dribbleComboBest, score)
                val newGames = current.dribbleComboGames + 1
                val newCross = current.dribbleCrossovers + hitsOrCrossovers
                p.edit()
                    .putInt(KEY_DRIBBLE_SCORE, newScore)
                    .putInt(KEY_DRIBBLE_BEST, newBest)
                    .putInt(KEY_DRIBBLE_GAMES, newGames)
                    .putInt(KEY_DRIBBLE_CROSSOVERS, newCross)
                    .apply()
                current.copy(
                    dribbleComboScore = newScore,
                    dribbleComboBest = newBest,
                    dribbleComboGames = newGames,
                    dribbleCrossovers = newCross
                )
            }
            "DEFEND_ZONE" -> {
                val newScore = current.defendZoneScore + score
                val newBest = maxOf(current.defendZoneBest, score)
                val newGames = current.defendZoneGames + 1
                val newShields = current.defendShields + hitsOrCrossovers
                p.edit()
                    .putInt(KEY_DEFEND_SCORE, newScore)
                    .putInt(KEY_DEFEND_BEST, newBest)
                    .putInt(KEY_DEFEND_GAMES, newGames)
                    .putInt(KEY_DEFEND_SHIELDS, newShields)
                    .apply()
                current.copy(
                    defendZoneScore = newScore,
                    defendZoneBest = newBest,
                    defendZoneGames = newGames,
                    defendShields = newShields
                )
            }
            else -> current
        }
        _stats.value = updated
    }

    fun addShootingSession(totalShots: Int, makes: Int) {
        val p = prefs ?: return
        val current = _stats.value

        val newShots = current.shootingShots + totalShots
        val newMakes = current.shootingMakes + makes
        val newSessions = current.shootingSessions + 1
        // 10 XP por canasta encestada + 30 XP extra por completar la sesión de tiro
        val earnedXp = (makes * 10) + 30
        val newShootingXp = current.shootingXp + earnedXp

        p.edit()
            .putInt(KEY_SHOOTING_SHOTS, newShots)
            .putInt(KEY_SHOOTING_MAKES, newMakes)
            .putInt(KEY_SHOOTING_SESSIONS, newSessions)
            .putInt(KEY_SHOOTING_XP, newShootingXp)
            .apply()

        _stats.value = current.copy(
            shootingShots = newShots,
            shootingMakes = newMakes,
            shootingSessions = newSessions,
            shootingXp = newShootingXp
        )
    }

    /**
     * Permite al usuario simular o añadir puntos de prueba rápidamente para testear la subida de XP y ranking.
     */
    fun addTestScore(gameMode: String, amount: Int) {
        addMinigameScore(gameMode, amount, hitsOrCrossovers = amount / 5)
    }
}
