package com.example.profile

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stats.PlayerStats
import com.example.stats.PlayerStatsManager
import com.example.vision.SavedVideoAnalysis
import com.example.vision.SavedVideoManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shape geométrico vectorial para los escudos de liga y logros.
 */
val ShieldShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.12f, 0f)
    lineTo(w * 0.88f, 0f)
    cubicTo(w * 0.98f, 0f, w, 0.06f * h, w, 0.14f * h)
    lineTo(w, h * 0.58f)
    cubicTo(w, h * 0.80f, w * 0.65f, h * 0.93f, w * 0.5f, h)
    cubicTo(w * 0.35f, h * 0.93f, 0f, h * 0.80f, 0f, h * 0.58f)
    lineTo(0f, 0.14f * h)
    cubicTo(0f, 0.06f * h, w * 0.02f, 0f, w * 0.12f, 0f)
    close()
}

/**
 * Modelo de datos de Logro (Badge).
 */
data class ProfileAchievement(
    val id: String,
    val titleEs: String,
    val descriptionEs: String,
    val xpReward: Int,
    val isUnlocked: (PlayerStats, List<SavedVideoAnalysis>) -> Boolean,
    val progressText: (PlayerStats, List<SavedVideoAnalysis>) -> String
)

/**
 * Pantalla completa de Perfil del Usuario.
 * Replica de manera fidedigna el diseño de la captura de pantalla:
 * - Header con badge de rango, píldora de XP y botón de configuración
 * - Fila de información de usuario con avatar artístico, nombre con botón de edición y estadísticas (Siguiendo, Seguidores, Vídeos)
 * - Barra de progreso de liga ("LIGA ROOKIE 0/499")
 * - 4 Escudos de liga (ROOKIE, ALL-STAR, MVP, GOAT)
 * - Pestañas "Vídeos" y "Logros"
 * - Sección de vídeos guardados localmente
 * - Sección de escudos/logros interactivos con animación 3D al girar y cambio de color al conseguirlos.
 */
