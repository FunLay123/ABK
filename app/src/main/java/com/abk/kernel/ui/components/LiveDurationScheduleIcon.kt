package com.abk.kernel.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Minute hand from [Icons.Default.Schedule] (24×24 viewport), separate from the hour hand. */
private fun scheduleMinuteHandPath(): Path = Path().apply {
    moveTo(11f, 13f)
    relativeLineTo(5.25f, 3.15f)
    relativeLineTo(0.75f, -1.23f)
    relativeLineTo(-4.5f, -2.67f)
    close()
}

/**
 * Same clock as [Icons.Default.Schedule] on completed workflows; only the minute hand
 * (lower-right) rotates by [minuteHandRotationDegrees] around the dial center.
 */
@Composable
fun LiveDurationScheduleIcon(
    minuteHandRotationDegrees: Float,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val minutePath = remember { scheduleMinuteHandPath() }
    Box(
        modifier = modifier
            .size(14.dp)
            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val scale = size.minDimension / 24f
                val offsetX = (size.width - 24f * scale) / 2f
                val offsetY = (size.height - 24f * scale) / 2f
                translate(offsetX, offsetY) {
                    scale(scale, scale) {
                        drawPath(
                            path = minutePath,
                            color = Color.Black,
                            style = Fill,
                            blendMode = BlendMode.Clear,
                        )
                        rotate(minuteHandRotationDegrees, pivot = Offset(12f, 12f)) {
                            drawPath(
                                path = minutePath,
                                color = tint,
                                style = Fill,
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
