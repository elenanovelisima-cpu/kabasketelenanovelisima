package com.example.vision

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.theme.SportOrange

// Skeleton bone connections (pairs of MediaPipe landmark indices)
private val POSE_CONNECTIONS = listOf(
    Pair(11, 12), // Shoulders
    Pair(11, 23), // Left shoulder to left hip
    Pair(12, 24), // Right shoulder to right hip
    Pair(23, 24), // Hips
    Pair(11, 13), // Left arm: shoulder to elbow
    Pair(13, 15), // Left arm: elbow to wrist
    Pair(12, 14), // Right arm: shoulder to elbow
    Pair(14, 16), // Right arm: elbow to wrist
    Pair(23, 25), // Left leg: hip to knee
    Pair(25, 27), // Left leg: knee to ankle
    Pair(24, 26), // Right leg: hip to knee
    Pair(26, 28), // Right leg: knee to ankle
    Pair(27, 29), // Left ankle to heel
    Pair(28, 30), // Right ankle to heel
    Pair(29, 31), // Left heel to toe
    Pair(30, 32)  // Right heel to toe
)

@Composable
fun DetectionOverlay(
    state: VisionState,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("detection_overlay")
    ) {
        val w = size.width
        val h = size.height

        val hasFrameDim = state.frameWidth > 0 && state.frameHeight > 0
        val imgW = if (hasFrameDim) state.frameWidth.toFloat() else w
        val imgH = if (hasFrameDim) state.frameHeight.toFloat() else h
        val scale = if (hasFrameDim) maxOf(w / imgW, h / imgH) else 1f
        val scaledW = imgW * scale
        val scaledH = imgH * scale
        val offsetX = (w - scaledW) / 2f
        val offsetY = (h - scaledH) / 2f

        val mapX = { nx: Float -> offsetX + (nx * scaledW) }
        val mapY = { ny: Float -> offsetY + (ny * scaledH) }
        val scaleW = { nw: Float -> nw * scaledW }
        val scaleH = { nh: Float -> nh * scaledH }

        // 1. Draw Player Skeleton (Purple bones + Yellow joints) - enabled in Dribble mode, skeleton calibration, or if toggled
        if (state.showSkeleton || state.isDribbleMode || state.calibrationStep == CalibrationStep.CALIBRATE_SKELETON) {
            val skeleton = state.skeleton
            if (skeleton != null && skeleton.landmarks.isNotEmpty()) {
                val lm = skeleton.landmarks
                val boneColor = Color(0xFF9C27B0) // Ball AI purple
                val jointColor = Color(0xFFFFEB3B) // Glowing yellow
                val boneStroke = 3.5.dp.toPx()
                val jointRadius = 5.dp.toPx()

                // Draw Bones (Lines)
                for (conn in POSE_CONNECTIONS) {
                    if (conn.first < lm.size && conn.second < lm.size) {
                        val p1 = lm[conn.first]
                        val p2 = lm[conn.second]
                        if (p1.visibility > 0.20f && p2.visibility > 0.20f) {
                            drawLine(
                                color = boneColor,
                                start = Offset(mapX(p1.x), mapY(p1.y)),
                                end = Offset(mapX(p2.x), mapY(p2.y)),
                                strokeWidth = boneStroke,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Draw Joints (Yellow Circles)
                val mainJoints = listOf(0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28)
                for (idx in mainJoints) {
                    if (idx < lm.size) {
                        val p = lm[idx]
                        if (p.visibility > 0.20f) {
                            // Outer subtle glow
                            drawCircle(
                                color = Color(0x66FFEB3B),
                                radius = jointRadius + 2.dp.toPx(),
                                center = Offset(mapX(p.x), mapY(p.y))
                            )
                            // Inner yellow joint
                            drawCircle(
                                color = jointColor,
                                radius = jointRadius,
                                center = Offset(mapX(p.x), mapY(p.y))
                            )
                        }
                    }
                }
            }
        }

        // 2. Draw Hoop / Backboard with Automatic 3D Perspective - ONLY in Shooting mode and if enabled!
        if (!state.isDribbleMode && state.showHoop) {
            val activeHoop = state.lockedHoop ?: state.hoop?.let {
                LockedHoop(nx = it.nx, ny = it.ny, nw = it.nw, nh = it.nh, isLocked = false)
            }

            if (activeHoop != null) {
                val mappedHoop = activeHoop.copy(
                    nx = (mapX(activeHoop.nx) / w),
                    ny = (mapY(activeHoop.ny) / h),
                    nw = (scaleW(activeHoop.nw) / w),
                    nh = (scaleH(activeHoop.nh) / h)
                )
                drawHoopGraphic(
                    activeHoop = mappedHoop,
                    w = w,
                    h = h,
                    hoopVisual = state.hoopVisual
                )
            }
        }

        // 3. Draw Ball Targeting Ring - ONLY if enabled!
        if (state.showBall) {
            val ball = state.ball
            if (ball != null) {
                val bx = mapX(ball.nx)
                val by = mapY(ball.ny)
                val br = maxOf(scaleW(ball.nw), scaleH(ball.nh)) / 2f

                // Outer soft glow
                drawCircle(
                    color = Color(0x44FF6B1A),
                    radius = br + 3.dp.toPx(),
                    center = Offset(bx, by)
                )

                // Primary orange targeting ring
                drawCircle(
                    color = SportOrange,
                    radius = br,
                    center = Offset(bx, by),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Inner white precision ring (Hollow center)
                drawCircle(
                    color = Color.White,
                    radius = br * 0.65f,
                    center = Offset(bx, by),
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // Central reticle dot
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(bx, by)
                )
            }
        }
    }
}

/**
 * Renderiza la canasta y el tablero adaptando automáticamente la perspectiva
 * según el ángulo (frontal, lateral izquierdo, lateral derecho o cálculo adaptativo por posición nx).
 */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHoopGraphic(
    activeHoop: LockedHoop,
    w: Float,
    h: Float,
    hoopVisual: HoopVisual = HoopVisual.REALISTIC_FRONT
) {
    val hx = activeHoop.nx * w
    val hy = activeHoop.ny * h
    val hw = activeHoop.nw * w
    val hh = activeHoop.nh * h
    val boardCyan = Color(0xFF00E5FF)
    val yaw = activeHoop.getEffectiveYaw() // -0.85f .. +0.85f (0f = frontal)
    val absYaw = kotlin.math.abs(yaw).coerceIn(0f, 0.85f)

    if (absYaw < 0.08f) {
        // --- Perspectiva Frontal Directa (0°) ---
        // Tablero Exterior
        drawRoundRect(
            color = boardCyan,
            topLeft = Offset(hx - hw / 2f, hy - hh / 2f),
            size = Size(hw, hh),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Recuadro Interior del Tablero
        val innerW = hw * 0.44f
        val innerH = hh * 0.44f
        val innerX = hx - innerW / 2f
        val innerY = hy - hh * 0.22f
        drawRect(
            color = Color(0xCC00E5FF),
            topLeft = Offset(innerX, innerY),
            size = Size(innerW, innerH),
            style = Stroke(width = 1.8.dp.toPx())
        )

        if (hoopVisual == HoopVisual.REALISTIC_FRONT) {
            val rimY = innerY + innerH
            val rimW = hw * 0.46f
            val rimH = hh * 0.16f

            // Soporte metálico
            drawRect(
                color = Color(0xFFE65100),
                topLeft = Offset(hx - hw * 0.05f, rimY - hh * 0.04f),
                size = Size(hw * 0.10f, hh * 0.06f)
            )

            // Aro oval frontal
            drawOval(
                color = SportOrange,
                topLeft = Offset(hx - rimW / 2f, rimY - rimH / 2f),
                size = Size(rimW, rimH),
                style = Stroke(width = 3.dp.toPx())
            )

            // Red estética
            val netBottom = rimY + hh * 0.42f
            val netPath = Path().apply {
                moveTo(hx - rimW * 0.45f, rimY + rimH * 0.2f)
                lineTo(hx - rimW * 0.22f, netBottom)
                lineTo(hx + rimW * 0.22f, netBottom)
                lineTo(hx + rimW * 0.45f, rimY + rimH * 0.2f)
            }
            drawPath(
                path = netPath,
                color = Color(0x88FFFFFF),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawLine(
                color = Color(0x66FFFFFF),
                start = Offset(hx, rimY + rimH * 0.2f),
                end = Offset(hx, netBottom),
                strokeWidth = 1.2.dp.toPx()
            )
        }
    } else {
        // --- Perspectiva 3D Lateral / Adaptativa (con inclinación trapezoidal y proyección del aro) ---
        val effW = hw * (1f - 0.36f * absYaw)
        val depthK = 0.28f * absYaw

        // Cuando yaw > 0 (cámara al lado izquierdo, aro visto desde ángulo lateral izq):
        // El lado derecho está más cerca de la cancha / más alto, o viceversa.
        val (hLeft, hRight) = if (yaw > 0) {
            Pair(hh * (1f - depthK), hh * (1f + depthK))
        } else {
            Pair(hh * (1f + depthK), hh * (1f - depthK))
        }

        val xLeft = hx - effW / 2f
        val xRight = hx + effW / 2f

        // 1. Tablero Exterior en Perspectiva Trapezoidal
        val boardPath = Path().apply {
            moveTo(xLeft, hy - hLeft / 2f)
            lineTo(xRight, hy - hRight / 2f)
            lineTo(xRight, hy + hRight / 2f)
            lineTo(xLeft, hy + hLeft / 2f)
            close()
        }
        drawPath(path = boardPath, color = boardCyan, style = Stroke(width = 2.5.dp.toPx()))

        // Función de mapeo (u, v) a coordenadas de pantalla en el plano inclinado del tablero
        fun mapBoardPoint(u: Float, v: Float): Offset {
            val t = (u + 0.5f).coerceIn(0f, 1f)
            val px = xLeft + t * effW
            val localH = hLeft + t * (hRight - hLeft)
            val py = hy + v * localH
            return Offset(px, py)
        }

        // 2. Recuadro Interior del Tablero en Perspectiva
        val targetTL = mapBoardPoint(-0.22f, -0.05f)
        val targetTR = mapBoardPoint(0.22f, -0.05f)
        val targetBR = mapBoardPoint(0.22f, 0.44f)
        val targetBL = mapBoardPoint(-0.22f, 0.44f)
        val targetPath = Path().apply {
            moveTo(targetTL.x, targetTL.y)
            lineTo(targetTR.x, targetTR.y)
            lineTo(targetBR.x, targetBR.y)
            lineTo(targetBL.x, targetBL.y)
            close()
        }
        drawPath(path = targetPath, color = Color(0xCC00E5FF), style = Stroke(width = 1.8.dp.toPx()))

        if (hoopVisual == HoopVisual.REALISTIC_FRONT) {
            // Punto de anclaje base del aro en el tablero
            val mountBase = mapBoardPoint(0f, 0.44f)

            // El aro sobresale perpendicularmente hacia el centro de la pista
            val rimDirection = if (yaw > 0) -1f else 1f
            val rimDisplacementX = rimDirection * effW * 0.30f * absYaw
            val rimCenterX = mountBase.x + rimDisplacementX
            val rimCenterY = mountBase.y + hh * 0.12f
            val rimRadiusX = (effW * 0.28f).coerceAtLeast(hw * 0.16f)
            val rimRadiusY = hh * 0.11f

            // Soporte metálico hacia el aro
            drawLine(
                color = Color(0xFFE65100),
                start = mountBase,
                end = Offset(rimCenterX - rimDisplacementX * 0.25f, rimCenterY),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Aro elíptico en perspectiva 3D
            drawOval(
                color = SportOrange,
                topLeft = Offset(rimCenterX - rimRadiusX, rimCenterY - rimRadiusY),
                size = Size(rimRadiusX * 2f, rimRadiusY * 2f),
                style = Stroke(width = 3.dp.toPx())
            )

            // Red blanca que cuelga bajo gravedad
            val netBottom = rimCenterY + hh * 0.38f
            val netBottomRadiusX = rimRadiusX * 0.45f
            val netPath = Path().apply {
                moveTo(rimCenterX - rimRadiusX * 0.85f, rimCenterY + rimRadiusY * 0.2f)
                lineTo(rimCenterX - netBottomRadiusX, netBottom)
                lineTo(rimCenterX + netBottomRadiusX, netBottom)
                lineTo(rimCenterX + rimRadiusX * 0.85f, rimCenterY + rimRadiusY * 0.2f)
            }
            drawPath(
                path = netPath,
                color = Color(0x88FFFFFF),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawLine(
                color = Color(0x66FFFFFF),
                start = Offset(rimCenterX, rimCenterY + rimRadiusY * 0.2f),
                end = Offset(rimCenterX, netBottom),
                strokeWidth = 1.2.dp.toPx()
            )
        }
    }
}