@Composable
fun FullProfileScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerStats by PlayerStatsManager.stats.collectAsState()
    val savedVideoManager = remember { SavedVideoManager(context) }
    var savedVideos by remember { mutableStateOf<List<SavedVideoAnalysis>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Vídeos, 1: Logros (por defecto Logros como en la captura)
    var showEditNameDialog by remember { mutableStateOf(false) }

    // Cargar vídeos locales guardados
    LaunchedEffect(Unit) {
        savedVideos = savedVideoManager.getAllSaved()
    }

    // Lista de logros disponibles en español
    val achievements = remember {
        listOf(
            ProfileAchievement(
                id = "first_100_xp",
                titleEs = "PRIMEROS PASOS",
                descriptionEs = "CONSIGUE TUS PRIMEROS 100 XP EN CUALQUIER MODO DE JUEGO.",
                xpReward = 100,
                isUnlocked = { stats, _ -> stats.totalXp >= 100 },
                progressText = { stats, _ -> "${stats.totalXp.coerceAtMost(100)} / 100 XP" }
            ),
            ProfileAchievement(
                id = "1st_place",
                titleEs = "1ER LUGAR",
                descriptionEs = "CONSIGUE TU PRIMER RÉCORD O PUNTUACIÓN DESTACADA EN UN MINIJUEGO.",
                xpReward = 175,
                isUnlocked = { stats, _ ->
                    stats.reactionPointsBest > 0 || stats.dribbleComboBest > 0 || stats.defendZoneBest > 0
                },
                progressText = { stats, _ ->
                    val best = maxOf(stats.reactionPointsBest, stats.dribbleComboBest, stats.defendZoneBest)
                    if (best > 0) "Récord: $best pts" else "Sin récord aún"
                }
            ),
            ProfileAchievement(
                id = "talk_of_court",
                titleEs = "REY DE LA PISTA",
                descriptionEs = "COMPLETA AL MENOS 3 SESIONES COMPLETAS DE JUEGO O ENTRENAMIENTO.",
                xpReward = 100,
                isUnlocked = { stats, _ ->
                    (stats.reactionPointsGames + stats.dribbleComboGames + stats.defendZoneGames + stats.shootingSessions) >= 3
                },
                progressText = { stats, _ ->
                    val totalGames = stats.reactionPointsGames + stats.dribbleComboGames + stats.defendZoneGames + stats.shootingSessions
                    "${totalGames.coerceAtMost(3)} / 3 partidas"
                }
            ),
            ProfileAchievement(
                id = "publish_5_videos",
                titleEs = "CINCO VÍDEOS",
                descriptionEs = "GUARDA O ANALIZA 5 VÍDEOS DE JUGADAS EN LA CANCHA.",
                xpReward = 75,
                isUnlocked = { stats, videos ->
                    videos.size >= 5 || stats.shootingSessions >= 5
                },
                progressText = { stats, videos ->
                    val count = maxOf(videos.size, stats.shootingSessions)
                    "${count.coerceAtMost(5)} / 5 vídeos"
                }
            ),
            ProfileAchievement(
                id = "court_clout",
                titleEs = "FAMA EN LA CANCHA",
                descriptionEs = "ALCANZA EL RANGO PRO SUPERANDO LOS 250 XP TOTALES.",
                xpReward = 150,
                isUnlocked = { stats, _ -> stats.totalXp >= 250 },
                progressText = { stats, _ -> "${stats.totalXp.coerceAtMost(250)} / 250 XP" }
            ),
            ProfileAchievement(
                id = "defend_iron",
                titleEs = "DEFENSA DE HIERRO",
                descriptionEs = "SOBREVIVE Y PROTEGE LA ZONA EN DEFEND THE ZONE.",
                xpReward = 125,
                isUnlocked = { stats, _ -> stats.defendZoneBest >= 15 },
                progressText = { stats, _ -> "Récord: ${stats.defendZoneBest} pts" }
            ),
            ProfileAchievement(
                id = "dribble_master",
                titleEs = "MAESTRO DEL DRIBLE",
                descriptionEs = "ENCADENA COMBOS DE CROSSOVER EN DRIBBLE COMBO.",
                xpReward = 120,
                isUnlocked = { stats, _ -> stats.dribbleComboBest >= 20 || stats.dribbleCrossovers >= 5 },
                progressText = { stats, _ -> "Récord: ${stats.dribbleComboBest} pts" }
            ),
            ProfileAchievement(
                id = "sniper_shot",
                titleEs = "FRANCOTIRADOR",
                descriptionEs = "ANOTA 5 CANASTAS EN LA SESIÓN DE TIRO CON ANÁLISIS.",
                xpReward = 200,
                isUnlocked = { stats, _ -> stats.shootingMakes >= 5 || stats.shootingXp >= 80 },
                progressText = { stats, _ -> "${stats.shootingMakes.coerceAtMost(5)} / 5 canastas" }
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
            .testTag("full_profile_screen")
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ==========================================
            // 1. HEADER SUPERIOR (Píldoras y Ajustes)
            // ==========================================
            item(span = { GridItemSpan(2) }) {
                ProfileTopHeader(
                    rankTitle = playerStats.rankTitle,
                    totalXp = playerStats.totalXp,
                    onOpenSettings = onOpenSettings
                )
            }

            // ==========================================
            // 2. FILA DE PERFIL (Avatar, Nombre, Stats)
            // ==========================================
            item(span = { GridItemSpan(2) }) {
                ProfileUserCard(
                    playerName = playerStats.playerName,
                    videoCount = savedVideos.size,
                    onEditNameClick = { showEditNameDialog = true }
                )
            }

            // ==========================================
            // 3. BARRA DE PROGRESO DE LIGA
            // ==========================================
            item(span = { GridItemSpan(2) }) {
                ProfileLeagueProgress(
                    rankTitle = playerStats.rankTitle,
                    totalXp = playerStats.totalXp
                )
            }

            // ==========================================
            // 4. FILA DE 4 ESCUDOS DE LIGA
            // ==========================================
            item(span = { GridItemSpan(2) }) {
                ProfileLeagueBadgesRow(totalXp = playerStats.totalXp)
            }

            // ==========================================
            // 5. PESTAÑAS: VÍDEOS | LOGROS
            // ==========================================
            item(span = { GridItemSpan(2) }) {
                ProfileTabsRow(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it }
                )
            }

            // ==========================================
            // 6. CONTENIDO SEGÚN LA PESTAÑA SELECCIONADA
            // ==========================================
            if (selectedTab == 0) {
                // Pestaña VÍDEOS
                if (savedVideos.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        ProfileVideosEmptyState()
                    }
                } else {
                    items(savedVideos, key = { it.id }, span = { GridItemSpan(1) }) { video ->
                        ProfileVideoItemCard(video = video)
                    }
                }
            } else {
                // Pestaña LOGROS
                items(achievements, key = { it.id }, span = { GridItemSpan(1) }) { achievement ->
                    val unlocked = achievement.isUnlocked(playerStats, savedVideos)
                    ProfileAchievementShieldCard(
                        achievement = achievement,
                        isUnlocked = unlocked,
                        progressText = achievement.progressText(playerStats, savedVideos)
                    )
                }
            }
        }

        // Diálogo para editar el nombre de usuario
        if (showEditNameDialog) {
            EditPlayerNameDialog(
                currentName = playerStats.playerName,
                onDismiss = { showEditNameDialog = false },
                onSave = { newName ->
                    PlayerStatsManager.updatePlayerName(newName)
                    showEditNameDialog = false
                }
            )
        }
    }
}

