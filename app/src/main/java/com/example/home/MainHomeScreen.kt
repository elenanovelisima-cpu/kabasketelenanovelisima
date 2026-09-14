package com.example.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

enum class HomeBottomTab {
    HOME,
    THE_COURT,
    WORKOUT,
    PROFILE
}

data class HeroWorkoutSlide(
    val title: String,
    val subtitle: String = "Finish 5 more workouts to unlock Level 2!",
    val drillName: String,
    val duration: String = "2 MIN",
    val difficulty: String = "BEGINNER",
    val progressText: String = "0%",
    val isLocked: Boolean = false,
    val requiredXp: Int = 0,
    val currentXp: Int = 32,
    val levelNumber: Int = 1,
    val imageResId: Int = R.drawable.point,
    val onAction: () -> Unit
)

/**
 * Pantalla principal Home inspirada fielmente en la imagen de referencia:
 * - Header: WELCOME BACK SHARPWING3098! + Avatar H★ multicolor
 * - Banner incentivo: 🎉 Keep training and climbing the leaderboard!
 * - Tarjeta Hero convertida en Slide Horizontal (HorizontalPager) con diseño azul eléctrico,
 *   medidor 0%, botón Play negro y todas usando por el momento la imagen point.png.
 * - Fila con las 3 tarjetas de gamificación (ROOKIE, 0 DAYS, 0 XP) con badges flotantes.
 * - Barra inferior oscura con acceso a The Court, Workout, Profile y Home con botón elevado.
 */
