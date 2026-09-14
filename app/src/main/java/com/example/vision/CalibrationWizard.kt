package com.example.vision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theme.SportBorder
import com.example.theme.SportCanvas
import com.example.theme.SportInfo
import com.example.theme.SportMuted
import com.example.theme.SportOnBrand
import com.example.theme.SportOnSurface
import com.example.theme.SportOrange
import com.example.theme.SportSuccess
import com.example.theme.SportWarning

@Composable
fun CalibrationWizard(
    currentStep: CalibrationStep,
    suggestedHoop: LockedHoop?,
    ballDetected: Boolean,
    ballProgress: Float,
    isDribbleMode: Boolean = false,
    skeletonDetected: Boolean = false,
    skeletonProgress: Float = 0f,
    skeletonFeedback: String = "",
    ballFeedback: String = "",
    courtCalibration: CourtCalibration = CourtCalibration(),
    onHoopSelected: (Float, Float) -> Unit,
    onLockHoopConfirmed: () -> Unit,
    onNextStep: () -> Unit,
    onSkipCalibration: () -> Unit,
    onExitToMain: () -> Unit = {},
    onPickVideo: () -> Unit = {},
    onPerspectiveChanged: (HoopPerspective) -> Unit = {},
    onCourtPointMoved: (String, Float, Float) -> Unit = { _, _, _ -> },
    onCourtPresetApplied: (CourtPreset) -> Unit = {},
    onCourtReset: () -> Unit = {},
    onFastConfirmBall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("calibration_wizard")
    ) {
        when (currentStep) {
            CalibrationStep.POSITION_PHONE -> {
                PositionPhoneScreen(
                    onNext = onNextStep,
                    onSkip = onSkipCalibration,
                    onPickVideo = onPickVideo
                )
            }
            CalibrationStep.LOCK_HOOP -> {
                LockHoopScreen(
                    suggestedHoop = suggestedHoop,
                    onHoopSelected = onHoopSelected,
                    onPerspectiveChanged = onPerspectiveChanged,
                    onConfirmLock = onLockHoopConfirmed
                )
            }
            CalibrationStep.CALIBRATE_SKELETON -> {
                CalibrateSkeletonScreen(
                    detected = skeletonDetected,
                    progress = skeletonProgress,
                    feedback = skeletonFeedback,
                    onComplete = onNextStep,
                    onSkip = onSkipCalibration
                )
            }
            CalibrationStep.CHECK_BALL -> {
                CheckBallScreen(
                    ballDetected = ballDetected,
                    progress = ballProgress,
                    feedback = ballFeedback,
                    isDribbleMode = isDribbleMode,
                    onComplete = onNextStep,
                    onFastConfirmBall = onFastConfirmBall,
                    onSkip = onSkipCalibration
                )
            }
            CalibrationStep.COURT_ALIGNMENT -> {
                CourtPointSelectorView(
                    courtCalibration = courtCalibration,
                    onPointMoved = onCourtPointMoved,
                    onApplyPreset = onCourtPresetApplied,
                    onReset = onCourtReset,
                    onConfirm = onNextStep,
                    onSkip = onSkipCalibration
                )
            }
            CalibrationStep.COMPLETED -> {
                // Done - nothing rendered here
            }
        }

        // Botón simple con una X para salir a la pantalla principal
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xCC1B1C2A))
                .border(1.dp, Color(0x66FFFFFF), CircleShape)
                .clickable { onExitToMain() }
                .testTag("exit_calibration_to_main_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Salir a la pantalla principal",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PositionPhoneScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onPickVideo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0B0B0E))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FF6B1A))
                    .border(2.dp, SportOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = SportOrange,
                    modifier = Modifier.size(42.dp)
                )
            }

            Text(
                text = "PASO 1: COLOCA EL MÓVIL",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = SportOnSurface
            )

            Text(
                text = "Coloca el móvil en un trípode o apoyo estable apuntando a la pista. Debe verse el tirador y la canasta en el encuadre.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = SportMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Box(
                modifier = Modifier
                    .testTag("next_step_button")
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SportOrange)
                    .clickable { onNext() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CONTINUAR A FIJAR CANASTA",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = SportOnBrand
                )
            }

            // Option to test with pre-recorded video directly!
            Box(
                modifier = Modifier
                    .testTag("pick_video_initial_button")
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x2200E5FF))
                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp))
                    .clickable { onPickVideo() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📹",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "PROBAR CON VÍDEO DE LA GALERÍA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF00E5FF)
                    )
                }
            }

            Text(
                text = "Saltar calibración",
                fontSize = 13.sp,
                color = SportMuted,
                modifier = Modifier
                    .clickable { onSkip() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun LockHoopScreen(
    suggestedHoop: LockedHoop?,
    onHoopSelected: (Float, Float) -> Unit,
    onPerspectiveChanged: (HoopPerspective) -> Unit,
    onConfirmLock: () -> Unit
) {
    var hoopPos by remember {
        mutableStateOf(
            suggestedHoop ?: LockedHoop(nx = 0.5f, ny = 0.28f, isLocked = false)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val nx = offset.x / size.width
                    val ny = offset.y / size.height
                    hoopPos = hoopPos.copy(nx = nx, ny = ny)
                    onHoopSelected(nx, ny)
                }
            }
    ) {
        // Target overlay drawn over camera
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hx = hoopPos.nx * size.width
            val hy = hoopPos.ny * size.height
            val hw = hoopPos.nw * size.width
            val hh = hoopPos.nh * size.height

            // 1. Draw realistic 3D perspective basketball hoop matching real backboard & rim
            drawHoopGraphic(
                activeHoop = hoopPos,
                w = size.width,
                h = size.height,
                hoopVisual = HoopVisual.REALISTIC_FRONT
            )

            // 2. High-precision calibration target crosshairs
            drawLine(
                color = Color(0x7700E5FF),
                start = Offset(hx - hw * 0.9f, hy),
                end = Offset(hx + hw * 0.9f, hy),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                color = Color(0x7700E5FF),
                start = Offset(hx, hy - hh * 1.0f),
                end = Offset(hx, hy + hh * 1.0f),
                strokeWidth = 1.2.dp.toPx()
            )

            // Outer target reticle circle
            drawCircle(
                color = Color(0x8800E5FF),
                radius = hw * 0.45f,
                center = Offset(hx, hy),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // Instruction Pill Top + Perspective Selector
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEE0B0B0E))
                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOCA SOBRE EL ARO DE TU CANASTA REAL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color(0xFF00E5FF),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (hoopPos.perspective == HoopPerspective.AUTO) {
                            "Modo Auto: Inclinación adaptada (${if (hoopPos.getEffectiveYaw() == 0f) "Frontal" else if (hoopPos.getEffectiveYaw() < 0) "Lateral Izq" else "Lateral Der"})"
                        } else {
                            "Perspectiva fijada: ${hoopPos.perspective.label}"
                        },
                        fontSize = 10.sp,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Perspective Selector Chips (Auto, Frontal, Lateral Izq, Lateral Der)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HoopPerspective.values().forEach { persp ->
                    val isSelected = (hoopPos.perspective == persp)
                    Box(
                        modifier = Modifier
                            .testTag("hoop_perspective_${persp.name.lowercase()}")
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) Color(0xFF00E5FF) else Color(0xCC111116))
                            .border(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x44FFFFFF), RoundedCornerShape(999.dp))
                            .clickable {
                                hoopPos = hoopPos.copy(perspective = persp)
                                onPerspectiveChanged(persp)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = persp.shortLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // Bottom Lock Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .testTag("lock_hoop_button")
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF00E5FF))
                    .clickable { onConfirmLock() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Text(
                        text = "FIJAR CANASTA AQUÍ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.Black
                    )
                }
            }
            Text(
                text = "La perspectiva y posición se guardan para todo el entrenamiento",
                fontSize = 12.sp,
                color = SportMuted
            )
        }
    }
}

