package com.abk.kernel.viewmodel

import com.abk.kernel.data.model.WorkflowRun
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentRunsArtifactRefreshTest {

    private fun run(
        id: Long,
        name: String,
        status: String,
        displayTitle: String = name,
    ) = WorkflowRun(
        id = id,
        name = name,
        status = status,
        conclusion = if (status == "completed") "success" else null,
        htmlUrl = "",
        createdAt = "",
        updatedAt = "",
        runNumber = id.toInt(),
        workflowId = 1L,
        headBranch = "main",
        displayTitle = displayTitle,
    )

    @Test
    fun lightMode_includesCompletedPureManager() {
        val manager = run(1L, "Build ABK app", "completed")
        val kernel = run(2L, "Build kernel", "completed")
        val selected = runsNeedingArtifactRefresh(
            listOf(manager, kernel),
            includeCompleted = false,
            includeCompletedPureManagers = true,
        )
        assertEquals(listOf(1L), selected.map { it.id })
    }

    @Test
    fun lightMode_includesActiveKernel_notCompletedKernel() {
        val active = run(3L, "Build kernel", "in_progress")
        val done = run(4L, "Build kernel", "completed")
        val selected = runsNeedingArtifactRefresh(
            listOf(active, done),
            includeCompleted = false,
            includeCompletedPureManagers = true,
        )
        assertEquals(listOf(3L), selected.map { it.id })
    }

    @Test
    fun fullMode_includesAllCompleted() {
        val manager = run(1L, "Build ABK app", "completed")
        val kernel = run(2L, "Build kernel", "completed")
        val selected = runsNeedingArtifactRefresh(
            listOf(manager, kernel),
            includeCompleted = true,
            includeCompletedPureManagers = false,
        )
        assertEquals(setOf(1L, 2L), selected.map { it.id }.toSet())
    }
}