@Composable
fun MainHomeScreen(
    onNavigateToWorkouts: () -> Unit,
    onLaunchDefendZoneDrill: () -> Unit = onNavigateToWorkouts,
    onLaunchDribbleDrill: () -> Unit = onNavigateToWorkouts,
    onLaunchReactionPointsDrill: () -> Unit = onNavigateToWorkouts,
    onLaunchShootingDrill: () -> Unit = onNavigateToWorkouts,
    onLaunchKidsMiniBasketDrill: () -> Unit = onNavigateToWorkouts,
    userHandle: String = "SHARPWING3098",
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(HomeBottomTab.HOME) }
    var isGameMode by rememberSaveable { mutableStateOf(true) }
    var showXpAdPopup by remember { mutableStateOf(false) }
    var showProInfoDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    val currentUser by com.example.supabase.SupabaseAuthManager.currentUser.collectAsState()
    val playerStats by com.example.stats.PlayerStatsManager.stats.collectAsState()
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "home_pulse_anim")
    val playPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "play_pulse"
    )
    val flameGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_glow"
    )

    // Slides del Hero ordenados según preferencia:
    // 1. REACTION POINTS: point.png (R.drawable.point)
    // 2. DEFEND THE ZONE: mano2.jpg (R.drawable.mano2)
    // 3. SHOOTING: tiro2.jpg (R.drawable.tiro2)
    // 4. KIDS MINI BASKET: tiro.jpg (R.drawable.tiro)
    // 5. DRIBBLING: img_drill_dribbling.jpg (R.drawable.img_drill_dribbling)
    // 6, 7, 8. 3 tarjetas de ejemplos bloqueadas con progreso de XP
    val heroSlides = remember(onLaunchDefendZoneDrill, onLaunchDribbleDrill, onLaunchReactionPointsDrill, onLaunchShootingDrill, onLaunchKidsMiniBasketDrill) {
        listOf(
            // 1. REACTION POINTS
            HeroWorkoutSlide(
                title = "NEXT UP: DRIBBLE RUSH",
                subtitle = "Toca los objetivos luminosos mientras mantienes el bote",
                drillName = "REACTION POINTS",
                duration = "2 MIN",
                difficulty = "BEGINNER",
                isLocked = false,
                requiredXp = 0,
                currentXp = 32,
                levelNumber = 1,
                imageResId = R.drawable.point,
                onAction = onLaunchReactionPointsDrill
            ),
            // 2. DEFEND THE ZONE
            HeroWorkoutSlide(
                title = "¡NUEVO! DEFEND THE ZONE",
                subtitle = "Esquiva los defensores fantasma y protege tu bote (3 vidas)",
                drillName = "DEFEND ZONE",
                duration = "1 MIN",
                difficulty = "PRO",
                isLocked = false,
                requiredXp = 50,
                currentXp = 32,
                levelNumber = 2,
                imageResId = R.drawable.mano2,
                onAction = onLaunchDefendZoneDrill
            ),
            // 3. SHOOTING
            HeroWorkoutSlide(
                title = "NEXT UP: SHOOTING FORM",
                subtitle = "Perfecciona tu mecánica, arco y aciertos",
                drillName = "SHOOTING",
                duration = "3 MIN",
                difficulty = "BEGINNER",
                isLocked = false,
                requiredXp = 100,
                currentXp = 32,
                levelNumber = 3,
                imageResId = R.drawable.tiro2,
                onAction = onLaunchShootingDrill
            ),
            // 4. KIDS MINI BASKET (TIRO INFANTIL EN CASA)
            HeroWorkoutSlide(
                title = "¡NUEVO! KIDS MINI BASKET",
                subtitle = "Tiro infantil en casa con calibración de aro y pelota por foto",
                drillName = "MINI BASKET",
                duration = "1 MIN",
                difficulty = "KIDS / CASA",
                isLocked = false,
                requiredXp = 0,
                currentXp = 32,
                levelNumber = 4,
                imageResId = R.drawable.tiro,
                onAction = onLaunchKidsMiniBasketDrill
            ),
            // 5. DRIBBLING
            HeroWorkoutSlide(
                title = "NEXT UP: DRIBBLING COMBO",
                subtitle = "Entrena tu control y precisión de bote",
                drillName = "DRIBBLING",
                duration = "2 MIN",
                difficulty = "BEGINNER",
                isLocked = false,
                requiredXp = 250,
                currentXp = 32,
                levelNumber = 5,
                imageResId = R.drawable.img_drill_dribbling,
                onAction = onLaunchDribbleDrill
            ),
            // 6. EJEMPLO BLOQUEADO 1: CROSSOVER TARGETS
            HeroWorkoutSlide(
                title = "CROSSOVER TARGETS",
                subtitle = "Sube de nivel para desbloquear este entrenamiento",
                drillName = "CROSSOVER",
                duration = "2 MIN",
                difficulty = "LEVEL 2",
                isLocked = true,
                requiredXp = 500,
                currentXp = 32,
                levelNumber = 6,
                imageResId = R.drawable.manos,
                onAction = { }
            ),
            // 7. EJEMPLO BLOQUEADO 2: STEP-BACK SHOOTING
            HeroWorkoutSlide(
                title = "STEP-BACK SHOOTING",
                subtitle = "Sube de nivel para desbloquear este entrenamiento",
                drillName = "STEP-BACK",
                duration = "3 MIN",
                difficulty = "LEVEL 3",
                isLocked = true,
                requiredXp = 750,
                currentXp = 32,
                levelNumber = 7,
                imageResId = R.drawable.img_onboarding_player,
                onAction = { }
            ),
            // 8. EJEMPLO BLOQUEADO 3: PRO AGILITY & ATTACK
            HeroWorkoutSlide(
                title = "PRO AGILITY & ATTACK",
                subtitle = "Sube de nivel para desbloquear este entrenamiento",
                drillName = "AGILITY PRO",
                duration = "4 MIN",
                difficulty = "ALL-STAR",
                isLocked = true,
                requiredXp = 1000,
                currentXp = 32,
                levelNumber = 8,
                imageResId = R.drawable.jugador,
                onAction = { }
            )
        )
    }

    val backgroundColor = if (isGameMode) Color.White else Color(0xFF121212)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Contenido principal según la pestaña activa
        if (selectedTab == HomeBottomTab.PROFILE) {
            com.example.profile.FullProfileScreen(
                onOpenSettings = { showProfileDialog = true },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            )
        } else {
            // Contenido scrolleable del Home
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 90.dp)
                    .verticalScroll(scrollState)
            ) {
                // 1. TOP HEADER (Avatar H★ a la izquierda + "Hola Miguel" + Toggle GAME / PRO a la derecha)
                val greetingName = currentUser?.username?.ifBlank { playerStats.playerName } ?: playerStats.playerName
                HomeHeaderRow(
                    userGreeting = "Hola $greetingName",
                    isGameMode = isGameMode,
                    onToggleMode = { newMode ->
                        val switchingToPro = isGameMode && !newMode
                        isGameMode = newMode
                        if (switchingToPro) {
                            showProInfoDialog = true
                        }
                    },
                    onProfileClick = { selectedTab = HomeBottomTab.PROFILE },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )

                // 2. BANNER PUBLICITARIO / PROMOCIONAL
                // (El banner de texto original "TU PUBLICIDAD AQUI!" se conserva oculto debajo por si se desea recuperar en cualquier momento)
                /*
                HomeIncentiveBanner(
                    text = "🎉 TU PUBLICIDAD AQUI!",
                    isGameMode = isGameMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
                */
                HomeImageBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. TARJETA HERO EN FORMATO SLIDE HORIZONTAL
                HeroCardsSlider(
                    slides = heroSlides,
                    isGameMode = isGameMode,
                    onTransitionToCard = { fromIndex, toIndex ->
                        // Si pasa de la tarjeta 4 (índice 3) a la tarjeta 5 (índice 4)
                        if (fromIndex == 3 && toIndex == 4) {
                            showXpAdPopup = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // En modo GAME se muestran las métricas de gamificación y leaderboard.
                // En modo TRAIN (profesional) se ocultan completamente como solicitado.
                if (isGameMode) {
                    Spacer(modifier = Modifier.height(26.dp))

                    // 4. FILA DE GAMIFICACIÓN: ROOKIE (Magenta), DIAS (Cyan), XP (Azul)
                    GamificationCardsRow(
                        flameGlowAlpha = flameGlowAlpha,
                        rankTitle = playerStats.rankTitle,
                        totalXp = playerStats.totalXp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 5. SECCIÓN LEADERBOARD (Podium Top 3, Players Near You, Boost XP)
                    LeaderboardSection(
                        userHandle = greetingName,
                        userXp = "${playerStats.totalXp} XP",
                        onViewMoreClick = onNavigateToWorkouts,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 5. BARRA DE NAVEGACIÓN INFERIOR (#2FB2C9 en GAME, negro #111114 en TRAIN, con textos e iconos en blanco)
        HomeBottomNavigationBar(
            selectedTab = selectedTab,
            isGameMode = isGameMode,
            onTabSelected = { tab ->
                selectedTab = tab
                if (tab == HomeBottomTab.WORKOUT || tab == HomeBottomTab.THE_COURT) {
                    onNavigateToWorkouts()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Popup de anuncio para ganar más XP (al pasar de la tarjeta 2 a la 3)
        if (showXpAdPopup) {
            XpAdRewardPopup(
                onDismiss = { showXpAdPopup = false },
                onWatchAdClick = {
                    // Acción fake: no hace nada y cierra el popup
                    showXpAdPopup = false
                }
            )
        }

        // Popup informativo del MODO PRO al activarlo
        if (showProInfoDialog) {
            ProModeInfoDialog(
                onDismiss = { showProInfoDialog = false }
            )
        }

        // Diálogo de Perfil y Sincronización Supabase Cloud (accesible desde el engranaje de ajustes)
        if (showProfileDialog) {
            com.example.supabase.ProfileCloudSyncDialog(
                isGameMode = isGameMode,
                onDismiss = {
                    showProfileDialog = false
                }
            )
        }
    }
}

/**
 * Encabezado con logo a la izquierda, saludo "Hola Miguel" y toggle GAME/TRAIN a la derecha.
 */
@Composable
private fun HomeHeaderRow(
    userGreeting: String = "Hola Miguel",
    isGameMode: Boolean,
    onToggleMode: (Boolean) -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val greetingTextColor = if (isGameMode) Color(0xFF1E2229) else Color.White

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // LADO IZQUIERDO: Logo Hoopstars + "Hola Miguel"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onProfileClick() }
        ) {
            // Avatar / Logo en la izquierda: Contorno de colores en GAME y negro en PRO + imagen jugador.png
            val avatarBorderBrush = if (isGameMode) {
                Brush.sweepGradient(
                    listOf(
                        Color(0xFF00E5FF),
                        Color(0xFFFF2A85),
                        Color(0xFFFFD600),
                        Color(0xFF00E5FF)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF000000),
                        Color(0xFF000000)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .testTag("home_avatar_badge")
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(avatarBorderBrush)
                    .padding(2.5.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.jugador),
                    contentDescription = "Avatar de jugador",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = userGreeting,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.3).sp,
                color = greetingTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // LADO DERECHO: Toggle GAME / PRO
        GameTrainToggle(
            isGameMode = isGameMode,
            onToggle = onToggleMode
        )
    }
}

/**
 * Pulsador / Switch interactivo táctil para alternar entre modo GAME y modo PRO.
 * Presenta un diseño de botón físico/switch con deslizador animado,
 * y un relieve que destaca claramente cuál está activo.
 */
@Composable
fun GameTrainToggle(
    isGameMode: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dimensiones del pulsador
    val trackWidth = 142.dp
    val trackHeight = 38.dp
    val thumbWidth = 69.dp
    val thumbHeight = 32.dp

    // Desplazamiento horizontal animado del thumb
    val thumbOffset by animateDpAsState(
        targetValue = if (isGameMode) 3.dp else (trackWidth - thumbWidth - 3.dp),
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "toggle_thumb_offset"
    )

    // Si es GAME: #2FB2C9. Si es PRO: naranja de baloncesto (#EA580C)
    val selectedColor = if (isGameMode) Color(0xFF2FB2C9) else Color(0xFFEA580C)
    // Fondo del toggle: #befdff en GAME, y negro cuando pulsas PRO
    val trackBackgroundColor = if (isGameMode) Color(0xFFBEFDFF) else Color(0xFF000000)

    Box(
        modifier = modifier
            .testTag("game_train_toggle")
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(50))
            .background(trackBackgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onToggle(!isGameMode)
            }
    ) {
        // 1. Deslizador físico / Thumb activo sin bordes
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 3.dp)
                .width(thumbWidth)
                .height(thumbHeight)
                .clip(RoundedCornerShape(50))
                .background(selectedColor)
        )

        // 2. Fila con las 2 opciones (GAME / PRO) sin iconos ni bordes
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Opción GAME: blanco cuando está activo o cuando PRO está seleccionado (fondo negro)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onToggle(true)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GAME",
                    fontSize = 11.sp,
                    fontWeight = if (isGameMode) FontWeight.Black else FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = if (isGameMode) Color.White else Color.White
                )
            }

            // Opción PRO: blanco cuando está seleccionado, o texto oscuro sobre fondo turquesa claro
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onToggle(false)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PRO",
                    fontSize = 11.sp,
                    fontWeight = if (!isGameMode) FontWeight.Black else FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = if (!isGameMode) Color.White else Color(0xFF0F4C5C)
                )
            }
        }
    }
}

/**
 * Banner superior de incentivo:
 * - Modo GAME: Magenta vibrante (#E91E63) con texto blanco
 * - Modo TRAIN: Negro (#1A1A1A o #000000) con borde sutil y texto blanco
 */
@Composable
private fun HomeIncentiveBanner(
    text: String,
    isGameMode: Boolean,
    modifier: Modifier = Modifier
) {
    val bannerBg = if (isGameMode) Color(0xFFE91E63) else Color(0xFF1E1E1E)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (!isGameMode) Modifier.border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)) else Modifier)
            .background(bannerBg)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Banner de imagen gráfico que utiliza banner1.png ajustado a la pantalla con bordes redondeados.
 */
