package com.abk.kernel.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowRunClassificationTest {

    @Test
    fun buildAbkAppWorkflowStaysManagerWhenTitleMentionsKernelParameters() {
        val run = WorkflowRun(
            id = 320L,
            name = "Build ABK app",
            status = "in_progress",
            conclusion = null,
            htmlUrl = "",
            createdAt = "",
            updatedAt = "",
            runNumber = 320,
            workflowId = 1L,
            headBranch = "main",
            displayTitle = "ReSukiSU SUSFS kernel build"
        )

        assertTrue(run.isManagerBuild())
        assertFalse(run.isKernelBuild())
    }

    @Test
    fun buildAbkAppDevWorkflowStaysManagerWhenTitleMentionsKernelParameters() {
        val run = WorkflowRun(
            id = 322L,
            name = "Build ABK App Dev",
            status = "in_progress",
            conclusion = null,
            htmlUrl = "",
            createdAt = "",
            updatedAt = "",
            runNumber = 322,
            workflowId = 1L,
            headBranch = "dev",
            displayTitle = "ReSukiSU SUSFS kernel build"
        )

        assertTrue(run.isManagerBuild())
        assertFalse(run.isKernelBuild())
    }

    @Test
    fun kernelWorkflowStaysKernelWhenTitleMentionsManagerArtifact() {
        val run = WorkflowRun(
            id = 321L,
            name = "Build kernel",
            status = "in_progress",
            conclusion = null,
            htmlUrl = "",
            createdAt = "",
            updatedAt = "",
            runNumber = 321,
            workflowId = 1L,
            headBranch = "main",
            displayTitle = "Package manager APK"
        )

        assertTrue(run.isKernelBuild())
        assertFalse(run.isManagerBuild())
    }
}