/**
 * Encabezado superior con la píldora de Rango, píldora de XP y botón de ajustes ⚙️.
 */
@Composable
private fun ProfileTopHeader(
    rankTitle: String,
    totalXp: Int,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Píldora de Rango (ej. 🏆 ROOKIE)
        Surface(
            shape = CircleShape,
            color = Color(0xFFF1F5F9),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.height(30.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = rankTitle.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Píldora de XP (ej. 0XP / 100XP) con borde magenta como en la captura
        Surface(
            shape = CircleShape,
            color = Color(0xFFFDF2F8),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEC4899)),
            modifier = Modifier.height(30.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${totalXp}XP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFDB2777)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Botón de Configuración / Ajustes ⚙️
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(34.dp)
                .testTag("btn_profile_settings")
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Ajustes y Servidor",
                tint = Color(0xFF334155),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Fila de usuario con Avatar artístico, Nombre, Lápiz de edición y Contadores (Siguiendo, Seguidores, Vídeos).
 */
@Composable
private fun ProfileUserCard(
    playerName: String,
    videoCount: Int,
    onEditNameClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Avatar artístico con borde cyan/magenta y mini-escudo abajo a la derecha
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Fondo interior azul medianoche
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = radius,
                    center = center
                )

                // Aro exterior en degradado vibrante Cyan - Rosa
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF00E5FF),
                            Color(0xFFEC4899),
                            Color(0xFF8B5CF6),
                            Color(0xFF00E5FF)
                        )
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Líneas curvas de balón de baloncesto
                val arcColor = Color(0xFF00E5FF).copy(alpha = 0.5f)
                drawLine(
                    color = arcColor,
                    start = Offset(center.x, center.y - radius * 0.8f),
                    end = Offset(center.x, center.y + radius * 0.8f),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = arcColor,
                    start = Offset(center.x - radius * 0.8f, center.y),
                    end = Offset(center.x + radius * 0.8f, center.y),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Emblema central "H★"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "H",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5FF)
                )
                Text(
                    text = "★",
                    fontSize = 15.sp,
                    color = Color(0xFFEC4899)
                )
            }

            // Mini escudo en la esquina inferior derecha del avatar
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .shadow(4.dp, ShieldShape)
                    .clip(ShieldShape)
                    .background(Color(0xFF1E3A8A))
                    .border(1.dp, Color.White, ShieldShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsBasketball,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. Nombre y Estadísticas
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Fila de nombre con icono de lápiz
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playerName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onEditNameClick,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("btn_edit_profile_name")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar nombre",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Fila de estadísticas: Siguiendo / Seguidores / Vídeos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ProfileStatColumn(count = "3", label = "Siguiendo")
                ProfileStatColumn(count = "0", label = "Seguidores")
                ProfileStatColumn(count = "$videoCount", label = "Vídeos")
            }
        }
    }
}

