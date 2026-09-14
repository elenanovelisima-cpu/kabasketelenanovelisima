package com.example.vision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theme.SportOnSurface
import kotlin.math.roundToInt

// Colores inspirados en el diseño morado/neón de la foto de referencia
private val CourtNeonPurple = Color(0xFF7C4DFF)
private val CourtNeonGlow = Color(0xB3B388FF)
private val CourtPointColor = Color(0xFF9E00FF)

@Composable
fun CourtPointSelectorView(
    courtCalibration: CourtCalibration,
    onPointMoved: (String, Float, Float) -> Unit,
    onApplyPreset: (CourtPreset) -> Unit,
    onReset: () -> Unit,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activePointId by remember { mutableStateOf<String?>("3pt_wing_l") }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("court_point_selector_view")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // 1. Canvas de dibujo de las líneas de la pista proyectadas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pts = courtCalibration.points.associateBy { it.id }

            val pCornerL = pts["3pt_corner_l"]
            val pWingL = pts["3pt_wing_l"]
            val pTop = pts["3pt_top"]
            val pWingR = pts["3pt_wing_r"]
            val pCornerR = pts["3pt_corner_r"]

            val pKeyBaseL = pts["key_base_l"]
            val pKeyBaseR = pts["key_base_r"]
            val pFtL = pts["ft_line_l"]
            val pFtR = pts["ft_line_r"]

            // A) Dibujar zona / bombilla (Paint rectangle)
            if (pKeyBaseL != null && pKeyBaseR != null && pFtL != null && pFtR != null) {
                val keyPath = Path().apply {
                    moveTo(pKeyBaseL.xNorm * widthPx, pKeyBaseL.yNorm * heightPx)
                    lineTo(pKeyBaseR.xNorm * widthPx, pKeyBaseR.yNorm * heightPx)
                    lineTo(pFtR.xNorm * widthPx, pFtR.yNorm * heightPx)
                    lineTo(pFtL.xNorm * widthPx, pFtL.yNorm * heightPx)
                    close()
                }

                // Sombra y relleno translúcido de la zona
                drawPath(
                    path = keyPath,
                    color = CourtNeonPurple.copy(alpha = 0.12f)
                )
                // Líneas de la zona
                drawPath(
                    path = keyPath,
                    color = CourtNeonPurple.copy(alpha = 0.85f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Círculo de tiro libre (semicírculo hacia el triple)
                val ftCenterX = ((pFtL.xNorm + pFtR.xNorm) / 2f) * widthPx
                val ftCenterY = ((pFtL.yNorm + pFtR.yNorm) / 2f) * heightPx
                val ftRadius = ((pFtR.xNorm - pFtL.xNorm) * widthPx / 2f).coerceAtLeast(15.dp.toPx())

                drawCircle(
                    color = CourtNeonPurple.copy(alpha = 0.7f),
                    center = Offset(ftCenterX, ftCenterY),
                    radius = ftRadius,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                )
            }

            // B) Dibujar arco de 3 puntos (3PT Arc)
            if (pCornerL != null && pWingL != null && pTop != null && pWingR != null && pCornerR != null) {
                val threeArc = Path().apply {
                    moveTo(pCornerL.xNorm * widthPx, pCornerL.yNorm * heightPx)
                    // Esquina L a Wing L
                    lineTo(pWingL.xNorm * widthPx, pWingL.yNorm * heightPx)
                    // Curva cuadrática hacia el triple frontal (Top)
                    quadraticBezierTo(
                        (pWingL.xNorm + pTop.xNorm) / 2f * widthPx,
                        pTop.yNorm * heightPx,
                        pTop.xNorm * widthPx,
                        pTop.yNorm * heightPx
                    )
                    // Curva cuadrática de Top a Wing R
                    quadraticBezierTo(
                        (pWingR.xNorm + pTop.xNorm) / 2f * widthPx,
                        pTop.yNorm * heightPx,
                        pWingR.xNorm * widthPx,
                        pWingR.yNorm * heightPx
                    )
                    // Wing R a Esquina R
                    lineTo(pCornerR.xNorm * widthPx, pCornerR.yNorm * heightPx)
                }

                // Resplandor exterior de la línea de 3
                drawPath(
                    path = threeArc,
                    color = CourtNeonGlow.copy(alpha = 0.35f),
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Línea principal de 3
                drawPath(
                    path = threeArc,
                    color = Color.White,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // 2. Puntos interactivos arrastrables en pantalla
        courtCalibration.points.forEach { point ->
            val isActive = activePointId == point.id
            val pointPxX = point.xNorm * widthPx
            val pointPxY = point.yNorm * heightPx

            val pointDpX = with(density) { pointPxX.toDp() }
            val pointDpY = with(density) { pointPxY.toDp() }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (pointPxX - 22.dp.toPx()).roundToInt(),
                            (pointPxY - 22.dp.toPx()).roundToInt()
                        )
                    }
                    .size(44.dp)
                    .pointerInput(point.id) {
                        detectDragGestures(
                            onDragStart = {
                                activePointId = point.id
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newX = ((point.xNorm * widthPx) + dragAmount.x) / widthPx
                                val newY = ((point.yNorm * heightPx) + dragAmount.y) / heightPx
                                onPointMoved(point.id, newX, newY)
                            }
                        )
                    }
                    .testTag("court_point_${point.id}"),
                contentAlignment = Alignment.Center
            ) {
                // Anillo exterior brillante
                Box(
                    modifier = Modifier
                        .size(if (isActive) 34.dp else 24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) CourtNeonGlow.copy(alpha = 0.45f)
                            else CourtNeonPurple.copy(alpha = 0.25f)
                        )
                )
                // Círculo central con borde
                Box(
                    modifier = Modifier
                        .size(if (isActive) 20.dp else 16.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Color.White else CourtPointColor)
                        .border(
                            2.dp,
                            if (isActive) CourtPointColor else Color.White,
                            CircleShape
                        )
                )
            }

            // Etiqueta flotante justo encima del punto activo o seleccionado (como "3PT wing L" en la foto)
            if (isActive) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (pointPxX - 50.dp.toPx()).roundToInt(),
                                (pointPxY - 48.dp.toPx()).roundToInt()
                            )
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xE61B1C2A))
                        .border(1.dp, CourtNeonPurple, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = point.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        // 3. BARRA SUPERIOR: Instrucciones estilo HomeCourt ("Drag points to fit the court perfectly...")
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Píldora de instrucción central
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xD90B0B14))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(999.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Arrastra los puntos para encajarlos con la pista, luego pulsa Comenzar.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            // Presets de perspectiva (Frontal, Lat Izq, Lat Der, Reset)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CourtPreset.values().forEach { preset ->
                    val isSelected = courtCalibration.preset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CourtNeonPurple else Color(0x991B1C2A))
                            .border(1.dp, if (isSelected) Color.White else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .clickable { onApplyPreset(preset) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("preset_${preset.name}")
                    ) {
                        Text(
                            text = preset.shortLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Botón restablecer
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x991B1C2A))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                        .clickable { onReset() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("reset_court_points_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restablecer puntos",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 4. BARRA INFERIOR: Botón Saltar y Botón Comenzar (estilo Start morado de la foto)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón saltar si el usuario no quiere calibrar la pista
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC1B1C2A))
                    .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(14.dp))
                    .clickable { onSkip() }
                    .padding(vertical = 14.dp)
                    .testTag("skip_court_calibration_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SALTAR PISTA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xCCFFFFFF)
                )
            }

            // Botón Comenzar Entrenamiento (Start como en la captura)
            Box(
                modifier = Modifier
                    .weight(2f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(CourtNeonPurple, Color(0xFF651FFF))
                        )
                    )
                    .border(1.dp, Color(0x88FFFFFF), RoundedCornerShape(14.dp))
                    .clickable { onConfirm() }
                    .padding(vertical = 14.dp)
                    .testTag("confirm_court_calibration_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "COMENZAR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
