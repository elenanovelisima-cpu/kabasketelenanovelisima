package com.example.vision

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

/**
 * HUD principal para el minijuego "Dribble Combo & Crossover" (LV3, Slick Moves y Humo Comic en los pies).
 * Basado en la interfaz de HomeCourt / DribbleUp:
 * - Marcador grande arriba a la izquierda con dígitos amarillo neón (#E2FF39).
 * - Cronómetro arriba a la derecha con botón de ajustes previo.
 * - Medidor vertical de nivel "LV3" a la derecha con llenado dinámico por cadencia.
 * - Nubes de humo estilo dibujo animado / cómic ("humo estilo dibujo") en la zona de los pies al cambiar de mano.
 * - Banner espectacular "SLICK MOVES" / combos con tipografía cómic inclinada.
 * - Popups flotantes +3, +5, +6 PTS.
 */
@Composable
fun DribbleComboHUD(
    state: VisionState,
    onTriggerCrossover: () -> Unit,
    onDismissPopup: (Long) -> Unit,
    onDismissSmokePuff: (Long) -> Unit,
    onRestartDrill: () -> Unit,
    onExitToMain: () -> Unit,
    onToggleShowSkeleton: () -> Unit,
    onToggleShowHoop: () -> Unit,
    onToggleShowBall: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleRecording: () -> Unit,
    onRecalibrate: () -> Unit,
    onToggleShowFps: () -> Unit,
    onSelectDribbleCombo: () -> Unit = {},
    onSelectReactionPoints: () -> Unit = {},
    onSelectDefendZone: () -> Unit = {},
    onSelectShooting: () -> Unit = {},
    onSelectUploadVideo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    val remaining = state.dribbleTimerRemainingSec
    val timerFormatted = String.format("%02d:%02d", remaining / 60, remaining % 60)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tocar en la pantalla permite probar o simular un cambio de mano y ver todas las animaciones
                onTriggerCrossover()
            }
            .testTag("dribble_combo_hud")
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // 0. DETECCIÓN DE ESQUELETO Y BALÓN
        DetectionOverlay(state = state)

        // 1. NUBES DE HUMO ESTILO DIBUJO ANIMADO EN LOS PIES ("humo estilo dibujo")
        state.dribbleSmokePuffs.forEach { smoke ->
            CartoonFootSmokePuffView(
                smoke = smoke,
                screenWidth = screenWidth.value,
                screenHeight = screenHeight.value,
                onDismiss = { onDismissSmokePuff(smoke.id) }
            )
        }

        // 2. POPUPS FLOTANTES DE PUNTOS (+3, +5, +6)
        state.dribblePopups.forEach { popup ->
            DribbleComboScorePopupItem(
                popup = popup,
                screenWidth = screenWidth.value,
                screenHeight = screenHeight.value,
                onDismiss = { onDismissPopup(popup.id) }
            )
        }

        // 3. TOP HEADER (Arriba a la izquierda puntuación en amarillo neón, arriba a la derecha ajustes + cronómetro)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Arriba a la izquierda: CONTADOR DE PUNTOS (Estilo cápsula oscura con números amarillo neón 411)
            DribbleNeonScoreCounter(score = state.dribbleScore)

            // Arriba a la derecha: REC INDICATOR (SI GRABA) + BOTÓN AJUSTES + CRONÓMETRO CUENTA REGRESIVA
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

                // Botón Ajustes (antes del contador a su izquierda)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E283D))
                        .border(1.5.dp, Color(0x33446699), CircleShape)
                        .clickable { showSettingsDialog = true }
                        .testTag("dribble_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ajustes",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Badge de cuenta regresiva (Idéntico en altura y estilo, p.ej. 00:28)
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
                        .testTag("dribble_countdown_timer"),
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

        // 4. MEDIDOR VERTICAL DE NIVEL "LV3" (A la derecha de la pantalla, como en la imagen de referencia)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .testTag("dribble_level_meter")
        ) {
            DribbleLevelVerticalMeter(
                level = state.dribbleLevel,
                progress = state.dribbleGaugeProgress
            )
        }

        // 5. BANNER ESPECTACULAR "SLICK MOVES" / COMBO ANNOUNCER (Abajo a la derecha con estilo cómic)
        state.dribbleComboBanner?.let { comboText ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 28.dp, bottom = 85.dp)
                    .testTag("dribble_combo_banner")
            ) {
                SlickMovesComicBanner(text = comboText)
            }
        }

        // 6. MODAL DE FINALIZACIÓN CUANDO TERMINA LA SESIÓN (45 segundos)
        if (state.isDribbleSessionFinished) {
            DribbleFinishedDialog(
                score = state.dribbleScore,
                crossovers = state.dribbleCrossovers,
                level = state.dribbleLevel,
                onRestart = onRestartDrill,
                onExit = onExitToMain
            )
        }
    }

    // 8. DIÁLOGO DE AJUSTES
    if (showSettingsDialog) {
        VisionSettingsDialog(
            state = state,
            onDismiss = { showSettingsDialog = false },
            onToggleShowSkeleton = onToggleShowSkeleton,
            onToggleShowHoop = onToggleShowHoop,
            onToggleShowBall = onToggleShowBall,
            onSetHoopPerspective = {},
            onOpenSavedVideos = {},
            onToggleCamera = onToggleCamera,
            onToggleRecording = onToggleRecording,
            onRecalibrateHoop = onRecalibrate,
            onToggleShowFps = onToggleShowFps,
            onResetSession = {
                onRestartDrill()
                showSettingsDialog = false
            },
            onExitDrill = onExitToMain,
            onSelectDribbleCombo = onSelectDribbleCombo,
            onSelectReactionPoints = onSelectReactionPoints,
            onSelectDefendZone = onSelectDefendZone,
            onSelectShooting = onSelectShooting,
            onSelectUploadVideo = onSelectUploadVideo
        )
    }
}

