package com.example.vision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theme.SportOrange
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Visual styles for the 2D Basketball Court top-down view.
 */
enum class CourtSurfaceStyle(val label: String) {
    TACTICAL_DARK("Pizarra Táctica"),
    HARDWOOD_PARQUET("Parquet Clásico")
}

/**
 * Display modes for player grouping and tactical analysis.
 */
enum class ClusterVisualizationMode(val label: String) {
    CLUSTERS_AND_HULLS("Clusters y Polígonos"),
    CENTROIDS_AND_SPREAD("Centroides y Dispersión"),
    POSITIONS_ONLY("Solo Posiciones")
}

/**
 * High-fidelity Compose component rendering a 2D top-down view of a basketball court
 * to visualize player positioning, spatial spacing, and team clusters during match analysis.
 */
@Composable
fun TopDownCourtAnalysisView(
    modifier: Modifier = Modifier,
    players: List<TacticalPlayerTrack> = remember { getSampleTacticalPlayers() },
    ballPosition: Pair<Float, Float>? = remember { Pair(0.52f, 0.68f) },
    activePlayBadge: TacticalPlayType? = null,
    activePlayDescription: String? = null,
    onPlayerSelected: ((TacticalPlayerTrack?) -> Unit)? = null
) {
    var selectedPlayer by remember { mutableStateOf<TacticalPlayerTrack?>(null) }
    var surfaceStyle by remember { mutableStateOf(CourtSurfaceStyle.TACTICAL_DARK) }
    var clusterMode by remember { mutableStateOf(ClusterVisualizationMode.CLUSTERS_AND_HULLS) }
    var isHalfCourtFocus by remember { mutableStateOf(false) }
    var showPlayerNumbers by remember { mutableStateOf(true) }
    var showDirectionVectors by remember { mutableStateOf(true) }

    // Separate players by team
    val homePlayers = remember(players) { players.filter { it.team == TacticalTeam.HOME } }
    val awayPlayers = remember(players) { players.filter { it.team == TacticalTeam.AWAY } }

    // Calculate cluster centroids & dispersion radius
    val homeCentroid = remember(homePlayers) { computeCentroid(homePlayers) }
    val awayCentroid = remember(awayPlayers) { computeCentroid(awayPlayers) }
    val homeSpread = remember(homePlayers, homeCentroid) { computeSpreadRadius(homePlayers, homeCentroid) }
    val awaySpread = remember(awayPlayers, awayCentroid) { computeSpreadRadius(awayPlayers, awayCentroid) }

    // Inter-cluster centroid distance (assuming regulation 28m x 15m court)
    val interClusterDistanceMeters = remember(homeCentroid, awayCentroid) {
        if (homeCentroid != null && awayCentroid != null) {
            val dxMeters = (homeCentroid.second - awayCentroid.second) * 28f // Court length is 28m
            val dyMeters = (homeCentroid.first - awayCentroid.first) * 15f   // Court width is 15m
            sqrt(dxMeters * dxMeters + dyMeters * dyMeters)
        } else {
            0f
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_down_court_analysis_view"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10131B)),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar: Title, Active Play indicator & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterCenterFocus,
                            contentDescription = "Pizarra 2D",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "RADAR 2D • CLUSTERS Y POSICIONAMIENTO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF00E5FF)
                        )
                        Text(
                            text = if (isHalfCourtFocus) "Enfoque: Media Cancha Ofensiva" else "Cancha Completa (28m x 15m)",
                            fontSize = 10.sp,
                            color = Color(0xAAFFFFFF)
                        )
                    }
                }

                // Interactive Toggles Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Half / Full court toggle
                    Box(
                        modifier = Modifier
                            .testTag("toggle_court_view_button")
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isHalfCourtFocus) Color(0xFF00E5FF) else Color(0x22FFFFFF))
                            .clickable { isHalfCourtFocus = !isHalfCourtFocus }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropLandscape,
                                contentDescription = null,
                                tint = if (isHalfCourtFocus) Color.Black else Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isHalfCourtFocus) "1/2 CANCHA" else "COMPLETA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isHalfCourtFocus) Color.Black else Color.White
                            )
                        }
                    }

                    // Surface style toggle (Pizarra / Parquet)
                    Box(
                        modifier = Modifier
                            .testTag("toggle_surface_style_button")
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22FFFFFF))
                            .clickable {
                                surfaceStyle = if (surfaceStyle == CourtSurfaceStyle.TACTICAL_DARK) {
                                    CourtSurfaceStyle.HARDWOOD_PARQUET
                                } else {
                                    CourtSurfaceStyle.TACTICAL_DARK
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = surfaceStyle.label.split(" ").first().uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Cluster Mode Switcher
                    Box(
                        modifier = Modifier
                            .testTag("toggle_clusters_mode_button")
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22FFFFFF))
                            .clickable {
                                val modes = ClusterVisualizationMode.values()
                                val nextIdx = (clusterMode.ordinal + 1) % modes.size
                                clusterMode = modes[nextIdx]
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = SportOrange,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = when (clusterMode) {
                                    ClusterVisualizationMode.CLUSTERS_AND_HULLS -> "HULLS"
                                    ClusterVisualizationMode.CENTROIDS_AND_SPREAD -> "CENTROIDES"
                                    ClusterVisualizationMode.POSITIONS_ONLY -> "PUNTOS"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = SportOrange
                            )
                        }
                    }
                }
            }

            // Tactical Play Banner (if present)
            if (activePlayBadge != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(activePlayBadge.badgeColor).copy(alpha = 0.15f))
                        .border(1.dp, Color(activePlayBadge.badgeColor), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = activePlayBadge.iconEmoji, fontSize = 14.sp)
                        Text(
                            text = "${activePlayBadge.title.uppercase()}: ${activePlayDescription ?: ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(activePlayBadge.badgeColor)
                        )
                    }
                }
            }

            // 2D BASKETBALL COURT CANVAS
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isHalfCourtFocus) 1.25f else 1.88f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(12.dp))
            ) {
                val boxWidthPx = constraints.maxWidth.toFloat()
                val boxHeightPx = constraints.maxHeight.toFloat()

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(players, isHalfCourtFocus) {
                            detectTapGestures { tapOffset ->
                                // Hit test players
                                val clicked = findClickedPlayer(
                                    tap = tapOffset,
                                    width = boxWidthPx,
                                    height = boxHeightPx,
                                    players = players,
                                    isHalfCourt = isHalfCourtFocus
                                )
                                selectedPlayer = if (selectedPlayer?.id == clicked?.id) null else clicked
                                onPlayerSelected?.invoke(selectedPlayer)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Court Background Surface
                    drawCourtSurface(w, h, surfaceStyle)

                    // 2. Draw Court Markings (Perimeter, Midcourt, Keys, 3pt lines, Baskets)
                    drawCourtMarkings(w, h, isHalfCourtFocus, surfaceStyle)

                    // 3. Draw Team Clusters (Convex Hulls, Centroids & Dispersion)
                    if (clusterMode != ClusterVisualizationMode.POSITIONS_ONLY) {
                        drawTeamClusters(
                            w = w,
                            h = h,
                            homePlayers = homePlayers,
                            awayPlayers = awayPlayers,
                            homeCentroid = homeCentroid,
                            awayCentroid = awayCentroid,
                            homeSpread = homeSpread,
                            awaySpread = awaySpread,
                            clusterMode = clusterMode,
                            isHalfCourt = isHalfCourtFocus
                        )
                    }

                    // 4. Draw Matchup Vector between Centroids
                    if (clusterMode == ClusterVisualizationMode.CENTROIDS_AND_SPREAD && homeCentroid != null && awayCentroid != null) {
                        val cHome = mapCourtToCanvas(homeCentroid.first, homeCentroid.second, w, h, isHalfCourtFocus)
                        val cAway = mapCourtToCanvas(awayCentroid.first, awayCentroid.second, w, h, isHalfCourtFocus)

                        drawLine(
                            color = Color(0x88FFFFFF),
                            start = cHome,
                            end = cAway,
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )
                    }

                    // 5. Draw Selected Player Highlight & Tactical Proximity lines
                    selectedPlayer?.let { player ->
                        val pPos = mapCourtToCanvas(player.xNorm, player.yNorm, w, h, isHalfCourtFocus)

                        // Outer targeting ring
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 18.dp.toPx(),
                            center = pPos,
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                        )

                        // Line to offense basket (at x=0.5, y=0.95 in full court)
                        val basketPos = mapCourtToCanvas(0.5f, 0.95f, w, h, isHalfCourtFocus)
                        drawLine(
                            color = Color(0x66FF6B1A),
                            start = pPos,
                            end = basketPos,
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )

                        // Find closest opponent
                        val opponents = if (player.team == TacticalTeam.HOME) awayPlayers else homePlayers
                        val closestOpponent = opponents.minByOrNull {
                            hypot(it.xNorm - player.xNorm, it.yNorm - player.yNorm)
                        }
                        closestOpponent?.let { opp ->
                            val oppPos = mapCourtToCanvas(opp.xNorm, opp.yNorm, w, h, isHalfCourtFocus)
                            drawLine(
                                color = Color(0xFFFF5252),
                                start = pPos,
                                end = oppPos,
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                            )
                        }
                    }

                    // 6. Draw Player Tokens
                    drawPlayers(
                        w = w,
                        h = h,
                        players = players,
                        selectedPlayer = selectedPlayer,
                        showPlayerNumbers = showPlayerNumbers,
                        showDirectionVectors = showDirectionVectors,
                        isHalfCourt = isHalfCourtFocus
                    )

                    // 7. Draw Ball
                    if (ballPosition != null) {
                        val bPos = mapCourtToCanvas(ballPosition.first, ballPosition.second, w, h, isHalfCourtFocus)
                        // Ball glow
                        drawCircle(
                            color = Color(0x66FF6B1A),
                            radius = 8.dp.toPx(),
                            center = bPos
                        )
                        // Ball outer ring
                        drawCircle(
                            color = Color.Black,
                            radius = 5.dp.toPx(),
                            center = bPos
                        )
                        // Ball core
                        drawCircle(
                            color = SportOrange,
                            radius = 4.dp.toPx(),
                            center = bPos
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 1.5.dp.toPx(),
                            center = bPos
                        )
                    }
                }
            }

            // 8. Selected Player Inspector Detail Pill (if a player is clicked)
            AnimatedVisibility(
                visible = selectedPlayer != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedPlayer?.let { player ->
                    val isHome = player.team == TacticalTeam.HOME
                    val teamName = if (isHome) "LOCAL" else "VISITANTE"
                    val teamColor = if (isHome) Color(0xFF00E5FF) else Color(0xFFFF5722)

                    // Calculate real-world court distance to basket and nearest defender
                    val distToHoopMeters = hypot((player.xNorm - 0.5f) * 15f, (player.yNorm - 0.95f) * 28f)
                    val opponents = if (isHome) awayPlayers else homePlayers
                    val closestDefender = opponents.minByOrNull {
                        hypot((it.xNorm - player.xNorm) * 15f, (it.yNorm - player.yNorm) * 28f)
                    }
                    val defenderDistMeters = closestDefender?.let {
                        hypot((it.xNorm - player.xNorm) * 15f, (it.yNorm - player.yNorm) * 28f)
                    } ?: 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF181B26))
                            .border(1.dp, teamColor, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(teamColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${player.id}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                }

                                Column {
                                    Text(
                                        text = "$teamName • ${player.role} ${if (player.isWithBall) "🏀 (CON BALÓN)" else ""}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = teamColor
                                    )
                                    Text(
                                        text = "Dist. al Aro: ${String.format("%.1f", distToHoopMeters)} m | Defensor cercano: ${String.format("%.1f", defenderDistMeters)} m",
                                        fontSize = 10.sp,
                                        color = Color(0xDDFFFFFF)
                                    )
                                }
                            }

                            // Close selection button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x33FFFFFF))
                                    .clickable { selectedPlayer = null }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "CERRAR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 9. Cluster Metrics Strip (Spacing, Compactness & Matchup Distance)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141722))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Cluster Metric
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF))
                    )
                    Column {
                        Text(
                            text = "LOCAL (BLANCO)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                        Text(
                            text = "Dispersión: ${String.format("%.1f", homeSpread * 15f)} m",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Inter-Cluster Distance
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DIST. ENTRE BLOQUES",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0x88FFFFFF)
                    )
                    Text(
                        text = "${String.format("%.1f", interClusterDistanceMeters)} m",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = SportOrange
                    )
                }

                // Away Cluster Metric
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5722))
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "VISITANTE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722)
                        )
                        Text(
                            text = "Compacidad: ${String.format("%.1f", awaySpread * 15f)} m",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// COURT RENDERING & DRAWING HELPERS
// -----------------------------------------------------------------------------------------

/**
 * Maps normalized court coordinates (xNorm: 0..1 width, yNorm: 0..1 length)
 * to 2D canvas pixel coordinates (Canvas X: sideline to sideline, Canvas Y: baseline to baseline).
 */
private fun mapCourtToCanvas(
    xNorm: Float,
    yNorm: Float,
    w: Float,
    h: Float,
    isHalfCourt: Boolean
): Offset {
    val margin = 8f
    val courtW = w - margin * 2
    val courtH = h - margin * 2

    return if (!isHalfCourt) {
        // Full court landscape: Length along X, Width along Y
        val cx = margin + yNorm * courtW
        val cy = margin + xNorm * courtH
        Offset(cx, cy)
    } else {
        // Half court focus (attacking basket side: yNorm 0.5 .. 1.0)
        val normalizedY = ((yNorm - 0.5f) / 0.5f).coerceIn(0f, 1f)
        val cx = margin + normalizedY * courtW
        val cy = margin + xNorm * courtH
        Offset(cx, cy)
    }
}

/**
 * Draws the court background surface (Tactical Dark or Realistic Hardwood Parquet).
 */
private fun DrawScope.drawCourtSurface(w: Float, h: Float, style: CourtSurfaceStyle) {
    if (style == CourtSurfaceStyle.TACTICAL_DARK) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF141926), Color(0xFF0B0E14)),
                center = Offset(w / 2f, h / 2f),
                radius = w * 0.7f
            ),
            size = Size(w, h)
        )
    } else {
        // Hardwood Parquet style
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF8B5A2B), Color(0xFF9E6B38), Color(0xFF7A4A20)),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            size = Size(w, h)
        )
        // Parquet plank slats pattern
        val plankWidth = 14f
        var x = 0f
        while (x < w) {
            drawLine(
                color = Color(0x15000000),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f
            )
            x += plankWidth
        }
    }
}

