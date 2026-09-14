package com.example.vision

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Overlay para todos los juegos que se muestra tras completar o saltar la calibración.
 * 1. Muestra el botón animado idéntico a las tarjetas del Home ("¡JUGAR AHORA!")
 * 2. Al pulsarlo, lanza una cuenta regresiva de 3 segundos con números gigantes en blanco
 *    y fondo negro translúcido. Al llegar a 0, inicia el juego.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayNowCountdownOverlay(
    isAwaitingPlayStart: Boolean,
    countdownSec: Int?,
    isGameMode: Boolean,
    onPlayNow: () -> Unit,
    onExitToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isAwaitingPlayStart && countdownSec == null) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("play_now_countdown_overlay")
    ) {
        if (countdownSec != null) {
            // Fondo negro transparente durante la cuenta regresiva según requerimiento
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .testTag("countdown_overlay_active"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = countdownSec,
                        transitionSpec = {
                            (scaleIn(initialScale = 1.35f, animationSpec = tween(300)) + fadeIn(animationSpec = tween(200))) with
                                (scaleOut(targetScale = 0.65f, animationSpec = tween(300)) + fadeOut(animationSpec = tween(200)))
                        },
                        label = "countdown_number_anim"
                    ) { sec ->
                        Text(
                            text = "$sec",
                            fontSize = 150.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("countdown_number_$sec")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "¡PREPÁRATE!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = if (isGameMode) Color(0xFF2FB2C9) else Color(0xFFF97316),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (isAwaitingPlayStart) {
            // Fondo semitransparente sutil para que se vea el juego ya preparado debajo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .testTag("awaiting_play_start_overlay"),
                contentAlignment = Alignment.Center
            ) {
                // Botón de salir a la pantalla principal en la esquina superior izquierda
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 16.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC1B1C2A))
                        .border(1.dp, Color(0x66FFFFFF), CircleShape)
                        .clickable { onExitToMain() }
                        .testTag("exit_play_now_to_main_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Salir a la pantalla principal",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Animación estilo bouncer (idéntica a las tarjetas del Home)
                val infiniteTransition = rememberInfiniteTransition(label = "overlay_bouncer_transition")
                val bounceScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "overlay_bouncer_scale"
                )
                val bounceOffsetY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "overlay_bouncer_offset_y"
                )

                // Gradientes y sombras idénticos a los del Hero card
                val buttonGradient = if (isGameMode) {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF2FB2C9),
                            Color(0xFF0F869B)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFEA580C),
                            Color(0xFFC2410C)
                        )
                    )
                }

                val shadowAmbient = if (isGameMode) Color(0x662FB2C9) else Color(0x66EA580C)
                val shadowSpot = if (isGameMode) Color(0x990F869B) else Color(0x99C2410C)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Badge sutil de preparación completada
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xCC0F172A))
                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isGameMode) Color(0xFF2FB2C9) else Color(0xFFF97316),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CALIBRACIÓN COMPLETADA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botón central con diseño y movimiento idénticos al Hero card
                    Box(
                        modifier = Modifier
                            .offset(y = bounceOffsetY.dp)
                            .scale(bounceScale)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(50),
                                ambientColor = shadowAmbient,
                                spotColor = shadowSpot
                            )
                            .clip(RoundedCornerShape(50))
                            .background(buttonGradient)
                            .clickable { onPlayNow() }
                            .padding(horizontal = 34.dp, vertical = 18.dp)
                            .testTag("play_now_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = if (isGameMode) "¡JUGAR AHORA!" else "¡ENTRENAR AHORA!",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Toca para iniciar la cuenta regresiva",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E8F0),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
