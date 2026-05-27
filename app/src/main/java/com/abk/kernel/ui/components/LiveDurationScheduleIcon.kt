package com.abk.kernel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Same [Icons.Default.Schedule] glyph as the finished-workflow duration chip, but only the
 * minute hand appears to move: the icon rotates underneath while a fixed hour hand is
 * painted on top to mask the icon's hour hand.
 */
@Composable
fun LiveDurationScheduleIcon(
    rotationDegrees: Float,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationDegrees },
        )
        Canvas(Modifier.fillMaxSize()) {
            val center = this.center
            val hourLength = size.minDimension * 0.36f
            // Matches the default hour-hand pose on Material Schedule (≈10 o'clock).
            val hourAngleRad = Math.toRadians(-125.0)
            drawLine(
                color = tint,
                start = center,
                end = Offset(
                    x = center.x + hourLength * cos(hourAngleRad).toFloat(),
                    y = center.y + hourLength * sin(hourAngleRad).toFloat(),
                ),
                strokeWidth = size.minDimension * 0.095f,
                cap = StrokeCap.Round,
            )
        }
    }
}