/**
 * Draws official FIBA/NBA court markings in regulation proportions.
 */
private fun DrawScope.drawCourtMarkings(
    w: Float,
    h: Float,
    isHalfCourt: Boolean,
    style: CourtSurfaceStyle
) {
    val lineColor = if (style == CourtSurfaceStyle.TACTICAL_DARK) Color(0x8800E5FF) else Color(0xEEFFFFFF)
    val lineStroke = 1.4.dp.toPx()
    val margin = 8f

    val courtLeft = margin
    val courtTop = margin
    val courtRight = w - margin
    val courtBottom = h - margin
    val courtW = courtRight - courtLeft
    val courtH = courtBottom - courtTop

    // Outer Boundary
    drawRect(
        color = lineColor,
        topLeft = Offset(courtLeft, courtTop),
        size = Size(courtW, courtH),
        style = Stroke(width = lineStroke)
    )

    if (!isHalfCourt) {
        // Midcourt line
        val midX = courtLeft + courtW / 2f
        drawLine(
            color = lineColor,
            start = Offset(midX, courtTop),
            end = Offset(midX, courtBottom),
            strokeWidth = lineStroke
        )

        // Center jump circle (radius ~ 1.8m in FIBA, approx 12% of court width)
        val centerRadius = courtH * 0.15f
        drawCircle(
            color = lineColor,
            radius = centerRadius,
            center = Offset(midX, courtTop + courtH / 2f),
            style = Stroke(width = lineStroke)
        )

        // LEFT KEY (Defensive Basket)
        drawHalfCourtMarkings(
            isRightSide = false,
            courtLeft = courtLeft,
            courtTop = courtTop,
            courtW = courtW,
            courtH = courtH,
            lineColor = lineColor,
            lineStroke = lineStroke,
            style = style
        )
    }

    // RIGHT KEY (Offensive Basket)
    drawHalfCourtMarkings(
        isRightSide = true,
        courtLeft = courtLeft,
        courtTop = courtTop,
        courtW = courtW,
        courtH = courtH,
        lineColor = lineColor,
        lineStroke = lineStroke,
        style = style,
        isHalfCourtFocus = isHalfCourt
    )
}

