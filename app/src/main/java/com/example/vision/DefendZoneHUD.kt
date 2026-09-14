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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * HUD principal para el minijuego "Defend the Zone" (Esquiva a los Defensores Fantasma).
 * Requerimientos estrictos:
 * 1. Iconos de vidas ubicados arriba a la izquierda (3 vidas).
 * 2. Opción interactiva para elegir entre "Manos Fantasma" o "Lásers".
 * 3. Detección de botes de protección, cambios de mano/crossover y uso del cuerpo como escudo (Nivel Pro).
 */
@Composable
fun DefendZoneHUD(
    state: VisionState,
    onSelectThreatType: (DefendThreatType) -> Unit,
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
    onManualEvade: (Long) -> Unit = {},
    onManualSteal: (Long) -> Unit = {},
    onManualShield: (Long) -> Unit = {},
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
            onSelectDefendZone = {
                onRestartDrill()
                onSelectDefendZone()
            },
            onSelectShooting = onSelectShooting,
            onSelectUploadVideo = onSelectUploadVideo,
            onSelectThreatType = onSelectThreatType
        )
    }

    val remaining = state.defendTimerRemainingSec
    val timerFormatted = String.format("%02d:%02d", remaining / 60, remaining % 60)

    val infiniteTransition = rememberInfiniteTransition(label = "defend_anim")
    val pulseHeartScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_pulse"
    )

    val laserGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_glow"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // =====================================================================
        // 1. CAPA DE DIBUJO DE LÁSERS (SOLO EN MODO LÁSER)
        // =====================================================================
        Canvas(modifier = Modifier.fillMaxSize()) {
            val widthPx = size.width
            val heightPx = size.height

            for (threat in state.defendThreats) {
                if (threat.threatType == DefendThreatType.LASERS) {
                    val isLeft = threat.side == DefendSide.LEFT
                    val targetYPx = threat.targetY * heightPx
                    drawLaserThreat(
                        threat = threat,
                        isLeft = isLeft,
                        targetYPx = targetYPx,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        glowAlpha = laserGlowAlpha
                    )
                }
            }
        }

        // =====================================================================
        // 1B. CAPA DE MANOS DEFENSORAS (IMAGEN manos.png)
        // Sale desde la parte inferior hacia arriba buscando el punto de la pelota.
        // Si hay cambio de mano o posición inalcanzable, desaparece con efecto y sale una nueva.
        // =====================================================================
        for (threat in state.defendThreats) {
            if (threat.threatType == DefendThreatType.HANDS) {
                HandsThreatView(
                    threat = threat,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }
        }

        // =====================================================================
        // 2. FLASH ROJO EN PANTALLA AL PERDER VIDA POR ROBO
        // =====================================================================
        if (state.defendScreenFlashRed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99FF1744))
            )
        }

        // =====================================================================
        // 3. HEADER SUPERIOR
        // IDÉNTICO AL SISTEMA Y DISEÑO DEL JUEGO POINTS:
        // - Izquierda: Contador de puntos + 3 corazones a su derecha (estilo dibujo simple sin bordes)
        // - Derecha: Botón de ajustes + Contador de tiempo
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ARRIBA A LA IZQUIERDA: CONTADOR DE PUNTOS + 3 CORAZONES A SU DERECHA
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Contador de puntos con animación de rebote y cambio a amarillo neón al puntuar
                DefendScoreCounter(score = state.defendScore)

                // 3 corazones muy simples estilo dibujo sin bordes ni cosas raras
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag("defend_lives_hearts")
                ) {
                    for (i in 1..3) {
                        val isActive = i <= state.defendLives
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Vida $i",
                            tint = if (isActive) Color(0xFFFF2D55) else Color(0x33FFFFFF),
                            modifier = Modifier
                                .size(28.dp)
                                .scale(if (isActive && state.defendLives == 1) pulseHeartScale else 1.0f)
                        )
                    }
                }
            }

            // ARRIBA A LA DERECHA: BOTÓN AJUSTES + CONTADOR DE TIEMPO
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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

                // Botón Ajustes (idéntico al juego points)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E283D))
                        .border(1.5.dp, Color(0x33446699), CircleShape)
                        .clickable { showSettingsDialog = true }
                        .testTag("defend_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ajustes",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Contador de tiempo (idéntico al juego points)
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
                        .testTag("defend_countdown_timer"),
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

        // =====================================================================
        // 4. AVISOS DIRECCIONALES DE ATAQUE LÁSER (SOLO EN MODO LÁSERS)
        // =====================================================================
        for (threat in state.defendThreats) {
            if (threat.threatType == DefendThreatType.LASERS) {
                val isLeft = threat.side == DefendSide.LEFT
                val alertAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentAlignment = alertAlignment
                ) {
                    ThreatWarningBanner(
                        threat = threat,
                        isLeft = isLeft,
                        threatType = threat.threatType
                    )
                }
            }
        }

        // =====================================================================
        // 5. POPUPS FLOTANTES DE PUNTUACIÓN (+15 CAMBIO DE MANO, +10 ESQUIVADO, -1 ❤️)
        // =====================================================================
        for (popup in state.defendPopups) {
            DefendFloatingPopup(
                popup = popup,
                onDismiss = { onDismissPopup(popup.id) },
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }

        // =====================================================================
        // 6. DIÁLOGO GAME OVER (0 VIDAS) O VICTORIA (TIEMPO COMPLETADO)
        // =====================================================================
        if (state.isDefendGameOver || state.isDefendSessionFinished) {
            DefendEndGameDialog(
                isGameOver = state.isDefendGameOver,
                score = state.defendScore,
                shieldCount = state.defendShieldCount,
                threatType = state.defendThreatType,
                onRestart = onRestartDrill,
                onExit = onExitToMain,
                onToggleThreatType = {
                    val nextType = if (state.defendThreatType == DefendThreatType.HANDS) DefendThreatType.LASERS else DefendThreatType.HANDS
                    onSelectThreatType(nextType)
                }
            )
        }
    }
}

