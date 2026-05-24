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
        val hasDevArtifact = (remoteArtifactNames + localArtifactNames).any(::artifactNameIndicatesManagerDev)
        val fallbackRunTitleIsManager = run == null && runTitle.titleLooksLikeManager()
        val runIsManagerWorkflow = run?.isManagerBuild() == true || fallbackRunTitleIsManager
        if (runIsManagerWorkflow) {
            val branch = summary?.ksuBranch.orEmpty()
            val isDev = "dev" in workflowName || hasDevArtifact || ksuBranchIndicatesDev(branch)
            return if (isDev) FlashFilterManagerKind.Dev else FlashFilterManagerKind.Release
        }
        if (summary == null && !hasDevArtifact) return null
        val branch = summary?.ksuBranch.orEmpty()
        return when {
            ksuBranchIndicatesDev(branch) || hasDevArtifact -> FlashFilterManagerKind.Dev
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
            WorkflowPrimary.Kernel -> matchesKernelKindFilter(filter, kernelKind, workflowState)
            WorkflowPrimary.Manager -> {
                if (!filter.managerEnabled) return false
                if (filter.managerKinds.isEmpty()) return true
                managerKind != null && managerKind in filter.managerKinds
            }
            WorkflowPrimary.Unknown -> {
                if (!filter.kernelEnabled) return false
                matchesKernelKindFilter(filter, kernelKind, workflowState)
            }
        }
    }

    private fun matchesKernelKindFilter(
        filter: FlashFilter,
        kernelKind: FlashFilterKernelKind?,
        workflowState: FlashFilterWorkflowState?
    ): Boolean {
        if (!filter.kernelEnabled) return false
        if (filter.kernelKinds.isEmpty()) return true
        if (kernelKind != null) return kernelKind in filter.kernelKinds
        // In-progress kernel run before summary/variant is known — keep it visible
        // so the list does not empty out while the build is still running.
        return workflowState == FlashFilterWorkflowState.Running
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

/**
 * Detects dev manager artifacts without matching "device" / "development" substrings.
 * Release Build ABK App bundles also ship debug APKs, so "debug" alone is not a dev signal.
 */
internal fun artifactNameIndicatesManagerDev(name: String): Boolean {
    val lower = name.lowercase()
    return MANAGER_DEV_ARTIFACT_MARKERS.any { lower.contains(it) }
}

/** KSU branch field from build summary — exact dev branch, not "development". */
internal fun ksuBranchIndicatesDev(branch: String): Boolean {
    val b = branch.lowercase().trim()
    if (b.isBlank()) return false
    return b == "dev" || b.endsWith("/dev") || b.startsWith("dev/") || "-dev" in b
}

private val MANAGER_DEV_ARTIFACT_MARKERS = listOf(
    "-dev.apk",
    "_dev.apk",
    "-dev-",
    "_dev_",
    "/dev/",
    "abk-dev",
    "app-dev"
)
