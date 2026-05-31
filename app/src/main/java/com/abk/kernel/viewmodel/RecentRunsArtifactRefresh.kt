package com.abk.kernel.viewmodel

import com.abk.kernel.data.model.WorkflowRun
import com.abk.kernel.data.model.isPureManagerBuild

internal fun runsNeedingArtifactRefresh(
    runs: List<WorkflowRun>,
    includeCompleted: Boolean,
    includeCompletedPureManagers: Boolean,
): List<WorkflowRun> {
    val activeStatuses = setOf("queued", "waiting", "requested", "pending", "in_progress")
    return runs.filter { run ->
        run.status in activeStatuses ||
            (includeCompleted && run.status == "completed") ||
            (includeCompletedPureManagers &&
                run.isPureManagerBuild() &&
                run.status == "completed")
    }
}
