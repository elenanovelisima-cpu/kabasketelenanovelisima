package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlin.math.atan2

class PoseEstimator(private val context: Context) {

    companion object {
        private const val TAG = "PoseEstimator"
        private const val MODEL_NAME = "models/pose_landmarker_lite.task"
    }

    private var landmarker: PoseLandmarker? = null
    var isReady: Boolean = false
        private set

    private val temporalFilter = LandmarkTemporalFilter()

    init {
        setupPoseLandmarker()
    }

    private fun setupPoseLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_NAME)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinPoseDetectionConfidence(0.50f)
                .setMinPosePresenceConfidence(0.50f)
                .setMinTrackingConfidence(0.50f)
                .setRunningMode(RunningMode.IMAGE)
                .setNumPoses(1)
                .build()

            landmarker = PoseLandmarker.createFromOptions(context, options)
            isReady = true
            Log.i(TAG, "MediaPipe PoseLandmarker initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PoseLandmarker: ${e.message}", e)
            isReady = false
        }
    }

    @Synchronized
    fun estimate(bitmap: Bitmap): PoseSkeleton? {
        val currentLandmarker = landmarker ?: return null
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = currentLandmarker.detect(mpImage)
            val poses = result.landmarks()

            if (poses.isEmpty() || poses[0].isEmpty()) {
                return null
            }

            val rawLandmarks = poses[0]
            val rawPoints = rawLandmarks.map { lm ->
                val vis = lm.visibility().orElse(0.0f)
                val pres = lm.presence().orElse(0.0f)
                PosePoint(
                    x = lm.x(),
                    y = lm.y(),
                    z = lm.z(),
                    visibility = maxOf(vis, pres)
                )
            }

            // Apply adaptive temporal filtering: stabilizes jitter and eliminates dispersal/ghosting
            val points = temporalFilter.filter(rawPoints)

            // Extract shooting arm landmarks: 12 (right shoulder), 14 (right elbow), 16 (right wrist)
            // or 11 (left shoulder), 13 (left elbow), 15 (left wrist)
            val rWrist = if (points.size > 16) points[16] else null
            val lWrist = if (points.size > 15) points[15] else null
            val rElbow = if (points.size > 14) points[14] else null
            val rShoulder = if (points.size > 12) points[12] else null

            // Primary shooting wrist (usually higher in the air during release)
            val wristRelease = when {
                rWrist != null && lWrist != null -> if (rWrist.y < lWrist.y) rWrist else lWrist
                rWrist != null -> rWrist
                else -> lWrist
            }

            // Feet court ground point (ankles 27 and 28)
            val lAnkle = if (points.size > 27) points[27] else null
            val rAnkle = if (points.size > 28) points[28] else null
            val feetCourtPoint = if (lAnkle != null && rAnkle != null) {
                PosePoint(
                    x = (lAnkle.x + rAnkle.x) / 2f,
                    y = maxOf(lAnkle.y, rAnkle.y)
                )
            } else lAnkle ?: rAnkle

            // Calculate arm elevation release angle in degrees
            var calculatedAngle: Int? = null
            if (rElbow != null && rWrist != null) {
                val dy = -(rWrist.y - rElbow.y) // upward in screen space
                val dx = kotlin.math.abs(rWrist.x - rElbow.x)
                val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toInt()
                calculatedAngle = angle.coerceIn(35, 78)
            }

            val lShoulder = if (points.size > 11) points[11] else null
            val minShoulderY = when {
                rShoulder != null && lShoulder != null -> minOf(rShoulder.y, lShoulder.y)
                rShoulder != null -> rShoulder.y
                lShoulder != null -> lShoulder.y
                else -> 0.45f
            }
            val isShootingMotion = wristRelease != null && (wristRelease.y < minShoulderY + 0.05f)

            PoseSkeleton(
                landmarks = points,
                wristReleasePoint = wristRelease,
                feetCourtPoint = feetCourtPoint,
                releaseAngle = calculatedAngle,
                isShootingMotion = isShootingMotion
            )
        } catch (e: Exception) {
            Log.e(TAG, "Pose detection error: ${e.message}")
            null
        }
    }

    fun close() {
        try {
            temporalFilter.reset()
            landmarker?.close()
            landmarker = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing PoseLandmarker: ${e.message}")
        }
    }
}

/**
 * Filtro adaptativo temporal con velocidad dinámica (One-Euro / EMA híbrido).
 * - Cuando el cuerpo se mueve lento: aplica un suavizado alto para fijar el esqueleto y eliminar cualquier temblor.
 * - Cuando el movimiento es rápido (p. ej. cambio de mano o bote explosivo): reduce la latencia a prácticamente 0.
 * - Suprime saltos atípicos (outliers) para evitar que los puntos se dispersen o se separen del cuerpo.
 */
private class LandmarkTemporalFilter(
    private val smoothingFactorSlow: Float = 0.40f,
    private val smoothingFactorFast: Float = 0.88f
) {
    private var prevPoints: List<PosePoint>? = null
    private var lastUpdateTime: Long = 0L

    fun filter(rawPoints: List<PosePoint>): List<PosePoint> {
        val now = System.currentTimeMillis()
        val prev = prevPoints

        if (prev == null || prev.size != rawPoints.size || (now - lastUpdateTime) > 600L) {
            prevPoints = rawPoints
            lastUpdateTime = now
            return rawPoints
        }

        val smoothed = rawPoints.indices.map { i ->
            val curr = rawPoints[i]
            val past = prev[i]

            if (curr.visibility < 0.20f && past.visibility >= 0.20f) {
                past.copy(visibility = past.visibility * 0.90f)
            } else {
                val dist = kotlin.math.hypot(curr.x - past.x, curr.y - past.y)
                val alpha = when {
                    dist < 0.012f -> smoothingFactorSlow
                    dist > 0.075f -> smoothingFactorFast
                    else -> smoothingFactorSlow + (dist - 0.012f) / (0.075f - 0.012f) * (smoothingFactorFast - smoothingFactorSlow)
                }

                // Amortiguar saltos de dispersión superiores a 0.32 en coordenadas normalizadas
                val clampedX = if (dist > 0.32f) past.x + (curr.x - past.x) * 0.25f else curr.x
                val clampedY = if (dist > 0.32f) past.y + (curr.y - past.y) * 0.25f else curr.y

                val smoothX = past.x + alpha * (clampedX - past.x)
                val smoothY = past.y + alpha * (clampedY - past.y)
                val smoothZ = past.z + alpha * (curr.z - past.z)
                val smoothVis = past.visibility + alpha * (curr.visibility - past.visibility)

                PosePoint(x = smoothX, y = smoothY, z = smoothZ, visibility = smoothVis)
            }
        }

        prevPoints = smoothed
        lastUpdateTime = now
        return smoothed
    }

    fun reset() {
        prevPoints = null
        lastUpdateTime = 0L
    }
}
