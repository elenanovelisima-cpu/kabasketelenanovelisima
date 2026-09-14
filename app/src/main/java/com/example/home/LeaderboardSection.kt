package com.example.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Sección completa de LEADERBOARD que reproduce fielmente los componentes de las imágenes:
 * 1. Título "LEADERBOARD" con "View More >" y subtítulo
 * 2. Tarjeta del usuario SharpWing3098 en color magenta (#E91E63) con emblema, 👑 #102 y 0XP
 * 3. Contenedor de Clasificación con Podium de los Top 3 (VoltageKid, DeepCross con corona, JaylenFrost)
 * 4. Sección "PLAYERS NEAR YOU" con #101 JustStartedJoe, #102 SharpWing3098 (resaltado) y #103 Emilio
 * 5. Botón de XP BOOST interactivo (modo fake 24 horas con contador regresivo en tiempo real)
 */
@Composable
fun LeaderboardSection(
    userHandle: String = "SharpWing3098",
    userXp: String = "0 XP",
    onViewMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Estado del XP Boost (Por defecto desactivado, se activa 24h al pulsar)
    var isBoostActive by remember { mutableStateOf(false) }
    // 24 horas en segundos (24 * 3600 = 86400 segundos)
    var remainingSeconds by remember { mutableLongStateOf(24 * 3600L) }

    var supabaseLeaderboard by remember { mutableStateOf<List<com.example.supabase.LeaderboardItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        val res = com.example.supabase.SupabaseClient.getLeaderboard(limit = 10)
        if (res.isSuccess && res.getOrNull()?.isNotEmpty() == true) {
            supabaseLeaderboard = res.getOrNull()!!
        }
    }

    val top1Name = supabaseLeaderboard.getOrNull(0)?.username ?: "DeepCross"
    val top1Points = supabaseLeaderboard.getOrNull(0)?.let { "${it.score} pts" } ?: "2900 pts"

    val top2Name = supabaseLeaderboard.getOrNull(1)?.username ?: "VoltageKid"
    val top2Points = supabaseLeaderboard.getOrNull(1)?.let { "${it.score} pts" } ?: "2750 pts"

    val top3Name = supabaseLeaderboard.getOrNull(2)?.username ?: "JaylenFrost"
    val top3Points = supabaseLeaderboard.getOrNull(2)?.let { "${it.score} pts" } ?: "2600 pts"

    LaunchedEffect(isBoostActive) {
        while (isBoostActive && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
        if (remainingSeconds <= 0) {
            isBoostActive = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // --- 1. ENCABEZADO: LEADERBOARD + View More > ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CLASIFICACIÓN",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.5).sp,
                color = Color(0xFF1E2229)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false)
                    ) { onViewMoreClick() }
                    .padding(4.dp)
            ) {
                Text(
                    text = "Ver Más",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Ver Más",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Stack points, lead the boards and rise to the top!",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. TARJETA MAGENTA DEL USUARIO ACTUAL (SharpWing3098) ---
        UserLeaderboardHighlightCard(
            userHandle = userHandle,
            rank = "#102",
            xp = userXp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. CONTENEDOR DE CLASIFICACIÓN (Top 3 Podium + Players Near You) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFFF1F5F9))
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // TOP 3 PODIUM (VoltageKid #2, DeepCross #1, JaylenFrost #3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // #2 VoltageKid
                    PodiumPlayerCard(
                        rank = 2,
                        name = top2Name,
                        points = top2Points,
                        gradientColors = listOf(Color(0xFFBAE6FD), Color(0xFF38BDF8)),
                        badgeColor = Color(0xFFE2E8F0),
                        badgeTextColor = Color(0xFF475569),
                        avatar = { VoltageKidAvatar(modifier = Modifier.size(54.dp)) },
                        modifier = Modifier.weight(1f)
                    )

                    // #1 DeepCross (Con corona y tono resaltado)
                    PodiumPlayerCard(
                        rank = 1,
                        name = top1Name,
                        points = top1Points,
                        gradientColors = listOf(Color(0xFFA5F3FC), Color(0xFF06B6D4)),
                        badgeColor = Color(0xFFFBBF24),
                        badgeTextColor = Color(0xFF78350F),
                        hasCrown = true,
                        avatar = { DeepCrossAvatar(modifier = Modifier.size(54.dp)) },
                        modifier = Modifier.weight(1f)
                    )

                    // #3 JaylenFrost
                    PodiumPlayerCard(
                        rank = 3,
                        name = top3Name,
                        points = top3Points,
                        gradientColors = listOf(Color(0xFFBAE6FD), Color(0xFF38BDF8)),
                        badgeColor = Color(0xFFD97706),
                        badgeTextColor = Color.White,
                        avatar = { JaylenFrostAvatar(modifier = Modifier.size(54.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // --- 4. SECCIÓN "PLAYERS NEAR YOU" ---
                Text(
                    text = "PLAYERS NEAR YOU",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // #101 JustStartedJoe
                NearPlayerRowCard(
                    rank = "#101",
                    name = "JustStartedJoe",
                    xp = "1 XP",
                    isCurrentPlayer = false,
                    avatar = { JustStartedJoeAvatar(modifier = Modifier.size(34.dp)) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // #102 SharpWing3098 (Resaltado con borde magenta exacto)
                NearPlayerRowCard(
                    rank = "#102",
                    name = userHandle,
                    xp = userXp,
                    isCurrentPlayer = true,
                    avatar = { HoopstarsAvatarSmall(modifier = Modifier.size(34.dp)) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // #103 Emilio
                NearPlayerRowCard(
                    rank = "#103",
                    name = "Emilio",
                    xp = "0 XP",
                    isCurrentPlayer = false,
                    avatar = { HoopstarsAvatarSmall(modifier = Modifier.size(34.dp)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. BOTÓN DE XP BOOST (Activable 24 horas modo fake con timer) ---
        XpBoostButton(
            isActive = isBoostActive,
            remainingSeconds = remainingSeconds,
            onToggle = {
                if (!isBoostActive) {
                    isBoostActive = true
                    remainingSeconds = 24 * 3600L
                } else {
                    // Si ya estaba activo, reiniciar a 24 horas completas
                    remainingSeconds = 24 * 3600L
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Activate once every 24 hours — boosts XP for 1 hour.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Tarjeta vibrante magenta del usuario en la parte superior del Leaderboard.
 */
@Composable
private fun UserLeaderboardHighlightCard(
    userHandle: String,
    rank: String,
    xp: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("user_leaderboard_highlight_card")
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE91E63))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emblema circular multicolor Hoopstars
            HoopstarsAvatarSmall(modifier = Modifier.size(44.dp))

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = userHandle,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Pill amarilla con corona y posición (#102)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFACC15))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "👑",
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = rank,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Pill blanca con XP ("0XP")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Text(
                    text = xp,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }
    }
}

/**
 * Tarjeta individual de los 3 jugadores del podio (#1, #2, #3).
 */
@Composable
private fun PodiumPlayerCard(
    rank: Int,
    name: String,
    points: String,
    gradientColors: List<Color>,
    badgeColor: Color,
    badgeTextColor: Color,
    avatar: @Composable () -> Unit,
    hasCrown: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(gradientColors))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Corona opcional para el #1
            if (hasCrown) {
                Text(
                    text = "👑",
                    fontSize = 18.sp,
                    modifier = Modifier.offset(y = (-2).dp)
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Avatar con badge circular superpuesto en la parte inferior
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.size(60.dp)
            ) {
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    avatar()
                }

                // Círculo con el número del puesto (1, 2 o 3)
                Box(
                    modifier = Modifier
                        .offset(y = 6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = badgeTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = points,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xEEFFFFFF),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Fila para cada jugador en la sección "PLAYERS NEAR YOU".
 * Si es el jugador actual, incluye el borde magenta distintivo.
 */
@Composable
private fun NearPlayerRowCard(
    rank: String,
    name: String,
    xp: String,
    isCurrentPlayer: Boolean,
    avatar: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBorder = if (isCurrentPlayer) {
        BorderStroke(2.dp, Color(0xFFE91E63))
    } else {
        BorderStroke(1.dp, Color(0xFFE2E8F0))
    }

    val textColor = if (isCurrentPlayer) Color(0xFFE91E63) else Color(0xFF1E293B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(cardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rank,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                modifier = Modifier.width(42.dp)
            )

            avatar()

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = xp,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
    }
}

/**
 * Botón para activar o ver el estado del XP Boost (con temporizador regresivo de 24h).
 */
@Composable
private fun XpBoostButton(
    isActive: Boolean,
    remainingSeconds: Long,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60
    val timerText = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)

    Box(
        modifier = modifier
            .testTag("xp_boost_action_button")
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) Color(0xFFE2E8F0) else Color(0xFF1E293B))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White)
            ) { onToggle() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚡",
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isActive) {
                    "XP BOOST ACTIVE — $timerText"
                } else {
                    "BOOST XP"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = if (isActive) Color(0xFF64748B) else Color.White
            )
        }
    }
}

// ==========================================
// AVATARES PERSONALIZADOS PARA EL LEADERBOARD
// ==========================================

/**
 * Avatar para VoltageKid (Niño con pelo naranja llamativo y expresión entusiasta).
 */
@Composable
fun VoltageKidAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFFEF08A))
            .border(2.dp, Color(0xFFF59E0B), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Pelo naranja puntiagudo superior
            val hairPath = Path().apply {
                moveTo(w * 0.2f, h * 0.45f)
                lineTo(w * 0.15f, h * 0.25f)
                lineTo(w * 0.35f, h * 0.15f)
                lineTo(w * 0.5f, h * 0.08f)
                lineTo(w * 0.65f, h * 0.15f)
                lineTo(w * 0.85f, h * 0.25f)
                lineTo(w * 0.8f, h * 0.45f)
                close()
            }
            drawPath(hairPath, color = Color(0xFFEA580C))

            // Cara color piel
            drawCircle(
                color = Color(0xFFFFD1B3),
                radius = w * 0.34f,
                center = Offset(w * 0.5f, h * 0.55f)
            )

            // Flequillo
            drawCircle(
                color = Color(0xFFEA580C),
                radius = w * 0.18f,
                center = Offset(w * 0.4f, h * 0.38f)
            )
            drawCircle(
                color = Color(0xFFEA580C),
                radius = w * 0.15f,
                center = Offset(w * 0.6f, h * 0.38f)
            )

            // Ojos grandes animados
            drawCircle(
                color = Color.White,
                radius = w * 0.09f,
                center = Offset(w * 0.4f, h * 0.54f)
            )
            drawCircle(
                color = Color(0xFF1E293B),
                radius = w * 0.05f,
                center = Offset(w * 0.4f, h * 0.54f)
            )

            drawCircle(
                color = Color.White,
                radius = w * 0.09f,
                center = Offset(w * 0.6f, h * 0.54f)
            )
            drawCircle(
                color = Color(0xFF1E293B),
                radius = w * 0.05f,
                center = Offset(w * 0.6f, h * 0.54f)
            )

            // Sonrisa
            drawArc(
                color = Color(0xFF9A3412),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.42f, h * 0.66f),
                size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.10f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * Avatar para DeepCross (Fondo rosa, gafas elegantes, barba estilizada y sonrisa).
 */
@Composable
fun DeepCrossAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFFCE7F3))
            .border(2.dp, Color(0xFFF43F5E), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Pelo / turbante superior suave
            drawCircle(
                color = Color(0xFFF472B6),
                radius = w * 0.36f,
                center = Offset(w * 0.5f, h * 0.32f)
            )

            // Cara
            drawCircle(
                color = Color(0xFFD4A373),
                radius = w * 0.32f,
                center = Offset(w * 0.5f, h * 0.55f)
            )

            // Barba negra elegante
            val beardPath = Path().apply {
                moveTo(w * 0.26f, h * 0.56f)
                lineTo(w * 0.28f, h * 0.76f)
                lineTo(w * 0.5f, h * 0.88f)
                lineTo(w * 0.72f, h * 0.76f)
                lineTo(w * 0.74f, h * 0.56f)
                close()
            }
            drawPath(beardPath, color = Color(0xFF1E1E24))

            // Gafas circulares rojas
            drawCircle(
                color = Color(0xFFE11D48),
                radius = w * 0.12f,
                center = Offset(w * 0.38f, h * 0.52f),
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFE11D48),
                radius = w * 0.12f,
                center = Offset(w * 0.62f, h * 0.52f),
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawLine(
                color = Color(0xFFE11D48),
                start = Offset(w * 0.48f, h * 0.52f),
                end = Offset(w * 0.52f, h * 0.52f),
                strokeWidth = 2.5.dp.toPx()
            )

            // Ojos sonrientes detrás de gafas
            drawCircle(
                color = Color(0xFF1E1E24),
                radius = w * 0.04f,
                center = Offset(w * 0.38f, h * 0.52f)
            )
            drawCircle(
                color = Color(0xFF1E1E24),
                radius = w * 0.04f,
                center = Offset(w * 0.62f, h * 0.52f)
            )

            // Sonrisa blanca en la barba
            drawArc(
                color = Color.White,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.42f, h * 0.68f),
                size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.08f)
            )
        }
    }
}

/**
 * Avatar para JaylenFrost (Estilo pixel-art / rubio gamer).
 */
@Composable
fun JaylenFrostAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFE0F2FE))
            .border(2.dp, Color(0xFF38BDF8), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Cabeza base tipo pixel / bloque
            drawRect(
                color = Color(0xFFFFDBB5),
                topLeft = Offset(w * 0.25f, h * 0.28f),
                size = androidx.compose.ui.geometry.Size(w * 0.50f, h * 0.50f)
            )

            // Pelo pixelado rubio
            drawRect(
                color = Color(0xFFFACC15),
                topLeft = Offset(w * 0.22f, h * 0.18f),
                size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.22f)
            )
            drawRect(
                color = Color(0xFFFACC15),
                topLeft = Offset(w * 0.22f, h * 0.40f),
                size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.20f)
            )
            drawRect(
                color = Color(0xFFFACC15),
                topLeft = Offset(w * 0.66f, h * 0.40f),
                size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.20f)
            )

            // Ojos cuadrados estilo pixel
            drawRect(
                color = Color(0xFF0284C7),
                topLeft = Offset(w * 0.35f, h * 0.46f),
                size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.08f)
            )
            drawRect(
                color = Color(0xFF0284C7),
                topLeft = Offset(w * 0.57f, h * 0.46f),
                size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.08f)
            )

            // Boca pixel
            drawRect(
                color = Color(0xFFB91C1C),
                topLeft = Offset(w * 0.45f, h * 0.64f),
                size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.05f)
            )
        }
    }
}

