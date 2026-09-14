package com.example.vision

import java.util.UUID

enum class ShotLocation {
    LEFT,
    CENTER,
    RIGHT;

    fun displayName(): String = name.lowercase()
}

enum class ShotResult {
    MAKE,
    MISS
}

enum class DetectorState(val label: String) {
    IDLE("idle"),
    TRACKING("tracking"),
    BALL_ABOVE("ball-above"),
    BALL_BELOW("ball-below"),
    COOLDOWN("cooldown")
}

enum class ModelState {
    LOADING,
    LOADED,
    ERROR
}

enum class CalibrationStep {
    POSITION_PHONE,
    LOCK_HOOP,
    CHECK_BALL,
    COURT_ALIGNMENT,
    CALIBRATE_SKELETON,
    COMPLETED
}

data class BallColorProfile(
    val meanR: Int = 180,
    val meanG: Int = 95,
    val meanB: Int = 50,
    val minHue: Float = 8f,
    val maxHue: Float = 40f,
    val minSat: Float = 0.28f,
    val minVal: Float = 0.22f
)

enum class InputMode {
    LIVE_CAMERA,
    VIDEO_FILE,
    TACTICAL_MATCH
}

enum class AnalysisType(val title: String, val subtitle: String, val badge: String) {
    SHOOTING("Lanzamientos y Mecánica", "Aro, trayectoria de balón, ángulo, parábola, canastas/fallos y pose del tirador", "🏀 TIROS"),
    TACTICAL_MATCH("Partido y Patrones Tácticos", "10 jugadores, equipaciones, contraataques, pick & roll, espaciado e informe", "📋 TÁCTICA")
}

enum class AnalysisSpeed(val label: String, val subtitle: String, val stepMs: Long) {
    TURBO("Turbo (10 FPS)", "3x-4x más rápido • Recomendado", 100L),
    ULTRA("Ultra Rápido (5 FPS)", "6x más rápido • Para partidos largos", 180L),
    ACCURATE("Preciso (30 FPS)", "Cuadro a cuadro • Para cámara lenta", 33L)
}

data class SavedVideoAnalysis(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val type: AnalysisType,
    val videoFilePath: String,
    val thumbnailPath: String? = null,
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val totalFrames: Int = 0,
    val attempts: Int = 0,
    val makes: Int = 0,
    val accuracy: Int = 0,
    val avgReleaseAngle: Int? = null,
    val possessions: Int = 0,
    val fastBreaks: Int = 0,
    val pickAndRolls: Int = 0,
    val summaryText: String = "",
    val hasTacticalReport: Boolean = false
)

enum class HoopVisual {
    REALISTIC_FRONT,
    BOARD_ONLY
}

enum class HoopPerspective(val label: String, val shortLabel: String) {
    AUTO("Auto (Adaptativo)", "AUTO 🔄"),
    FRONTAL("Frontal (0°)", "FRONTAL 🎯"),
    SIDE_LEFT("Lateral Izquierdo", "LAT. IZQ ◀"),
    SIDE_RIGHT("Lateral Derecho", "LAT. DER ▶")
}

data class Det(
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val conf: Float
) {
    val nx: Float get() = cx / 640f
    val ny: Float get() = cy / 640f
    val nw: Float get() = w / 640f
    val nh: Float get() = h / 640f
}

data class PosePoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val visibility: Float = 1f
)

data class PoseSkeleton(
    val landmarks: List<PosePoint>,
    val wristReleasePoint: PosePoint? = null,
    val feetCourtPoint: PosePoint? = null,
    val releaseAngle: Int? = null,
    val isShootingMotion: Boolean = false
)

