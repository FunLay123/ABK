package com.abk.kernel.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import kotlin.math.pow
import kotlinx.coroutines.delay

/** Fraction of hide progress at end of peek (nav + back visual remap). */
const val CHILD_PAGE_MOTION_PEEK_FRACTION = 0.28f

// Bottom nav — browser variant 10 (hide ↓)
const val BOTTOM_NAV_HIDE_PEEK_MS = 160L
const val BOTTOM_NAV_HIDE_HOLD_MS = 80L
const val BOTTOM_NAV_HIDE_SLIDE_MS = 360L

// Bottom nav — browser variant 7 (show ↑)
const val BOTTOM_NAV_SHOW_PEEK_MS = 140L
const val BOTTOM_NAV_SHOW_HOLD_MS = 100L
const val BOTTOM_NAV_SHOW_SLIDE_MS = 560L

// Child-page back dismiss — browser variant 9
const val CHILD_PAGE_BACK_DISMISS_PEEK_MS = 240L
const val CHILD_PAGE_BACK_DISMISS_HOLD_MS = 240L
const val CHILD_PAGE_BACK_DISMISS_SLIDE_MS = 520L

private const val CHILD_PAGE_BACK_DISMISS_TOTAL_MS: Long =
    CHILD_PAGE_BACK_DISMISS_PEEK_MS + CHILD_PAGE_BACK_DISMISS_HOLD_MS + CHILD_PAGE_BACK_DISMISS_SLIDE_MS

/** [Animatable] progress 0 = fully hidden, 1 = fully visible at peek plateau. */
const val CHILD_PAGE_BACK_DISMISS_PEEK_FRACTION: Float =
    CHILD_PAGE_BACK_DISMISS_PEEK_MS.toFloat() / CHILD_PAGE_BACK_DISMISS_TOTAL_MS.toFloat()

/** Progress through dismiss spring where hold plateau ends (variant 9). */
const val CHILD_PAGE_BACK_DISMISS_HOLD_END: Float =
    (CHILD_PAGE_BACK_DISMISS_PEEK_MS + CHILD_PAGE_BACK_DISMISS_HOLD_MS).toFloat() /
        CHILD_PAGE_BACK_DISMISS_TOTAL_MS.toFloat()

/** Defer clearing parent [childPageVisible] after NavHost/detail pop (0 = show nav immediately). */
const val CHILD_PAGE_NAV_EXIT_DELAY_MS = 0L

fun childPageBackPeekAmount(dismissProgress: Float): Float {
    val progress = dismissProgress.coerceIn(0f, 1f)
    if (progress <= CHILD_PAGE_BACK_DISMISS_PEEK_FRACTION) {
        return (progress / CHILD_PAGE_BACK_DISMISS_PEEK_FRACTION).coerceIn(0f, 1f)
    }
    if (progress <= CHILD_PAGE_BACK_DISMISS_HOLD_END) {
        return 1f
    }
    return 1f
}

fun childPageBackTranslationX(
    dismissProgress: Float,
    peekPx: Float,
    screenWidthPx: Float,
    visualExponent: Float,
): Float {
    val progress = dismissProgress.coerceIn(0f, 1f)
    if (progress <= CHILD_PAGE_BACK_DISMISS_PEEK_FRACTION) {
        val visual = childPageBackPeekAmount(progress).toDouble().pow(visualExponent.toDouble()).toFloat()
        return peekPx * visual
    }
    if (progress <= CHILD_PAGE_BACK_DISMISS_HOLD_END) {
        val visual = childPageBackPeekAmount(CHILD_PAGE_BACK_DISMISS_PEEK_FRACTION)
            .toDouble()
            .pow(visualExponent.toDouble())
            .toFloat()
        return peekPx * visual
    }
    val slideT = (
        (progress - CHILD_PAGE_BACK_DISMISS_HOLD_END) /
            (1f - CHILD_PAGE_BACK_DISMISS_HOLD_END)
        ).coerceIn(0f, 1f)
    val peekVisual = childPageBackPeekAmount(CHILD_PAGE_BACK_DISMISS_PEEK_FRACTION)
        .toDouble()
        .pow(visualExponent.toDouble())
        .toFloat()
    return peekPx * peekVisual + (screenWidthPx - peekPx * peekVisual) * slideT
}

fun childPageBackScrimAlpha(dismissProgress: Float, maxAlpha: Float, visualExponent: Float): Float {
    val progress = dismissProgress.coerceIn(0f, 1f)
    if (progress <= CHILD_PAGE_BACK_DISMISS_PEEK_FRACTION) {
        val visual = childPageBackPeekAmount(progress).toDouble().pow(visualExponent.toDouble()).toFloat()
        return maxAlpha * visual
    }
    if (progress <= CHILD_PAGE_BACK_DISMISS_HOLD_END) {
        return maxAlpha
    }
    val fadeT = (
        (progress - CHILD_PAGE_BACK_DISMISS_HOLD_END) /
            (1f - CHILD_PAGE_BACK_DISMISS_HOLD_END)
        ).coerceIn(0f, 1f)
    return maxAlpha * (1f - fadeT)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
suspend fun Animatable<Float, *>.animateChildPageBackDismiss(motionScheme: MotionScheme) {
    animateTo(1f, motionScheme.defaultSpatialSpec())
}

private const val BOTTOM_NAV_PROGRESS_EPSILON = 0.02f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
suspend fun Animatable<Float, *>.animateBottomNavHide(motionScheme: MotionScheme) {
    val spec = motionScheme.fastSpatialSpec<Float>()
    val current = value.coerceIn(0f, 1f)
    if (current <= BOTTOM_NAV_PROGRESS_EPSILON) return

    val peekVisible = 1f - CHILD_PAGE_MOTION_PEEK_FRACTION
    if (current > peekVisible + BOTTOM_NAV_PROGRESS_EPSILON) {
        animateTo(peekVisible, spec)
        delay(BOTTOM_NAV_HIDE_HOLD_MS)
    } else if (current > BOTTOM_NAV_PROGRESS_EPSILON) {
        delay(BOTTOM_NAV_HIDE_HOLD_MS)
    }
    animateTo(0f, spec)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
suspend fun Animatable<Float, *>.animateBottomNavShow(motionScheme: MotionScheme) {
    val spec = motionScheme.defaultSpatialSpec<Float>()
    val current = value.coerceIn(0f, 1f)
    if (current >= 1f - BOTTOM_NAV_PROGRESS_EPSILON) return

    val peek = CHILD_PAGE_MOTION_PEEK_FRACTION
    if (current > peek + BOTTOM_NAV_PROGRESS_EPSILON) {
        // Already partially visible — finish upward without dipping to peek first.
        animateTo(1f, spec)
        return
    }
    if (current < peek - BOTTOM_NAV_PROGRESS_EPSILON) {
        animateTo(peek, spec)
        delay(BOTTOM_NAV_SHOW_HOLD_MS)
    } else {
        delay(BOTTOM_NAV_SHOW_HOLD_MS)
    }
    animateTo(1f, spec)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
suspend fun Animatable<Float, *>.animateBottomNavForChildPage(
    childPageVisible: Boolean,
    motionScheme: MotionScheme,
) {
    if (childPageVisible) {
        animateBottomNavHide(motionScheme)
    } else {
        animateBottomNavShow(motionScheme)
    }
}
