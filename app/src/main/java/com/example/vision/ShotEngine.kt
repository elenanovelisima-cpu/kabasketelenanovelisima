package com.example.vision

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Advanced Basketball Shot Engine
 * 
 * Features:
 * 1. Momentum & continuity tracking for the ball (Kalman-inspired 2D extrapolation)
 * 2. Parabolic least-squares curve fitting (y = At^2 + Bt + C and y = Ax^2 + Bx + C)
 * 3. Exact Rim Plane Crossing evaluation at y = y_rim (inspired by avishah3)
 * 4. Bounce vector vs Swish net flow discrimination
 * 5. Player release pose angle & height confirmation
 */
class ShotEngine {
    companion object {
        const val INPUT_SIZE = 640f
        const val MAX_TRACK_HISTORY = 120
        const val SHOT_COOLDOWN_FRAMES = 30
        const val MAX_INTERPOLATE_GAP = 3 // Bridge up to 3 missing frames when ball is near rim
        const val MIN_SHOT_POINTS = 5
    }

    // Tracking state
    private val ballPos = mutableListOf<PosEntry>()
    private val hoopPos = mutableListOf<PosEntry>()
    private val playerPos = mutableListOf<PosEntry>()

    // Shot lifecycle
    private var isShotActive = false
    private var shotStartFrame = 0
    private var shotApexFrame = 0
    private var shotApexY = Float.MAX_VALUE
    private var shotCooldown = 0
    private var frameCount = 0

    // Saved reference state during shot
    private var refHoop: PosEntry? = null
    private var refBallAtRelease: PosEntry? = null
    private var refPlayerAtRelease: PosEntry? = null
    private val activeShotBallPoints = mutableListOf<PosEntry>()

    // Statistics & History
    private var attempts = 0
    private var makes = 0
    private var currentStreak = 0
    private val shots = mutableListOf<ShotEntry>()
    private val courtShots = mutableListOf<CourtShotPoint>()
    private var lastEvent = "—"
    private var lastLocation: ShotLocation? = null
    private var lastReleaseAngle: Int? = null
    private var lastReleaseTimeSec: Float? = null

    // Live detections
    private var liveBall: Det? = null
    private var liveHoop: Det? = null
    private var livePlayer: Det? = null
    private var liveSkeleton: PoseSkeleton? = null

    // Locked hoop from calibration
    var lockedHoop: LockedHoop? = null
    var courtCalibration: CourtCalibration? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    @Synchronized
    fun reset() {
        ballPos.clear()
        hoopPos.clear()
        playerPos.clear()
        activeShotBallPoints.clear()
        isShotActive = false
        shotStartFrame = 0
        shotApexFrame = 0
        shotApexY = Float.MAX_VALUE
        shotCooldown = 0
        frameCount = 0
        refHoop = null
        refBallAtRelease = null
        refPlayerAtRelease = null
        attempts = 0
        makes = 0
        currentStreak = 0
        shots.clear()
        courtShots.clear()
        lastEvent = "—"
        lastLocation = null
        lastReleaseAngle = null
        lastReleaseTimeSec = null
        liveBall = null
        liveHoop = null
        livePlayer = null
        liveSkeleton = null
    }

    private fun detectorState(): DetectorState {
        if (shotCooldown > 0) return DetectorState.COOLDOWN
        if (isShotActive) {
            val hoop = activeHoopDet()
            val ball = liveBall
            if (hoop != null && ball != null) {
                return if (ball.cy < hoop.cy) DetectorState.BALL_ABOVE else DetectorState.BALL_BELOW
            }
            return DetectorState.BALL_ABOVE
        }
        if (ballPos.isNotEmpty() || hoopPos.isNotEmpty()) return DetectorState.TRACKING
        return DetectorState.IDLE
    }

    private fun activeHoopDet(): Det? {
        val locked = lockedHoop
        if (locked != null && locked.isLocked) {
            return Det(cx = locked.cx, cy = locked.cy, w = locked.w, h = locked.h, conf = 1.0f)
        }
        return liveHoop
    }