@Composable
private fun ProfileStatColumn(count: String, label: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = count,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF64748B)
        )
    }
}

/**
 * Barra de progreso horizontal de liga (ej. "LIGA ROOKIE" y "0/499").
 */
@Composable
private fun ProfileLeagueProgress(
    rankTitle: String,
    totalXp: Int
) {
    val maxLeagueXp = 499
    val currentProgressXp = (totalXp % 500).coerceIn(0, maxLeagueXp)
    val progressFloat = (currentProgressXp.toFloat() / maxLeagueXp.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIGA ${rankTitle.uppercase()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                letterSpacing = 0.5.sp
            )
            Text(
                text = "$currentProgressXp/$maxLeagueXp",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Barra de progreso horizontal con extremos redondeados
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFloat.coerceAtLeast(0.02f))
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2563EB))
            )
        }
    }
}

/**
 * Fila de 4 Escudos de Liga representativos:
 * 1. ROOKIE LEAGUE (Azul con balón)
 * 2. ALL-STAR LEAGUE (Cyan con estrella)
 * 3. MVP LEAGUE (Rosa con trofeo)
 * 4. GOAT LEAGUE (Dorado con corona/cabra)
 */
@Composable
private fun ProfileLeagueBadgesRow(totalXp: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LeagueShieldItem(
            name = "ROOKIE",
            subtitle = "LEAGUE",
            baseColor = Color(0xFF2563EB),
            iconType = 0,
            isActive = true
        )
        LeagueShieldItem(
            name = "ALL-STAR",
            subtitle = "LEAGUE",
            baseColor = Color(0xFF38BDF8),
            iconType = 1,
            isActive = totalXp >= 750
        )
        LeagueShieldItem(
            name = "MVP",
            subtitle = "LEAGUE",
            baseColor = Color(0xFFF472B6),
            iconType = 2,
            isActive = totalXp >= 1500
        )
        LeagueShieldItem(
            name = "GOAT",
            subtitle = "LEAGUE",
            baseColor = Color(0xFFCA8A04),
            iconType = 3,
            isActive = totalXp >= 3000
        )
    }
}

@Composable
private fun LeagueShieldItem(
    name: String,
    subtitle: String,
    baseColor: Color,
    iconType: Int,
    isActive: Boolean
) {
    val opacity = if (isActive) 1.0f else 0.45f

    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 94.dp)
            .graphicsLayer { alpha = opacity }
            .shadow(if (isActive) 4.dp else 1.dp, ShieldShape)
            .clip(ShieldShape)
            .background(baseColor)
            .border(1.5.dp, Color.White.copy(alpha = 0.8f), ShieldShape)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 0.5.sp
                )
            }

            // Icono central representativo
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                when (iconType) {
                    0 -> Icon(
                        imageVector = Icons.Default.SportsBasketball,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    1 -> Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    2 -> Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Selector de pestañas: "Vídeos" | "Logros".
 */
@Composable
private fun ProfileTabsRow(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
    ) {
        // Pestaña Vídeos
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onSelectTab(0) }
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Vídeos",
                fontSize = 15.sp,
                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                color = if (selectedTab == 0) Color(0xFF0F172A) else Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(2.5.dp)
                    .background(if (selectedTab == 0) Color(0xFF2563EB) else Color.Transparent)
            )
        }

        // Pestaña Logros (reemplaza Badges como pidió el usuario)
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onSelectTab(1) }
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Logros",
                fontSize = 15.sp,
                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                color = if (selectedTab == 1) Color(0xFF2563EB) else Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(2.5.dp)
                    .background(if (selectedTab == 1) Color(0xFF2563EB) else Color.Transparent)
            )
        }
    }
}

/**
 * Tarjeta individual de Logro / Escudo con animación 3D de giro (Flip Card)
 * y cambio de color destacado cuando se consigue.
 */