/**
 * Marcador de puntuación superior con dígitos amarillo neón (#E2FF39) y animación de rebote.
 */
@Composable
private fun DribbleNeonScoreCounter(
    score: Int,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(score) {
        if (score > 0) {
            scaleAnim.animateTo(
                targetValue = 1.18f,
                animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
            )
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .scale(scaleAnim.value)
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1E283D))
            .border(1.5.dp, Color(0x33446699), RoundedCornerShape(18.dp))
            .padding(horizontal = 24.dp)
            .testTag("dribble_score_counter"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$score",
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFE2FF39), // Amarillo neón idéntico al de la foto de referencia
            letterSpacing = (-0.5).sp
        )
    }
}

/**
 * Medidor vertical de nivel (LV1, LV2, LV3) en cápsula de cristal con gradiente cyan/verde neón.
 */
@Composable
private fun DribbleLevelVerticalMeter(
    level: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_meter")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val labelColor = when (level) {
        3 -> Color(0xFF00FFC2)
        2 -> Color(0xFF00E5FF)
        else -> Color.White
    }

    Column(
        modifier = modifier.width(54.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Título de Nivel: "LV3"
        Text(
            text = "LV$level",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = labelColor,
            letterSpacing = 1.sp,
            modifier = Modifier.alpha(if (level == 3) glowAlpha else 1f)
        )

        // Cápsula vertical
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(210.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xCC0D1424))
                .border(
                    width = 2.dp,
                    color = if (level == 3) Color(0x9900FFC2) else Color(0x4400E5FF),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(3.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Barra de progreso interior
            val fillFraction = progress.coerceIn(0.08f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillFraction)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = when (level) {
                                3 -> listOf(Color(0xFF00FFC2), Color(0xFF00E5FF), Color(0xFF0091EA))
                                2 -> listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF1565C0))
                                else -> listOf(Color(0xFF00B0FF), Color(0xFF1E88E5), Color(0xFF0D47A1))
                            }
                        )
                    )
            )

            // Marcas horizontales que delimitan los tercios (LV1, LV2, LV3)
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.width(14.dp).height(1.dp).background(Color(0x55FFFFFF)))
                Box(modifier = Modifier.width(14.dp).height(1.dp).background(Color(0x55FFFFFF)))
            }
        }

        // Multiplicador inferior
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x4400E5FF))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${level}X",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

/**
 * Rótulo dinámico en estilo cómic / graffiti de acción: "SLICK MOVES" (como en la foto de referencia).
 */
@Composable
private fun SlickMovesComicBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(0.4f) }

    LaunchedEffect(text) {
        scaleAnim.snapTo(0.4f)
        scaleAnim.animateTo(
            targetValue = 1.15f,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
        )
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .scale(scaleAnim.value)
            .rotate(-7f) // Ligera inclinación estilo cómic
    ) {
        // Sombra 3D profunda
        Text(
            text = text,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = Color(0xEE0D1424),
            letterSpacing = 2.sp,
            modifier = Modifier.offset(x = 4.dp, y = 5.dp)
        )

        // Borde exterior oscuro
        Text(
            text = text,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = Color(0xFF1E283D),
            letterSpacing = 2.sp,
            modifier = Modifier.offset(x = 2.dp, y = 2.dp)
        )

        // Texto principal blanco puro
        Text(
            text = text,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = Color.White,
            letterSpacing = 2.sp
        )
    }
}

/**
 * Nubes de humo estilo dibujo animado ("humo estilo dibujo") renderizadas dinámicamente en los pies.
 * Simula los efectos clásicos de anime/cómics cuando un jugador clava el pie o hace un cambio explosivo.
 */