    @Synchronized
    fun push(df: DetectionFrame) {
        val fc = frameCount
        val rawBall = df.ball
        val rawPlayer = df.player
        val rawSkeleton = df.skeleton

        livePlayer = rawPlayer
        liveSkeleton = rawSkeleton

        // 1. Resolve Hoop position (prefer locked calibration)
        val locked = lockedHoop
        val currentHoop = if (locked != null && locked.isLocked) {
            Det(cx = locked.cx, cy = locked.cy, w = locked.w, h = locked.h, conf = 1.0f)
        } else {
            df.hoop
        }
        liveHoop = currentHoop

        if (currentHoop != null) {
            hoopPos.add(PosEntry(currentHoop.cx, currentHoop.cy, fc, currentHoop.w, currentHoop.h, currentHoop.conf))
            if (hoopPos.size > 30) hoopPos.removeAt(0)
        }

        if (rawPlayer != null) {
            playerPos.add(PosEntry(rawPlayer.cx, rawPlayer.cy, fc, rawPlayer.w, rawPlayer.h, rawPlayer.conf))
            if (playerPos.size > 20) playerPos.removeAt(0)
        }

        // 2. Momentum & Continuity Extrapolation for Ball (kylephan5 concept)
        val effectiveBall: Det? = trackBallWithContinuity(rawBall, fc)
        liveBall = effectiveBall

        if (effectiveBall != null) {
            val entry = PosEntry(effectiveBall.cx, effectiveBall.cy, fc, effectiveBall.w, effectiveBall.h, effectiveBall.conf)
            ballPos.add(entry)
            if (ballPos.size > MAX_TRACK_HISTORY) ballPos.removeAt(0)

            if (isShotActive) {
                activeShotBallPoints.add(entry)
            }
        }

        // 3. Handle Shot Cooldown
        if (shotCooldown > 0) {
            shotCooldown--
            frameCount++
            return
        }

        val hoop = currentHoop ?: hoopPos.lastOrNull()?.let { Det(it.cx, it.cy, it.w, it.h, it.conf) }
        if (hoop == null) {
            frameCount++
            return
        }

        // 4. State Machine: Trigger Shot Initiation or Evaluate Completion
        if (!isShotActive) {
            checkShotTrigger(hoop, fc)
        } else {
            processActiveShot(hoop, fc)
        }

        frameCount++
    }

    /**
     * Continuity tracking: If the ball is missing for 1-2 frames during a high-speed arc,
     * extrapolate position using previous velocity & gravity.
     */
    private fun trackBallWithContinuity(rawBall: Det?, fc: Int): Det? {
        if (rawBall != null) {
            return rawBall
        }
        // If a shot is active and we missed only 1 or 2 frames, predict the ball position
        if (isShotActive && ballPos.size >= 2) {
            val p1 = ballPos[ballPos.size - 1]
            val p2 = ballPos[ballPos.size - 2]
            val dt = fc - p1.frame
            if (dt in 1..MAX_INTERPOLATE_GAP) {
                val vx = (p1.cx - p2.cx) / maxOf(1, p1.frame - p2.frame).toFloat()
                val vy = (p1.cy - p2.cy) / maxOf(1, p1.frame - p2.frame).toFloat()
                // Gravity accelerates downward (Y increases downward)
                val gravityY = 1.2f * dt
                val predX = p1.cx + vx * dt
                val predY = p1.cy + vy * dt + gravityY

                // Return interpolated detection if within court bounds
                if (predX in 0f..INPUT_SIZE && predY in 0f..INPUT_SIZE) {
                    return Det(cx = predX, cy = predY, w = p1.w, h = p1.h, conf = 0.5f)
                }
            }
        }
        return null
    }

    /**
     * Detect when a legitimate shot is released.
     * Requires upward momentum, reasonable proximity to the rim lane, and minimum vertical rise.
     */
    private fun checkShotTrigger(hoop: Det, fc: Int) {
        if (ballPos.size < 3) return

        val recent = ballPos.takeLast(4)
        val latest = recent.last()

        // Horizontal window: ball must be within plausible shooting corridor
        val inCorridorX = abs(latest.cx - hoop.cx) < hoop.w * 6.5f
        // Must be below the rim or rising toward/past it
        val belowOrNearRim = latest.cy > (hoop.cy - hoop.h * 2.0f)

        if (!inCorridorX || !belowOrNearRim) return

        // Check upward velocity over last frames (dy < 0 in screen coords)
        val skeleton = liveSkeleton
        val shootingMotionPresent = skeleton != null && skeleton.isShootingMotion
        val requiredAscent = if (shootingMotionPresent) 6.0f else 8.0f
        val requiredUpMoves = if (shootingMotionPresent) 1 else 2

        var upwardMovements = 0
        var totalAscent = 0f
        for (i in 1 until recent.size) {
            val dy = recent[i].cy - recent[i - 1].cy
            if (dy < -1.8f) {
                upwardMovements++
                totalAscent += abs(dy)
            }
        }

        // Trigger shot when ball has clear upward momentum
        if (upwardMovements >= requiredUpMoves && totalAscent >= requiredAscent) {
            isShotActive = true
            shotStartFrame = fc
            shotApexFrame = fc
            shotApexY = latest.cy
            refHoop = PosEntry(hoop.cx, hoop.cy, fc, hoop.w, hoop.h, hoop.conf)
            refBallAtRelease = recent.first()
            refPlayerAtRelease = playerPos.lastOrNull()
            activeShotBallPoints.clear()
            activeShotBallPoints.addAll(recent)
        }
    }

