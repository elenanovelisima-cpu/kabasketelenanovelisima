package com.example.vision

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Overlay para renderizar los efectos dinámicos de "+5 PTS" en el modo Bote/Crossover.
 * Cada vez que el jugador cambia de mano el balón, se genera una animación
 * explosiva con rebote elástico, partículas radiantes y elevación con desvanecimiento.
 */
@Composable
fun DribbleScorePopupOverlay(
    popups: List<DribblePopup>,
    onPopupExpired: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("dribble_score_popup_overlay")
    ) {
        val screenW = maxWidth.value
        val screenH = maxHeight.value

        popups.forEach { popup ->
            SingleDribblePopupView(
                popup = popup,
                screenW = screenW,
                screenH = screenH,
                onDismiss = { onPopupExpired(popup.id) }
            )
        }
    }
}

@Composable
private fun SingleDribblePopupView(
    popup: DribblePopup,
    screenW: Float,
    screenH: Float,
    onDismiss: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(popup.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1350, easing = LinearEasing)
        )
        onDismiss()
    }

    val curProgress = progress.value

    // 1. Escala elástica: surge de 0.25 -> 1.38 -> 1.0
    val scale = when {
        curProgress < 0.22f -> 0.25f + (curProgress / 0.22f) * 1.13f
        curProgress < 0.42f -> 1.38f - ((curProgress - 0.22f) / 0.20f) * 0.38f
        else -> 1.0f - (curProgress - 0.42f) * 0.2f
    }

    // 2. Alpha: rápido a 1f, se mantiene y luego se desvanece suavemente
    val alpha = when {
        curProgress < 0.12f -> curProgress / 0.12f
        curProgress < 0.60f -> 1f
        else -> (1f - ((curProgress - 0.60f) / 0.40f)).coerceIn(0f, 1f)
    }

    // 3. Elevación: flota verticalmente hacia arriba
    val floatUpDp = curProgress * 110f

    // 4. Posicionamiento en pantalla
    val posX = if (popup.nx in 0.1f..0.9f) popup.nx * screenW else screenW * 0.5f
    val posY = if (popup.ny in 0.15f..0.85f) popup.ny * screenH else screenH * 0.52f

    // Inclinación divertida basada en el ID
    val tilt = (popup.id % 13 - 6) * 1.2f

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (posX.dp.toPx() - 100.dp.toPx()).roundToInt(),
                    y = ((posY - floatUpDp).dp.toPx() - 50.dp.toPx()).roundToInt()
                )
            }
            .size(width = 200.dp, height = 100.dp)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        // --- Partículas radiantes explosivas ---
        val particleCount = 8
        val particleDist = curProgress * 65.dp.value
        val particleAlpha = (1f - curProgress * 1.4f).coerceAtLeast(0f)

        if (particleAlpha > 0f) {
            for (i in 0 until particleCount) {
                val angleRad = (i * (360.0 / particleCount) + (popup.id % 45)) * (Math.PI / 180.0)
                val pX = (cos(angleRad) * particleDist).toFloat()
                val pY = (sin(angleRad) * particleDist).toFloat()
                val pColor = if (i % 2 == 0) Color(0xFFFFEB3B) else Color(0xFFEB5637)

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = pX.dp.toPx().roundToInt(),
                                y = pY.dp.toPx().roundToInt()
                            )
                        }
                        .size(if (i % 2 == 0) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(pColor)
                        .alpha(particleAlpha)
                )
            }
        }

        // --- Tarjeta / Insignia central "+5 PTS" ---
        Column(
            modifier = Modifier
                .scale(scale)
                .rotate(tilt)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = Color(0xFFEB5637),
                    ambientColor = Color(0xFFFF9800)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFEB5637), // Naranja corporativo
                            Color(0xFFFF7A33),
                            Color(0xFFFF9800)  // Toque dorado deportivo
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color(0xFFFFEB3B), // Borde amarillo neón
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚡",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+5",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "PTS!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFEB3B)
                )
            }

            // Sub-etiqueta: CROSSOVER
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x55000000))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "CAMBIO DE MANO",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}
