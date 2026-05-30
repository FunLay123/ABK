package com.abk.kernel.utils

import com.abk.kernel.data.model.BuildProgress
import com.abk.kernel.data.model.BuildStatus
import com.abk.kernel.data.model.WorkflowRun
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRunDisplayTest {

    @Test
    fun computeKindBuildProgress_isolatesKernelAndManagerSteps() {
        val kernelRun = WorkflowRun(
            id = 100L,
            name = "Build kernel",
            status = "in_progress",
            conclusion = null,
            htmlUrl = "",
            createdAt = "",
            updatedAt = "",
            runNumber = 100,
            workflowId = 1L,
            headBranch = "main",
            displayTitle = "Kernel build"
        )
        val managerRun = WorkflowRun(
            id = 200L,
            name = "Build ABK app",
            status = "in_progress",
            conclusion = null,
            htmlUrl = "",
            createdAt = "",
            updatedAt = "",
            runNumber = 200,
            workflowId = 2L,
            headBranch = "main",
            displayTitle = "Manager packaging"
        )
        val kernelStep = "KERNEL_ONLY_COMPILE_STEP"
        val managerStep = "MANAGER_ONLY_PACKAGE_STEP"
        val progressByRunId = mapOf(
            kernelRun.id to BuildProgress(percent = 40, currentStep = kernelStep),
            managerRun.id to BuildProgress(percent = 60, currentStep = managerStep)
        )
        val activeRuns = listOf(kernelRun, managerRun)
        val descriptors = emptyMap<Long, BuildProgressUtils.RunDescriptor>()

        val kernelProgress = computeKindBuildProgress(
            forKernel = true,
            activeRuns = activeRuns,
            progressByRunId = progressByRunId,
            fallbackRun = null,
            fallbackStatus = BuildStatus.IDLE,
            fallbackProgress = BuildProgress(),
            descriptors = descriptors
        )
        val managerProgress = computeKindBuildProgress(
            forKernel = false,
            activeRuns = activeRuns,
            progressByRunId = progressByRunId,
            fallbackRun = null,
            fallbackStatus = BuildStatus.IDLE,
            fallbackProgress = BuildProgress(),
            descriptors = descriptors
        )

        assertTrue(kernelProgress.currentStep.contains(kernelStep))
        assertFalse(kernelProgress.currentStep.contains(managerStep))
        assertTrue(managerProgress.currentStep.contains(managerStep))
        assertFalse(managerProgress.currentStep.contains(kernelStep))
    }
}
