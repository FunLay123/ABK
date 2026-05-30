package com.abk.kernel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Hand poses matching [androidx.compose.material.icons.filled.Schedule] (≈10:10). */
private const val HOUR_HAND_DEGREES = -125f
private const val MINUTE_HAND_BASE_DEGREES = -5f

/**
 * Small schedule-style clock for the live workflow duration chip: the dial stays fixed and
 * only the minute hand rotates ([rotationDegrees]).
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
        Canvas(Modifier.fillMaxSize()) {
            val center = this.center
            val dim = size.minDimension
            val radius = dim * 0.46f
            drawCircle(
                color = tint,
                radius = radius,
                center = center,
                style = Stroke(width = dim * 0.085f),
            )
            val hourLength = dim * 0.36f
            val minuteLength = dim * 0.42f
            val stroke = dim * 0.095f
            val minuteAngleRad = Math.toRadians(
                (MINUTE_HAND_BASE_DEGREES + rotationDegrees).toDouble(),
            )
            val hourAngleRad = Math.toRadians(HOUR_HAND_DEGREES.toDouble())
            fun handEnd(length: Float, angleRad: Double) = Offset(
                x = center.x + length * cos(angleRad).toFloat(),
                y = center.y + length * sin(angleRad).toFloat(),
            )
            drawLine(
                color = tint,
                start = center,
                end = handEnd(minuteLength, minuteAngleRad),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = center,
                end = handEnd(hourLength, hourAngleRad),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}
