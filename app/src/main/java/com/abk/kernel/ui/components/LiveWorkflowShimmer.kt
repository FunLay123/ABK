package com.abk.kernel.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

const val LIVE_DURATION_SHIMMER_PERIOD_MS = 6_000

@Composable
fun rememberLiveWorkflowShimmerPhase(enabled: Boolean): Float {
    if (!enabled) return 0f
    val transition = rememberInfiniteTransition(label = "live-workflow-shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(LIVE_DURATION_SHIMMER_PERIOD_MS, easing = LinearEasing),
        ),
        label = "shimmer-phase",
    )
    return phase
}

fun liveWorkflowShimmerBrush(size: Size, phase: Float, accent: Color): Brush {
    val base = accent.copy(alpha = 0.14f)
    val highlight = accent.copy(alpha = 0.28f)
    val band = size.width * 1.6f
    val travel = size.width + band
    val offset = (phase * travel) % travel - band
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset, 0f),
        end = Offset(offset + band, size.height),
    )
}

fun DrawScope.drawLiveWorkflowShimmer(phase: Float, accent: Color) {
    drawRect(liveWorkflowShimmerBrush(size, phase, accent))
}