@Composable
private fun HomeImageBanner(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .testTag("home_image_banner"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.banner1),
            contentDescription = "Banner Publicitario",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        )
    }
}

/**
 * Slide Horizontal que contiene las tarjetas Hero de entrenamiento.
 * Permite deslizar horizontalmente entre las tarjetas. Cada tarjeta muestra
 * completamente la imagen point.png sin ningún texto superpuesto ni elementos externos.
 */
@Composable
private fun HeroCardsSlider(
    slides: List<HeroWorkoutSlide>,
    isGameMode: Boolean = true,
    onTransitionToCard: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    var lastRecordedPage by remember { mutableStateOf(pagerState.currentPage) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { newPage ->
            if (newPage != lastRecordedPage) {
                onTransitionToCard(lastRecordedPage, newPage)
                lastRecordedPage = newPage
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val slide = slides[page]
            SingleHeroWorkoutCard(
                slide = slide,
                isGameMode = isGameMode,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Indicador de paginación sutil para el slider (naranja en modo PRO, azul #0D63F3 en modo GAME)
        val selectedIndicatorColor = if (isGameMode) Color(0xFF0D63F3) else Color(0xFFEA580C)
        val unselectedIndicatorColor = if (isGameMode) Color(0xFFD1D5DB) else Color(0xFF374151)

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until slides.size) {
                val isSelected = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) selectedIndicatorColor else unselectedIndicatorColor)
                )
            }
        }
    }
}

