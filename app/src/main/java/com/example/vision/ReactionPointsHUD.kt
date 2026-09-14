package com.example.vision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import com.example.theme.SportOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReactionPointsHUD(
    state: VisionState,
    onHitPoint: (Long) -> Unit,
    onDismissPopup: (Long) -> Unit,
    onRestartDrill: () -> Unit,
    onExitToMain: () -> Unit,
    onToggleShowSkeleton: () -> Unit,
    onToggleShowHoop: () -> Unit,
    onToggleShowBall: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleRecording: () -> Unit,
    onRecalibrate: () -> Unit = {},
    onSelectDribbleCombo: () -> Unit = {},
    onSelectReactionPoints: () -> Unit = {},
    onSelectDefendZone: () -> Unit = {},
    onSelectShooting: () -> Unit = {},
    onSelectUploadVideo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        VisionSettingsDialog(
            state = state,
            onDismiss = { showSettingsDialog = false },
            onToggleShowSkeleton = onToggleShowSkeleton,
            onToggleShowHoop = onToggleShowHoop,
            onToggleShowBall = onToggleShowBall,
            onSetHoopPerspective = {},
            onOpenSavedVideos = {},
            onRecalibrateHoop = onRecalibrate,
            onResetSession = onRestartDrill,
            onToggleCamera = onToggleCamera,
            onToggleRecording = onToggleRecording,
            onExitDrill = onExitToMain,
            onSelectDribbleCombo = onSelectDribbleCombo,
            onSelectReactionPoints = onSelectReactionPoints,
            onSelectDefendZone = onSelectDefendZone,
            onSelectShooting = onSelectShooting,
            onSelectUploadVideo = onSelectUploadVideo
        )
    }

    val remaining = state.reactionTimerRemainingSec
    val timerFormatted = String.format("%02d:%02d", remaining / 60, remaining % 60)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // 1. TOP HEADER (Arriba a la izquierda contador idéntico a las capturas, arriba a la derecha cuenta regresiva y ajustes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Arriba a la izquierda: CONTADOR DE PUNTOS con animación de rebote y cambio de color a amarillo neón al puntuar
            ReactionScoreCounter(
                score = state.reactionScore
            )

            // Arriba a la derecha: INDICADOR REC (SI APLICA) + BOTÓN AJUSTES + BADGE DEL CONTADOR DE TIEMPO
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Si la grabación está activa (iniciada desde ajustes), mostrar indicador discreto
                if (state.isRecordingLive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xDDF44336))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "REC ${state.recordingDurationSec}s",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Botón de Voz del Entrenador (Silenciar / Activar)
                var isVoiceMuted by remember { mutableStateOf(!VoiceCoachManager.isVoiceEnabled) }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isVoiceMuted) Color(0x33FF4444) else Color(0xFF1E283D))
                        .border(1.5.dp, if (isVoiceMuted) Color(0x88FF4444) else Color(0x33446699), CircleShape)
                        .clickable {
                            isVoiceMuted = !isVoiceMuted
                            VoiceCoachManager.isVoiceEnabled = !isVoiceMuted
                            if (isVoiceMuted) VoiceCoachManager.stop()
                        }
                        .testTag("reaction_voice_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVoiceMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isVoiceMuted) "Activar voz del entrenador" else "Silenciar voz",
                        tint = if (isVoiceMuted) Color(0xFFFF6666) else Color(0xFF2FB2C9),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Botón de Ajustes (arriba a la derecha, a la izquierda del contador de tiempo)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E283D))
                        .border(1.5.dp, Color(0x33446699), CircleShape)
                        .clickable { showSettingsDialog = true }
                        .testTag("reaction_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ajustes",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Badge de cuenta regresiva con la misma altura y diseño que el contador de puntos, y ancho más largo para el tiempo
                Box(
                    modifier = Modifier
                        .height(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E283D))
                        .border(
                            1.5.dp,
                            if (remaining <= 10) Color(0xFFFF5252) else Color(0x33446699),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 24.dp)
                        .testTag("reaction_countdown_timer"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timerFormatted,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (remaining <= 10) Color(0xFFFF5252) else Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // 2. ACTIVE REACTION POINT (Puntos que aparecen a la izquierda y derecha del jugador)
        val activePoint = state.activeReactionPoint
        if (activePoint != null && state.isReactionTimerRunning && !state.isReactionSessionFinished) {
            val pointSize = 98.dp
            val targetX = (screenWidth * activePoint.xNorm) - (pointSize / 2)
            val targetY = (screenHeight * activePoint.yNorm) - (pointSize / 2)

            ReactionPointTarget(
                number = activePoint.number,
                spawnTimeMs = activePoint.spawnTimeMs,
                durationMs = activePoint.durationMs,
                isHandOnlyBlocked = activePoint.isHandOnlyBlocked,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = targetX.roundToPx(),
                            y = targetY.roundToPx()
                        )
                    }
                    .size(pointSize)
                    .testTag("reaction_point_target_${activePoint.number}")
            )
        }

        // 2b. AVISO ANTI-TRAMPAS EN VIVO (Cuando se intenta tocar solo con la mano vacía)
        if (state.reactionWarningMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xEE1F0A0A))
                    .border(2.dp, Color(0xFFFF3B30), RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .zIndex(15f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.reactionWarningMessage,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        // Barra informativa de regla de juego
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xCC0F172A))
                .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "🏀 Regla: Bota el balón controlado hacia el círculo para puntuar",
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 3. POPUPS DE PUNTOS CON ANIMACIÓN (+1 flotando hacia arriba y desvaneciéndose en la zona del point)
        state.reactionPopups.forEach { popup ->
            ReactionScorePopupItem(
                popup = popup,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                onDismiss = { onDismissPopup(popup.id) }
            )
        }

        // 4. MODAL DE FINALIZACIÓN CUANDO TERMINAN LOS 60 SEGUNDOS
        if (state.isReactionSessionFinished) {
            ReactionFinishedDialog(
                score = state.reactionScore,
                onRestart = onRestartDrill,
                onExit = onExitToMain
            )
        }
    }
}