// =============================================================================
// SUBCOMPONENTES ESTILIZADOS
// =============================================================================

/**
 * Contador de puntos en la esquina superior izquierda idéntico al juego Points:
 * - Rectángulo redondeado azul marino oscuro (#1E283D) con tipografía atlética grande.
 * - Al sumar nuevos puntos se activa una animación de rebote y desplazamiento,
 *   y cambia al color amarillo/verde neón (#E2FF39).
 * - Tras un instante, regresa fluidamente a su estado original blanco.
 */
@Composable
private fun DefendScoreCounter(
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
        label = "defendTextColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isHitHighlighted) Color(0x88E2FF39) else Color(0x33446699),
        animationSpec = tween(durationMillis = if (isHitHighlighted) 60 else 400, easing = LinearEasing),
        label = "defendBorderColor"
    )

    LaunchedEffect(score) {
        if (score > previousScore) {
            previousScore = score
            isHitHighlighted = true
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
            .testTag("defend_score_counter"),
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
 * Componente visual de amenaza defensiva que renderiza la imagen 'manos.png' (R.drawable.manos).
 * Mecánica:
 * - Emerge desde la parte inferior de la pantalla hacia arriba buscando suavemente la mano/balón.
 * - Sigue la posición del balón mientras el jugador bota.
 * - Si el jugador cambia de mano (crossover) o saca el balón de su trayectoria, la mano desaparece
 *   con un efecto de desvanecimiento y disolución espectral y reaparece una nueva.
 */
@Composable
fun HandsThreatView(
    threat: DefendThreat,
    screenWidth: Dp,
    screenHeight: Dp
) {
    val handWidth = 160.dp
    val handHeight = 280.dp

    // threat.currentY va desde 1.15f (fuera de pantalla inferior) subiendo hacia targetY (altura del balón)
    val handCenterX = screenWidth * threat.currentX
    val handTopY = screenHeight * threat.currentY

    val isFading = threat.isFadingMiss
    val fadeProgress = if (isFading) threat.progress.coerceIn(0f, 1f) else 0f

    val alpha = if (isFading) {
        (1f - fadeProgress).coerceIn(0f, 1f)
    } else {
        0.95f
    }

    val scale = if (isFading) {
        1.0f + (fadeProgress * 0.40f)
    } else {
        1.0f
    }

    // Inclinación dinámica orientada hacia la trayectoria del balón
    val angle = ((threat.targetX - threat.currentX) * 35f).coerceIn(-25f, 25f)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (handCenterX - (handWidth / 2)).roundToPx(),
                    y = (handTopY - (if (isFading) (fadeProgress * 30).dp else 0.dp)).roundToPx()
                )
            }
            .size(width = handWidth, height = handHeight)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                rotationZ = angle
            },
        contentAlignment = Alignment.TopCenter
    ) {
        if (isFading) {
            // Efecto de desvanecimiento y disolución cuando no llega al balón / cambio de mano
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension * 0.38f * (1f + fadeProgress)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xBB00E5FF),
                            Color(0x667C4DFF),
                            Color.Transparent
                        )
                    ),
                    radius = radius,
                    center = Offset(size.width / 2f, size.height * 0.22f)
                )
            }
        }

        Image(
            painter = painterResource(id = R.drawable.manos),
            contentDescription = "Manos defensoras",
            modifier = Modifier
                .fillMaxSize()
                .testTag("threat_hand_${threat.id}")
        )
    }
}