/**
 * Tarjeta Hero individual del slider.
 * Muestra la imagen point.png completa con bordes redondeados y un botón central
 * de tamaño normal con animación estilo bouncer (rebote suave continuo) para "¡JUGAR AHORA!".
 */
@Composable
private fun SingleHeroWorkoutCard(
    slide: HeroWorkoutSlide,
    isGameMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Animación continua estilo bouncer (rebote elástico suave y pulsación de escala) para el botón de jugar
    val infiniteTransition = rememberInfiniteTransition(label = "hero_bouncer_transition")
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_bouncer_scale"
    )
    val bounceOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_bouncer_offset_y"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                if (!slide.isLocked) {
                    slide.onAction()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Imagen base de la tarjeta (en modo bloqueado se muestra en escala de grises atenuada)
        val grayscaleFilter = remember {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.05f) })
        }
        Image(
            painter = painterResource(id = slide.imageResId),
            contentDescription = slide.drillName,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Crop,
            colorFilter = if (slide.isLocked) grayscaleFilter else null
        )

        if (slide.isLocked) {
            // Fondo gris oscuro semitransparente que bloquea visualmente la tarjeta
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD91F2937)) // Gris oscuro carbón elegante
            )

            // Cartel central translúcido con candado y XP requerida
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xCC0F172A)) // Cartel oscuro tipo vidrio esmerilado
                    .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(22.dp))
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Candado dorado/bronce centrado con halo circular sutil
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0x40EAB308),
                                        Color(0x10EAB308),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(2.dp, Color(0xFFD97706), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloqueado",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Texto instructivo "Sube de nivel para desbloquear"
                    Text(
                        text = "Sube de nivel para desbloquear",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E8F0),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Barra de progreso de XP (turquesa #2FB2C9 con base oscura)
                    val progress = remember(slide.currentXp, slide.requiredXp) {
                        if (slide.requiredXp > 0) {
                            (slide.currentXp.toFloat() / slide.requiredXp.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF2FB2C9),
                                            Color(0xFF56D6EB)
                                        )
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cartel REQUERIDO: XXX XP en turquesa (#2FB2C9)
                    Text(
                        text = "REQUERIDO: ${slide.requiredXp} XP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color(0xFF2FB2C9),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Indicador de XP actual únicamente (texto del nivel quitado)
                    Text(
                        text = "XP actual: ${slide.currentXp}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Botón central con animación estilo bouncer: naranja en modo PRO, turquesa (#2FB2C9) en modo GAME
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

            Box(
                modifier = Modifier
                    .offset(y = bounceOffsetY.dp)
                    .scale(bounceScale)
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = shadowAmbient,
                        spotColor = shadowSpot
                    )
                    .clip(RoundedCornerShape(50))
                    .background(buttonGradient)
                    .clickable { slide.onAction() }
                    .padding(horizontal = 28.dp, vertical = 15.dp)
                    .testTag("hero_card_play_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isGameMode) "¡JUGAR AHORA!" else "¡ENTRENAR AHORA!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Fila con las 3 tarjetas de gamificación (Rookie, 0 Days, 0 XP)
 * idéntica a la imagen de referencia con badges circulares flotantes superiores.
 */
@Composable
private fun GamificationCardsRow(
    flameGlowAlpha: Float,
    rankTitle: String = "ROOKIE",
    totalXp: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. ROOKIE / PRO / MVP CARD (Magenta / Rosa)
        GamificationBadgeCard(
            topBadgeContent = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD2E3))
                        .border(2.dp, Color(0xFFE91E63), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "MVP",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFC2185B),
                            lineHeight = 9.sp
                        )
                        Text(
                            text = "🏆",
                            fontSize = 12.sp
                        )
                    }
                }
            },
            title = rankTitle,
            subtitle = "Sigue subiendo",
            cardColor = when (rankTitle) {
                "MVP" -> Color(0xFFF59E0B)
                "ALL-STAR" -> Color(0xFF8B5CF6)
                "PRO" -> Color(0xFF00BCD4)
                else -> Color(0xFFE91E63)
            },
            modifier = Modifier.weight(1f)
        )

        // 2. 0 DAYS STREAK CARD (Cyan / Turquesa con llama animada)
        GamificationBadgeCard(
            topBadgeContent = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB2EBF2))
                        .border(2.dp, Color(0xFF00BCD4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔥",
                        fontSize = 18.sp,
                        modifier = Modifier.scale(flameGlowAlpha)
                    )
                }
            },
            title = "3 DIAS",
            subtitle = "No rompas la racha",
            cardColor = Color(0xFF00ACC1),
            modifier = Modifier.weight(1f)
        )

        // 3. XP CARD (Royal Blue)
        GamificationBadgeCard(
            topBadgeContent = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFBBDEFB))
                        .border(2.dp, Color(0xFF2196F3), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1565C0)
                    )
                }
            },
            title = "$totalXp",
            subtitle = "Experiencia",
            cardColor = Color(0xFF1E88E5),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Componente individual de tarjeta con badge flotante superior.
 */