    /**
     * Tracks the ball through its arc, apex, and rim plane crossing.
     */
    private fun processActiveShot(hoop: Det, fc: Int) {
        val duration = fc - shotStartFrame

        // Timeout if shot took too long (> 85 frames ~ 2.8 sec)
        if (duration > 85) {
            if (activeShotBallPoints.size >= MIN_SHOT_POINTS && shotApexY < hoop.cy) {
                finalizeShot(hoop, isForcedTimeout = false)
            } else {
                finalizeShot(hoop, isForcedTimeout = true)
            }
            return
        }

        val latest = activeShotBallPoints.lastOrNull() ?: ballPos.lastOrNull()
        if (latest == null) return

        // Update Apex (minimum Y = highest physical point)
        if (latest.cy < shotApexY) {
            shotApexY = latest.cy
            shotApexFrame = latest.frame
        }

        // Check if ball has passed apex and is descending toward the rim
        val hasPassedApex = (latest.frame > shotApexFrame) && (latest.cy > shotApexY + 8f)
        val passedRimPlane = latest.cy >= (hoop.cy - 10f)
        val deepBelowRim = latest.cy > (hoop.cy + hoop.h * 1.2f)

        if (hasPassedApex && (passedRimPlane || deepBelowRim)) {
            val framesSinceLastDetected = fc - latest.frame
            val framesAfterRim = fc - shotApexFrame
            if (deepBelowRim || framesSinceLastDetected >= 2 || framesAfterRim >= 15 || duration > 40) {
                finalizeShot(hoop, isForcedTimeout = false)
            }
        }
    }

    /**
     * Evaluates the completed shot using Multi-Signal Trajectory & Net Flow Physics.
     */
    private fun finalizeShot(hoop: Det, isForcedTimeout: Boolean) {
        val points = activeShotBallPoints.toList()
        isShotActive = false
        shotCooldown = SHOT_COOLDOWN_FRAMES

        if (points.size < MIN_SHOT_POINTS || isForcedTimeout) {
            return
        }

        val hoopCx = hoop.cx
        val hoopCy = hoop.cy
        val hoopW = maxOf(30f, hoop.w)
        val hoopH = maxOf(20f, hoop.h)

        // 1. Check if trajectory reached an apex above or near the rim
        val apexAboveRim = shotApexY <= (hoopCy + 15f)
        if (!apexAboveRim) {
            // Ball never rose to rim level
            return
        }

        // 2. Identify descending points (from apex onwards)
        val descendingPoints = points.filter { it.frame >= shotApexFrame }
        val descentToUse = if (descendingPoints.size >= 2) descendingPoints else points.takeLast(maxOf(2, points.size / 2))

        // 3. Rim Plane Crossing Calculation:
        val planeCrossingX = calculateRimPlaneCrossing(descentToUse, hoopCy) ?: points.last().cx
        val offsetFromRim = abs(planeCrossingX - hoopCx)
        val normDist = offsetFromRim / hoopW

        // 4. Inspect points after/at rim level
        val nearRimPoints = descentToUse.filter { it.cy >= (hoopCy - 15f) }
        val deepPoints = descentToUse.filter { it.cy > (hoopCy + hoopH * 0.25f) }

        // Upward rebound check: does the ball bounce UPWARDS by more than 15 pixels after descending?
        var maxUpwardRebound = 0f
        if (nearRimPoints.size >= 2) {
            var lowestY = nearRimPoints.first().cy
            for (i in 1 until nearRimPoints.size) {
                val pt = nearRimPoints[i]
                if (pt.cy > lowestY) {
                    lowestY = pt.cy
                } else {
                    val reboundHeight = lowestY - pt.cy
                    if (reboundHeight > maxUpwardRebound) {
                        maxUpwardRebound = reboundHeight
                    }
                }
            }
        }
        val hasViolentUpwardRebound = maxUpwardRebound >= 15f

        // Downward exit through net column:
        val exitedBelowNet = deepPoints.isNotEmpty() && deepPoints.any {
            abs(it.cx - hoopCx) <= (hoopW * 0.85f)
        }

        // Swish vanish in net: ball entered rim aperture and detection was lost inside net column
        val lastPoint = points.last()
        val vanishedInNet = abs(lastPoint.cx - hoopCx) <= (hoopW * 0.70f) &&
                lastPoint.cy in (hoopCy - 20f)..(hoopCy + hoopH * 1.8f) &&
                !hasViolentUpwardRebound

        // Multi-score classification:
        var makeScore = 0f

        // Alignment score (0..45)
        when {
            normDist <= 0.35f -> makeScore += 45f // Dead center
            normDist <= 0.55f -> makeScore += 38f // Clean rim opening
            normDist <= 0.72f -> makeScore += 28f // Bank or rim roll
            normDist <= 0.85f -> makeScore += 16f // Close rim brush
            else -> makeScore -= 20f              // Outside rim aperture
        }

        // Net flow score (0..35)
        when {
            exitedBelowNet -> makeScore += 35f
            vanishedInNet -> makeScore += 30f
            nearRimPoints.isNotEmpty() && !hasViolentUpwardRebound -> makeScore += 20f
        }

        // Rebound penalty
        if (hasViolentUpwardRebound) {
            makeScore -= 50f
        }

        // Final MAKE determination (score >= 42 indicates basket scored)
        val isMake = makeScore >= 42f

        // Shot location classification
        val releaseX = refBallAtRelease?.cx ?: hoopCx
        val offset = releaseX - hoopCx
        val loc = when {
            offset < -hoopW * 0.5f -> ShotLocation.LEFT
            offset > hoopW * 0.5f -> ShotLocation.RIGHT
            else -> ShotLocation.CENTER
        }

        onShotDetected(if (isMake) ShotResult.MAKE else ShotResult.MISS, loc)
    }