@Composable
private fun CalibrateSkeletonScreen(
    detected: Boolean,
    progress: Float,
    feedback: String,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val isDone = progress >= 1.0f

    // Auto-advance once skeleton is successfully calibrated with adequate pause for visual confirmation
    androidx.compose.runtime.LaunchedEffect(isDone) {
        if (isDone) {
            kotlinx.coroutines.delay(1100)
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x550B0B0E)) // Semi-translucent so player sees their body & skeleton
            .padding(16.dp)
    ) {
        // Player framing guide silhouette in the center of the camera
        Canvas(modifier = Modifier.fillMaxSize()) {
            val isLandscape = size.width > size.height
            val cx = size.width * 0.5f
            val cy = if (isLandscape) size.height * 0.48f else size.height * 0.45f
            val guideH = if (isLandscape) size.height * 0.70f else size.height * 0.62f
            val guideW = if (isLandscape) guideH * 0.54f else (size.width * 0.48f).coerceAtMost(size.height * 0.40f)
            val left = cx - guideW / 2f
            val top = cy - guideH / 2f

            drawRoundRect(
                color = if (isDone) SportSuccess else if (progress > 0.15f) Color(0xFF00E5FF) else Color(0x66FFFFFF),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(guideW, guideH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))
                )
            )
        }

        // Top info pill
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xEE111116))
                    .border(1.dp, if (isDone) SportSuccess else Color(0xFF00E5FF), RoundedCornerShape(999.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isDone) SportSuccess else if (progress > 0.1f) Color(0xFF00E5FF) else SportOrange)
                    )
                    Text(
                        text = if (isDone) "ESQUELETO CALIBRADO" else "CALIBRACIÓN AUTOMÁTICA DE ESQUELETO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = if (isDone) SportSuccess else Color(0xFF00E5FF)
                    )
                }
            }

            Text(
                text = feedback.ifEmpty { if (detected) "¡Detectado! Mantén la posición..." else "Colócate frente a la cámara frontal" },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        // Bottom progress & buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Linear progress indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x44FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isDone) SportSuccess else Color(0xFF00E5FF))
                )
            }

            Text(
                text = if (isDone) "¡100% Calibrado! Pasando a calibración de balón..." else "Calibrando automáticamente: ${(progress * 100).toInt()}%",
                fontSize = 11.sp,
                color = if (isDone) SportSuccess else Color(0xFFB0BEC5)
            )

            // Manual continue button when 100% calibrated
            if (isDone) {
                Box(
                    modifier = Modifier
                        .testTag("continue_skeleton_button")
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SportSuccess)
                        .clickable { onComplete() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SIGUIENTE: CALIBRAR BALÓN ›",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }

            // PROMINENT "SALTAR CALIBRACIÓN" BUTTON (ALWAYS ACCESSIBLE)
            Box(
                modifier = Modifier
                    .testTag("skip_skeleton_calibration_button")
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF))
                    .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp))
                    .clickable { onSkip() }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO CALIBRAR Y SALTAR (ENTRENAR YA)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CheckBallScreen(
    ballDetected: Boolean,
    progress: Float,
    feedback: String = "",
    isDribbleMode: Boolean = false,
    onComplete: () -> Unit,
    onFastConfirmBall: () -> Unit = {},
    onSkip: () -> Unit
) {
    val isDone = progress >= 1.0f

    // Auto-advance in dribble mode once ball calibration completes with clear visual confirmation
    androidx.compose.runtime.LaunchedEffect(isDone) {
        if (isDone && isDribbleMode) {
            kotlinx.coroutines.delay(1200)
            onComplete()
        }
    }

    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x550B0B0E)) // Semi-translucent so user sees the camera and ball clearly
            .padding(16.dp)
    ) {
        // Visual real ball calibration reticle in the center of camera
        val reticleSize = if (isLandscape) 140.dp else 190.dp
        val progressSize = if (isLandscape) 130.dp else 175.dp
        val iconSize = if (isLandscape) 38.dp else 52.dp

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(reticleSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 6.dp.toPx()

                // Target crosshairs
                drawLine(
                    color = Color(0x44FFFFFF),
                    start = Offset(centerOffset.x - radius * 1.15f, centerOffset.y),
                    end = Offset(centerOffset.x + radius * 1.15f, centerOffset.y),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = Color(0x44FFFFFF),
                    start = Offset(centerOffset.x, centerOffset.y - radius * 1.15f),
                    end = Offset(centerOffset.x, centerOffset.y + radius * 1.15f),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Outer circle with dashed ring
                drawCircle(
                    color = if (isDone) SportSuccess else if (progress > 0.15f) SportOrange else Color(0x88FFFFFF),
                    radius = radius,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))
                    )
                )

                // Fill glowing indicator when ball is in
                if (progress > 0.05f) {
                    drawCircle(
                        color = (if (isDone) SportSuccess else SportOrange).copy(alpha = 0.12f * progress),
                        radius = radius * 0.95f
                    )
                }
            }

            // Radial Progress Indicator around the ball
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(progressSize),
                color = if (isDone) SportSuccess else SportOrange,
                trackColor = Color(0x22FFFFFF),
                strokeWidth = if (isLandscape) 4.dp else 6.dp
            )

            // Center icon
            Icon(
                imageVector = if (isDone) Icons.Default.Check else Icons.Default.SportsBasketball,
                contentDescription = null,
                tint = if (isDone) SportSuccess else SportOrange,
                modifier = Modifier.size(iconSize)
            )
        }

        // Top info pill
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xEE111116))
                    .border(1.dp, if (isDone) SportSuccess else SportOrange, RoundedCornerShape(999.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isDone) SportSuccess else SportOrange)
                    )
                    Text(
                        text = if (isDone) "BALÓN CALIBRADO" else "CALIBRACIÓN REAL DE BALÓN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = if (isDone) SportSuccess else SportOrange
                    )
                }
            }

            Text(
                text = feedback.ifEmpty {
                    if (isDone) "¡Balón verificado con éxito!"
                    else if (progress > 0.1f) "Verificando color y textura de cuero..."
                    else "Sostén el balón dentro del círculo guía"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        // Bottom action controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isDone) "¡100% Calibrado! Detector optimizado para tu balón" else "Calibrando: ${(progress * 100).toInt()}% (requiere balón real)",
                fontSize = 11.sp,
                color = if (isDone) SportSuccess else Color(0xFFB0BEC5)
            )

            if (isDone) {
                Box(
                    modifier = Modifier
                        .testTag("complete_ball_calibration_button")
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SportSuccess)
                        .clickable { onComplete() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDribbleMode) "¡LISTO! EMPEZAR ENTRENAMIENTO ›" else "PASO 3: ALINEAR PISTA (TRIPLE Y ZONA) ›",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            } else {
                // BOTÓN DE CALIBRACIÓN INSTANTÁNEA (NO TE HACE ESPERAR)
                Box(
                    modifier = Modifier
                        .testTag("fast_confirm_ball_button")
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SportOrange)
                        .clickable { onFastConfirmBall() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "CALIBRAR AL INSTANTE (USAR ESTE BALÓN)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = Color.Black
                        )
                    }
                }
            }

            // PROMINENT "SALTAR CALIBRACIÓN" BUTTON (ALWAYS AVAILABLE)
            Box(
                modifier = Modifier
                    .testTag("skip_ball_calibration_button")
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF))
                    .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp))
                    .clickable { onSkip() }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO CALIBRAR Y SALTAR (ENTRENAR YA)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color.White
                )
            }
        }
    }
}