@Composable
private fun GamificationBadgeCard(
    topBadgeContent: @Composable () -> Unit,
    title: String,
    subtitle: String,
    cardColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        // Cuerpo de la tarjeta con esquinas redondeadas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(cardColor)
                .padding(top = 28.dp, bottom = 14.dp, start = 4.dp, end = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xEEFFFFFF),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Badge flotante en la parte superior que sobresale hacia arriba
        Box(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            topBadgeContent()
        }
    }
}

/**
 * Barra de navegación inferior:
 * - Modo GAME: Barra en color turquesa (#2FB2C9).
 * - Modo PRO/TRAIN: Barra en color negro (#111114).
 * - Todos los elementos (Inicio, The Court, Workout, Perfil) integrados armónicamente en el diseño del menú.
 * - Modo GAME: Barra en color turquesa (#2FB2C9).
 * - Modo PRO/TRAIN: Barra en color negro (#111114).
 * - Resaltado sutil pero claro del icono de la pantalla activa mediante una cápsula translúcida,
 *   escala suave, contraste de opacidad y punto indicador activo.
 */
@Composable
private fun HomeBottomNavigationBar(
    selectedTab: HomeBottomTab,
    isGameMode: Boolean,
    onTabSelected: (HomeBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val barBackgroundColor = if (isGameMode) Color(0xFF2FB2C9) else Color(0xFF111114)
    val topBorderColor = if (isGameMode) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Barra de navegación con línea superior sutil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(barBackgroundColor)
                .border(
                    width = 1.dp,
                    color = topBorderColor,
                    shape = androidx.compose.ui.graphics.RectangleShape
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Inicio
                HomeNavItem(
                    tab = HomeBottomTab.HOME,
                    selectedTab = selectedTab,
                    isGameMode = isGameMode,
                    label = "Inicio",
                    onTabSelected = onTabSelected,
                    icon = { tint, iconMod ->
                        BasketballBallNavIcon(
                            color = tint,
                            modifier = iconMod
                        )
                    }
                )

                // 2. The Court
                HomeNavItem(
                    tab = HomeBottomTab.THE_COURT,
                    selectedTab = selectedTab,
                    isGameMode = isGameMode,
                    label = "The Court",
                    onTabSelected = onTabSelected,
                    icon = { tint, iconMod ->
                        BasketballCourtNavIcon(
                            color = tint,
                            modifier = iconMod
                        )
                    }
                )

                // 3. Workout
                HomeNavItem(
                    tab = HomeBottomTab.WORKOUT,
                    selectedTab = selectedTab,
                    isGameMode = isGameMode,
                    label = "Workout",
                    onTabSelected = onTabSelected,
                    icon = { tint, iconMod ->
                        BasketballHoopNavIcon(
                            color = tint,
                            modifier = iconMod
                        )
                    }
                )

                // 4. Perfil
                HomeNavItem(
                    tab = HomeBottomTab.PROFILE,
                    selectedTab = selectedTab,
                    isGameMode = isGameMode,
                    label = "Perfil",
                    onTabSelected = onTabSelected,
                    icon = { tint, iconMod ->
                        Icon(
                            imageVector = if (selectedTab == HomeBottomTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Perfil",
                            tint = tint,
                            modifier = iconMod
                        )
                    }
                )
            }
        }
    }
}

