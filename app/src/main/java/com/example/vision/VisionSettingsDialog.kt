package com.example.vision

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Colores del tema juvenil / deportivo (idénticos al popup y a la home)
private val DarkBgColor = Color(0xFF141416)           // Negro carbono puro del popup
private val CardBgColor = Color(0xFF1D1D22)           // Tarjeta oscura del popup
private val CardBorderColor = Color(0xFF282830)       // Borde sutil
private val ActionOrange = Color(0xFFEA580C)          // Naranja deportivo inicial / botón PRO
private val TextWhite = Color.White
private val TextMuted = Color(0xFF9E9EA8)

@Composable
fun VisionSettingsDialog(
    state: VisionState,
    onDismiss: () -> Unit,
    onToggleShowSkeleton: () -> Unit,
    onToggleShowHoop: () -> Unit,
    onToggleShowBall: () -> Unit,
    onSetHoopPerspective: (HoopPerspective) -> Unit,
    onOpenSavedVideos: () -> Unit,
    onRecalibrateHoop: () -> Unit = {},
    onResetSession: () -> Unit = {},
    onToggleShowFps: () -> Unit = {},
    onToggleCamera: () -> Unit = {},
    onAlignCourtPoints: () -> Unit = {},
    onToggleRecording: () -> Unit = {},
    onExitDrill: (() -> Unit)? = null,
    onSelectDribbleCombo: (() -> Unit)? = null,
    onSelectReactionPoints: (() -> Unit)? = null,
    onSelectDefendZone: (() -> Unit)? = null,
    onSelectKidsMiniBasket: (() -> Unit)? = null,
    onSelectShooting: (() -> Unit)? = null,
    onSelectUploadVideo: (() -> Unit)? = null,
    onSelectThreatType: (DefendThreatType) -> Unit = {}
) {
    // Determine active training context
    val isShootingMode = !state.isDribbleMode && !state.isReactionPointsMode && !state.isDefendZoneMode && !state.isKidsMiniBasketMode
    val isDefendMode = state.isDefendZoneMode
    val isReactionMode = state.isReactionPointsMode
    val isDribbleMode = state.isDribbleMode
    val isKidsMode = state.isKidsMiniBasketMode

    var showModeSelector by remember { mutableStateOf(false) }

    if (showModeSelector) {
        ModeSelectionDialog(
            currentModeIsDribble = state.isDribbleMode,
            currentModeIsReaction = state.isReactionPointsMode,
            currentModeIsDefend = state.isDefendZoneMode,
            currentModeIsKids = state.isKidsMiniBasketMode,
            onDismiss = { showModeSelector = false },
            onSelectKidsMiniBasket = {
                showModeSelector = false
                onDismiss()
                onSelectKidsMiniBasket?.invoke()
            },
            onSelectDribbleCombo = {
                showModeSelector = false
                onDismiss()
                onSelectDribbleCombo?.invoke()
            },
            onSelectReactionPoints = {
                showModeSelector = false
                onDismiss()
                onSelectReactionPoints?.invoke()
            },
            onSelectDefendZone = {
                showModeSelector = false
                onDismiss()
                onSelectDefendZone?.invoke()
            },
            onSelectShooting = {
                showModeSelector = false
                onDismiss()
                onSelectShooting?.invoke()
            },
            onSelectUploadVideo = {
                showModeSelector = false
                onDismiss()
                onSelectUploadVideo?.invoke()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(28.dp))
                .background(DarkBgColor)
                .padding(horizontal = 20.dp, vertical = 22.dp)
                .testTag("vision_settings_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. DIALOG HEADER (Estilo juvenil, sin estética IA)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF26262B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = ActionOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            val modeBadge = when {
                                isDefendMode -> "DEFEND THE ZONE"
                                isReactionMode -> "REACTION DRILL"
                                isDribbleMode -> "CONTROL DE BOTE"
                                else -> "SESIÓN DE TIRO"
                            }
                            Text(
                                text = modeBadge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = ActionOrange,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "AJUSTES",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 0.5.sp,
                                color = TextWhite
                            )
                        }
                    }

                    // Botón cerrar (X) idéntico al popup
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF26262B))
                            .clickable { onDismiss() }
                            .testTag("close_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFFD0D0D5),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. SCROLLABLE CONTENT BODY
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // SECCIÓN: MINIJUEGOS Y MODOS
                    if (onSelectDribbleCombo != null) {
                        SettingsSectionTitle(title = "MODOS DE JUEGO")

                        SettingActionCard(
                            title = "Cambiar de Minijuego",
                            subtitle = "Defend the Zone, Dribble Combo, Reaction Points o Tiro",
                            icon = Icons.Default.SportsEsports,
                            actionLabel = "ELEGIR ›",
                            testTag = "setting_switch_mode_button",
                            onClick = { showModeSelector = true }
                        )
                    }

                    // SECCIÓN: GRABACIÓN Y VÍDEO
                    if (state.inputMode == InputMode.LIVE_CAMERA) {
                        SettingsSectionTitle(title = "VÍDEO Y GRABACIÓN")

                        SettingActionCard(
                            title = if (state.isRecordingLive) "Detener Grabación" else "Grabar Sesión en Vídeo",
                            subtitle = if (state.isRecordingLive)
                                "Grabando: ${state.recordingDurationSec}s"
                            else
                                "Guarda el entrenamiento en tu galería",
                            icon = if (state.isRecordingLive) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            actionLabel = if (state.isRecordingLive) "DETENER" else "GRABAR",
                            actionHighlighted = state.isRecordingLive,
                            testTag = "setting_record_live_button",
                            onClick = onToggleRecording
                        )
                    }

                    // Mis Vídeos Guardados
                    SettingActionCard(
                        title = "Mis Vídeos Guardados",
                        subtitle = "${state.savedVideos.size} vídeos disponibles",
                        icon = Icons.Default.VideoLibrary,
                        actionLabel = "VER ›",
                        testTag = "setting_open_saved_videos_button",
                        onClick = {
                            onDismiss()
                            onOpenSavedVideos()
                        }
                    )

                    // SECCIÓN: CÁMARA Y DETECCIÓN
                    SettingsSectionTitle(title = "CÁMARA Y SENSORES")

                    // Selector Cámara Frontal / Trasera
                    if (state.inputMode == InputMode.LIVE_CAMERA) {
                        SettingActionCard(
                            title = "Cambiar Cámara",
                            subtitle = if (state.useFrontCamera) "Cámara Frontal (Selfie)" else "Cámara Trasera (Pista)",
                            icon = Icons.Default.Cameraswitch,
                            actionLabel = if (state.useFrontCamera) "FRONTAL" else "TRASERA",
                            testTag = "setting_toggle_camera",
                            onClick = onToggleCamera
                        )
                    }

                    // Toggle: Esqueleto del Jugador
                    SettingToggleCard(
                        title = "Postura y Esqueleto",
                        subtitle = "Detecta brazos, muñecas y posición corporal",
                        checked = state.showSkeleton,
                        testTag = "setting_toggle_skeleton",
                        onToggle = onToggleShowSkeleton
                    )

                    // Toggle: Seguimiento del Balón
                    SettingToggleCard(
                        title = "Seguimiento del Balón",
                        subtitle = "Indicador de posición del balón en pista",
                        checked = state.showBall,
                        testTag = "setting_toggle_ball",
                        onToggle = onToggleShowBall
                    )

                    // Toggle: Contador de FPS
                    SettingToggleCard(
                        title = "Contador de FPS",
                        subtitle = "Muestra fotogramas por segundo en pantalla",
                        checked = state.showFps,
                        testTag = "setting_toggle_fps",
                        onToggle = onToggleShowFps
                    )

                    // Opción de Recalibrar Jugador y Balón
                    if (isReactionMode || isDribbleMode) {
                        SettingActionCard(
                            title = "Calibrar Jugador y Balón",
                            subtitle = if (state.skeletonCalibrated && state.ballCalibrated)
                                "Calibración lista ✓"
                            else
                                "Ajustar postura y detección de balón",
                            icon = Icons.Default.CropFree,
                            actionLabel = "CALIBRAR ›",
                            testTag = "setting_recalibrate_front_drills_button",
                            onClick = {
                                onDismiss()
                                onRecalibrateHoop()
                            }
                        )
                    }

                    // SECCIÓN: CANASTA Y PISTA
                    if (isShootingMode) {
                        SettingsSectionTitle(title = "CANASTA Y PISTA")

                        // Capa: Aro y Canasta
                        SettingToggleCard(
                            title = "Aro y Canasta",
                            subtitle = "Detección de canastas y red",
                            checked = state.showHoop,
                            testTag = "setting_toggle_hoop",
                            onToggle = onToggleShowHoop
                        )

                        // Selector de Perspectiva 3D del Aro
                        if (state.showHoop) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardBgColor)
                                    .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "PERSPECTIVA DE LA CANASTA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ActionOrange
                                )
                                Text(
                                    text = "Inclinación del tablero según ángulo de cámara:",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )

                                val currentPerspective = state.lockedHoop?.perspective ?: HoopPerspective.AUTO
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HoopPerspective.values().forEach { persp ->
                                        val isSelected = currentPerspective == persp
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("setting_perspective_${persp.name}")
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) ActionOrange else Color(0xFF26262B))
                                                .border(
                                                    1.dp,
                                                    if (isSelected) ActionOrange else CardBorderColor,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { onSetHoopPerspective(persp) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = persp.shortLabel,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) TextWhite else Color(0xFFD0D0D5)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Calibrar Aro
                        SettingActionCard(
                            title = "Calibrar Aro",
                            subtitle = "Ajustar posición de la canasta",
                            icon = Icons.Default.CropFree,
                            actionLabel = "CALIBRAR ›",
                            testTag = "setting_recalibrate_button",
                            onClick = {
                                onDismiss()
                                onRecalibrateHoop()
                            }
                        )

                        // Alinear Pista 3PT
                        SettingActionCard(
                            title = "Alinear Líneas de Pista",
                            subtitle = "Línea de 3 y zona de tiro",
                            icon = Icons.Default.Tune,
                            actionLabel = "ALINEAR ›",
                            testTag = "setting_align_court_button",
                            onClick = {
                                onDismiss()
                                onAlignCourtPoints()
                            }
                        )
                    }

                    // SECCIÓN: DEFENSOR Y AMENAZAS
                    if (isDefendMode) {
                        SettingsSectionTitle(title = "DEFENSA Y AMENAZA")

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBgColor)
                                .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "TIPO DE DEFENSOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ActionOrange
                            )
                            Text(
                                text = "Elige si esquivas manos o haces de láser:",
                                fontSize = 11.sp,
                                color = TextMuted
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DefendThreatType.values().forEach { type ->
                                    val isSelected = state.defendThreatType == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) ActionOrange else Color(0xFF26262B))
                                            .border(
                                                1.dp,
                                                if (isSelected) ActionOrange else CardBorderColor,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onSelectThreatType(type) }
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = type.icon,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (type == DefendThreatType.HANDS) "MANOS" else "LÁSER",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TextWhite
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECCIÓN: ACCIONES RÁPIDAS
                    SettingsSectionTitle(title = "ACCIONES")

                    val resetTitle = when {
                        isReactionMode -> "Reiniciar Reto"
                        isDribbleMode -> "Reiniciar Bote"
                        else -> "Reiniciar Tiros"
                    }
                    val resetSubtitle = when {
                        isReactionMode -> "Reiniciar el temporizador de 60s"
                        isDribbleMode -> "Poner a cero la puntuación y racha"
                        else -> "Poner a cero aciertos y fallos"
                    }

                    SettingActionCard(
                        title = resetTitle,
                        subtitle = resetSubtitle,
                        icon = Icons.Default.Refresh,
                        actionLabel = "REINICIAR",
                        testTag = "setting_reset_session_button",
                        onClick = {
                            onDismiss()
                            onResetSession()
                        }
                    )

                    // Salir al Menú Principal
                    if (onExitDrill != null) {
                        SettingActionCard(
                            title = "Salir del Entrenamiento",
                            subtitle = "Volver a la pantalla principal",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            actionLabel = "SALIR",
                            actionHighlighted = true,
                            testTag = "setting_exit_drill_button",
                            onClick = {
                                onDismiss()
                                onExitDrill()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 3. DIALOG FOOTER: BOTÓN NARANJA DE LA PÁGINA INICIAL
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActionOrange
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settings_done_button")
                ) {
                    Text(
                        text = "GUARDAR Y CONTINUAR",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = TextWhite
                    )
                }
            }
        }
    }
}

