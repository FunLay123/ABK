package com.abk.kernel.ui

import com.abk.kernel.data.model.WorkflowRun
import com.abk.kernel.data.model.isFailedFlashRun
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashFailedWorkflowFilterTest {

    private fun run(conclusion: String?, status: String = "completed") = WorkflowRun(
        id = 1L,
        name = "build",
        status = status,
        conclusion = conclusion,
        htmlUrl = "https://github.com/o/r/actions/runs/1",
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T01:00:00Z",
        runNumber = 1,
        workflowId = 9L,
        headBranch = "dev",
        displayTitle = "Build kernel",
    )

    @Test
    fun isFailedFlashRun_isTrueOnlyForCompletedFailure() {
        assertTrue(run("failure").isFailedFlashRun())
        assertFalse(run("success").isFailedFlashRun())
        assertFalse(run("cancelled").isFailedFlashRun())
        assertFalse(run(null).isFailedFlashRun())
        assertFalse(run("failure", status = "in_progress").isFailedFlashRun())
    }
}