/**
 * Elemento individual del menú inferior con una cápsula redondeada que envuelve
 * tanto el icono como el texto de la pantalla activa, sin punto inferior.
 */
@Composable
private fun HomeNavItem(
    tab: HomeBottomTab,
    selectedTab: HomeBottomTab,
    isGameMode: Boolean,
    label: String,
    onTabSelected: (HomeBottomTab) -> Unit,
    icon: @Composable (tint: Color, modifier: Modifier) -> Unit
) {
    val isSelected = selectedTab == tab

    // Colores de la cápsula redondeada que envuelve tanto icono como texto
    val activePillBg = if (isGameMode) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.White.copy(alpha = 0.13f)
    }
    val activePillBorder = if (isGameMode) {
        Color.White.copy(alpha = 0.40f)
    } else {
        Color.White.copy(alpha = 0.26f)
    }

    val animatedPillBg by animateColorAsState(
        targetValue = if (isSelected) activePillBg else Color.Transparent,
        animationSpec = tween(220),
        label = "nav_pill_bg_$label"
    )
    val animatedPillBorder by animateColorAsState(
        targetValue = if (isSelected) activePillBorder else Color.Transparent,
        animationSpec = tween(220),
        label = "nav_pill_border_$label"
    )

    // Tono del icono
    val iconColor = if (isSelected) {
        Color.White
    } else {
        if (isGameMode) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.45f)
    }
    val animatedIconColor by animateColorAsState(
        targetValue = iconColor,
        animationSpec = tween(220),
        label = "nav_icon_color_$label"
    )

    // Tono del texto
    val textColor = if (isSelected) {
        Color.White
    } else {
        if (isGameMode) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.48f)
    }
    val animatedTextColor by animateColorAsState(
        targetValue = textColor,
        animationSpec = tween(220),
        label = "nav_text_color_$label"
    )

    // Ligera escala del icono activo para mayor dinamismo sutil
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = tween(220),
        label = "nav_icon_scale_$label"
    )

    // Cápsula redondeada que envuelve tanto el icono como el texto
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(animatedPillBg)
            .border(
                width = 1.dp,
                color = animatedPillBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTabSelected(tab) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("nav_tab_${label.lowercase().replace(" ", "_")}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon(
                animatedIconColor,
                Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = animatedTextColor
            )
        }
    }
}

