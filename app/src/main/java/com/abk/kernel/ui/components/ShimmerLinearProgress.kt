package com.abk.kernel.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Linear progress with a live shimmer on the unfilled segment only (same look as the live duration chip).
 * The filled segment is solid [MaterialTheme.colorScheme.primary]; the tail uses the animated gradient only.
 */
@Composable
fun ShimmerLinearProgress(
    progress: () -> Float?,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
) {
    val accent = MaterialTheme.colorScheme.primary
    val phase = rememberLiveWorkflowShimmerPhase(enabled = true)
    val fraction = progress()?.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(percent = 50)
    val layoutDirection = LocalLayoutDirection.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .drawWithCache {
                // Tie cache invalidation to animated phase (see LiveWorkflowShimmer).
                val shimmerPhase = phase
                val cornerRadius = CornerRadius(size.height / 2f)
                val totalWidth = size.width
                val barHeight = size.height
                onDrawBehind {
                    if (totalWidth <= 0f) return@onDrawBehind
                    val filledWidth = if (fraction == null) 0f else totalWidth * fraction
                    val isRtl = layoutDirection == LayoutDirection.Rtl

                    val shimmerLeft = if (isRtl) 0f else filledWidth
                    val shimmerRight = if (isRtl) totalWidth - filledWidth else totalWidth
                    if (shimmerRight > shimmerLeft) {
                        clipRect(shimmerLeft, 0f, shimmerRight, barHeight) {
                            drawRect(liveWorkflowShimmerBrush(size, shimmerPhase, accent))
                        }
                    }

                    if (fraction != null && filledWidth > 0f) {
                        val fillLeft = if (isRtl) totalWidth - filledWidth else 0f
                        val fillRight = if (isRtl) totalWidth else filledWidth
                        clipRect(fillLeft, 0f, fillRight, barHeight) {
                            drawRoundRect(color = accent, cornerRadius = cornerRadius)
                        }
                    }
                }
            },
    )
}
