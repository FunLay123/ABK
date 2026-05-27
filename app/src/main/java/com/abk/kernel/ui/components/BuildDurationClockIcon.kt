package com.abk.kernel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BuildDurationClockIcon(
    rotationDegrees: Float,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(14.dp).then(modifier)) {
        val radius = size.minDimension / 2f
        val center = this.center
        drawCircle(
            color = tint,
            radius = radius,
            style = Stroke(width = 1.2f),
        )
        // Hour hand — static
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x - radius * 0.22f, center.y - radius * 0.4f),
            strokeWidth = 1.3f,
            cap = StrokeCap.Round,
        )
        // Minute hand — rotates with elapsed animation
        val angleRad = Math.toRadians((rotationDegrees - 90).toDouble())
        val handLength = radius * 0.62f
        val end = Offset(
            center.x + (handLength * cos(angleRad)).toFloat(),
            center.y + (handLength * sin(angleRad)).toFloat(),
        )
        drawLine(
            color = tint,
            start = center,
            end = end,
            strokeWidth = 1.6f,
            cap = StrokeCap.Round,
        )
    }
}
