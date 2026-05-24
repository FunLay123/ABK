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
 * - On first entry, optionally delays [onVisibleChange](true) so the bottom nav
 *   stays visible while the child page enter transition runs.
 * - On exit, delays [onVisibleChange](false) so the nav can rise after the child
 *   page pop transition.
 * - When [visible] becomes false without a prior child page, clears immediately
 *   (tab open) without playing the rise animation.
 */
@Composable
fun ObserveChildPageVisibility(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    enterDelayMs: Long = CHILD_PAGE_EXIT_DELAY_MS,
    exitDelayMs: Long = CHILD_PAGE_EXIT_DELAY_MS,
    onAfterExitDelay: () -> Unit = {}
) {
    var childWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            if (!childWasVisible && enterDelayMs > 0L) {
                delay(enterDelayMs)
            }
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
