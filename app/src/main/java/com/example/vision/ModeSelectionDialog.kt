package com.example.vision

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Paleta coherente con el popup oscuro y los colores de la pantalla de inicio
private val DarkBgColor = Color(0xFF141416)
private val CardBgColor = Color(0xFF1D1D22)
private val CardBorderColor = Color(0xFF282830)
private val ActionOrange = Color(0xFFEA580C)
private val TextWhite = Color.White
private val TextMuted = Color(0xFF9E9EA8)

/**
 * Diálogo modal para cambiar entre modos de entrenamiento
 * con estética idéntica al popup negro, botones sobrios y tipografía blanca.
 */
@Composable
fun ModeSelectionDialog(
    currentModeIsDribble: Boolean,
    currentModeIsReaction: Boolean,
    currentModeIsDefend: Boolean = false,
    currentModeIsKids: Boolean = false,
    onDismiss: () -> Unit,
    onSelectKidsMiniBasket: () -> Unit = {},
    onSelectDribbleCombo: () -> Unit,
    onSelectReactionPoints: () -> Unit,
    onSelectDefendZone: () -> Unit = {},
    onSelectShooting: () -> Unit,
    onSelectUploadVideo: () -> Unit
) {
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
                .clip(RoundedCornerShape(28.dp))
                .background(DarkBgColor)
                .padding(22.dp)
                .testTag("mode_selection_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabecera del selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF26262B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = ActionOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MODOS DE JUEGO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = ActionOrange,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Elige qué entrenar",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                color = TextWhite
                            )
                        }
                    }

                    // Botón cerrar circular
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF26262B))
                            .clickable { onDismiss() },
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

                Spacer(modifier = Modifier.height(2.dp))

                // -1. KIDS MINI BASKET
                ModeCardItem(
                    title = "Kids Mini Basket (Casa)",
                    badge = "🏀 ¡NUEVO! • CALIBRACIÓN",
                    description = "Tiro a canasta infantil en casa con calibración guiada de aro y pelota por foto.",
                    emoji = "🎉",
                    isActive = currentModeIsKids,
                    testTag = "select_mode_kids_mini_basket",
                    onClick = {
                        onDismiss()
                        onSelectKidsMiniBasket()
                    }
                )

                // 0. DEFEND THE ZONE
                ModeCardItem(
                    title = "Defend the Zone",
                    badge = "3 VIDAS",
                    description = "Esquiva obstáculos que intentan cortar tu bote y usa tu cuerpo de escudo.",
                    emoji = "🛡️",
                    isActive = currentModeIsDefend,
                    testTag = "select_mode_defend_zone",
                    onClick = {
                        onDismiss()
                        onSelectDefendZone()
                    }
                )

                // 1. DRIBBLE COMBO
                ModeCardItem(
                    title = "Dribble Combo (LV3)",
                    badge = "BOTE RÁPIDO",
                    description = "Medidor de nivel, combos de bote continuo y cambio de mano a máxima velocidad.",
                    emoji = "⚡",
                    isActive = currentModeIsDribble,
                    testTag = "select_mode_dribble_combo",
                    onClick = {
                        onDismiss()
                        onSelectDribbleCombo()
                    }
                )

                // 2. REACTION POINTS
                ModeCardItem(
                    title = "Reaction Points (60s)",
                    badge = "REACCIÓN",
                    description = "Bota con una mano mientras tocas los objetivos con la mano libre en 60 segundos.",
                    emoji = "🎯",
                    isActive = currentModeIsReaction,
                    testTag = "select_mode_reaction_points",
                    onClick = {
                        onDismiss()
                        onSelectReactionPoints()
                    }
                )

                // 3. SESIÓN DE TIRO EN CANASTA
                val isShootingActive = !currentModeIsDribble && !currentModeIsReaction && !currentModeIsDefend
                ModeCardItem(
                    title = "Sesión de Tiro en Canasta",
                    badge = "TIRO Y ARO",
                    description = "Calibra la canasta y registra tus tiros, aciertos, ángulos y estadísticas.",
                    emoji = "🏀",
                    isActive = isShootingActive,
                    testTag = "select_mode_shooting",
                    onClick = {
                        onDismiss()
                        onSelectShooting()
                    }
                )

                // 4. SUBIR VÍDEO
                ModeCardItem(
                    title = "Analizar Vídeo de Galería",
                    badge = "VÍDEO",
                    description = "Analiza una grabación desde tu móvil para revisar la técnica de tiro o bote.",
                    emoji = "🎬",
                    isActive = false,
                    testTag = "select_mode_upload_video",
                    onClick = {
                        onDismiss()
                        onSelectUploadVideo()
                    }
                )
            }
        }
    }
}

@Composable
private fun ModeCardItem(
    title: String,
    badge: String,
    description: String,
    emoji: String,
    isActive: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBgColor)
            .border(
                width = 1.dp,
                color = if (isActive) ActionOrange else CardBorderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isActive) ActionOrange.copy(alpha = 0.2f) else Color(0xFF26262B)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) ActionOrange.copy(alpha = 0.2f) else Color(0xFF26262B))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = if (isActive) ActionOrange else Color(0xFFD0D0D5),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                if (isActive) {
                    Text(
                        text = "• ACTIVO",
                        color = ActionOrange,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = description,
                fontSize = 11.sp,
                color = TextMuted,
                lineHeight = 15.sp,
                maxLines = 2
            )
        }
    }
}