/**
 * Contador de puntos en la esquina superior izquierda idéntico a las capturas 1 y 2 del usuario:
 * - Rectángulo redondeado azul marino oscuro con tipografía atlética grande.
 * - Al sumar nuevos puntos se activa una animación de movimiento (rebote de escala y desplazamiento)
 *   y cambia al color amarillo/verde neón característico (captura 2).
 * - Tras un instante, regresa fluidamente a su estado original blanco (captura 1).
 */
@Composable
private fun ReactionScoreCounter(
    score: Int,
    modifier: Modifier = Modifier
) {
    var previousScore by remember { mutableStateOf(score) }
    var isHitHighlighted by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    val offsetY = remember { Animatable(0f) }

    val textColor by animateColorAsState(
        targetValue = if (isHitHighlighted) Color(0xFFE2FF39) else Color.White,
        animationSpec = tween(durationMillis = if (isHitHighlighted) 60 else 400, easing = LinearEasing),
        label = "textColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isHitHighlighted) Color(0x88E2FF39) else Color(0x33446699),
        animationSpec = tween(durationMillis = if (isHitHighlighted) 60 else 400, easing = LinearEasing),
        label = "borderColor"
    )

    LaunchedEffect(score) {
        if (score > previousScore) {
            previousScore = score
            isHitHighlighted = true
            // Animación de rebote y desplazamiento en movimiento como solicitado
            launch {
                scale.animateTo(1.24f, tween(110, easing = FastOutSlowInEasing))
                scale.animateTo(1.0f, tween(200, easing = FastOutSlowInEasing))
            }
            launch {
                offsetY.animateTo(-6f, tween(110, easing = FastOutSlowInEasing))
                offsetY.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
            }
            delay(280L)
            isHitHighlighted = false
        } else {
            previousScore = score
        }
    }

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1E283D))
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 24.dp)
            .testTag("reaction_score_counter"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$score",
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            color = textColor,
            letterSpacing = 1.sp,
            modifier = Modifier
                .scale(scale.value)
                .offset(y = offsetY.value.dp)
        )
    }
}

/**
 * Visualización exacta del "Point" que aparece a los lados del jugador según las capturas 3 y 4 del usuario:
 * - Alrededor del círculo amarillo: envoltorio blanco que es como una barra circular que va desapareciendo en 5 segundos
 *   recorriendo todo el contorno.
 * - Pista de fondo gris oscura sobre la que se consume la barra blanca.
 * - Al llegar al final del tiempo (0s), el point desaparece y sale en otro lugar.
 * - Círculo interior con degradado amarillo brillante a naranja intenso y número atlético en carbón oscuro.
 */