/**
 * Avatar para JustStartedJoe (Niño preocupado/nervioso).
 */
@Composable
fun JustStartedJoeAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFE0F2FE))
            .border(1.5.dp, Color(0xFF93C5FD), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Cara piel clara
            drawCircle(
                color = Color(0xFFFFDFC4),
                radius = w * 0.38f,
                center = Offset(w * 0.5f, h * 0.52f)
            )

            // Pelo castaño claro fino
            drawArc(
                color = Color(0xFF92400E),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.25f, h * 0.18f),
                size = androidx.compose.ui.geometry.Size(w * 0.50f, h * 0.32f)
            )

            // Cejas tristes/preocupadas
            drawLine(
                color = Color(0xFF78350F),
                start = Offset(w * 0.32f, h * 0.44f),
                end = Offset(w * 0.44f, h * 0.40f),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color(0xFF78350F),
                start = Offset(w * 0.56f, h * 0.40f),
                end = Offset(w * 0.68f, h * 0.44f),
                strokeWidth = 2.dp.toPx()
            )

            // Ojitos caídos
            drawCircle(
                color = Color(0xFF1E293B),
                radius = w * 0.04f,
                center = Offset(w * 0.38f, h * 0.50f)
            )
            drawCircle(
                color = Color(0xFF1E293B),
                radius = w * 0.04f,
                center = Offset(w * 0.62f, h * 0.50f)
            )

            // Gota de sudor / nervio
            drawCircle(
                color = Color(0xFF60A5FA),
                radius = w * 0.06f,
                center = Offset(w * 0.74f, h * 0.42f)
            )

            // Boquita ondulada triste
            drawArc(
                color = Color(0xFF78350F),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.42f, h * 0.66f),
                size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.08f),
                style = Stroke(width = 1.8.dp.toPx())
            )
        }
    }
}

/**
 * Avatar pequeño Hoopstars con el degradado y el balón con "H★".
 */
@Composable
fun HoopstarsAvatarSmall(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    listOf(
                        Color(0xFF00E5FF),
                        Color(0xFFFF2A85),
                        Color(0xFFFFD600),
                        Color(0xFF00E5FF)
                    )
                )
            )
            .padding(2.dp)
            .clip(CircleShape)
            .background(Color(0xFF00BCD4)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                color = Color(0xFFFF4081),
                radius = w * 0.45f,
                style = Stroke(width = 1.8f)
            )
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(0f, h * 0.5f),
                end = Offset(w, h * 0.5f),
                strokeWidth = 1.8f
            )
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(w * 0.5f, 0f),
                end = Offset(w * 0.5f, h),
                strokeWidth = 1.8f
            )
        }

        Text(
            text = "H★",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF111827)
        )
    }
}
