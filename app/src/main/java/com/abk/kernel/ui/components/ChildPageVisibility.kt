package com.abk.kernel.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** Default exit delay aligned with child-page slide/fade transitions. */
const val CHILD_PAGE_EXIT_DELAY_MS = 280L

/**
 * Keeps the bottom navigation bar in sync with overlay child pages.
 *
 * - Shows the bar immediately when [visible] becomes false on first composition
 *   (tab open) without playing the rise animation.
 * - Delays hiding the bar only when leaving a child page that was actually shown,
 *   so the nav can animate back in after the page exit transition.
 */
@Composable
fun ObserveChildPageVisibility(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    exitDelayMs: Long = CHILD_PAGE_EXIT_DELAY_MS,
    onAfterExitDelay: () -> Unit = {}
) {
    var childWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            childWasVisible = true
            onVisibleChange(true)
        } else {
            if (childWasVisible) {
                delay(exitDelayMs)
                childWasVisible = false
                onAfterExitDelay()
            }
            onVisibleChange(false)
        }
    }
}