@Composable
private fun ReactionPointTarget(
    number: Int,
    spawnTimeMs: Long,
    durationMs: Long,
    isHandOnlyBlocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val progress = remember(spawnTimeMs) { Animatable(1f) }

    LaunchedEffect(spawnTimeMs) {
        val now = System.currentTimeMillis()
        val elapsed = (now - spawnTimeMs).coerceAtLeast(0L)
        val remaining = (durationMs - elapsed).coerceAtLeast(0L)
        val initialFraction = if (durationMs > 0) (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 1f
        progress.snapTo(initialFraction)
        if (remaining > 0L) {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = remaining.toInt(),
                    easing = LinearEasing
                )
            )
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Envoltorio exterior: Barra circular blanca que desaparece recorriendo el contorno en 5 segundos
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(diameter, diameter)

            // Pista de fondo: oscura/gris normal o roja parpadeante si se bloquea por mano vacía
            drawArc(
                color = if (isHandOnlyBlocked) Color(0xFFFF2222) else Color(0xFF4A4E5A),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )

            // Barra blanca que recorre el contorno y desaparece a lo largo de 5 segundos
            // Dirección contraria: de derecha hacia la izquierda (sentido antihorario)
            val currentSweep = 360f * progress.value.coerceIn(0f, 1f)
            if (currentSweep > 0f) {
                drawArc(
                    color = if (isHandOnlyBlocked) Color(0xFFFF8888) else Color.White,
                    startAngle = -90f,
                    sweepAngle = -currentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
            }
        }

        // Círculo interior amarillo-naranja con el número (o rojo si intento de trampa con mano vacía)
        Box(
            modifier = Modifier
                .fillMaxSize(0.74f)
                .clip(CircleShape)
                .background(
                    if (isHandOnlyBlocked) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF4444),
                                Color(0xFFB71C1C)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFEE33), // Amarillo vibrante superior
                                Color(0xFFFF9900), // Ámbar medio
                                Color(0xFFFF5722)  // Naranja intenso inferior
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    }
                )
                .border(
                    if (isHandOnlyBlocked) 2.5.dp else 1.5.dp,
                    if (isHandOnlyBlocked) Color(0xFFFFCCCC) else Color(0x33222222),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isHandOnlyBlocked) "🚫" else "$number",
                fontSize = if (isHandOnlyBlocked) 26.sp else 38.sp,
                fontWeight = FontWeight.Black,
                color = if (isHandOnlyBlocked) Color.White else Color(0xFF26262B),
                letterSpacing = (-1).sp
            )
        }
    }
}

/**
 * Animación del popup de puntos (+1):
 * - Emerge exactamente en la zona del point alcanzado
 * - Número grande (60sp) y visible con color amarillo neón y sombra para alto contraste
 * - Se eleva hacia arriba y se desvanece suavemente
 */
@Composable
private fun ReactionScorePopupItem(
    popup: ReactionPopup,
    screenWidth: Dp,
    screenHeight: Dp,
    onDismiss: () -> Unit
) {
    val animOffset = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }
    val animScale = remember { Animatable(0.6f) }

    LaunchedEffect(popup.id) {
        animScale.animateTo(1.4f, tween(160, easing = FastOutSlowInEasing))
        animScale.animateTo(1.05f, tween(120, easing = LinearEasing))
        animOffset.animateTo(-65f, tween(650, easing = FastOutSlowInEasing))
        animAlpha.animateTo(0f, tween(250, easing = LinearEasing))
        onDismiss()
    }

    val popupSize = 140.dp
    val posX = (screenWidth * popup.xNorm) - (popupSize / 2)
    val posY = (screenHeight * popup.yNorm) - (popupSize / 2) + animOffset.value.dp

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = posX.roundToPx(),
                    y = posY.roundToPx()
                )
            }
            .size(popupSize)
            .scale(animScale.value)
            .alpha(animAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = popup.text,
            fontSize = 60.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFE2FF39), // Amarillo neón llamativo
            letterSpacing = 1.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0xCC000000),
                    offset = Offset(2f, 4f),
                    blurRadius = 10f
                )
            )
        )
    }
}

/**
 * Modal de resultado tras los 60 segundos
 */
@Composable
private fun ReactionFinishedDialog(
    score: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141926))
                .border(2.dp, Color(0xFFFF9800), RoundedCornerShape(24.dp))
                .padding(24.dp)
                .testTag("reaction_finished_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🏀 ¡TIEMPO COMPLETADO!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF9800)
                )

                Text(
                    text = "Sesión de Bote & Reaction (60s)",
                    fontSize = 13.sp,
                    color = Color(0xFFAAAAAA)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E2638))
                        .padding(horizontal = 32.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "PUNTOS ALCANZADOS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300),
                            letterSpacing = 1.sp
                        )
                    }
                }

                Text(
                    text = if (score >= 35) "¡Nivel sobresaliente! Coordinación mano libre excepcional."
                    else if (score >= 20) "¡Buen ritmo! Sigue practicando para aumentar la velocidad de reacción."
                    else "¡Buen entrenamiento! Mantén el bote firme mientras alcanzas cada punto.",
                    fontSize = 12.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reaction_restart_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Text(
                            text = "REPETIR RETO (60s)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }

                Button(
                    onClick = onExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("reaction_exit_menu_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222B3D))
                ) {
                    Text(
                        text = "SALIR AL MENÚ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