/**
 * Banner de aviso lateral que parpadea anunciando por qué lado viene la amenaza.
 */
@Composable
fun ThreatWarningBanner(
    threat: DefendThreat,
    isLeft: Boolean,
    threatType: DefendThreatType
) {
    val infiniteTransition = rememberInfiniteTransition(label = "banner_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_alpha"
    )

    val isLaser = threatType == DefendThreatType.LASERS
    val bannerColor = if (isLaser) Color(0xFFFF1744) else Color(0xFF7C4DFF)
    val text = if (isLeft) {
        if (isLaser) "⚡ ¡LÁSER POR LA IZQUIERDA!" else "👻 ¡CORTE POR LA IZQUIERDA!"
    } else {
        if (isLaser) "⚡ ¡LÁSER POR LA DERECHA!" else "👻 ¡CORTE POR LA DERECHA!"
    }

    Box(
        modifier = Modifier
            .alpha(alpha)
            .clip(RoundedCornerShape(14.dp))
            .background(bannerColor.copy(alpha = 0.85f))
            .border(1.5.dp, Color.White, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Popup flotante animado para esquivas, escudos corporales y robos.
 */
@Composable
fun DefendFloatingPopup(
    popup: DefendPopup,
    onDismiss: () -> Unit,
    screenWidth: androidx.compose.ui.unit.Dp,
    screenHeight: androidx.compose.ui.unit.Dp
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(popup.id) {
        delay(1100L)
        visible = false
        delay(300L)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        val posX = screenWidth * popup.xNorm
        val posY = screenHeight * popup.yNorm

        Box(
            modifier = Modifier
                .offset { IntOffset(posX.roundToPx(), posY.roundToPx()) }
                .clip(RoundedCornerShape(20.dp))
                .background(if (popup.isBonus) Color(0xEE00E676) else Color(0xEEFF1744))
                .border(1.5.dp, Color.White, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = popup.text,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Diálogo de fin de partida (Game Over o Victoria).
 */
@Composable
fun DefendEndGameDialog(
    isGameOver: Boolean,
    score: Int,
    shieldCount: Int,
    threatType: DefendThreatType,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    onToggleThreatType: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0F172A))
                .border(
                    2.dp,
                    if (isGameOver) Color(0xFFFF1744) else Color(0xFF00E676),
                    RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isGameOver) "💀 ¡FIN DE PARTIDA!" else "🏆 ¡ZONA DEFENDIDA!",
                    color = if (isGameOver) Color(0xFFFF1744) else Color(0xFF00E676),
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isGameOver)
                        "Los defensores fantasma te han robado las 3 vidas. ¡Entrena el bote de protección y el uso del cuerpo como escudo!"
                    else
                        "¡Has aguantado el tiempo completo protegiendo tu bote y esquivando los cortes!",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                // Resumen de estadísticas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PUNTOS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$score", color = Color(0xFFFFD600), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ESCUDOS PRO", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$shieldCount", color = Color(0xFFE040FB), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AMENAZA", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(threatType.icon, fontSize = 20.sp)
                    }
                }

                // Botón cambiar tipo de amenaza
                Button(
                    onClick = onToggleThreatType,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CAMBIAR A ${if (threatType == DefendThreatType.HANDS) "⚡ LÁSERS" else "🖐️ MANOS"}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Botón Reintentar
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGameOver) Color(0xFFFF1744) else Color(0xFF00E676)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "VOLVER A JUGAR",
                        color = if (isGameOver) Color.White else Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                // Botón Salir
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SALIR AL MENÚ PRINCIPAL", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =============================================================================
// DIBUJO GRÁFICO EN CANVAS: LÁSERS Y MANOS FANTASMA
// =============================================================================

fun DrawScope.drawLaserThreat(
    threat: DefendThreat,
    isLeft: Boolean,
    targetYPx: Float,
    widthPx: Float,
    heightPx: Float,
    glowAlpha: Float
) {
    val startXPx = if (isLeft) 0f else widthPx
    val maxReachPx = widthPx * 0.52f
    val currentReachPx = if (threat.isTelegraph) {
        widthPx * 0.18f
    } else {
        (widthPx * 0.18f) + (threat.progress * (maxReachPx - widthPx * 0.18f))
    }

    val endXPx = if (isLeft) currentReachPx else (widthPx - currentReachPx)

    // Base del cañón emisor de láser en el lateral
    val cannonWidth = 24f
    val cannonHeight = 60f
    val cannonX = if (isLeft) 0f else (widthPx - cannonWidth)
    drawRoundRect(
        color = Color(0xFF263238),
        topLeft = Offset(cannonX, targetYPx - cannonHeight / 2f),
        size = Size(cannonWidth, cannonHeight),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Luz guía del cañón
    drawCircle(
        color = Color(0xFFFF1744),
        radius = 8f,
        center = Offset(if (isLeft) cannonWidth else (widthPx - cannonWidth), targetYPx)
    )

    if (threat.isTelegraph) {
        // Línea de aviso punteada / telegrafiada
        drawLine(
            color = Color(0xFFFF5252).copy(alpha = glowAlpha * 0.65f),
            start = Offset(startXPx, targetYPx),
            end = Offset(endXPx, targetYPx),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    } else {
        // Haz de láser activo de alta energía con resplandor
        // Halo exterior
        drawLine(
            color = Color(0xFFFF1744).copy(alpha = 0.35f * glowAlpha),
            start = Offset(startXPx, targetYPx),
            end = Offset(endXPx, targetYPx),
            strokeWidth = 28f,
            cap = StrokeCap.Round
        )
        // Núcleo medio
        drawLine(
            color = Color(0xFFFF5252).copy(alpha = 0.85f),
            start = Offset(startXPx, targetYPx),
            end = Offset(endXPx, targetYPx),
            strokeWidth = 12f,
            cap = StrokeCap.Round
        )
        // Núcleo blanco caliente
        drawLine(
            color = Color.White,
            start = Offset(startXPx, targetYPx),
            end = Offset(endXPx, targetYPx),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // Punta del haz con chispa de energía
        drawCircle(
            color = Color.White,
            radius = 12f,
            center = Offset(endXPx, targetYPx)
        )
        drawCircle(
            color = Color(0xFFFF1744).copy(alpha = glowAlpha),
            radius = 22f,
            center = Offset(endXPx, targetYPx)
        )
    }
}