@Composable
private fun ProfileAchievementShieldCard(
    achievement: ProfileAchievement,
    isUnlocked: Boolean,
    progressText: String
) {
    var isFlipped by remember { mutableStateOf(false) }

    // Animación suave del giro en el eje Y (0° a 180°)
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "shield_flip_${achievement.id}"
    )

    // Colores: cuando está desbloqueado resalta con color brillante; si está bloqueado, plata/gris neutro
    val shieldBaseGradient = if (isUnlocked) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF59E0B), // Dorado ámbar brillante
                Color(0xFFD97706),
                Color(0xFFB45309)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF1F5F9), // Gris perla / plateado como en la captura
                Color(0xFFE2E8F0),
                Color(0xFFCBD5E1)
            )
        )
    }

    val borderColor = if (isUnlocked) Color(0xFFFCD34D) else Color(0xFF94A3B8).copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { isFlipped = !isFlipped }
            .testTag("achievement_card_${achievement.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 132.dp)
                .graphicsLayer {
                    this.rotationY = rotationY
                    cameraDistance = 14f * density
                },
            contentAlignment = Alignment.Center
        ) {
            if (rotationY <= 90f) {
                // ==========================================
                // CARA FRONTAL: ESCUDO VISUAL
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(if (isUnlocked) 8.dp else 2.dp, ShieldShape)
                        .clip(ShieldShape)
                        .background(shieldBaseGradient)
                        .border(2.dp, borderColor, ShieldShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Marca de agua central (rombo) o icono destacado
                    if (isUnlocked) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Conseguido",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "¡CONSEGUIDO!",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else {
                        // Rombo central plateado idéntico a la captura
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w / 2f, 0f)
                                lineTo(w, h / 2f)
                                lineTo(w / 2f, h)
                                lineTo(0f, h / 2f)
                                close()
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF94A3B8).copy(alpha = 0.5f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = Color(0xFF94A3B8).copy(alpha = 0.7f),
                                radius = 2.dp.toPx()
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // CARA TRASERA (CUANDO SE GIRA): TEXTO DEL LOGRO
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.rotationY = 180f } // Para que el texto no se vea invertido
                        .shadow(if (isUnlocked) 6.dp else 2.dp, ShieldShape)
                        .clip(ShieldShape)
                        .background(if (isUnlocked) Color(0xFFFFFBEB) else Color(0xFFF8FAFC))
                        .border(
                            2.dp,
                            if (isUnlocked) Color(0xFFF59E0B) else Color(0xFF94A3B8).copy(alpha = 0.5f),
                            ShieldShape
                        )
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = achievement.descriptionEs,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isUnlocked) Color(0xFF78350F) else Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progreso o estado
                        Text(
                            text = if (isUnlocked) "✅ Conseguido" else progressText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) Color(0xFF059669) else Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Título del logro en español
        Text(
            text = achievement.titleEs,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (isUnlocked) Color(0xFFD97706) else Color(0xFF475569),
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp
        )

        // Puntos XP del logro
        Text(
            text = "${achievement.xpReward} XP",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isUnlocked) Color(0xFFB45309) else Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Estado vacío cuando aún no hay vídeos grabados localmente.
 */
@Composable
private fun ProfileVideosEmptyState() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Vídeos Guardados en Local",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Aquí aparecerán los clips y repeticiones que grabes en tus partidas de juego. Todos los archivos se guardarán directamente en la memoria de tu dispositivo.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE0F2FE)
            ) {
                Text(
                    text = "📹 Próximamente: Grabación continua de jugadas",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0369A1),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Tarjeta individual de vídeo guardado localmente.
 */
@Composable
private fun ProfileVideoItemCard(video: SavedVideoAnalysis) {
    val thumbBitmap = remember(video.thumbnailPath) {
        video.thumbnailPath?.let { path ->
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap.asImageBitmap(),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.SportsBasketball,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = video.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Clip local guardado",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

/**
 * Diálogo para editar el nombre del jugador.
 */
@Composable
private fun EditPlayerNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cambiar Nombre de Jugador",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Introduce tu nuevo apodo en el juego:",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    placeholder = { Text("Ej: SharpWing3098") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_player_name")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onSave(nameInput.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
