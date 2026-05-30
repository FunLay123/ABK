package com.abk.kernel.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildSessionOutcomePolicyTest {

    @Test
    fun successAndCancelled_resolvesToSuccess() {
        val action = resolveBuildSessionNotificationAction(
            listOf(BuildSessionOutcome.Success, BuildSessionOutcome.Cancelled)
        )
        assertEquals(BuildSessionNotificationAction.NotifySuccess, action)
    }

    @Test
    fun allCancelled_resolvesToCancel() {
        val action = resolveBuildSessionNotificationAction(
            listOf(BuildSessionOutcome.Cancelled, BuildSessionOutcome.Cancelled)
        )
        assertEquals(BuildSessionNotificationAction.CancelNotification, action)
    }

    @Test
    fun anyFailure_resolvesToFailure() {
        val action = resolveBuildSessionNotificationAction(
            listOf(BuildSessionOutcome.Success, BuildSessionOutcome.Failure)
        )
        assertEquals(BuildSessionNotificationAction.NotifyFailure, action)
    }

    @Test
    fun emptyOutcomes_returnsNull() {
        assertEquals(null, resolveBuildSessionNotificationAction(emptyList()))
    }
}