@Composable
private fun BasketballCourtNavIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.1f, h * 0.15f),
            size = Size(w * 0.8f, h * 0.7f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style = Stroke(width = 1.6.dp.toPx())
        )

        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.15f),
            end = Offset(w * 0.5f, h * 0.85f),
            strokeWidth = 1.4.dp.toPx()
        )

        drawCircle(
            color = color,
            radius = w * 0.16f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = 1.4.dp.toPx())
        )
    }
}

@Composable
private fun BasketballHoopNavIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val boardLeft = w * 0.15f
        val boardRight = w * 0.85f
        val boardTop = h * 0.15f
        val boardBottom = h * 0.48f

        drawRoundRect(
            color = color,
            topLeft = Offset(boardLeft, boardTop),
            size = Size(boardRight - boardLeft, boardBottom - boardTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )

        val innerLeft = w * 0.35f
        val innerRight = w * 0.65f
        val innerTop = h * 0.28f
        val innerBottom = h * 0.48f
        drawRect(
            color = color,
            topLeft = Offset(innerLeft, innerTop),
            size = Size(innerRight - innerLeft, innerBottom - innerTop),
            style = Stroke(width = 1.2.dp.toPx())
        )

        val rimY = h * 0.50f
        val rimLeft = w * 0.30f
        val rimRight = w * 0.70f
        drawLine(
            color = color,
            start = Offset(rimLeft, rimY),
            end = Offset(rimRight, rimY),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )

        val netBottomY = h * 0.82f
        val netBottomLeft = w * 0.38f
        val netBottomRight = w * 0.62f

        drawLine(
            color = color,
            start = Offset(rimLeft, rimY),
            end = Offset(netBottomLeft, netBottomY),
            strokeWidth = 1.4.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(rimRight, rimY),
            end = Offset(netBottomRight, netBottomY),
            strokeWidth = 1.4.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(netBottomLeft, netBottomY),
            end = Offset(netBottomRight, netBottomY),
            strokeWidth = 1.4.dp.toPx()
        )
    }
}