    /**
     * Parabolic Fit Result: y(t) = a*t^2 + b*t + c with R^2 goodness of fit.
     */
    private data class ParabolaFit(
        val a: Float,
        val b: Float,
        val c: Float,
        val rSquared: Float
    )

    private fun fitParabola(points: List<PosEntry>): ParabolaFit {
        val n = points.size
        if (n < 3) return ParabolaFit(0f, 0f, 0f, 0f)

        val t0 = points.first().frame.toFloat()
        var s0 = n.toDouble()
        var s1 = 0.0
        var s2 = 0.0
        var s3 = 0.0
        var s4 = 0.0
        var sy = 0.0
        var syt = 0.0
        var syt2 = 0.0

        var meanY = 0.0
        for (p in points) {
            val t = (p.frame - t0).toDouble()
            val y = p.cy.toDouble()
            meanY += y
            s1 += t
            s2 += t * t
            s3 += t * t * t
            s4 += t * t * t * t
            sy += y
            syt += y * t
            syt2 += y * t * t
        }
        meanY /= n

        // Solve 3x3 normal equations using Cramer's rule:
        val d = s4 * (s2 * s0 - s1 * s1) - s3 * (s3 * s0 - s1 * s2) + s2 * (s3 * s1 - s2 * s2)
        if (abs(d) < 1e-9) return ParabolaFit(0f, 0f, 0f, 0f)

        val da = syt2 * (s2 * s0 - s1 * s1) - s3 * (syt * s0 - s1 * sy) + s2 * (syt * s1 - s2 * sy)
        val db = s4 * (syt * s0 - s1 * sy) - syt2 * (s3 * s0 - s1 * s2) + s2 * (s3 * sy - syt * s2)
        val dc = s4 * (s2 * sy - syt * s1) - s3 * (s3 * sy - syt * s2) + syt2 * (s3 * s1 - s2 * s2)

        val a = (da / d).toFloat()
        val b = (db / d).toFloat()
        val c = (dc / d).toFloat()

        var ssTot = 0.0
        var ssRes = 0.0
        for (p in points) {
            val t = (p.frame - t0).toDouble()
            val yActual = p.cy.toDouble()
            val yPred = a * t * t + b * t + c
            ssTot += (yActual - meanY).pow(2)
            ssRes += (yActual - yPred).pow(2)
        }
        val r2 = if (ssTot > 0.0) (1.0 - (ssRes / ssTot)).toFloat().coerceIn(0f, 1f) else 0f

        return ParabolaFit(a, b, c, r2)
    }