data class LockedHoop(
    val nx: Float = 0.5f,
    val ny: Float = 0.28f,
    val nw: Float = 0.16f,
    val nh: Float = 0.08f,
    val isLocked: Boolean = false,
    val perspective: HoopPerspective = HoopPerspective.AUTO
) {
    val cx: Float get() = nx * 640f
    val cy: Float get() = ny * 640f
    val w: Float get() = nw * 640f
    val h: Float get() = nh * 640f

    /**
     * Calcula el ángulo de inclinación 3D (yaw) efectivo entre -0.85f y +0.85f (0f = frontal plano).
     * En modo AUTO, infiere automáticamente la perspectiva según la posición horizontal (nx) en el encuadre:
     * - Si la canasta está situada a la derecha (nx > 0.54), la cámara la está grabando desde un ángulo lateral izquierdo -> yaw positivo.
     * - Si la canasta está situada a la izquierda (nx < 0.46), la cámara la está grabando desde un ángulo lateral derecho -> yaw negativo.
     * - Si está centrada (nx aprox 0.50), la perspectiva es frontal directa.
     */
    fun getEffectiveYaw(): Float {
        return when (perspective) {
            HoopPerspective.FRONTAL -> 0f
            HoopPerspective.SIDE_LEFT -> -0.65f
            HoopPerspective.SIDE_RIGHT -> 0.65f
            HoopPerspective.AUTO -> {
                val delta = (nx - 0.5f)
                if (kotlin.math.abs(delta) < 0.06f) {
                    0f
                } else {
                    (delta * 2.2f).coerceIn(-0.85f, 0.85f)
                }
            }
        }
    }
}

data class CourtShotPoint(
    val xNorm: Float, // 0..1 on court width
    val yNorm: Float, // 0..1 on court depth
    val made: Boolean
)

data class DetectionFrame(
    val ball: Det?,
    val hoop: Det?,
    val player: Det?,
    val skeleton: PoseSkeleton? = null
)

data class TacticalDetectionFrame(
    val ball: Det?,
    val hoop: Det?,
    val player: Det?,
    val allPlayers: List<Det> = emptyList()
)

data class PosEntry(
    val cx: Float,
    val cy: Float,
    val frame: Int,
    val w: Float,
    val h: Float,
    val conf: Float
)

data class ShotEntry(
    val id: String = UUID.randomUUID().toString(),
    val made: Boolean,
    val shotLocation: ShotLocation,
    val takenAt: String,
    val releaseAngle: Int? = null,
    val courtPoint: CourtShotPoint? = null
)

data class VideoFrameAnalysis(
    val timestampMs: Long,
    val ball: Det?,
    val hoop: Det?,
    val player: Det?,
    val skeleton: PoseSkeleton?,
    val attempts: Int = 0,
    val makes: Int = 0,
    val misses: Int = 0,
    val accuracy: Int = 0,
    val currentStreak: Int = 0,
    val lastEvent: String = "—",
    val lastLocation: ShotLocation? = null,
    val releaseAngle: Int? = null,
    val releaseTimeSec: Float? = null,
    val shots: List<ShotEntry> = emptyList(),
    val courtShots: List<CourtShotPoint> = emptyList(),
    val tacticalAnalysis: TacticalFrameAnalysis? = null
)

