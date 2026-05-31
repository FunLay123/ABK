package com.abk.kernel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerLinearProgress(
    progress: () -> Float?,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = colorScheme.primary
    val trackColor = colorScheme.surfaceVariant
    val indicatorColor = colorScheme.primary
    val phase = rememberLiveWorkflowShimmerPhase(enabled = true)
    val shape = RoundedCornerShape(percent = 50)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape),
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val barHeight = constraints.maxHeight.toFloat()
        val cornerRadius = CornerRadius(barHeight / 2f)
        val fraction = progress()?.coerceIn(0f, 1f)

        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(color = trackColor, cornerRadius = cornerRadius)

            if (fraction == null) {
                drawLiveWorkflowShimmer(phase, accent)
            } else {
                val filledWidth = totalWidth * fraction
                val shimmerLeft: Float
                val shimmerRight: Float
                val fillLeft: Float
                val fillRight: Float
                if (isRtl) {
                    shimmerLeft = 0f
                    shimmerRight = totalWidth - filledWidth
                    fillLeft = totalWidth - filledWidth
                    fillRight = totalWidth
                } else {
                    shimmerLeft = filledWidth
                    shimmerRight = totalWidth
                    fillLeft = 0f
                    fillRight = filledWidth
                }

                if (shimmerRight > shimmerLeft) {
                    clipRect(shimmerLeft, 0f, shimmerRight, barHeight) {
                        drawLiveWorkflowShimmer(phase, accent)
                    }
                }
                if (fillRight > fillLeft) {
                    clipRect(fillLeft, 0f, fillRight, barHeight) {
                        drawRoundRect(color = indicatorColor, cornerRadius = cornerRadius)
                    }
                }
            }
        }
    }
}
