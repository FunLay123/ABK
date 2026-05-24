package com.abk.kernel.utils

import com.abk.kernel.data.model.BuildParameterSummary
import com.abk.kernel.data.model.WorkflowRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashWorkflowFilterDevSignalTest {

    @Test
    fun artifactDevMarkersDoNotMatchDeviceSubstring() {
        assertFalse(artifactNameIndicatesManagerDev("kernel-device-flash.zip"))
        assertFalse(artifactNameIndicatesManagerDev("development-build.zip"))
        assertTrue(artifactNameIndicatesManagerDev("app-dev.apk"))
        assertFalse(artifactNameIndicatesManagerDev("app-debug.apk"))
        assertFalse(artifactNameIndicatesManagerDev("abk-apks"))
    }

    @Test
    fun ksuBranchDevIsExactNotDevelopment() {
        assertTrue(ksuBranchIndicatesDev("dev"))
        assertFalse(ksuBranchIndicatesDev("development"))
        assertFalse(ksuBranchIndicatesDev("main"))
    }

    @Test
    fun releaseManagerNotShownInDevOnlyFilter() {
        val run = WorkflowRun(
            id = 1L,
            name = "Build ABK app",
            status = "completed",
            conclusion = "success",
            htmlUrl = "",
            createdAt = "",
            updatedAt = "",
            runNumber = 1,
            workflowId = 1L,
            headBranch = "main",
            displayTitle = "kernel-device artifact"
        )
        val filter = FlashFilter(
            kernelEnabled = false,
            managerEnabled = true,
            managerKinds = setOf(FlashFilterManagerKind.Dev)
        )
        val primary = FlashWorkflowFilter.primaryKind(run, "Build ABK app", false, true)
        val mKind = FlashWorkflowFilter.managerKind(
            run = run,
            runTitle = "Build ABK app",
            remoteArtifactNames = listOf("kernel-device-flash.zip"),
            localArtifactNames = emptyList(),
            summary = null
        )
        assertEquals(FlashFilterManagerKind.Release, mKind)
        assertFalse(
            FlashWorkflowFilter.matchesFilter(
                primary = primary,
                filter = filter,
                kernelKind = null,
                managerKind = mKind,
                workflowState = FlashFilterWorkflowState.Finished
            )
        )
    }

    @Test
    fun devManagerWorkflowStillMatchesDevFilter() {
        val run = WorkflowRun(
            id = 2L,
            name = "Build ABK App Dev",
            status = "completed",
            conclusion = "success",
            htmlUrl = "",
            createdAt = "",
            updatedAt = "",
            runNumber = 2,
            workflowId = 2L,
            headBranch = "dev",
            displayTitle = "anything"
        )
        val filter = FlashFilter(
            kernelEnabled = false,
            managerEnabled = true,
            managerKinds = setOf(FlashFilterManagerKind.Dev)
        )
        val primary = FlashWorkflowFilter.primaryKind(run, "Build ABK App Dev", false, true)
        val mKind = FlashWorkflowFilter.managerKind(run, "Build ABK App Dev", listOf("abk-apks"), emptyList(), null)
        assertEquals(FlashFilterManagerKind.Dev, mKind)
        assertTrue(
            FlashWorkflowFilter.matchesFilter(
                primary = primary,
                filter = filter,
                kernelKind = null,
                managerKind = mKind,
                workflowState = FlashFilterWorkflowState.Finished
            )
        )
    }
}
