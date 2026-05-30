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

/** Hour hand at 10 o'clock — matches [androidx.compose.material.icons.filled.Schedule]. */
private const val HOUR_HAND_DEGREES_FROM_TWELVE = 300f

/**
 * Schedule-style clock for live workflow duration: fixed dial + hour hand,
 * only the second hand sweeps ([secondHandDegreesFromTwelve], 0° = 12 o'clock).
 */
@Composable
fun LiveDurationScheduleIcon(
    secondHandDegreesFromTwelve: Float,
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
            val stroke = dim * 0.085f
            drawCircle(
                color = tint,
                radius = radius,
                center = center,
                style = Stroke(width = stroke),
            )
            val hourLength = dim * 0.32f
            val secondLength = dim * 0.40f
            val hourStroke = dim * 0.09f
            val secondStroke = dim * 0.075f
            drawHand(
                center = center,
                length = hourLength,
                degreesFromTwelve = HOUR_HAND_DEGREES_FROM_TWELVE,
                color = tint,
                strokeWidth = hourStroke,
            )
            drawHand(
                center = center,
                length = secondLength,
                degreesFromTwelve = secondHandDegreesFromTwelve,
                color = tint,
                strokeWidth = secondStroke,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHand(
    center: Offset,
    length: Float,
    degreesFromTwelve: Float,
    color: Color,
    strokeWidth: Float,
) {
    val end = handEnd(center, length, degreesFromTwelve)
    drawLine(
        color = color,
        start = center,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

/** Clockwise degrees from 12 o'clock; 0° points up. */
private fun handEnd(center: Offset, length: Float, degreesFromTwelve: Float): Offset {
    val rad = Math.toRadians(degreesFromTwelve.toDouble())
    return Offset(
        x = center.x + (length * sin(rad)).toFloat(),
        y = center.y - (length * cos(rad)).toFloat(),
    )
}