data class VideoAnalysisCompletedNotice(
    val videoUri: android.net.Uri,
    val totalFrames: Int,
    val makes: Int,
    val attempts: Int,
    val tacticalPlayCount: Int = 0,
    val summaryText: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DribbleSmokePuff(
    val id: Long = System.currentTimeMillis() + (0..9999).random(),
    val nx: Float,
    val ny: Float,
    val isLeftFoot: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class DribblePopup(
    val id: Long = System.currentTimeMillis(),
    val nx: Float,
    val ny: Float,
    val points: Int = 5,
    val text: String = "+5",
    val isStreakBonus: Boolean = false,
    val comboName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ReactionTargetSide {
    LEFT, RIGHT
}

data class ReactionPoint(
    val id: Long = System.currentTimeMillis(),
    val number: Int = 1,
    val side: ReactionTargetSide = ReactionTargetSide.LEFT,
    val xNorm: Float = 0.20f,
    val yNorm: Float = 0.40f,
    val isHit: Boolean = false,
    val isHandOnlyBlocked: Boolean = false,
    val spawnTimeMs: Long = System.currentTimeMillis(),
    val durationMs: Long = 5000L
)

data class ReactionPopup(
    val id: Long = System.currentTimeMillis(),
    val text: String = "+1",
    val xNorm: Float,
    val yNorm: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Modo Defend the Zone: Esquiva a los Defensores Fantasma
 * Permite seleccionar entre "Manos de defensores virtuales" o "Lásers de intercepción".
 */
enum class DefendThreatType(val label: String, val icon: String) {
    HANDS("Manos Fantasma", "🖐️"),
    LASERS("Lásers de Corte", "⚡")
}

enum class DefendSide {
    LEFT, RIGHT
}

data class DefendThreat(
    val id: Long = System.currentTimeMillis(),
    val side: DefendSide,
    val targetY: Float = 0.65f,         // Altura a la que ataca el corte (zona media/baja de bote)
    val progress: Float = 0f,           // 0.0f (borde lateral) -> 1.0f (alcance máximo)
    val isTelegraph: Boolean = true,    // Fase de aviso previo (aviso láser o sombra de fantasma)
    val isBlockedByBody: Boolean = false, // Nivel Pro: El cuerpo del jugador hace pantalla/escudo protector
    val isEvaded: Boolean = false,      // Esquivado con crossover / cambio de mano
    val isStolen: Boolean = false,      // Robó el balón (pérdida de vida)
    val threatType: DefendThreatType = DefendThreatType.HANDS,
    val spawnTimeMs: Long = System.currentTimeMillis(),
    val targetX: Float = 0.5f,          // Coordenada X del balón / mano objetivo hacia donde sube la mano
    val currentX: Float = 0.5f,         // Posición X actual interpolada
    val currentY: Float = 1.15f,        // Posición Y actual interpolada (comienza fuera de pantalla abajo: 1.15f)
    val isFadingMiss: Boolean = false   // Cuando el balón cambia de mano/posición inalcanzable, se desvanece con efecto
)

data class DefendPopup(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val xNorm: Float,
    val yNorm: Float,
    val isBonus: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class VisionState(
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val player: Det? = null,
    val ball: Det? = null,
    val hoop: Det? = null,
    val skeleton: PoseSkeleton? = null,
    val lockedHoop: LockedHoop? = null,
    val calibrationStep: CalibrationStep = CalibrationStep.POSITION_PHONE,
    val skeletonCalibrationProgress: Float = 0f,
    val skeletonCalibrated: Boolean = false,
    val skeletonCalibrationFeedback: String = "Colócate frente a la cámara",
    val ballCalibrated: Boolean = false,
    val ballCalibrationProgress: Float = 0f,
    val ballCalibrationFeedback: String = "Coloca el balón dentro del círculo guía",
    val calibratedBallColor: BallColorProfile? = null,
    val attempts: Int = 0,
    val makes: Int = 0,
    val misses: Int = 0,
    val accuracy: Int = 0,
    val currentStreak: Int = 0,
    val framesProcessed: Int = 0,
    val detectorState: DetectorState = DetectorState.IDLE,
    val lastEvent: String = "—",
    val lastLocation: ShotLocation? = null,
    val trajectory: List<Pair<Float, Float>> = emptyList(),
    val parabolicTrajectory: List<Pair<Float, Float>> = emptyList(),
    val courtShots: List<CourtShotPoint> = emptyList(),
    val releaseAngle: Int? = null,
    val releaseTimeSec: Float? = null,
    val sessionDurationSec: Long = 0L,
    val shots: List<ShotEntry> = emptyList(),
    val fps: Int = 0,
    val modelState: ModelState = ModelState.LOADING,
    val cameraActive: Boolean = false,
    val isSimulating: Boolean = false,
    val inputMode: InputMode = InputMode.LIVE_CAMERA,
    val selectedVideoUri: android.net.Uri? = null,
    val isRecordingLive: Boolean = false,
    val recordingDurationSec: Int = 0,
    val lastRecordedVideoUri: android.net.Uri? = null,
    val showRecordedVideoBanner: Boolean = false,
    val isVideoLoading: Boolean = false,
    val isVideoAnalyzing: Boolean = false,
    val isVideoAnalysisInBackground: Boolean = false,
    val videoAnalysisProgress: Float = 0f,
    val videoAnalyzedFrames: Int = 0,
    val videoTotalFrames: Int = 0,
    val videoAnalysisStatus: String = "",
    val videoTimeline: List<VideoFrameAnalysis> = emptyList(),
    val videoAnalysisCompletedNotice: VideoAnalysisCompletedNotice? = null,
    val hoopVisual: HoopVisual = HoopVisual.REALISTIC_FRONT,
    val tacticalReport: TacticalMatchReport? = null,
    val tacticalTimeline: List<TacticalFrameAnalysis> = emptyList(),
    val showTacticalReportDialog: Boolean = false,
    val currentTacticalAnalysis: TacticalFrameAnalysis? = null,
    val isTacticalMode: Boolean = false,
    val isSimulatingTactical: Boolean = false,
    val isDribbleMode: Boolean = false,
    val useFrontCamera: Boolean = false,
    val dribbleScore: Int = 0,
    val dribbleCrossovers: Int = 0,
    val dribbleStreak: Int = 0,
    val dribbleLevel: Int = 1,
    val dribbleGaugeProgress: Float = 0.25f,
    val dribbleComboBanner: String? = null,
    val dribbleTimerRemainingSec: Int = 45,
    val isDribbleTimerRunning: Boolean = false,
    val isDribbleSessionFinished: Boolean = false,
    val dribbleSmokePuffs: List<DribbleSmokePuff> = emptyList(),
    val dribblePopups: List<DribblePopup> = emptyList(),
    val activeAnalysisType: AnalysisType = AnalysisType.SHOOTING,
    val activeAnalysisSpeed: AnalysisSpeed = AnalysisSpeed.TURBO,
    val savedVideos: List<SavedVideoAnalysis> = emptyList(),
    val showSavedVideosDialog: Boolean = false,
    val showSkeleton: Boolean = true,
    val showHoop: Boolean = true,
    val showBall: Boolean = true,
    val showFps: Boolean = false,
    val courtCalibration: CourtCalibration = CourtCalibration(),
    val showCourtPointSelector: Boolean = false,
    val isReactionPointsMode: Boolean = false,
    val reactionScore: Int = 0,
    val reactionTimerRemainingSec: Int = 60,
    val isReactionTimerRunning: Boolean = false,
    val isReactionSessionFinished: Boolean = false,
    val activeReactionPoint: ReactionPoint? = null,
    val reactionPopups: List<ReactionPopup> = emptyList(),
    val reactionWarningMessage: String? = null,
    val isDefendZoneMode: Boolean = false,
    val defendThreatType: DefendThreatType = DefendThreatType.HANDS,
    val defendLives: Int = 3,
    val defendScore: Int = 0,
    val defendStreak: Int = 0,
    val defendShieldCount: Int = 0,
    val defendThreats: List<DefendThreat> = emptyList(),
    val defendPopups: List<DefendPopup> = emptyList(),
    val defendTimerRemainingSec: Int = 60,
    val isDefendTimerRunning: Boolean = false,
    val isDefendSessionFinished: Boolean = false,
    val isDefendGameOver: Boolean = false,
    val defendWarningMessage: String? = null,
    val defendScreenFlashRed: Boolean = false,
    val isAwaitingPlayStart: Boolean = false,
    val playStartCountdownSec: Int? = null,
    // --- MODO KIDS MINI BASKET (TIRO INFANTIL EN CASA) ---
    val isKidsMiniBasketMode: Boolean = false,
    val kidsBasketScore: Int = 0,
    val kidsBasketMakes: Int = 0,
    val kidsBasketAttempts: Int = 0,
    val kidsBasketStreak: Int = 0,
    val kidsBasketTimerRemainingSec: Int = 60,
    val isKidsTimerRunning: Boolean = false,
    val isKidsSessionFinished: Boolean = false,
    val kidsCalibrationStep: KidsCalibrationStep = KidsCalibrationStep.NOT_STARTED,
    val kidsHoopX: Float = 0.50f,
    val kidsHoopY: Float = 0.25f,
    val kidsHoopRadius: Float = 0.085f,
    val kidsBallX: Float = 0.50f,
    val kidsBallY: Float = 0.60f,
    val kidsBallRadius: Float = 0.045f,
    val kidsBallColorR: Int = 230,
    val kidsBallColorG: Int = 100,
    val kidsBallColorB: Int = 40,
    val kidsBallHue: Float = 22f,
    val kidsBallSat: Float = 0.82f,
    val kidsBallVal: Float = 0.85f,
    val isKidsBallCalibrated: Boolean = false,
    val kidsBallScanProgress: Float = 0f,
    val kidsBallPaletteColors: List<Int> = emptyList(),
    val kidsBasketPopups: List<KidsBasketPopup> = emptyList(),
    val kidsSwishCelebration: Boolean = false
)

enum class KidsCalibrationStep {
    NOT_STARTED,
    SCAN_BALL_HAND,
    PLACE_PHONE_STATIC,
    ADJUST_HOOP_VIEW,
    COMPLETED
}

data class KidsBasketPopup(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isMake: Boolean = true,
    val xNorm: Float = 0.5f,
    val yNorm: Float = 0.25f,
    val timestamp: Long = System.currentTimeMillis()
)