@Composable
private fun BasketballBallNavIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = w * 0.44f

            // Círculo exterior
            drawCircle(
                color = color,
                radius = r,
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(width = 1.6.dp.toPx())
            )

            // Línea central horizontal
            drawLine(
                color = color,
                start = Offset(w * 0.08f, h * 0.5f),
                end = Offset(w * 0.92f, h * 0.5f),
                strokeWidth = 1.2.dp.toPx()
            )

            // Línea central vertical
            drawLine(
                color = color,
                start = Offset(w * 0.5f, h * 0.08f),
                end = Offset(w * 0.5f, h * 0.92f),
                strokeWidth = 1.2.dp.toPx()
            )
        }

        Text(
            text = "H★",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

/**
 * Cuadro de diálogo / Popup emergente idéntico al diseño de referencia:
 * - Fondo negro carbón (#151515) con esquinas redondeadas generosas (~28dp)
 * - Botón circular de cierre "X" en la esquina superior derecha
 * - Logo circular centrado de Hoopstars (balón multicolor turquesa/magenta/amarillo con H★)
 * - Título en negrita mayúsculas: "ENJOYING THE APP?"
 * - Subtítulo: "Switch to Pro and unlock all drills, levels, and premium features."
 * - Botón principal con color de fondo #2FB2C9: "BECOME A PRO"
 * - Enlace discreto abajo: "Maybe later"
 */
@Composable
private fun XpAdRewardPopup(
    onDismiss: () -> Unit,
    onWatchAdClick: () -> Unit
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
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF141416))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // Botón cerrar (X) en la esquina superior derecha con fondo circular oscuro
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF26262B))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color(0xFFD0D0D5),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                // 1. Logo circular grande exactamente como en la imagen
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFF2FB2C9),
                                    Color(0xFFE91E63),
                                    Color(0xFFFFD600),
                                    Color(0xFF2FB2C9)
                                )
                            )
                        )
                        .padding(3.5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2FB2C9)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Curvas de balón / costuras con estilo
                        drawCircle(
                            color = Color(0xFFE91E63),
                            radius = w * 0.45f,
                            style = Stroke(width = 3.5f)
                        )
                        drawLine(
                            color = Color(0xFF111827),
                            start = Offset(0f, h * 0.5f),
                            end = Offset(w, h * 0.5f),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = Color(0xFF111827),
                            start = Offset(w * 0.5f, 0f),
                            end = Offset(w * 0.5f, h),
                            strokeWidth = 3f
                        )
                    }

                    Text(
                        text = "H★",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF111827)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 2. Título principal idéntico a la imagen
                Text(
                    text = "ENJOYING THE APP?",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Subtítulo limpio y centrado
                Text(
                    text = "Switch to Pro and unlock all drills,\nlevels, and premium features.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFB5B5BE),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 4. Botón principal con color de fondo #2FB2C9
                Button(
                    onClick = onWatchAdClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2FB2C9)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "BECOME A PRO",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 5. Enlace "Maybe later"
                Text(
                    text = "Maybe later",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E8E98),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Diálogo popup informativo para el MODO PRO:
 * - Fondo negro profundo (#141416) idéntico al estilo del popup de tarjetas.
 * - Icono superior con aro estilizado en naranja PRO (#EA580C) e insignia "PRO".
 * - Explica que el Modo PRO está diseñado para entrenar sin distracciones:
 *   sin clasificaciones, sin XP ni ruidos, enfocado 100% en la práctica con análisis e informes avanzados solo para ti.
 */
@Composable
private fun ProModeInfoDialog(
    onDismiss: () -> Unit
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
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF141416))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // Botón cerrar (X) en la esquina superior derecha
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF26262B))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = Color(0xFFD0D0D5),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // 1. Icono circular con halo naranja PRO
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFEA580C),
                                    Color(0xFFFF7A00),
                                    Color(0xFFF59E0B),
                                    Color(0xFFEA580C)
                                )
                            )
                        )
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1917)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Líneas baloncestísticas sutiles
                        drawCircle(
                            color = Color(0xFFEA580C).copy(alpha = 0.4f),
                            radius = w * 0.44f,
                            style = Stroke(width = 2.5f)
                        )
                        drawLine(
                            color = Color(0xFFEA580C).copy(alpha = 0.5f),
                            start = Offset(0f, h * 0.5f),
                            end = Offset(w, h * 0.5f),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color(0xFFEA580C).copy(alpha = 0.5f),
                            start = Offset(w * 0.5f, 0f),
                            end = Offset(w * 0.5f, h),
                            strokeWidth = 2f
                        )
                    }

                    Text(
                        text = "PRO",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color(0xFFEA580C)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Título principal en blanco
                Text(
                    text = "BIENVENIDO AL MODO PRO",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Subtítulo destacando el entrenamiento sin distracciones
                Text(
                    text = "Tu espacio de alto rendimiento enfocado 100% en ti.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEA580C),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Tarjetas de características clave
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProFeatureRow(
                        bullet = "⚡",
                        title = "Cero distracciones",
                        description = "Sin tablas de clasificación ni puntos XP. Toda la atención puesta en la pista y en cada repetición."
                    )
                    ProFeatureRow(
                        bullet = "📊",
                        title = "Análisis e informes avanzados",
                        description = "Estadísticas detalladas de tu precisión, velocidad y postura registradas de forma privada y exclusiva para ti."
                    )
                    ProFeatureRow(
                        bullet = "🎯",
                        title = "Entrenamiento directo",
                        description = "Acceso inmediato a todos los drills y modos tácticos diseñados para llevar tu juego al siguiente nivel."
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Botón de acción principal en naranja PRO
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA580C)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "ENTENDIDO, A ENTRENAR",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ProFeatureRow(
    bullet: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1D1D22))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = bullet,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 1.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF9CA3AF),
                lineHeight = 16.sp
            )
        }
    }
}


