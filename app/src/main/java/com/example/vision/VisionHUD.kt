package com.example.vision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theme.SportBorder
import com.example.theme.SportError
import com.example.theme.SportMuted
import com.example.theme.SportOnBrand
import com.example.theme.SportOnSurface
import com.example.theme.SportOrange
import com.example.theme.SportSuccess
import com.example.theme.SportWarning

@Composable
fun PulseDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

@Composable
fun VisionHUD(
    state: VisionState,
    cameraReady: Boolean,
    debug: Boolean,
    onToggleDebug: () -> Unit,
    onReset: () -> Unit,
    onRecalibrateHoop: () -> Unit,
    onSimulateShot: (Boolean) -> Unit,
    onToggleRecording: () -> Unit,
    onPickVideo: () -> Unit,
    onAnalyzeRecordedVideo: () -> Unit,
    onDismissVideoBanner: () -> Unit,
    onToggleHoopVisual: () -> Unit = {},
    onToggleTacticalMode: () -> Unit = {},
    onShowTacticalReport: () -> Unit = {},
    onSimulateTactical: () -> Unit = {},
    onOpenSavedVideos: () -> Unit = {},
    onReopenOnboarding: () -> Unit = {},
    onTriggerDribbleCrossover: () -> Unit = {},
    onDismissDribblePopup: (Long) -> Unit = {},
    onToggleDribbleMode: () -> Unit = {},
    onToggleShowSkeleton: () -> Unit = {},
    onToggleShowHoop: () -> Unit = {},
    onToggleShowBall: () -> Unit = {},
    onCycleHoopPerspective: () -> Unit = {},
    onSetHoopPerspective: (HoopPerspective) -> Unit = {},
    onToggleShowFps: () -> Unit = {},
    onToggleCamera: () -> Unit = {},
    onOpenCourtAlignment: () -> Unit = {},
    onCourtPointMoved: (String, Float, Float) -> Unit = { _, _, _ -> },
    onCourtPresetApplied: (CourtPreset) -> Unit = {},
    onCourtReset: () -> Unit = {},
    onCloseCourtAlignment: () -> Unit = {},
    onSelectDribbleCombo: () -> Unit = {},
    onSelectReactionPoints: () -> Unit = {},
    onSelectDefendZone: () -> Unit = {},
    onSelectKidsMiniBasket: () -> Unit = {},
    onSelectShooting: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showModeSelectionDialog by remember { mutableStateOf(false) }
    var isSoundMuted by remember { mutableStateOf(false) }
    var showStopConfirmDialog by remember { mutableStateOf(false) }

    if (showStopConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmDialog = false },
            title = {
                Text(
                    text = "FINALIZAR ENTRENAMIENTO",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "¿Deseas finalizar la sesión actual de tiro y regresar al menú principal?",
                    fontSize = 14.sp,
                    color = Color(0xCCFFFFFF)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirmDialog = false
                        onReopenOnboarding()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE50914)
                    )
                ) {
                    Text("FINALIZAR", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStopConfirmDialog = false }
                ) {
                    Text("CONTINUAR", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                }
            },
            containerColor = Color(0xFF1E283D),
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showModeSelectionDialog) {
        ModeSelectionDialog(
            currentModeIsDribble = state.isDribbleMode,
            currentModeIsReaction = state.isReactionPointsMode,
            currentModeIsDefend = state.isDefendZoneMode,
            currentModeIsKids = state.isKidsMiniBasketMode,
            onDismiss = { showModeSelectionDialog = false },
            onSelectKidsMiniBasket = onSelectKidsMiniBasket,
            onSelectDribbleCombo = onSelectDribbleCombo,
            onSelectReactionPoints = onSelectReactionPoints,
            onSelectDefendZone = onSelectDefendZone,
            onSelectShooting = onSelectShooting,
            onSelectUploadVideo = onPickVideo
        )
    }

    if (showSettingsDialog) {
        VisionSettingsDialog(
            state = state,
            onDismiss = { showSettingsDialog = false },
            onToggleShowSkeleton = onToggleShowSkeleton,
            onToggleShowHoop = onToggleShowHoop,
            onToggleShowBall = onToggleShowBall,
            onSetHoopPerspective = onSetHoopPerspective,
            onOpenSavedVideos = onOpenSavedVideos,
            onRecalibrateHoop = onRecalibrateHoop,
            onResetSession = onReset,
            onToggleShowFps = onToggleShowFps,
            onToggleCamera = onToggleCamera,
            onAlignCourtPoints = onOpenCourtAlignment,
            onToggleRecording = onToggleRecording,
            onExitDrill = onReopenOnboarding,
            onSelectDribbleCombo = onSelectDribbleCombo,
            onSelectReactionPoints = onSelectReactionPoints,
            onSelectDefendZone = onSelectDefendZone,
            onSelectKidsMiniBasket = onSelectKidsMiniBasket,
            onSelectShooting = onSelectShooting,
            onSelectUploadVideo = onPickVideo
        )
    }

    val minutes = state.sessionDurationSec / 60
    val seconds = state.sessionDurationSec % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val recMin = state.recordingDurationSec / 60
    val recSec = state.recordingDurationSec % 60
    val recFormatted = String.format("%02d:%02d", recMin, recSec)

    Box(modifier = modifier.fillMaxSize()) {
        // Edge scrims
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xCC000000), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000))
                    )
                )
        )

        // Always render skeleton, locked hoop, and trajectory overlay
        DetectionOverlay(state = state)

        // Dribble Score popups animation (+5 points effect when changing hands)
        if (state.isDribbleMode && state.dribblePopups.isNotEmpty()) {
            DribbleScorePopupOverlay(
                popups = state.dribblePopups,
                onPopupExpired = onDismissDribblePopup
            )
        }

        // HUD Content Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Botón de sonido a la izquierda + Contador de tiempo, STOP y Ajustes a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Botón de sonido (arriba a la izquierda, idéntico a la foto de referencia)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0x992B2B2B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { isSoundMuted = !isSoundMuted }
                            .testTag("shooting_sound_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (!isSoundMuted) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isSoundMuted) "Activar sonido" else "Silenciar sonido",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // REC indicator when recording
                    if (state.isRecordingLive) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xDDF44336))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PulseDot(color = Color.White)
                            Text(
                                text = "REC $recFormatted",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    // FPS badge (activable desde Ajustes por petición del usuario)
                    if (state.showFps && state.fps > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xCC111827))
                                .border(1.dp, Color(0x4400E676), RoundedCornerShape(999.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (state.fps >= 24) Color(0xFF00E676) else Color(0xFFFFB300))
                            )
                            Text(
                                text = "${state.fps} FPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Right: [Contador de tiempo (Points style)]  [Botón STOP (rojo)]  [Icono Ajustes]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Contador de tiempo: el mismo contador de tiempo que en el juego de points
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E283D))
                            .border(
                                1.5.dp,
                                Color(0x33446699),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 18.dp)
                            .testTag("shooting_countdown_timer"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeFormatted,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }

                    // 2. Botón de STOP: colocado arriba a la derecha antes del botón de ajustes
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE50914))
                            .border(1.dp, Color(0x44FFFFFF), CircleShape)
                            .clickable { showStopConfirmDialog = true }
                            .testTag("shooting_stop_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "STOP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // 3. Icono de Ajustes
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E283D))
                            .border(1.5.dp, Color(0x33446699), CircleShape)
                            .clickable { showSettingsDialog = true }
                            .testTag("vision_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // NOTIFICATION BANNER: When a recorded video is ready to analyze or view in Mis Videos
            AnimatedVisibility(
                visible = state.showRecordedVideoBanner,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xEE1E1E24))
                        .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡ENTRENAMIENTO GUARDADO EN MIS VÍDEOS!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF)
                            )
                            Text(
                                text = "Guardado con tus estadísticas. Toca para analizar ahora o consúltalo en Mis Vídeos.",
                                fontSize = 11.sp,
                                color = SportOnSurface
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .testTag("analyze_now_button")
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFF00E5FF))
                                    .clickable { onAnalyzeRecordedVideo() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "ANALIZAR AHORA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                                    .clickable { onDismissVideoBanner() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = SportOnSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // MIDDLE SECTION: Debug info if enabled
            if (debug) {
                Column(
                    modifier = Modifier
                        .testTag("debug_panel")
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xCC000000))
                        .border(1.dp, SportBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(text = "ESQUELETO: ${if (state.skeleton != null) "DETECTADO (${state.skeleton.landmarks.size} pts)" else "BUSCANDO"}", fontSize = 11.sp, color = Color(0xFF9C27B0), fontWeight = FontWeight.Bold)
                    Text(text = "BALÓN: ${state.ball?.let { String.format("%.2f", it.conf) } ?: "—"}", fontSize = 11.sp, color = SportOrange, fontWeight = FontWeight.Bold)
                    Text(text = "ARO: ${if (state.lockedHoop?.isLocked == true) "BLOQUEADO" else state.hoop?.let { String.format("%.2f", it.conf) } ?: "—"}", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    Text(text = "ESTADO TIRO: ${state.detectorState.label} | ÚLTIMO: ${state.lastEvent}", fontSize = 11.sp, color = SportOnSurface, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }

            if (state.isTacticalMode) {
                // TACTICAL FLOATING CONTROL AND 2D COURT RADAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xDD121218))
                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tactical Court Radar (2D Homography View)
                    TacticalCourtMap(
                        analysis = state.currentTacticalAnalysis,
                        modifier = Modifier
                            .width(170.dp)
                            .height(110.dp)
                    )

                    // Tactical Play & Report Controls
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Play badge
                        val activePlay = state.currentTacticalAnalysis?.activePlayBadge
                        val activeDesc = state.currentTacticalAnalysis?.activePlayDescription ?: "Analizando posicionamiento táctico..."

                        if (activePlay != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(activePlay.badgeColor))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = activePlay.title,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Text(
                            text = activeDesc,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xEEFFFFFF),
                            maxLines = 2
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Demo Simulator button
                            Box(
                                modifier = Modifier
                                    .testTag("simulate_tactical_button")
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (state.isSimulatingTactical) SportOrange else Color(0x3300E5FF))
                                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                                    .clickable { onSimulateTactical() }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (state.isSimulatingTactical) "SIMULANDO..." else "DEMO PATRONES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (state.isSimulatingTactical) Color.Black else Color(0xFF00E5FF)
                                )
                            }

                            // View Report button
                            Box(
                                modifier = Modifier
                                    .testTag("show_tactical_report_button")
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF00E5FF))
                                    .clickable { onShowTacticalReport() }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "VER INFORME",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }

            // BOTTOM BAR: Scoreboard (Center) + Controls (Left) + Court Map / Dribble Badge (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left Controls (Clean layout: calibration moved to Settings; test buttons removed)
                if (state.isDribbleMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // In Dribble Mode: Button to simulate crossover (+5 points popup effect!)
                        Box(
                            modifier = Modifier
                                .testTag("simulate_dribble_crossover_button")
                                .clip(RoundedCornerShape(999.dp))
                                .background(SportOrange)
                                .clickable { onTriggerDribbleCrossover() }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "⚡", fontSize = 14.sp)
                                Text(
                                    text = "SIMULAR CAMBIO (+5)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Reset Session Button
                        Box(
                            modifier = Modifier
                                .testTag("reset_button")
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                                .border(1.dp, SportBorder, CircleShape)
                                .clickable { onReset() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar",
                                tint = SportOnSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    // Empty placeholder matching CourtShotMap width (175.dp) to keep the Center Scoreboard perfectly centered
                    Box(modifier = Modifier.width(175.dp))
                }

                // Center Scoreboard
                if (state.isDribbleMode) {
                    // Scoreboard Modo Bote / Dribble AI
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.testTag("dribble_scoreboard")
                    ) {
                        Text(
                            text = "CONTROL DE BOTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = SportOrange
                        )

                        Text(
                            text = "${state.dribbleScore} PTS",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )

                        Text(
                            text = "${state.dribbleCrossovers} cambios de mano  •  Racha 🔥 x${state.dribbleStreak}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = Color(0xCCFFFFFF)
                        )
                    }
                } else {
                    // Scoreboard Modo Tiro en Canasta (Estética exacta de la foto de referencia)
                    ShootingScoreboardMinimal(
                        makes = state.makes,
                        attempts = state.attempts,
                        modifier = Modifier.testTag("shooting_scoreboard")
                    )
                }

                // Right: Mini Court Shot Chart en modo tiro; en modo bote se quita el badge a petición del usuario
                if (!state.isDribbleMode) {
                    CourtShotMap(shots = state.courtShots)
                } else {
                    // En modo bote se quita el badge inferior derecho ("MODO BOTE / SIN CANASTA")
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }
        }

        // Overlay: Re-alinear puntos de la pista desde Ajustes o HUD
        if (state.showCourtPointSelector) {
            CourtPointSelectorView(
                courtCalibration = state.courtCalibration,
                onPointMoved = onCourtPointMoved,
                onApplyPreset = onCourtPresetApplied,
                onReset = onCourtReset,
                onConfirm = onCloseCourtAlignment,
                onSkip = onCloseCourtAlignment
            )
        }
    }
}

/**
 * Scoreboard inferior central con la estética exacta de la foto de referencia:
 * Números grandes destacados, línea divisoria horizontal y textos MAKE / ATTEMPT,
 * divididos por una línea vertical sutil.
 */
@Composable
private fun ShootingScoreboardMinimal(
    makes: Int,
    attempts: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        // Columna MAKE
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$makes",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 44.sp
            )
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(1.5.dp)
                    .background(Color(0xB3FFFFFF))
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "MAKE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xCCFFFFFF)
            )
        }

        // Línea divisoria vertical
        Box(
            modifier = Modifier
                .height(58.dp)
                .width(1.5.dp)
                .background(Color(0xB3FFFFFF))
        )

        // Columna ATTEMPT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$attempts",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 44.sp
            )
            Box(
                modifier = Modifier
                    .width(74.dp)
                    .height(1.5.dp)
                    .background(Color(0xB3FFFFFF))
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ATTEMPT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xCCFFFFFF)
            )
        }
    }
}
