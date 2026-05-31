package com.abk.kernel.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Progress bar: solid [primary] fill + animated shimmer tail (visible through transparent M3 track).
 */
@Composable
fun ShimmerLinearProgress(
    progress: () -> Float?,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    val accent = MaterialTheme.colorScheme.primary
    val phase = rememberLiveWorkflowShimmerPhase(enabled = true)
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape),
    ) {
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(liveWorkflowProgressShimmerBrush(size, phase, accent))
                },
        )
        if (progress() != null) {
            LinearProgressIndicator(
                progress = { progress()!!.coerceIn(0f, 1f) },
                modifier = Modifier.matchParentSize(),
                color = accent,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Butt,
            )
        }
    }
}
