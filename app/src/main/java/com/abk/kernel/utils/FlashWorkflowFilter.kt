package com.abk.kernel.utils

import com.abk.kernel.data.model.BuildParameterSummary
import com.abk.kernel.data.model.WorkflowRun
import com.abk.kernel.data.model.isKernelBuild
import com.abk.kernel.data.model.isManagerBuild

enum class FlashFilterKernelKind { ResuKisu, SukiSu, Official, None }

enum class FlashFilterManagerKind { Release, Dev }

enum class FlashFilterWorkflowState { Running, Finished }

data class FlashFilter(
    val kernelEnabled: Boolean = true,
    val kernelKinds: Set<FlashFilterKernelKind> = emptySet(),
    val managerEnabled: Boolean = true,
    val managerKinds: Set<FlashFilterManagerKind> = setOf(FlashFilterManagerKind.Release),
    val workflowStates: Set<FlashFilterWorkflowState> = emptySet(),
)

enum class WorkflowPrimary { Kernel, Manager, Unknown }

object FlashWorkflowFilter {

    fun primaryKind(
        run: WorkflowRun?,
        runTitle: String,
        hasKernelArtifact: Boolean,
        hasManagerArtifact: Boolean
    ): WorkflowPrimary {
        if (run != null) {
            if (run.isManagerBuild()) return WorkflowPrimary.Manager
            if (run.isKernelBuild()) return WorkflowPrimary.Kernel
        }
        if (runTitle.titleLooksLikeKernel()) return WorkflowPrimary.Kernel
        if (runTitle.titleLooksLikeManager()) return WorkflowPrimary.Manager
        if (hasKernelArtifact) return WorkflowPrimary.Kernel
        if (hasManagerArtifact) return WorkflowPrimary.Manager
        return WorkflowPrimary.Unknown
    }

    fun managerKind(
        run: WorkflowRun?,
        runTitle: String,
        remoteArtifactNames: List<String>,
        localArtifactNames: List<String>,
        summary: BuildParameterSummary?
    ): FlashFilterManagerKind? {
        val workflowName = (run?.name ?: runTitle).orEmpty().lowercase()
        val hasDevName = (remoteArtifactNames + localArtifactNames).any { it.lowercase().contains("dev") }
        val fallbackRunTitleIsManager = run == null && runTitle.titleLooksLikeManager()
        val runIsManagerWorkflow = run?.isManagerBuild() == true || fallbackRunTitleIsManager
        if (runIsManagerWorkflow) {
            val branch = summary?.ksuBranch.orEmpty().lowercase()
            val isDev = "dev" in workflowName || hasDevName || "dev" in branch
            return if (isDev) FlashFilterManagerKind.Dev else FlashFilterManagerKind.Release
        }
        if (summary == null && !hasDevName) return null
        val branch = summary?.ksuBranch.orEmpty().lowercase()
        return when {
            "dev" in branch || hasDevName -> FlashFilterManagerKind.Dev
            else -> FlashFilterManagerKind.Release
        }
    }

    fun kernelKind(
        summary: BuildParameterSummary?,
        fallbackVariant: String?
    ): FlashFilterKernelKind? {
        val raw = summary?.ksuVariant.orEmpty().ifBlank { fallbackVariant.orEmpty() }
        val v = raw.lowercase()
        if (v.isBlank()) return null
        return when {
            "resuki" in v || "re-suki" in v || "resukisu" in v -> FlashFilterKernelKind.ResuKisu
            "sukisu" in v -> FlashFilterKernelKind.SukiSu
            "kernelsu" in v || "official" in v -> FlashFilterKernelKind.Official
            else -> FlashFilterKernelKind.None
        }
    }

    fun matchesFilter(
        primary: WorkflowPrimary,
        filter: FlashFilter,
        kernelKind: FlashFilterKernelKind?,
        managerKind: FlashFilterManagerKind?,
        workflowState: FlashFilterWorkflowState?
    ): Boolean {
        if (filter.workflowStates.isNotEmpty()) {
            if (workflowState == null || workflowState !in filter.workflowStates) return false
        }
        return when (primary) {
            WorkflowPrimary.Kernel -> {
                if (!filter.kernelEnabled) return false
                if (filter.kernelKinds.isEmpty()) return true
                kernelKind != null && kernelKind in filter.kernelKinds
            }
            WorkflowPrimary.Manager -> {
                if (!filter.managerEnabled) return false
                if (filter.managerKinds.isEmpty()) return true
                managerKind != null && managerKind in filter.managerKinds
            }
            WorkflowPrimary.Unknown -> {
                if (!filter.kernelEnabled) return false
                if (filter.kernelKinds.isEmpty()) return true
                kernelKind != null && kernelKind in filter.kernelKinds
            }
        }
    }

    /** Pending queue config applies only to kernel-primary active runs still linking. */
    fun shouldUsePendingDispatchedConfig(run: WorkflowRun?, hasKernelArtifact: Boolean): Boolean {
        if (run?.isManagerBuild() == true) return false
        return run?.isKernelBuild() == true || (run == null && hasKernelArtifact)
    }

    fun isPureManagerBuild(run: WorkflowRun): Boolean =
        run.isManagerBuild() && !run.isKernelBuild()
}

private fun String.titleLooksLikeManager(): Boolean {
    val n = lowercase()
    return "abk app" in n || "abk-app" in n || "build app" in n ||
        "manager" in n || "管理器" in n || "getmanager" in n
}

private fun String.titleLooksLikeKernel(): Boolean {
    val n = lowercase()
    return "kernel" in n || "内核" in n
}
