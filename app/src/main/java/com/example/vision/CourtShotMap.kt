package com.example.vision

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.theme.SportBorder
import com.example.theme.SportError
import com.example.theme.SportSuccess

@Composable
fun CourtShotMap(
    shots: List<CourtShotPoint>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 175.dp, height = 125.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xEE0B0B0E))
            .border(1.2.dp, SportBorder, RoundedCornerShape(12.dp))
            .testTag("court_shot_map")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val lineColor = Color(0x77FFFFFF)
            val strokeW = 1.2.dp.toPx()

            // 1. Half-court perimeter
            drawRect(
                color = lineColor,
                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                size = Size(w - 8.dp.toPx(), h - 8.dp.toPx()),
                style = Stroke(width = strokeW)
            )

            // 2. Key / Paint (centered top)
            val keyW = w * 0.32f
            val keyH = h * 0.45f
            val keyLeft = (w - keyW) / 2f
            drawRect(
                color = lineColor,
                topLeft = Offset(keyLeft, 4.dp.toPx()),
                size = Size(keyW, keyH),
                style = Stroke(width = strokeW)
            )

            // 3. Free throw circle
            drawCircle(
                color = lineColor,
                radius = keyW / 2f,
                center = Offset(w / 2f, 4.dp.toPx() + keyH),
                style = Stroke(width = strokeW)
            )

            // 4. Backboard & Basket Ring
            drawLine(
                color = Color.White,
                start = Offset(w / 2f - 7.dp.toPx(), 10.dp.toPx()),
                end = Offset(w / 2f + 7.dp.toPx(), 10.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(
                color = Color(0xFFFF6B1A),
                radius = 2.5.dp.toPx(),
                center = Offset(w / 2f, 13.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 5. 3-Point Arc
            drawArc(
                color = lineColor,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.08f, -h * 0.12f),
                size = Size(w * 0.84f, h * 1.0f),
                style = Stroke(width = strokeW)
            )

            // 6. Draw Shots (Makes as green dots, Misses as red X's)
            val pointSize = 4.5.dp.toPx()
            for (shot in shots) {
                val px = 10.dp.toPx() + shot.xNorm * (w - 20.dp.toPx())
                val py = 20.dp.toPx() + shot.yNorm * (h - 32.dp.toPx())

                if (shot.made) {
                    drawCircle(
                        color = SportSuccess,
                        radius = pointSize,
                        center = Offset(px, py)
                    )
                } else {
                    val xArm = pointSize * 0.95f
                    drawLine(
                        color = SportError,
                        start = Offset(px - xArm, py - xArm),
                        end = Offset(px + xArm, py + xArm),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = SportError,
                        start = Offset(px - xArm, py + xArm),
                        end = Offset(px + xArm, py - xArm),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
