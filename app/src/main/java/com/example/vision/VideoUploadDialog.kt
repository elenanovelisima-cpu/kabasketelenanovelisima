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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun VideoUploadDialog(
    initialType: AnalysisType = AnalysisType.SHOOTING,
    initialSpeed: AnalysisSpeed = AnalysisSpeed.TURBO,
    initialEnableSkeleton: Boolean = false,
    savedVideosCount: Int = 0,
    onDismiss: () -> Unit,
    onOpenSavedVideos: () -> Unit,
    onPickFromGallery: (type: AnalysisType, speed: AnalysisSpeed, inBackground: Boolean, enableSkeleton: Boolean) -> Unit,
    onPickFromFilePicker: (type: AnalysisType, speed: AnalysisSpeed, inBackground: Boolean, enableSkeleton: Boolean) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedSpeed by remember { mutableStateOf(initialSpeed) }
    var enableSkeleton by remember { mutableStateOf(initialEnableSkeleton) }
    var runInBackground by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE60A0A0F))
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF13141F))
                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x3300E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ANALIZAR VÍDEO CON IA",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Configura el objetivo y velocidad del análisis",
                                fontSize = 11.sp,
                                color = Color(0xAAFFFFFF)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable config options
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Link to Mis Vídeos
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x2200E5FF))
                            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(12.dp))
                            .clickable {
                                onDismiss()
                                onOpenSavedVideos()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Abrir biblioteca Mis Vídeos ($savedVideosCount guardados)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "VER ›",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }

                    // SECTION 1: TIPO DE ANÁLISIS
                    Text(
                        text = "1. SELECCIONA EL TIPO DE VÍDEO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 0.5.sp
                    )

                    // Option A: Shooting
                    AnalysisTypeOptionCard(
                        icon = Icons.Default.SportsBasketball,
                        title = "🏀 Vídeo de Lanzamientos y Mecánica",
                        tag = "TIROS INDIVIDUALES",
                        tagColor = Color(0xFFFF9800),
                        description = "Diseñado para entrenamientos de tiro. Detecta la canasta, el arco parabólico de la pelota, ángulo de lanzamiento del muñeca/codo, registro de aciertos/fallos y shot chart 2D.",
                        isSelected = selectedType == AnalysisType.SHOOTING,
                        onClick = { selectedType = AnalysisType.SHOOTING }
                    )

                    // Option B: Tactical Match
                    AnalysisTypeOptionCard(
                        icon = Icons.Default.Description,
                        title = "📋 Vídeo de Partido y Patrones Tácticos",
                        tag = "5V5 / EQUIPO COMPLETO",
                        tagColor = Color(0xFF00E5FF),
                        description = "Diseñado para partidos completos o scouting táctico. Detecta 10 jugadores, colores de equipación, contraataques rápidos, situaciones de pick & roll, espaciado (spacing) y genera informe técnico para el entrenador.",
                        isSelected = selectedType == AnalysisType.TACTICAL_MATCH,
                        onClick = { selectedType = AnalysisType.TACTICAL_MATCH }
                    )

                    // SECTION 2: VELOCIDAD DE ANÁLISIS
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "2. VELOCIDAD DEL ANÁLISIS (OPTIMIZACIÓN)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF),
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = "¿Por qué tarda el análisis? Analizar a 30 FPS procesa hasta 900 fotogramas en 30s. Selecciona el modo Turbo para acelerar hasta 3x-6x sin perder precisión:",
                            fontSize = 11.sp,
                            color = Color(0xBBFFFFFF),
                            lineHeight = 15.sp
                        )

                        SpeedOptionRow(
                            speed = AnalysisSpeed.TURBO,
                            icon = Icons.Default.Bolt,
                            isSelected = selectedSpeed == AnalysisSpeed.TURBO,
                            onClick = { selectedSpeed = AnalysisSpeed.TURBO }
                        )

                        SpeedOptionRow(
                            speed = AnalysisSpeed.ULTRA,
                            icon = Icons.Default.Speed,
                            isSelected = selectedSpeed == AnalysisSpeed.ULTRA,
                            onClick = { selectedSpeed = AnalysisSpeed.ULTRA }
                        )

                        SpeedOptionRow(
                            speed = AnalysisSpeed.ACCURATE,
                            icon = Icons.Default.SportsBasketball,
                            isSelected = selectedSpeed == AnalysisSpeed.ACCURATE,
                            onClick = { selectedSpeed = AnalysisSpeed.ACCURATE }
                        )
                    }

                    // SECTION 3: DETECCIÓN DE ESQUELETO / BIOMECÁNICA (OPCIONAL)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1B1C2A))
                            .border(1.dp, if (enableSkeleton) Color(0xFFBA68C8) else Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "🦴", fontSize = 14.sp)
                                    Text(
                                        text = "Estimar esqueleto del jugador",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (enableSkeleton)
                                        "Activado: Calcula articulaciones y ángulos. El procesamiento tardará más tiempo."
                                    else
                                        "Desactivado (Acelera hasta un 50%): El vídeo se analiza el doble de rápido y ahorra batería. La posición en pista y el cómputo de aciertos/fallos son idénticos.",
                                    fontSize = 11.sp,
                                    color = if (enableSkeleton) Color(0xFFCE93D8) else Color(0x99FFFFFF),
                                    lineHeight = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = enableSkeleton,
                                onCheckedChange = { enableSkeleton = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFBA68C8),
                                    uncheckedThumbColor = Color(0x88FFFFFF),
                                    uncheckedTrackColor = Color(0x22FFFFFF)
                                )
                            )
                        }
                    }

                    // SECTION 4: SEGUNDO PLANO
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1B1C2A))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Analizar en segundo plano",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Podrás seguir usando la cámara o navegar por la app. Te avisará con una notificación cuando termine.",
                                    fontSize = 11.sp,
                                    color = Color(0x99FFFFFF),
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(
                                checked = runInBackground,
                                onCheckedChange = { runInBackground = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color(0xFF00E5FF),
                                    uncheckedThumbColor = Color(0x88FFFFFF),
                                    uncheckedTrackColor = Color(0x22FFFFFF)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Buttons: Pick Video
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pick Files
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_video_files_button")
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF00E5FF))
                            .clickable {
                                onDismiss()
                                onPickFromFilePicker(selectedType, selectedSpeed, runInBackground, enableSkeleton)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "SELECCIONAR VÍDEO (ARCHIVOS / DESCARGAS)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }

                    // Pick Gallery
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_video_gallery_button")
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF222436))
                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(14.dp))
                            .clickable {
                                onDismiss()
                                onPickFromGallery(selectedType, selectedSpeed, runInBackground, enableSkeleton)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Abrir Galería de Fotos",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalysisTypeOptionCard(
    icon: ImageVector,
    title: String,
    tag: String,
    tagColor: Color,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) tagColor else Color(0x22FFFFFF)
    val bgColor = if (isSelected) Color(0xFF1F2235) else Color(0xFF171824)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tagColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tagColor.copy(alpha = 0.2f))
                        .border(1.dp, tagColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = tagColor
                    )
                }
            }

            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xAAFFFFFF),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun SpeedOptionRow(
    speed: AnalysisSpeed,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = Color(0xFF00E5FF)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF1E2638) else Color(0xFF161722))
            .border(
                1.dp,
                if (isSelected) activeColor else Color(0x22FFFFFF),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) activeColor else Color(0x88FFFFFF),
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = speed.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xCCFFFFFF)
                    )
                    Text(
                        text = speed.subtitle,
                        fontSize = 10.sp,
                        color = if (isSelected) activeColor else Color(0x77FFFFFF)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (isSelected) activeColor else Color(0x44FFFFFF), CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(activeColor)
                    )
                }
            }
        }
    }
}