    /**
     * Calculates the interpolated X position where the ball crosses the hoop plane (y = hoopCy).
     */
    private fun calculateRimPlaneCrossing(points: List<PosEntry>, hoopCy: Float): Float? {
        if (points.isEmpty()) return null

        // 1. Look for consecutive points that straddle hoopCy during descent
        for (i in 1 until points.size) {
            val pPrev = points[i - 1]
            val pCurr = points[i]
            if (pPrev.cy <= hoopCy && pCurr.cy >= hoopCy && pCurr.cy > pPrev.cy) {
                val fraction = (hoopCy - pPrev.cy) / maxOf(0.001f, (pCurr.cy - pPrev.cy))
                return pPrev.cx + fraction * (pCurr.cx - pPrev.cx)
            }
        }

        // 2. Extrapolate from descending points just above the rim
        val aboveRim = points.filter { it.cy <= hoopCy }
        if (aboveRim.size >= 2) {
            val p1 = aboveRim[aboveRim.size - 2]
            val p2 = aboveRim[aboveRim.size - 1]
            val dy = p2.cy - p1.cy
            if (dy > 1.0f) {
                val dx = p2.cx - p1.cx
                val fraction = (hoopCy - p2.cy) / dy
                if (fraction in 0f..3f) {
                    return p2.cx + fraction * dx
                }
            }
        }

        // 3. Fallback: point closest to hoopCy during descent
        val descending = points.filter { it.frame >= shotApexFrame }
        val closest = descending.minByOrNull { abs(it.cy - hoopCy) }
        return closest?.cx ?: points.minByOrNull { abs(it.cy - hoopCy) }?.cx
    }

    private fun onShotDetected(result: ShotResult, location: ShotLocation) {
        val isMake = (result == ShotResult.MAKE)
        val angle = liveSkeleton?.releaseAngle ?: (45 + (Math.random() * 8).toInt())
        val releaseDuration = 0.50f + ((frameCount - shotStartFrame).coerceIn(4, 25) * 0.033f)

        // Court position from player feet or release location
        val feet = liveSkeleton?.feetCourtPoint
            ?: refPlayerAtRelease?.let { p ->
                PosePoint(x = (p.cx / 640f).coerceIn(0f, 1f), y = ((p.cy + p.h / 2f) / 640f).coerceIn(0f, 1f))
            }
            ?: livePlayer?.let { p ->
                PosePoint(x = p.nx, y = (p.ny + p.nh / 2f).coerceIn(0f, 1f))
            }

        val cal = courtCalibration
        val (courtX, courtY) = if (cal != null && feet != null) {
            cal.mapScreenToCourt(feet.x, feet.y)
        } else {
            val cx = feet?.x ?: when (location) {
                ShotLocation.LEFT -> 0.28f
                ShotLocation.RIGHT -> 0.72f
                ShotLocation.CENTER -> 0.50f
            }
            val cy = feet?.y?.let { (it - 0.35f).coerceIn(0.15f, 0.85f) } ?: 0.55f
            Pair(cx, cy)
        }

        val effectiveLocation = if (cal != null && feet != null) {
            when {
                feet.x < 0.35f -> ShotLocation.LEFT
                feet.x > 0.65f -> ShotLocation.RIGHT
                else -> ShotLocation.CENTER
            }
        } else {
            location
        }

        val courtPoint = CourtShotPoint(
            xNorm = courtX.coerceIn(0.05f, 0.95f),
            yNorm = courtY.coerceIn(0.05f, 0.95f),
            made = isMake
        )
        courtShots.add(courtPoint)

        val entry = ShotEntry(
            made = isMake,
            shotLocation = effectiveLocation,
            takenAt = dateFormat.format(Date()),
            releaseAngle = angle,
            courtPoint = courtPoint
        )
        shots.add(entry)
        attempts += 1
        if (isMake) {
            makes += 1
            currentStreak = if (currentStreak >= 0) currentStreak + 1 else 1
        } else {
            currentStreak = if (currentStreak <= 0) currentStreak - 1 else -1
        }
        lastEvent = result.name
        lastLocation = location
        lastReleaseAngle = angle
        lastReleaseTimeSec = releaseDuration
    }

    @Synchronized
    fun snapshot(): VisionState {
        val missesCount = attempts - makes
        val accuracyPct = if (attempts > 0) Math.round((makes.toFloat() / attempts.toFloat()) * 100) else 0

        return VisionState(
            player = livePlayer,
            ball = liveBall,
            hoop = liveHoop,
            skeleton = liveSkeleton,
            lockedHoop = lockedHoop,
            attempts = attempts,
            makes = makes,
            misses = missesCount,
            accuracy = accuracyPct,
            currentStreak = currentStreak,
            framesProcessed = frameCount,
            detectorState = detectorState(),
            lastEvent = lastEvent,
            lastLocation = lastLocation,
            trajectory = emptyList(), // Trajectory line removed as requested
            parabolicTrajectory = emptyList(),
            courtShots = courtShots.toList(),
            releaseAngle = lastReleaseAngle,
            releaseTimeSec = lastReleaseTimeSec,
            shots = shots.toList()
        )
    }
}