/**
 * Título de sección discreto y limpio
 */
@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        color = Color(0xFF6B7280),
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

/**
 * Tarjeta de acción limpia, fondo negro (#1D1D22), tipografía blanca y detalles en naranja
 */
@Composable
private fun SettingActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionLabel: String,
    testTag: String,
    onClick: () -> Unit,
    actionHighlighted: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBgColor)
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF26262B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (actionHighlighted) Color(0xFFEF4444) else ActionOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Pastilla de acción
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (actionHighlighted) Color(0xFFEF4444).copy(alpha = 0.18f)
                        else ActionOrange.copy(alpha = 0.16f)
                    )
                    .border(
                        1.dp,
                        if (actionHighlighted) Color(0xFFEF4444).copy(alpha = 0.4f)
                        else ActionOrange.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (actionHighlighted) Color(0xFFEF4444) else ActionOrange,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Tarjeta de conmutador (Switch) con diseño sobrio y juvenil
 */
@Composable
private fun SettingToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBgColor)
            .border(
                1.dp,
                if (checked) ActionOrange.copy(alpha = 0.4f) else CardBorderColor,
                RoundedCornerShape(14.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextWhite,
                    checkedTrackColor = ActionOrange,
                    uncheckedThumbColor = Color(0xFF8E8E98),
                    uncheckedTrackColor = Color(0xFF26262B)
                )
            )
        }
    }
}
