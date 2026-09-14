package com.example.supabase

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.stats.PlayerStats
import com.example.stats.PlayerStatsManager
import kotlinx.coroutines.launch

@Composable
fun ProfileCloudSyncDialog(
    isGameMode: Boolean,
    onDismiss: () -> Unit
) {
    val currentUser by SupabaseAuthManager.currentUser.collectAsState()
    val isSyncing by SupabaseSyncManager.isSyncing.collectAsState()
    val pendingCount by SupabaseSyncManager.pendingCount.collectAsState()
    val lastSyncStatus by SupabaseSyncManager.lastSyncStatus.collectAsState()
    val playerStats by PlayerStatsManager.stats.collectAsState()

    val scope = rememberCoroutineScope()
    var authModeIsSignUp by remember { mutableStateOf(false) }

    // Form states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf(currentUser?.username ?: playerStats.playerName) }
    var team by remember { mutableStateOf(currentUser?.team ?: "") }

    var isEditingName by remember { mutableStateOf(false) }
    var editingNameInput by remember { mutableStateOf(playerStats.playerName) }

    var isLoadingAuth by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var authSuccessMessage by remember { mutableStateOf<String?>(null) }

    var showServerConfig by remember { mutableStateOf(false) }
    val currentServerUrl by SupabaseConfig.currentBaseUrl.collectAsState()
    val currentServerKey by SupabaseConfig.currentAnonKey.collectAsState()
    var configUrlInput by remember(currentServerUrl) { mutableStateOf(currentServerUrl) }
    var configKeyInput by remember(currentServerKey) { mutableStateOf(currentServerKey) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<String?>(null) }
    var isConnectionSuccess by remember { mutableStateOf(false) }

    val accentColor = if (isGameMode) Color(0xFF2FB2C9) else Color(0xFFEA580C)
    val cardBg = if (isGameMode) Color(0xFFF8FAFC) else Color(0xFF18181E)
    val textColor = if (isGameMode) Color(0xFF0F172A) else Color.White
    val subTextColor = if (isGameMode) Color(0xFF64748B) else Color(0xFF94A3B8)

    val displayName = currentUser?.username?.ifBlank { playerStats.playerName } ?: playerStats.playerName

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
                color = cardBg,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar (Título + Botón Cerrar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🏀",
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Perfil de Jugador",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = if (currentUser != null) "Conectado con Supabase" else "Modo Jugador Local / Sin cuenta",
                                    fontSize = 12.sp,
                                    color = if (currentUser != null) Color(0xFF10B981) else subTextColor
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isGameMode) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.1f))
                                .testTag("btn_close_profile_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cerrar",
                                tint = textColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ==========================================
                    // 1. TARJETA PRINCIPAL DEL JUGADOR & AVATAR
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.22f),
                                        accentColor.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(accentColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayName.take(2).uppercase().ifEmpty { "JU" },
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (isEditingName) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = editingNameInput,
                                                onValueChange = { editingNameInput = it },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = accentColor,
                                                    unfocusedBorderColor = subTextColor,
                                                    focusedTextColor = textColor,
                                                    unfocusedTextColor = textColor
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Button(
                                                onClick = {
                                                    PlayerStatsManager.updatePlayerName(editingNameInput)
                                                    isEditingName = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("OK", fontSize = 12.sp)
                                            }
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = displayName,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Editar nombre",
                                                tint = subTextColor,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        editingNameInput = displayName
                                                        isEditingName = true
                                                    }
                                            )
                                        }
                                    }

                                    if (currentUser != null && currentUser!!.team.isNotBlank()) {
                                        Text(
                                            text = "Club: ${currentUser!!.team}",
                                            fontSize = 12.sp,
                                            color = accentColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        Text(
                                            text = "Hoopstars Athlete",
                                            fontSize = 12.sp,
                                            color = subTextColor
                                        )
                                    }

                                    if (currentUser != null) {
                                        Text(
                                            text = currentUser!!.email,
                                            fontSize = 11.sp,
                                            color = subTextColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // BARRA DE PROGRESO DE XP Y RANGO
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (playerStats.rankTitle) {
                                                    "MVP" -> Color(0xFFF59E0B)
                                                    "ALL-STAR" -> Color(0xFF8B5CF6)
                                                    "PRO" -> Color(0xFF06B6D4)
                                                    else -> Color(0xFFE91E63)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = playerStats.rankTitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${playerStats.totalXp} XP Total",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }

                                Text(
                                    text = "Siguiente: ${playerStats.nextRankXp} XP",
                                    fontSize = 11.sp,
                                    color = subTextColor
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { playerStats.rankProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = accentColor,
                                trackColor = if (isGameMode) Color(0xFFE2E8F0) else Color(0xFF2E2E38),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ====================================================
                    // 2. DESGLOSE DE PUNTOS POR JUEGO (REGISTRO ACTIVO)
                    // ====================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Puntos Conseguidos por Juego",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Suman a tu XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // REACTION POINTS CARD
                    GameScoreStatCard(
                        iconEmoji = "⚡",
                        gameTitle = "Reaction Points",
                        totalPoints = playerStats.reactionPointsScore,
                        bestScore = playerStats.reactionPointsBest,
                        extraLabel = "Dianas tocadas: ${playerStats.reactionPointsHits}",
                        gamesPlayed = playerStats.reactionPointsGames,
                        cardColor = Color(0xFF8B5CF6),
                        isGameMode = isGameMode
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // DRIBBLE COMBO CARD
                    GameScoreStatCard(
                        iconEmoji = "🏀",
                        gameTitle = "Dribble Combo",
                        totalPoints = playerStats.dribbleComboScore,
                        bestScore = playerStats.dribbleComboBest,
                        extraLabel = "Crossovers: ${playerStats.dribbleCrossovers}",
                        gamesPlayed = playerStats.dribbleComboGames,
                        cardColor = Color(0xFF06B6D4),
                        isGameMode = isGameMode
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // DEFEND ZONE CARD
                    GameScoreStatCard(
                        iconEmoji = "🛡️",
                        gameTitle = "Defend The Zone",
                        totalPoints = playerStats.defendZoneScore,
                        bestScore = playerStats.defendZoneBest,
                        extraLabel = "Escudos / Robos: ${playerStats.defendShields}",
                        gamesPlayed = playerStats.defendZoneGames,
                        cardColor = Color(0xFF10B981),
                        isGameMode = isGameMode
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // SHOOTING WORKOUTS CARD
                    GameScoreStatCard(
                        iconEmoji = "🎯",
                        gameTitle = "Tiro a Canasta",
                        totalPoints = playerStats.shootingXp,
                        bestScore = playerStats.shootingMakes,
                        bestScoreLabel = "Canastas",
                        extraLabel = "Precisión: ${playerStats.shootingAccuracyPct}% (${playerStats.shootingMakes}/${playerStats.shootingShots})",
                        gamesPlayed = playerStats.shootingSessions,
                        gamesLabel = "Sesiones",
                        cardColor = Color(0xFFF59E0B),
                        isGameMode = isGameMode
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // CHIPS DE PRUEBA RÁPIDA (PARA TESTEAR LA SUMA DE XP)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isGameMode) Color(0xFFF1F5F9) else Color(0xFF22222B))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "🧪 Pruebas Rápidas de Suma de Puntos y XP:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = subTextColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuickTestChip(
                                    text = "+50 ⚡",
                                    onClick = { PlayerStatsManager.addTestScore("REACTION_POINTS", 50) },
                                    color = Color(0xFF8B5CF6),
                                    modifier = Modifier.weight(1f)
                                )
                                QuickTestChip(
                                    text = "+50 🏀",
                                    onClick = { PlayerStatsManager.addTestScore("DRIBBLE_COMBO", 50) },
                                    color = Color(0xFF06B6D4),
                                    modifier = Modifier.weight(1f)
                                )
                                QuickTestChip(
                                    text = "+50 🛡️",
                                    onClick = { PlayerStatsManager.addTestScore("DEFEND_ZONE", 50) },
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.weight(1f)
                                )
                                QuickTestChip(
                                    text = "+50 🎯",
                                    onClick = { PlayerStatsManager.addShootingSession(5, 5) },
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ==========================================
                    // 3. SECCIÓN DE GESTIÓN SUPABASE / SESIÓN
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isGameMode) Color(0xFFE0F2FE) else Color(0xFF132337))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val pulseScale by infiniteTransition.animateFloat(
                                    initialValue = 0.85f,
                                    targetValue = 1.15f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(900, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulseScale"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Servidor Supabase",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        text = currentServerUrl.removePrefix("https://"),
                                        fontSize = 11.sp,
                                        color = if (isGameMode) Color(0xFF0369A1) else Color(0xFF7DD3FC),
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (showServerConfig) accentColor else (if (isGameMode) Color(0xFFBAE6FD) else Color(0xFF1E3A5F)),
                                modifier = Modifier
                                    .clickable { showServerConfig = !showServerConfig }
                                    .testTag("btn_toggle_server_config")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Configurar",
                                        tint = if (showServerConfig) Color.White else (if (isGameMode) Color(0xFF0369A1) else Color(0xFF93C5FD)),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showServerConfig) "Cerrar" else "Ajustes",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (showServerConfig) Color.White else (if (isGameMode) Color(0xFF0369A1) else Color(0xFF93C5FD))
                                    )
                                }
                            }
                        }

                        // Panel expandible para ver y editar URL y Anon Key
                        AnimatedVisibility(visible = showServerConfig) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Text(
                                    text = "Comprueba o cambia la URL y la API Key pública de Supabase:",
                                    fontSize = 11.sp,
                                    color = subTextColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = configUrlInput,
                                    onValueChange = {
                                        configUrlInput = it
                                        connectionTestResult = null
                                    },
                                    label = { Text("Supabase Project URL", fontSize = 11.sp) },
                                    placeholder = { Text("https://xxx.supabase.co") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = if (isGameMode) Color(0xFFCBD5E1) else Color(0xFF334155),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_supabase_url")
                                )

                                Text(
                                    text = "💡 No agregues /rest/v1. Se normaliza automáticamente.",
                                    fontSize = 10.sp,
                                    color = subTextColor,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = configKeyInput,
                                    onValueChange = {
                                        configKeyInput = it
                                        connectionTestResult = null
                                    },
                                    label = { Text("Supabase Anon / Publishable Key", fontSize = 11.sp) },
                                    placeholder = { Text("sb_publishable_... o eyJhbGciOi...") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = if (isGameMode) Color(0xFFCBD5E1) else Color(0xFF334155),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_supabase_key")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                if (connectionTestResult != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isConnectionSuccess) Color(0xFF052E16) else Color(0xFF450A0A),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isConnectionSuccess) Color(0xFF22C55E) else Color(0xFFEF4444)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isConnectionSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                                contentDescription = null,
                                                tint = if (isConnectionSuccess) Color(0xFF4ADE80) else Color(0xFFF87171),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = connectionTestResult ?: "",
                                                fontSize = 11.sp,
                                                color = if (isConnectionSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            isTestingConnection = true
                                            connectionTestResult = null
                                            scope.launch {
                                                val clean = SupabaseConfig.cleanUrl(configUrlInput)
                                                val res = SupabaseClient.testConnection(clean, configKeyInput)
                                                isTestingConnection = false
                                                if (res.isSuccess) {
                                                    isConnectionSuccess = true
                                                    connectionTestResult = res.getOrNull()
                                                    // Guardar automáticamente si funcionó
                                                    SupabaseConfig.updateCredentials(clean, configKeyInput)
                                                } else {
                                                    isConnectionSuccess = false
                                                    connectionTestResult = res.exceptionOrNull()?.message ?: "Error al conectar"
                                                }
                                            }
                                        },
                                        enabled = !isTestingConnection,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("btn_test_supabase_connection")
                                    ) {
                                        if (isTestingConnection) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Probar Conexión", fontSize = 12.sp, color = Color.White)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            SupabaseConfig.updateCredentials(configUrlInput, configKeyInput)
                                            connectionTestResult = "💾 ¡Credenciales guardadas!"
                                            isConnectionSuccess = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("btn_save_supabase_config")
                                    ) {
                                        Text("Guardar", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentUser != null) {
                        // Sincronización en la nube para usuario autenticado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Cola de sincronización:",
                                    fontSize = 12.sp,
                                    color = subTextColor
                                )
                                Text(
                                    text = if (pendingCount == 0) "Al día (0 pendientes)" else "$pendingCount datos listos para subir",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (pendingCount > 0) Color(0xFFF59E0B) else Color(0xFF10B981)
                                )
                            }

                            Button(
                                onClick = {
                                    SupabaseSyncManager.syncPendingNow { _, _ -> }
                                },
                                enabled = !isSyncing,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                modifier = Modifier.testTag("btn_sync_now")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sincronizar", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }

                        if (!lastSyncStatus.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = lastSyncStatus ?: "",
                                fontSize = 11.sp,
                                color = subTextColor,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // BOTÓN DE CERRAR SESIÓN
                        Button(
                            onClick = {
                                SupabaseAuthManager.signOut()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red.copy(alpha = 0.12f),
                                contentColor = Color(0xFFEF4444)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_logout")
                        ) {
                            Text("Cerrar Sesión", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        // FORMULARIO DE INICIAR SESIÓN / CREAR CUENTA
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isGameMode) Color(0xFFE2E8F0) else Color(0xFF26262E))
                                .padding(4.dp)
                        ) {
                            // Tab Iniciar Sesión
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!authModeIsSignUp) accentColor else Color.Transparent)
                                    .clickable {
                                        authModeIsSignUp = false
                                        authErrorMessage = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Iniciar Sesión",
                                    fontSize = 13.sp,
                                    fontWeight = if (!authModeIsSignUp) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!authModeIsSignUp) Color.White else subTextColor
                                )
                            }

                            // Tab Registrarse
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (authModeIsSignUp) accentColor else Color.Transparent)
                                    .clickable {
                                        authModeIsSignUp = true
                                        authErrorMessage = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Crear Cuenta",
                                    fontSize = 13.sp,
                                    fontWeight = if (authModeIsSignUp) FontWeight.Bold else FontWeight.Medium,
                                    color = if (authModeIsSignUp) Color.White else subTextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (authModeIsSignUp) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Nombre o Apodo de Jugador") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Person, contentDescription = null, tint = accentColor)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = subTextColor.copy(alpha = 0.3f),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = team,
                                onValueChange = { team = it },
                                label = { Text("Equipo / Club (Opcional)") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Group, contentDescription = null, tint = accentColor)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = subTextColor.copy(alpha = 0.3f),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo Electrónico") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Email, contentDescription = null, tint = accentColor)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = subTextColor.copy(alpha = 0.3f),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = accentColor)
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = subTextColor.copy(alpha = 0.3f),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )

                        if (!authErrorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val isRateLimit = authErrorMessage!!.contains("rate limit", ignoreCase = true) ||
                                    authErrorMessage!!.contains("límite de correos", ignoreCase = true)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isRateLimit) Color(0xFF451A03) else Color(0xFF450A0A),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isRateLimit) Color(0xFFF59E0B) else Color(0xFFEF4444)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isRateLimit) "⏳ Límite de Correos Alcanzado" else "⚠️ Atención",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRateLimit) Color(0xFFFBBF24) else Color(0xFFF87171)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = authErrorMessage!!,
                                        fontSize = 11.sp,
                                        color = if (isRateLimit) Color(0xFFFEF3C7) else Color(0xFFFEE2E2),
                                        lineHeight = 16.sp
                                    )

                                    if (isRateLimit) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFD97706),
                                            modifier = Modifier
                                                .clickable {
                                                    authModeIsSignUp = false
                                                    authErrorMessage = null
                                                }
                                        ) {
                                            Text(
                                                text = "👉 Cambiar a 'Iniciar Sesión' (por si ya existe)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!authSuccessMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = authSuccessMessage!!,
                                fontSize = 12.sp,
                                color = Color(0xFF10B981),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    authErrorMessage = "Por favor ingresa correo y contraseña"
                                    return@Button
                                }
                                isLoadingAuth = true
                                authErrorMessage = null
                                authSuccessMessage = null

                                scope.launch {
                                    if (authModeIsSignUp) {
                                        val finalUsername = if (username.isNotBlank()) username else playerStats.playerName
                                        val res = SupabaseAuthManager.signUp(email, password, finalUsername, team)
                                        isLoadingAuth = false
                                        if (res.isSuccess) {
                                            val u = res.getOrNull()
                                            if (u != null && u.accessToken != SupabaseConfig.anonKey && u.accessToken.isNotBlank()) {
                                                authSuccessMessage = "¡Cuenta creada y sesión iniciada con éxito!"
                                            } else {
                                                authSuccessMessage = "¡Cuenta creada en Supabase! Revisa tu correo electrónico para confirmar la cuenta (o inicia sesión si tu proyecto no requiere confirmación)."
                                            }
                                            PlayerStatsManager.updatePlayerName(finalUsername)
                                            SupabaseSyncManager.syncPendingNow()
                                        } else {
                                            authErrorMessage = res.exceptionOrNull()?.message ?: "Error al registrarse"
                                        }
                                    } else {
                                        val res = SupabaseAuthManager.signIn(email, password)
                                        isLoadingAuth = false
                                        if (res.isSuccess) {
                                            authSuccessMessage = "¡Sesión iniciada correctamente!"
                                            res.getOrNull()?.let { u ->
                                                if (u.username.isNotBlank()) {
                                                    PlayerStatsManager.updatePlayerName(u.username)
                                                }
                                            }
                                            SupabaseSyncManager.syncPendingNow()
                                        } else {
                                            authErrorMessage = res.exceptionOrNull()?.message ?: "Error al iniciar sesión"
                                        }
                                    }
                                }
                            },
                            enabled = !isLoadingAuth,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_auth_submit")
                        ) {
                            if (isLoadingAuth) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (authModeIsSignUp) "Crear Cuenta en Supabase" else "Iniciar Sesión",
                                    fontSize = 14.sp,
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
}

@Composable
private fun GameScoreStatCard(
    iconEmoji: String,
    gameTitle: String,
    totalPoints: Int,
    bestScore: Int,
    bestScoreLabel: String = "Récord",
    extraLabel: String,
    gamesPlayed: Int,
    gamesLabel: String = "Partidas",
    cardColor: Color,
    isGameMode: Boolean
) {
    val bg = if (isGameMode) Color.White else Color(0xFF22222B)
    val textColor = if (isGameMode) Color(0xFF0F172A) else Color.White
    val subTextColor = if (isGameMode) Color(0xFF64748B) else Color(0xFF94A3B8)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, cardColor.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(cardColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = iconEmoji, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = gameTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                // Badge de puntos acumulados / XP sumada
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+$totalPoints XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = cardColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = extraLabel,
                    fontSize = 11.sp,
                    color = subTextColor
                )
                Text(
                    text = "$bestScoreLabel: $bestScore  •  $gamesPlayed $gamesLabel",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun QuickTestChip(
    text: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
