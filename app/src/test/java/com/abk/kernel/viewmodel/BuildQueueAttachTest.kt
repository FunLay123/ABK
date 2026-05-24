package com.abk.kernel.viewmodel

import com.abk.kernel.data.model.BuildQueueItem
import com.abk.kernel.data.model.BuildQueueItemStatus
import com.abk.kernel.data.model.KernelBuildConfig
import com.abk.kernel.data.model.WorkflowRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildQueueAttachTest {

    private val kernelWfId = 10L
    private val oneplusWfId = 20L

    @Test
    fun prefersDispatchingSlotWithMatchingWorkflow() {
        val queue = listOf(
            item(status = BuildQueueItemStatus.RUNNING, workflowId = kernelWfId, runId = 100L),
            item(status = BuildQueueItemStatus.DISPATCHING, workflowId = kernelWfId, runId = 0L)
        )
        val run = run(id = 101L, workflowId = kernelWfId)
        assertEquals(queue[1].id, findBuildQueueItemForRun(queue, run)?.id)
    }

    @Test
    fun doesNotStealRunningSlotBoundToAnotherRun() {
        val queue = listOf(
            item(status = BuildQueueItemStatus.RUNNING, workflowId = kernelWfId, runId = 100L)
        )
        val run = run(id = 101L, workflowId = kernelWfId)
        assertNull(findBuildQueueItemForRun(queue, run))
    }

    @Test
    fun matchesByExistingRunId() {
        val queue = listOf(
            item(status = BuildQueueItemStatus.RUNNING, workflowId = kernelWfId, runId = 55L)
        )
        val run = run(id = 55L, workflowId = kernelWfId)
        assertEquals(queue.single().id, findBuildQueueItemForRun(queue, run)?.id)
    }

    @Test
    fun ignoresDifferentWorkflowId() {
        val queue = listOf(
            item(status = BuildQueueItemStatus.DISPATCHING, workflowId = kernelWfId, runId = 0L)
        )
        val run = run(id = 200L, workflowId = oneplusWfId)
        assertNull(findBuildQueueItemForRun(queue, run))
    }

    private fun item(
        status: BuildQueueItemStatus,
        workflowId: Long,
        runId: Long
    ) = BuildQueueItem(
        id = "q-$workflowId-$runId-$status",
        name = "test",
        config = KernelBuildConfig(),
        status = status,
        workflowId = workflowId,
        runId = runId
    )

    private fun run(id: Long, workflowId: Long) = WorkflowRun(
        id = id,
        name = "Build kernel",
        status = "in_progress",
        conclusion = null,
        htmlUrl = "",
        createdAt = "",
        updatedAt = "",
        runNumber = id,
        workflowId = workflowId,
        headBranch = "main",
        displayTitle = ""
    )
}
