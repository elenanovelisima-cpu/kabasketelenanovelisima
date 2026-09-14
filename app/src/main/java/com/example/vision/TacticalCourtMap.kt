package com.example.vision

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theme.SportBorder
import com.example.theme.SportOrange

@Composable
fun TacticalCourtMap(
    analysis: TacticalFrameAnalysis?,
    modifier: Modifier = Modifier
) {
    TacticalCourtMap(
        players = analysis?.players ?: emptyList(),
        ballPosition = analysis?.ballPosition,
        activePlayBadge = analysis?.activePlayBadge,
        activePlayDescription = analysis?.activePlayDescription,
        spacingArea = analysis?.offensiveSpacingArea ?: 0.74f,
        modifier = modifier
    )
}

@Composable
fun TacticalCourtMap(
    players: List<TacticalPlayerTrack>,
    ballPosition: Pair<Float, Float>?,
    activePlayBadge: TacticalPlayType? = null,
    activePlayDescription: String? = null,
    spacingArea: Float = 0.74f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xE610121A))
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(16.dp))
            .testTag("tactical_court_map")
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live Tactical Play Banner if any pattern is detected
            AnimatedVisibility(
                visible = activePlayBadge != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (activePlayBadge != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(activePlayBadge.badgeColor).copy(alpha = 0.20f))
                            .border(1.dp, Color(activePlayBadge.badgeColor), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = activePlayBadge.iconEmoji,
                                fontSize = 14.sp
                            )
                            Column {
                                Text(
                                    text = activePlayBadge.title.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = Color(activePlayBadge.badgeColor)
                                )
                                if (!activePlayDescription.isNullOrEmpty()) {
                                    Text(
                                        text = activePlayDescription,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tactical 2D Full Basketball Court (Length x Width)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D1117))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(width = 320.dp, height = 190.dp)) {
                    val w = size.width
                    val h = size.height
                    val courtLine = Color(0x55FFFFFF)
                    val strokeW = 1.2.dp.toPx()

                    // Full court outer boundary
                    drawRect(
                        color = courtLine,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(w - 8.dp.toPx(), h - 8.dp.toPx()),
                        style = Stroke(width = strokeW)
                    )

                    // Midcourt line
                    drawLine(
                        color = courtLine,
                        start = Offset(w / 2f, 4.dp.toPx()),
                        end = Offset(w / 2f, h - 4.dp.toPx()),
                        strokeWidth = strokeW
                    )

                    // Center jump circle
                    drawCircle(
                        color = courtLine,
                        radius = 20.dp.toPx(),
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = strokeW)
                    )

                    // Left Basket / Key (Defense hoop)
                    val keyW = 44.dp.toPx()
                    val keyH = 50.dp.toPx()
                    drawRect(
                        color = courtLine,
                        topLeft = Offset(4.dp.toPx(), (h - keyH) / 2f),
                        size = Size(keyW, keyH),
                        style = Stroke(width = strokeW)
                    )
                    // Left free-throw circle
                    drawCircle(
                        color = courtLine,
                        radius = keyH / 2f,
                        center = Offset(4.dp.toPx() + keyW, h / 2f),
                        style = Stroke(width = strokeW)
                    )
                    // Left backboard and rim
                    drawLine(
                        color = Color.White,
                        start = Offset(10.dp.toPx(), h / 2f - 10.dp.toPx()),
                        end = Offset(10.dp.toPx(), h / 2f + 10.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(
                        color = SportOrange,
                        radius = 3.dp.toPx(),
                        center = Offset(13.dp.toPx(), h / 2f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Left 3-point arc
                    drawArc(
                        color = courtLine,
                        startAngle = -75f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = Offset(-10.dp.toPx(), 10.dp.toPx()),
                        size = Size(80.dp.toPx(), h - 20.dp.toPx()),
                        style = Stroke(width = strokeW)
                    )

                    // Right Basket / Key (Offense hoop)
                    drawRect(
                        color = courtLine,
                        topLeft = Offset(w - 4.dp.toPx() - keyW, (h - keyH) / 2f),
                        size = Size(keyW, keyH),
                        style = Stroke(width = strokeW)
                    )
                    // Right free-throw circle
                    drawCircle(
                        color = courtLine,
                        radius = keyH / 2f,
                        center = Offset(w - 4.dp.toPx() - keyW, h / 2f),
                        style = Stroke(width = strokeW)
                    )
                    // Right backboard and rim
                    drawLine(
                        color = Color.White,
                        start = Offset(w - 10.dp.toPx(), h / 2f - 10.dp.toPx()),
                        end = Offset(w - 10.dp.toPx(), h / 2f + 10.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(
                        color = SportOrange,
                        radius = 3.dp.toPx(),
                        center = Offset(w - 13.dp.toPx(), h / 2f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Right 3-point arc
                    drawArc(
                        color = courtLine,
                        startAngle = 105f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = Offset(w - 70.dp.toPx(), 10.dp.toPx()),
                        size = Size(80.dp.toPx(), h - 20.dp.toPx()),
                        style = Stroke(width = strokeW)
                    )

                    // Spacing Polygon for attacking team (if 3+ players)
                    val attackingTeam = players.firstOrNull { it.isWithBall }?.team ?: TacticalTeam.HOME
                    val attackers = players.filter { it.team == attackingTeam }
                    if (attackers.size >= 3) {
                        val polyPath = Path()
                        val sortedAttackers = attackers.sortedBy { kotlin.math.atan2(it.yNorm - 0.5f, it.xNorm - 0.5f) }
                        sortedAttackers.forEachIndexed { i, p ->
                            // Map xNorm (court width 0..1) to canvas Y (sideline to sideline)
                            // Map yNorm (court length 0..1) to canvas X (baseline to baseline)
                            val px = 8.dp.toPx() + p.yNorm * (w - 16.dp.toPx())
                            val py = 8.dp.toPx() + p.xNorm * (h - 16.dp.toPx())
                            if (i == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
                        }
                        polyPath.close()

                        drawPath(
                            path = polyPath,
                            color = Color(attackingTeam.colorCode).copy(alpha = 0.12f)
                        )
                        drawPath(
                            path = polyPath,
                            color = Color(attackingTeam.colorCode).copy(alpha = 0.35f),
                            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Draw Players
                    val playerDotRadius = 6.dp.toPx()
                    for (player in players) {
                        val px = 8.dp.toPx() + player.yNorm * (w - 16.dp.toPx())
                        val py = 8.dp.toPx() + player.xNorm * (h - 16.dp.toPx())
                        val teamColor = Color(player.team.colorCode)

                        // Ball halo if player possesses the ball
                        if (player.isWithBall) {
                            drawCircle(
                                color = Color(0x66FF6B1A),
                                radius = playerDotRadius + 6.dp.toPx(),
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = SportOrange,
                                radius = playerDotRadius + 3.dp.toPx(),
                                center = Offset(px, py),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }

                        // Player base dot
                        drawCircle(
                            color = teamColor,
                            radius = playerDotRadius,
                            center = Offset(px, py)
                        )
                        // Inner ring / white core
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = playerDotRadius * 0.45f,
                            center = Offset(px, py)
                        )
                    }

                    // Draw Ball (if not directly on a player)
                    if (ballPosition != null) {
                        val bx = 8.dp.toPx() + ballPosition.second * (w - 16.dp.toPx())
                        val by = 8.dp.toPx() + ballPosition.first * (h - 16.dp.toPx())
                        drawCircle(
                            color = SportOrange,
                            radius = 4.5.dp.toPx(),
                            center = Offset(bx, by)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = Offset(bx, by)
                        )
                    }
                }
            }

            // Legend & Spacing bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team colors legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(TacticalTeam.HOME.colorCode))
                        )
                        Text(
                            text = "LOCAL (BLANCO)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xCCFFFFFF)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(TacticalTeam.AWAY.colorCode))
                        )
                        Text(
                            text = "VISITANTE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xCCFFFFFF)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SportOrange)
                        )
                        Text(
                            text = "BALÓN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SportOrange
                        )
                    }
                }

                // Spacing metric
                Text(
                    text = "SPACING: ${(spacingArea * 100).toInt()}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5FF)
                )
            }
        }
    }
}
