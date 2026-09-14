package com.example.vision

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * HUD y Sistema de Calibración Infantil para "Kids Mini Basket Arcade".
 * 
 * Especialmente diseñado para jugar en casa con canastas infantiles y balones pequeños/gomaespuma:
 * - Sin mapa táctico de pista ni esqueleto pesado.
 * - Calibración guiada por foto fija: Ubicación exacta del aro y muestra de color/tamaño del balón.
 * - Marcadores arcade de alta visibilidad, efectos de resplandor, confeti y sonido eufórico.
 */
@Composable
fun KidsMiniBasketHUD(
    state: VisionState,
    frozenBitmap: Bitmap?,
    onAdvanceToPlacePhone: () -> Unit,
    onTakePhoto: () -> Unit,
    onBackToScanBall: () -> Unit = {},
    onBackToPlacePhone: () -> Unit = {},
    onUpdateHoopPosition: (Float, Float) -> Unit,
    onUpdateHoopRadius: (Float) -> Unit,
    onConfirmHoopAndStart: () -> Unit,
    onRestartSession: () -> Unit,
    onRecalibrate: () -> Unit,
    onManualScoreBasket: () -> Unit,
    onExitToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCalibrating = state.kidsCalibrationStep != KidsCalibrationStep.COMPLETED

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("kids_mini_basket_screen")
    ) {
        if (isCalibrating) {
            // ==========================================
            // FLUJO DE CALIBRACIÓN INFANTIL (3 PASOS)
            // ==========================================
            KidsCalibrationView(
                step = state.kidsCalibrationStep,
                scanProgress = state.kidsBallScanProgress,
                paletteColors = state.kidsBallPaletteColors,
                frozenBitmap = frozenBitmap,
                hoopX = state.kidsHoopX,
                hoopY = state.kidsHoopY,
                hoopRadius = state.kidsHoopRadius,
                onAdvanceToPlacePhone = onAdvanceToPlacePhone,
                onTakePhoto = onTakePhoto,
                onBackToScanBall = onBackToScanBall,
                onBackToPlacePhone = onBackToPlacePhone,
                onUpdateHoopPosition = onUpdateHoopPosition,
                onUpdateHoopRadius = onUpdateHoopRadius,
                onConfirmHoopAndStart = onConfirmHoopAndStart,
                onExit = onExitToMain
            )
        } else {
            // ==========================================
            // HUD DE JUEGO ARCADE EN TIEMPO REAL
            // ==========================================
            KidsGameplayView(
                state = state,
                onRestartSession = onRestartSession,
                onRecalibrate = onRecalibrate,
                onManualScoreBasket = onManualScoreBasket,
                onExit = onExitToMain
            )
        }
    }
}

/**
 * Vista de Calibración guiada de 3 pasos:
 * 1. Móvil en mano: Escaneo circular 360º de la pelota infantil (paleta de colores).
 * 2. Apoyar el móvil: Instrucción visual para fijar el teléfono en una superficie estable.
 * 3. Móvil quieto: Foto fija y ajuste del aro infantil sobre la imagen real.
 */
