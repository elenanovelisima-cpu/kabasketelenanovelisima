package com.example.vision

data class TeamColorInfo(
    val name: String,
    val hexColor: Long,
    val isLight: Boolean
)

enum class TacticalPlayType(val title: String, val badgeColor: Long, val iconEmoji: String) {
    TRANSITION_FASTBREAK("Contraataque / Transición Rápida", 0xFFFF9800, "⚡"),
    FULL_COURT_PRESS("Presión a Toda Pista", 0xFFE91E63, "🛡️"),
    PICK_AND_ROLL("Bloqueo Directo (Pick & Roll)", 0xFF00E5FF, "🔄"),
    PICK_AND_POP("Bloqueo y Apertura (Pick & Pop)", 0xFF9C27B0, "🎯"),
    ISOLATION("Aclarado (1 vs 1)", 0xFFFF5722, "💥"),
    ZONE_DEFENSE("Defensa en Zona (2-3)", 0xFF4CAF50, "🧱"),
    MAN_TO_MAN("Defensa Individual (Hombre a Hombre)", 0xFF3F51B5, "👥"),
    GOOD_SPACING("Buen Espaciado Ofensivo (Spacing)", 0xFF2196F3, "📐")
}

data class TacticalPlayEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestampMs: Long,
    val playType: TacticalPlayType,
    val team: TacticalTeam,
    val description: String,
    val outcome: String = "Ventaja generada",
    val pointsScored: Int = 0
) {
    val formattedTime: String
        get() {
            val totalSec = timestampMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return String.format("%02d:%02d", min, sec)
        }
}

enum class TacticalTeam(val displayName: String, val shortName: String, val colorCode: Long) {
    HOME("Equipo Local (Blanco / Claro)", "LOCAL", 0xFF00E5FF),
    AWAY("Equipo Visitante (Oscuro / Color)", "VISITANTE", 0xFFFF5722),
    REFEREE("Árbitro / Oficial", "ÁRB.", 0xFF9E9E9E),
    UNKNOWN("Indeterminado", "INDET.", 0xFF607D8B)
}

data class TacticalPlayerTrack(
    val id: Int,
    val xNorm: Float, // 0..1 on court width (0 = left sideline, 1 = right sideline)
    val yNorm: Float, // 0..1 on court length (0 = defense hoop, 0.5 = mid court, 1 = offense hoop)
    val team: TacticalTeam,
    val isWithBall: Boolean = false,
    val speedNorm: Float = 0f,
    val role: String = "Jugador"
)

data class TacticalFrameAnalysis(
    val timestampMs: Long,
    val players: List<TacticalPlayerTrack>,
    val ballPosition: Pair<Float, Float>?, // xNorm, yNorm on court
    val offensiveSpacingArea: Float, // 0..1 court percentage
    val isPressingFullCourt: Boolean,
    val isFastBreak: Boolean,
    val isPickAndRollOccurring: Boolean,
    val dominantTeamWithBall: TacticalTeam,
    val activePlayBadge: TacticalPlayType? = null,
    val activePlayDescription: String? = null
)

data class TacticalMatchReport(
    val matchTitle: String = "Informe Táctico de Baloncesto",
    val generatedAt: String = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
    val durationFormatted: String = "10:00",
    val totalPossessions: Int = 38,
    val homePossessionPct: Int = 53,
    val awayPossessionPct: Int = 47,
    // Fastbreak metrics
    val fastBreakCount: Int = 7,
    val fastBreakSuccessRate: Int = 71,
    val fastBreakPoints: Int = 10,
    val avgTransitionTimeSec: Float = 3.2f,
    // Full court press metrics
    val fullCourtPressCount: Int = 5,
    val fullCourtPressBreakRate: Int = 60,
    val forcedTurnoversFromPress: Int = 2,
    // Pick and Roll metrics
    val pickAndRollCount: Int = 14,
    val pickAndRollRollCount: Int = 9,
    val pickAndRollPopCount: Int = 5,
    val pickAndRollPointsPerPlay: Float = 1.14f,
    // Spacing
    val homeAverageSpacing: Int = 76,
    val awayAverageSpacing: Int = 68,
    // Defense
    val manToManPossessions: Int = 28,
    val zoneDefensePossessions: Int = 10,
    val dominantOffensiveStyle: String = "Transición rápida y Bloqueo Directo (Pick & Roll)",
    val dominantDefensiveStyle: String = "Defensa individual agresiva con trampas de presión",
    val keyPlays: List<TacticalPlayEvent> = emptyList(),
    val tacticalTakeaways: List<String> = emptyList()
) {
    fun toShareableText(): String {
        val sb = StringBuilder()
        sb.append("🏀 INFORME TÁCTICO DE PARTIDO - KaBasket AI\n")
        sb.append("📅 Fecha: $generatedAt | Duración: $durationFormatted\n")
        sb.append("📊 Posesiones analizadas: $totalPossessions (Local: $homePossessionPct% | Visitante: $awayPossessionPct%)\n\n")

        sb.append("⚡ CONTRAATAQUES Y TRANSICIÓN RÁPIDA:\n")
        sb.append("• Jugadas de contraataque: $fastBreakCount\n")
        sb.append("• Efectividad: $fastBreakSuccessRate% ($fastBreakPoints pts anotados)\n")
        sb.append("• Tiempo medio de llegada: ${String.format(java.util.Locale.US, "%.1f", avgTransitionTimeSec)}s\n\n")

        sb.append("🛡️ PRESIÓN A TODA PISTA (Full-Court Press):\n")
        sb.append("• Posesiones con presión: $fullCourtPressCount\n")
        sb.append("• Éxito rompiendo la presión rival: $fullCourtPressBreakRate%\n")
        sb.append("• Pérdidas forzadas provocadas: $forcedTurnoversFromPress\n\n")

        sb.append("🔄 BLOQUEO DIRECTO (Pick & Roll):\n")
        sb.append("• Bloqueos jugados: $pickAndRollCount ($pickAndRollRollCount caídas al aro, $pickAndRollPopCount aperturas al triple)\n")
        sb.append("• Eficiencia: ${String.format(java.util.Locale.US, "%.2f", pickAndRollPointsPerPlay)} puntos por posesión\n\n")

        sb.append("📐 ESPACIADO OFENSIVO (Spacing):\n")
        sb.append("• Spacing Local: $homeAverageSpacing% (Excelente)\n")
        sb.append("• Spacing Visitante: $awayAverageSpacing% (Aceptable)\n\n")

        sb.append("🧱 ESTRUCTURA DEFENSIVA:\n")
        sb.append("• Hombre a Hombre: $manToManPossessions posesiones | Zona 2-3: $zoneDefensePossessions posesiones\n\n")

        if (tacticalTakeaways.isNotEmpty()) {
            sb.append("📋 CONCLUSIONES TÁCTICAS PARA EL ENTRENADOR:\n")
            tacticalTakeaways.forEachIndexed { idx, takeaway ->
                sb.append("${idx + 1}. $takeaway\n")
            }
            sb.append("\n")
        }

        if (keyPlays.isNotEmpty()) {
            sb.append("⭐ JUGADAS CLAVE DESTACADAS:\n")
            keyPlays.take(6).forEach { play ->
                sb.append("[${play.formattedTime}] ${play.team.shortName} - ${play.playType.title}: ${play.description} (${play.outcome})\n")
            }
        }

        sb.append("\nGenerado automáticamente por KaBasket Computer Vision & Tactical Engine.")
        return sb.toString()
    }
}