@Composable
private fun CartoonFootSmokePuffView(
    smoke: DribbleSmokePuff,
    screenWidth: Float,
    screenHeight: Float,
    onDismiss: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(smoke.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = LinearEasing)
        )
        onDismiss()
    }

    val curProgress = progress.value

    // Escala del humo: se expande rápidamente de 0.25 a 1.28 y luego se expande lentamente
    val scale = when {
        curProgress < 0.25f -> 0.25f + (curProgress / 0.25f) * 1.03f
        else -> 1.28f + (curProgress - 0.25f) * 0.22f
    }

    // Alpha: visible y luego se desvanece
    val alpha = (1f - (curProgress * curProgress)).coerceIn(0f, 1f)

    // Elevación y deriva del humo
    val floatUp = curProgress * 30.dp.value
    val driftX = if (smoke.isLeftFoot) -curProgress * 22.dp.value else curProgress * 22.dp.value

    val targetX = smoke.nx * screenWidth + driftX
    val targetY = smoke.ny * screenHeight - floatUp

    val sizeDp = 90.dp

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (targetX.dp.toPx() - (sizeDp / 2).toPx()).roundToInt(),
                    y = (targetY.dp.toPx() - (sizeDp / 2).toPx()).roundToInt()
                )
            }
            .size(sizeDp)
            .scale(scale)
            .alpha(alpha)
    ) {
        // Dibujamos las nubes de humo estilo cómic con Canvas (círculos agrupados con borde oscuro)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.55f

            val cloudColor = Color(0xF2F8FAFC)
            val shadowColor = Color(0x66CBD5E1)
            val strokeColor = Color(0xDD1E293B)
            val strokeWidth = 3.dp.toPx()

            // Lista de bolitas de humo que forman la nube compuesta
            val puffCircles = listOf(
                Triple(cx - w * 0.22f, cy + h * 0.05f, w * 0.26f),
                Triple(cx + w * 0.20f, cy + h * 0.06f, w * 0.24f),
                Triple(cx - w * 0.08f, cy - h * 0.16f, w * 0.32f),
                Triple(cx + w * 0.12f, cy - h * 0.10f, w * 0.28f),
                Triple(cx - w * 0.36f, cy - h * 0.05f, w * 0.16f), // bolita pequeña de polvo
                Triple(cx + w * 0.36f, cy - h * 0.02f, w * 0.14f)  // bolita pequeña de polvo
            )

            // 1. Capa de relleno base
            puffCircles.forEach { (px, py, radius) ->
                drawCircle(
                    color = shadowColor,
                    radius = radius + 1.dp.toPx(),
                    center = Offset(px, py + 2.dp.toPx())
                )
                drawCircle(
                    color = cloudColor,
                    radius = radius,
                    center = Offset(px, py)
                )
            }

            // 2. Capa de contornos estilo dibujo animado
            puffCircles.forEach { (px, py, radius) ->
                drawCircle(
                    color = strokeColor,
                    radius = radius,
                    center = Offset(px, py),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}

/**
 * Popup flotante de puntuación "+3", "+5" o "+6".
 */
@Composable
private fun DribbleComboScorePopupItem(
    popup: DribblePopup,
    screenWidth: Float,
    screenHeight: Float,
    onDismiss: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(popup.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = LinearEasing)
        )
        onDismiss()
    }

    val curProgress = progress.value

    val scale = when {
        curProgress < 0.20f -> 0.3f + (curProgress / 0.20f) * 0.95f
        curProgress < 0.40f -> 1.25f - ((curProgress - 0.20f) / 0.20f) * 0.25f
        else -> 1.0f - (curProgress - 0.40f) * 0.15f
    }

    val alpha = when {
        curProgress < 0.10f -> curProgress / 0.10f
        curProgress < 0.65f -> 1f
        else -> (1f - ((curProgress - 0.65f) / 0.35f)).coerceIn(0f, 1f)
    }

    val floatUp = curProgress * 95.dp.value
    val posX = (popup.nx * screenWidth).coerceIn(40f, screenWidth - 40f)
    val posY = (popup.ny * screenHeight).coerceIn(60f, screenHeight - 60f) - floatUp

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (posX.dp.toPx() - 60.dp.toPx()).roundToInt(),
                    y = (posY.dp.toPx() - 40.dp.toPx()).roundToInt()
                )
            }
            .size(width = 120.dp, height = 80.dp)
            .scale(scale)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = popup.text,
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.sp,
            modifier = Modifier.shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = Color(0xFF00E5FF),
                spotColor = Color(0xFF00FFC2)
            )
        )
    }
}

/**
 * Modal cuando concluye la sesión de 45 segundos de Dribble Combo.
 */
@Composable
private fun DribbleFinishedDialog(
    score: Int,
    crossovers: Int,
    level: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(26.dp))
                .testTag("dribble_finished_dialog"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101726))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                        .border(2.dp, Color(0xFF00E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⚡", fontSize = 36.sp)
                }

                Text(
                    text = "¡SESIÓN COMPLETADA!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                // Puntuación Final
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF182236))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "PUNTUACIÓN TOTAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90A4AE),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$score",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE2FF39)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "NIVEL MÁXIMO", fontSize = 10.sp, color = Color(0xFF90A4AE))
                            Text(text = "LV$level", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF00FFC2))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "CROSSOVERS", fontSize = 10.sp, color = Color(0xFF90A4AE))
                            Text(text = "$crossovers", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }

                // Botones
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                            Text(text = "ENTRENAR DE NUEVO", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 14.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color(0x66FFFFFF), Color(0x66FFFFFF))))
                    ) {
                        Text(text = "SALIR AL MENÚ", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