@Composable
private fun KidsCalibrationView(
    step: KidsCalibrationStep,
    scanProgress: Float,
    paletteColors: List<Int>,
    frozenBitmap: Bitmap?,
    hoopX: Float,
    hoopY: Float,
    hoopRadius: Float,
    onAdvanceToPlacePhone: () -> Unit,
    onTakePhoto: () -> Unit,
    onBackToScanBall: () -> Unit = {},
    onBackToPlacePhone: () -> Unit = {},
    onUpdateHoopPosition: (Float, Float) -> Unit,
    onUpdateHoopRadius: (Float) -> Unit,
    onConfirmHoopAndStart: () -> Unit,
    onExit: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rot"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // En paso 3 (ADJUST_HOOP_VIEW), si tenemos foto congelada, la mostramos estática
        if (step == KidsCalibrationStep.ADJUST_HOOP_VIEW && frozenBitmap != null && !frozenBitmap.isRecycled) {
            Image(
                bitmap = frozenBitmap.asImageBitmap(),
                contentDescription = "Foto fija de la canasta",
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay interactivo según el paso activo
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(step) {
                    if (step == KidsCalibrationStep.ADJUST_HOOP_VIEW) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = (hoopX + dragAmount.x / size.width).coerceIn(0.1f, 0.9f)
                            val newY = (hoopY + dragAmount.y / size.height).coerceIn(0.08f, 0.85f)
                            onUpdateHoopPosition(newX, newY)
                        }
                    }
                }
                .pointerInput(step, Unit) {
                    if (step == KidsCalibrationStep.ADJUST_HOOP_VIEW) {
                        detectTapGestures { offset ->
                            val normX = (offset.x / size.width).coerceIn(0.1f, 0.9f)
                            val normY = (offset.y / size.height).coerceIn(0.08f, 0.85f)
                            onUpdateHoopPosition(normX, normY)
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            when (step) {
                KidsCalibrationStep.SCAN_BALL_HAND, KidsCalibrationStep.NOT_STARTED -> {
                    // Círculo central guía para escanear el balón (móvil en mano)
                    val cx = canvasW * 0.5f
                    val cy = canvasH * 0.48f
                    val rPx = (minOf(canvasW, canvasH) * 0.32f)

                    // Área oscurecida exterior con recorte central EvenOdd
                    val cutoutPath = Path().apply {
                        fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                        addRect(androidx.compose.ui.geometry.Rect(0f, 0f, canvasW, canvasH))
                        addOval(androidx.compose.ui.geometry.Rect(cx - rPx, cy - rPx, cx + rPx, cy + rPx))
                    }
                    drawPath(path = cutoutPath, color = Color(0x88000000))

                    // Anillo exterior brillante
                    drawCircle(
                        color = Color(0xFF2FB2C9).copy(alpha = pulseAlpha),
                        radius = rPx,
                        center = Offset(cx, cy),
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Arco de progreso de escaneo circular
                    val sweepAngle = (scanProgress.coerceIn(0f, 1f)) * 360f
                    drawArc(
                        color = Color(0xFFFF9800),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(cx - rPx, cy - rPx),
                        size = Size(rPx * 2f, rPx * 2f),
                        style = Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )

                    // Cruz guía central sutil
                    drawLine(
                        color = Color(0x88FFFFFF),
                        start = Offset(cx - 20.dp.toPx(), cy),
                        end = Offset(cx + 20.dp.toPx(), cy),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color(0x88FFFFFF),
                        start = Offset(cx, cy - 20.dp.toPx()),
                        end = Offset(cx, cy + 20.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                KidsCalibrationStep.PLACE_PHONE_STATIC -> {
                    // Guía suave de encuadre de la canasta infantil mientras se apoya el móvil
                    val top = canvasH * 0.15f
                    val bottom = canvasH * 0.65f
                    val left = canvasW * 0.15f
                    val right = canvasW * 0.85f

                    drawRoundRect(
                        color = Color(0x882FB2C9),
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f))
                        )
                    )
                }

                KidsCalibrationStep.ADJUST_HOOP_VIEW -> {
                    // Dibujar Aro Calibrado sobre la foto fija
                    val cx = hoopX * canvasW
                    val cy = hoopY * canvasH
                    val rPx = hoopRadius * canvasW

                    // Zona de entrada (superior)
                    drawOval(
                        color = Color(0x33FF9800),
                        topLeft = Offset(cx - rPx, cy - rPx * 0.4f),
                        size = Size(rPx * 2f, rPx * 0.8f)
                    )

                    // Aro metálico / virtual
                    drawOval(
                        color = Color(0xFFFF9800),
                        topLeft = Offset(cx - rPx, cy - rPx * 0.35f),
                        size = Size(rPx * 2f, rPx * 0.7f),
                        style = Stroke(width = 6.dp.toPx())
                    )

                    // Red de la canasta infantil (efecto visual)
                    val netBottom = cy + rPx * 1.3f
                    val netWidth = rPx * 1.2f
                    val path = Path().apply {
                        moveTo(cx - rPx * 0.9f, cy)
                        lineTo(cx - netWidth * 0.5f, netBottom)
                        lineTo(cx + netWidth * 0.5f, netBottom)
                        lineTo(cx + rPx * 0.9f, cy)
                    }
                    drawPath(
                        path = path,
                        color = Color(0x88FFFFFF),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                        )
                    )

                    // Cruz central para ajuste milimétrico
                    drawLine(
                        color = Color(0xFFFFCC00),
                        start = Offset(cx - 16.dp.toPx(), cy),
                        end = Offset(cx + 16.dp.toPx(), cy),
                        strokeWidth = 3.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFFFCC00),
                        start = Offset(cx, cy - 16.dp.toPx()),
                        end = Offset(cx, cy + 16.dp.toPx()),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                else -> Unit
            }
        }

        // ==========================================
        // TARJETA DE CONTROL Y BOTONES SUPERIORES
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header con botón volver y título del paso
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xDD0D1B2A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🏀 MINI BASKET CASA",
                            color = Color(0xFFFFA500),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Banner instructivo superior según el paso
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (step) {
                        KidsCalibrationStep.SCAN_BALL_HAND, KidsCalibrationStep.NOT_STARTED -> Color(0xE6102A43)
                        KidsCalibrationStep.PLACE_PHONE_STATIC -> Color(0xE60A2540)
                        KidsCalibrationStep.ADJUST_HOOP_VIEW -> Color(0xE67A3E00)
                        else -> Color(0xE6102A43)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .border(
                        1.5.dp,
                        when (step) {
                            KidsCalibrationStep.SCAN_BALL_HAND, KidsCalibrationStep.NOT_STARTED -> Color(0xFF2FB2C9)
                            KidsCalibrationStep.PLACE_PHONE_STATIC -> Color(0xFF00C853)
                            KidsCalibrationStep.ADJUST_HOOP_VIEW -> Color(0xFFFF9800)
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (step) {
                            KidsCalibrationStep.SCAN_BALL_HAND, KidsCalibrationStep.NOT_STARTED -> "PASO 1 DE 3: ESCANEAR PELOTA (MÓVIL EN MANO)"
                            KidsCalibrationStep.PLACE_PHONE_STATIC -> "PASO 2 DE 3: APOYA EL MÓVIL FIJO"
                            KidsCalibrationStep.ADJUST_HOOP_VIEW -> "PASO 3 DE 3: AJUSTAR ARO DE LA CANASTA"
                            else -> ""
                        },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (step) {
                            KidsCalibrationStep.SCAN_BALL_HAND, KidsCalibrationStep.NOT_STARTED -> "Con el móvil en mano, enfoca la pelota dentro del círculo y dale la vuelta despacio (360º) para captar todos sus dibujos y colores."
                            KidsCalibrationStep.PLACE_PHONE_STATIC -> "Apoya el móvil en una mesa, silla o soporte frente a la canasta infantil donde no se mueva."
                            KidsCalibrationStep.ADJUST_HOOP_VIEW -> "Con el móvil ya quieto, arrastra la diana naranja sobre la canasta y ajusta su tamaño."
                            else -> ""
                        },
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ==========================================
            // BOTONERA INFERIOR DE ACCIÓN SEGÚN EL PASO
            // ==========================================
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE0B132B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (step) {
                        KidsCalibrationStep.SCAN_BALL_HAND, KidsCalibrationStep.NOT_STARTED -> {
                            // Muestra de progreso y paleta de colores capturada
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Escaneo 360º: ${(scanProgress * 100).roundToInt()}%",
                                    color = Color(0xFFFF9800),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )

                                if (paletteColors.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        paletteColors.forEach { colorInt ->
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .background(Color(colorInt), CircleShape)
                                                    .border(1.dp, Color.White, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = onAdvanceToPlacePhone,
                                    modifier = Modifier.weight(1f).height(50.dp)
                                ) {
                                    Text(
                                        text = "⚡ Salto rápido",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                Button(
                                    onClick = onAdvanceToPlacePhone,
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(50.dp)
                                        .testTag("kids_advance_place_phone_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (scanProgress > 0.3f || paletteColors.isNotEmpty()) Color(0xFF2FB2C9) else Color(0xFF334155)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = if (scanProgress >= 0.9f) "¡PELOTA LISTA! SIGUIENTE ➔" else "SIGUIENTE: APOYAR MÓVIL ➔",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        KidsCalibrationStep.PLACE_PHONE_STATIC -> {
                            Text(
                                text = "💡 Coloca el móvil estable frente a la canasta",
                                color = Color(0xFFFFCC00),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = onBackToScanBall,
                                    modifier = Modifier.weight(0.9f).height(52.dp)
                                ) {
                                    Text("⬅ Pelota", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = onTakePhoto,
                                    modifier = Modifier
                                        .weight(2.1f)
                                        .height(52.dp)
                                        .testTag("kids_take_photo_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00C853)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = "📸 HACER FOTO FIJA",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        KidsCalibrationStep.ADJUST_HOOP_VIEW -> {
                            // Controles para cambiar tamaño de aro
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tamaño Aro Infantil:",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IconButton(
                                        onClick = { onUpdateHoopRadius((hoopRadius - 0.01f).coerceAtLeast(0.04f)) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF334155), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Menor", tint = Color.White)
                                    }
                                    Text(
                                        text = "${(hoopRadius * 100).roundToInt()}%",
                                        color = Color(0xFFFF9800),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                    IconButton(
                                        onClick = { onUpdateHoopRadius((hoopRadius + 0.01f).coerceAtMost(0.22f)) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF334155), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Mayor", tint = Color.White)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onBackToPlacePhone,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("🔄 Repetir Foto", fontSize = 12.sp, color = Color.White)
                                }

                                Button(
                                    onClick = onConfirmHoopAndStart,
                                    modifier = Modifier.weight(1.4f).height(48.dp).testTag("kids_start_play_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("¡FIJAR Y JUGAR! 🚀", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}

/**
 * Vista de Gameplay en vivo sin mapa táctico ni esqueleto.
 * Máxima fluidez a 60 FPS y gráficos arcade infantiles.
 */
@Composable
private fun KidsGameplayView(
    state: VisionState,
    onRestartSession: () -> Unit,
    onRecalibrate: () -> Unit,
    onManualScoreBasket: () -> Unit,
    onExit: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "kids_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // ==========================================
        // DIBUJO DEL ARO VIRTUAL EN TIEMPO REAL
        // ==========================================
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val cx = state.kidsHoopX * canvasW
            val cy = state.kidsHoopY * canvasH
            val rPx = state.kidsHoopRadius * canvasW

            // Si hay celebración de canasta reciente, resplandor brillante
            val isSwish = state.kidsSwishCelebration
            val rimColor = if (isSwish) Color(0xFFFFD700) else Color(0xFFFF9800)

            // Resplandor del aro
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        if (isSwish) Color(0x66FFD700) else Color(0x33FF9800),
                        Color.Transparent
                    )
                ),
                radius = rPx * 1.8f * (if (isSwish) 1.3f else glowPulse),
                center = Offset(cx, cy)
            )

            // Tablero infantil virtual transparente
            drawRoundRect(
                color = Color(0x44FFFFFF),
                topLeft = Offset(cx - rPx * 1.3f, cy - rPx * 1.2f),
                size = Size(rPx * 2.6f, rPx * 1.4f),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 3.dp.toPx())
            )

            // Aro metálico / virtual
            drawOval(
                color = rimColor,
                topLeft = Offset(cx - rPx, cy - rPx * 0.35f),
                size = Size(rPx * 2f, rPx * 0.7f),
                style = Stroke(width = if (isSwish) 8.dp.toPx() else 5.dp.toPx())
            )

            // Red de baloncesto estilizada
            val netBottom = cy + rPx * 1.4f
            val netWidth = rPx * 1.1f
            val path = Path().apply {
                moveTo(cx - rPx * 0.9f, cy)
                lineTo(cx - netWidth * 0.5f, netBottom)
                lineTo(cx + netWidth * 0.5f, netBottom)
                lineTo(cx + rPx * 0.9f, cy)
            }
            drawPath(
                path = path,
                color = if (isSwish) Color(0xEEFFFFFF) else Color(0x99FFFFFF),
                style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))
            )
        }

        // ==========================================
        // POPUPS FLOTANTES DE PUNTOS ("¡¡CANASTÓN!!")
        // ==========================================
        state.kidsBasketPopups.forEach { popup ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            (popup.xNorm * 1000).toInt(),
                            (popup.yNorm * 1000).toInt()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (popup.isMake) Color(0xFFFF9800) else Color(0xFF2FB2C9)
                    ),
                    modifier = Modifier
                        .scale(glowPulse)
                        .shadow(16.dp, RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = popup.text,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // ==========================================
        // MARCADOR SUPERIOR ARCADE (PUNTOS, CANASTAS, TIEMPO)
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Fila de Controles (Volver, Voz, Recalibrar, Reiniciar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Salir", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Botón Voz
                    var isMuted by remember { mutableStateOf(!VoiceCoachManager.isVoiceEnabled) }
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            VoiceCoachManager.isVoiceEnabled = !isMuted
                            if (isMuted) VoiceCoachManager.stop()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Voz",
                            tint = if (isMuted) Color(0xFFFF6666) else Color(0xFF2FB2C9)
                        )
                    }

                    // Botón Recalibrar
                    IconButton(
                        onClick = onRecalibrate,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Recalibrar", tint = Color.White)
                    }

                    // Botón Reiniciar
                    IconButton(
                        onClick = onRestartSession,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", tint = Color.White)
                    }
                }
            }

            // MARCADOR ARCADE INFANTIL
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xDD0D1B2A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0x66FF9800), RoundedCornerShape(22.dp))
                    .shadow(12.dp, RoundedCornerShape(22.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. PUNTOS GIGANTES
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PUNTOS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFB703),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${state.kidsBasketScore}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    // 2. CANASTAS / INTENTOS
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CANASTAS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90E0EF)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SportsBasketball,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${state.kidsBasketMakes}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    // 3. RACHA DE FUEGO
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "RACHA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.kidsBasketStreak >= 2) Color(0xFFFF4444) else Color(0xFFCBD5E1)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.kidsBasketStreak >= 2) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = "Fuego",
                                    tint = Color(0xFFFF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "x${state.kidsBasketStreak}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = if (state.kidsBasketStreak >= 2) Color(0xFFFF4444) else Color.White
                            )
                        }
                    }

                    // 4. TIEMPO RESTANTE
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TIEMPO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF56D6EB)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFF56D6EB),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${state.kidsBasketTimerRemainingSec}s",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = if (state.kidsBasketTimerRemainingSec <= 10) Color(0xFFFF4444) else Color.White
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // BOTÓN TÁCTIL DE RESPALDO MANUAL (+2 CANASTA)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Button(
                onClick = onManualScoreBasket,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xDDFF9800)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .shadow(10.dp, RoundedCornerShape(16.dp))
                    .testTag("kids_manual_score_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.SportsBasketball, contentDescription = null, tint = Color.White)
                    Text("+2 CANASTA", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                }
            }
        }

        // ==========================================
        // PANTALLA DE FINAL DE SESIÓN
        // ==========================================
        if (state.isKidsSessionFinished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE000000)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .border(2.dp, Color(0xFFFF9800), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🎉 ¡¡PARTIDAZO!! 🎉",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD700)
                        )

                        Text(
                            text = "Has conseguido ${state.kidsBasketScore} puntos y metido ${state.kidsBasketMakes} canastas.",
                            color = Color.White,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onRestartSession,
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("🔄 OTRA VEZ", fontWeight = FontWeight.Black, color = Color.White)
                            }

                            Button(
                                onClick = onExit,
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("SALIR", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