/**
 * Draws keys, 3-point arcs, backboards, and rims on one half of the court.
 */
private fun DrawScope.drawHalfCourtMarkings(
    isRightSide: Boolean,
    courtLeft: Float,
    courtTop: Float,
    courtW: Float,
    courtH: Float,
    lineColor: Color,
    lineStroke: Float,
    style: CourtSurfaceStyle,
    isHalfCourtFocus: Boolean = false
) {
    val keyWidth = if (!isHalfCourtFocus) courtW * 0.20f else courtW * 0.38f
    val keyHeight = courtH * 0.32f
    val keyTop = courtTop + (courtH - keyHeight) / 2f

    val baselineX = if (isRightSide) courtLeft + courtW else courtLeft
    val keyX = if (isRightSide) baselineX - keyWidth else baselineX

    // Key / Paint area tint
    val paintColor = if (style == CourtSurfaceStyle.TACTICAL_DARK) {
        if (isRightSide) Color(0x1800E5FF) else Color(0x10FFFFFF)
    } else {
        Color(0x331B5E20) // Deep forest green paint
    }
    drawRect(
        color = paintColor,
        topLeft = Offset(keyX, keyTop),
        size = Size(keyWidth, keyHeight)
    )

    // Key border
    drawRect(
        color = lineColor,
        topLeft = Offset(keyX, keyTop),
        size = Size(keyWidth, keyHeight),
        style = Stroke(width = lineStroke)
    )

    // Free throw circle
    val ftCircleCenter = Offset(
        if (isRightSide) keyX else keyX + keyWidth,
        courtTop + courtH / 2f
    )
    val ftRadius = keyHeight / 2f

    drawCircle(
        color = lineColor,
        radius = ftRadius,
        center = ftCircleCenter,
        style = Stroke(width = lineStroke)
    )

    // Three-point line
    val threePtArcRadius = courtH * 0.44f
    val arcCenter = Offset(
        if (isRightSide) baselineX - 12.dp.toPx() else baselineX + 12.dp.toPx(),
        courtTop + courtH / 2f
    )

    if (isRightSide) {
        drawArc(
            color = lineColor,
            startAngle = 100f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(arcCenter.x - threePtArcRadius, arcCenter.y - threePtArcRadius),
            size = Size(threePtArcRadius * 2, threePtArcRadius * 2),
            style = Stroke(width = lineStroke)
        )
    } else {
        drawArc(
            color = lineColor,
            startAngle = -80f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(arcCenter.x - threePtArcRadius, arcCenter.y - threePtArcRadius),
            size = Size(threePtArcRadius * 2, threePtArcRadius * 2),
            style = Stroke(width = lineStroke)
        )
    }

    // Backboard and Rim
    val backboardX = if (isRightSide) baselineX - 10.dp.toPx() else baselineX + 10.dp.toPx()
    val backboardLen = 22.dp.toPx()
    drawLine(
        color = Color.White,
        start = Offset(backboardX, courtTop + courtH / 2f - backboardLen / 2f),
        end = Offset(backboardX, courtTop + courtH / 2f + backboardLen / 2f),
        strokeWidth = 2.dp.toPx()
    )

    val rimX = if (isRightSide) backboardX - 4.dp.toPx() else backboardX + 4.dp.toPx()
    drawCircle(
        color = SportOrange,
        radius = 3.5.dp.toPx(),
        center = Offset(rimX, courtTop + courtH / 2f),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

// -----------------------------------------------------------------------------------------
// TEAM CLUSTERING & HULL COMPUTATION
// -----------------------------------------------------------------------------------------

/**
 * Draws team clusters using 2D Convex Hulls and Centroid Spreads.
 */
private fun DrawScope.drawTeamClusters(
    w: Float,
    h: Float,
    homePlayers: List<TacticalPlayerTrack>,
    awayPlayers: List<TacticalPlayerTrack>,
    homeCentroid: Pair<Float, Float>?,
    awayCentroid: Pair<Float, Float>?,
    homeSpread: Float,
    awaySpread: Float,
    clusterMode: ClusterVisualizationMode,
    isHalfCourt: Boolean
) {
    // 1. Draw Home Cluster
    drawSingleTeamCluster(
        w = w,
        h = h,
        teamPlayers = homePlayers,
        centroid = homeCentroid,
        spread = homeSpread,
        teamColor = Color(0xFF00E5FF),
        clusterMode = clusterMode,
        isHalfCourt = isHalfCourt
    )

    // 2. Draw Away Cluster
    drawSingleTeamCluster(
        w = w,
        h = h,
        teamPlayers = awayPlayers,
        centroid = awayCentroid,
        spread = awaySpread,
        teamColor = Color(0xFFFF5722),
        clusterMode = clusterMode,
        isHalfCourt = isHalfCourt
    )
}

/**
 * Renders cluster polygon and centroid for one specific team.
 */
private fun DrawScope.drawSingleTeamCluster(
    w: Float,
    h: Float,
    teamPlayers: List<TacticalPlayerTrack>,
    centroid: Pair<Float, Float>?,
    spread: Float,
    teamColor: Color,
    clusterMode: ClusterVisualizationMode,
    isHalfCourt: Boolean
) {
    if (teamPlayers.isEmpty()) return

    val canvasPoints = teamPlayers.map {
        mapCourtToCanvas(it.xNorm, it.yNorm, w, h, isHalfCourt)
    }

    // A. Convex Hull Polygon
    if (clusterMode == ClusterVisualizationMode.CLUSTERS_AND_HULLS && canvasPoints.size >= 3) {
        val hull = computeConvexHull(canvasPoints)
        if (hull.size >= 3) {
            val path = Path().apply {
                moveTo(hull[0].x, hull[0].y)
                for (i in 1 until hull.size) {
                    lineTo(hull[i].x, hull[i].y)
                }
                close()
            }

            // Fill cluster area
            drawPath(
                path = path,
                color = teamColor.copy(alpha = 0.12f),
                style = Fill
            )

            // Outline cluster contour
            drawPath(
                path = path,
                color = teamColor.copy(alpha = 0.50f),
                style = Stroke(
                    width = 1.6.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )
            )
        }
    }

    // B. Centroid & Spread Radius Circle
    if (centroid != null) {
        val cPos = mapCourtToCanvas(centroid.first, centroid.second, w, h, isHalfCourt)

        // Dispersion spread circle
        if (clusterMode == ClusterVisualizationMode.CENTROIDS_AND_SPREAD) {
            val spreadRadiusPx = spread * (h * 0.8f).coerceAtLeast(30f)
            drawCircle(
                color = teamColor.copy(alpha = 0.08f),
                radius = spreadRadiusPx,
                center = cPos
            )
            drawCircle(
                color = teamColor.copy(alpha = 0.35f),
                radius = spreadRadiusPx,
                center = cPos,
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            )
        }

        // Centroid Crosshair
        val crossLen = 6.dp.toPx()
        drawLine(
            color = teamColor,
            start = Offset(cPos.x - crossLen, cPos.y),
            end = Offset(cPos.x + crossLen, cPos.y),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = teamColor,
            start = Offset(cPos.x, cPos.y - crossLen),
            end = Offset(cPos.x, cPos.y + crossLen),
            strokeWidth = 2.dp.toPx()
        )
        drawCircle(
            color = teamColor,
            radius = 3.dp.toPx(),
            center = cPos
        )
    }
}

/**
 * Draws player tokens, jersey IDs, and motion vectors.
 */
private fun DrawScope.drawPlayers(
    w: Float,
    h: Float,
    players: List<TacticalPlayerTrack>,
    selectedPlayer: TacticalPlayerTrack?,
    showPlayerNumbers: Boolean,
    showDirectionVectors: Boolean,
    isHalfCourt: Boolean
) {
    val playerRadius = 7.dp.toPx()

    for (player in players) {
        val pos = mapCourtToCanvas(player.xNorm, player.yNorm, w, h, isHalfCourt)
        val isHome = player.team == TacticalTeam.HOME
        val teamColor = if (isHome) Color(0xFF00E5FF) else Color(0xFFFF5722)
        val isSelected = selectedPlayer?.id == player.id

        // Motion / Speed Vector Arrow
        if (showDirectionVectors && player.speedNorm > 0.08f) {
            // Direction pointing roughly towards basket or lateral cut
            val angleRad = if (isHome) 0f else Math.PI.toFloat() // Direction heading
            val arrowLen = (player.speedNorm * 28.dp.toPx()).coerceIn(10f, 32f)
            val endX = pos.x + cos(angleRad).toFloat() * arrowLen
            val endY = pos.y + sin(angleRad).toFloat() * arrowLen

            drawLine(
                color = teamColor.copy(alpha = 0.7f),
                start = pos,
                end = Offset(endX, endY),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Arrowhead
            val arrowHeadSize = 5f
            drawLine(
                color = teamColor.copy(alpha = 0.7f),
                start = Offset(endX, endY),
                end = Offset(endX - arrowHeadSize, endY - arrowHeadSize),
                strokeWidth = 1.8.dp.toPx()
            )
            drawLine(
                color = teamColor.copy(alpha = 0.7f),
                start = Offset(endX, endY),
                end = Offset(endX - arrowHeadSize, endY + arrowHeadSize),
                strokeWidth = 1.8.dp.toPx()
            )
        }

        // Ball possessor halo
        if (player.isWithBall) {
            drawCircle(
                color = Color(0x66FF6B1A),
                radius = playerRadius + 6.dp.toPx(),
                center = pos
            )
            drawCircle(
                color = SportOrange,
                radius = playerRadius + 3.dp.toPx(),
                center = pos,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Main Player Body Circle
        drawCircle(
            color = if (isSelected) Color.White else teamColor,
            radius = if (isSelected) playerRadius + 2.dp.toPx() else playerRadius,
            center = pos
        )

        // Inner ring
        drawCircle(
            color = if (isHome) Color.Black else Color.White,
            radius = playerRadius * 0.45f,
            center = pos
        )
    }
}

/**
 * Finds the closest player to a touch gesture on canvas.
 */
private fun findClickedPlayer(
    tap: Offset,
    width: Float,
    height: Float,
    players: List<TacticalPlayerTrack>,
    isHalfCourt: Boolean
): TacticalPlayerTrack? {
    val hitThresholdPx = 28.dp.value * 2f // generous 28dp hit target
    var closest: TacticalPlayerTrack? = null
    var minDistance = Float.MAX_VALUE

    for (p in players) {
        val pCanvas = mapCourtToCanvas(p.xNorm, p.yNorm, width, height, isHalfCourt)
        val dist = hypot(pCanvas.x - tap.x, pCanvas.y - tap.y)
        if (dist < minDistance && dist <= hitThresholdPx) {
            minDistance = dist
            closest = p
        }
    }
    return closest
}

// -----------------------------------------------------------------------------------------
// MATHEMATICAL CONVEX HULL & CENTROID UTILITIES
// -----------------------------------------------------------------------------------------

/**
 * Computes the 2D arithmetic centroid (mean position) of a list of players.
 */
private fun computeCentroid(players: List<TacticalPlayerTrack>): Pair<Float, Float>? {
    if (players.isEmpty()) return null
    var sumX = 0f
    var sumY = 0f
    for (p in players) {
        sumX += p.xNorm
        sumY += p.yNorm
    }
    return Pair(sumX / players.size, sumY / players.size)
}

/**
 * Computes the dispersion radius (standard deviation from centroid).
 */
private fun computeSpreadRadius(players: List<TacticalPlayerTrack>, centroid: Pair<Float, Float>?): Float {
    if (players.isEmpty() || centroid == null) return 0.2f
    var sumDistSq = 0f
    for (p in players) {
        val dx = p.xNorm - centroid.first
        val dy = p.yNorm - centroid.second
        sumDistSq += dx * dx + dy * dy
    }
    return sqrt(sumDistSq / players.size).coerceIn(0.08f, 0.45f)
}

/**
 * Computes the 2D Convex Hull of a set of 2D points using Andrew's Monotone Chain algorithm.
 * Runs in O(N log N) time, extremely fast and robust for any polygon rendering.
 */
private fun computeConvexHull(points: List<Offset>): List<Offset> {
    if (points.size <= 2) return points

    // Sort lexicographically by X, then Y
    val sorted = points.sortedWith(compareBy({ it.x }, { it.y }))

    fun crossProduct(o: Offset, a: Offset, b: Offset): Float {
        return (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
    }

    // Lower hull
    val lower = mutableListOf<Offset>()
    for (p in sorted) {
        while (lower.size >= 2 && crossProduct(lower[lower.size - 2], lower[lower.size - 1], p) <= 0) {
            lower.removeAt(lower.size - 1)
        }
        lower.add(p)
    }

    // Upper hull
    val upper = mutableListOf<Offset>()
    for (i in sorted.indices.reversed()) {
        val p = sorted[i]
        while (upper.size >= 2 && crossProduct(upper[upper.size - 2], upper[upper.size - 1], p) <= 0) {
            upper.removeAt(upper.size - 1)
        }
        upper.add(p)
    }

    // Remove duplicates at junction
    lower.removeAt(lower.size - 1)
    upper.removeAt(upper.size - 1)

    return lower + upper
}

/**
 * Realistic default sample player positions for Top-Down match analysis.
 * Represents a standard 5v5 half-court set: 5 Home Attackers in 4-Out 1-In spacing vs 5 Away Defenders.
 */
fun getSampleTacticalPlayers(): List<TacticalPlayerTrack> {
    return listOf(
        // HOME ATTACKERS (Cyan)
        TacticalPlayerTrack(id = 1, xNorm = 0.50f, yNorm = 0.66f, team = TacticalTeam.HOME, isWithBall = true, speedNorm = 0.25f, role = "Base (PG)"),
        TacticalPlayerTrack(id = 2, xNorm = 0.20f, yNorm = 0.75f, team = TacticalTeam.HOME, isWithBall = false, speedNorm = 0.12f, role = "Escolta (SG)"),
        TacticalPlayerTrack(id = 3, xNorm = 0.80f, yNorm = 0.75f, team = TacticalTeam.HOME, isWithBall = false, speedNorm = 0.10f, role = "Alero (SF)"),
        TacticalPlayerTrack(id = 4, xNorm = 0.15f, yNorm = 0.88f, team = TacticalTeam.HOME, isWithBall = false, speedNorm = 0.05f, role = "Ala-Pívot (PF)"),
        TacticalPlayerTrack(id = 5, xNorm = 0.48f, yNorm = 0.84f, team = TacticalTeam.HOME, isWithBall = false, speedNorm = 0.18f, role = "Pívot (C)"),

        // AWAY DEFENDERS (Orange)
        TacticalPlayerTrack(id = 6, xNorm = 0.50f, yNorm = 0.70f, team = TacticalTeam.AWAY, isWithBall = false, speedNorm = 0.22f, role = "Defensa Balón"),
        TacticalPlayerTrack(id = 7, xNorm = 0.24f, yNorm = 0.77f, team = TacticalTeam.AWAY, isWithBall = false, speedNorm = 0.08f, role = "Defensa Exterior"),
        TacticalPlayerTrack(id = 8, xNorm = 0.76f, yNorm = 0.77f, team = TacticalTeam.AWAY, isWithBall = false, speedNorm = 0.07f, role = "Defensa Exterior"),
        TacticalPlayerTrack(id = 9, xNorm = 0.20f, yNorm = 0.86f, team = TacticalTeam.AWAY, isWithBall = false, speedNorm = 0.06f, role = "Ayuda Esquina"),
        TacticalPlayerTrack(id = 10, xNorm = 0.50f, yNorm = 0.87f, team = TacticalTeam.AWAY, isWithBall = false, speedNorm = 0.14f, role = "Protector de Aro")
    )
}
