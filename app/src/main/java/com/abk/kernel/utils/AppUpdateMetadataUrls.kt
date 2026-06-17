package com.abk.kernel.utils

import com.abk.kernel.BuildConfig
import com.abk.kernel.data.model.APP_UPDATE_SOURCE_FORK
import com.abk.kernel.data.model.normalizeAppUpdateSource

object AppUpdateMetadataUrls {
    fun resolve(
        source: String,
        upstreamMetadataUrl: String = BuildConfig.APP_UPDATE_METADATA_URL,
        forkRepoFullName: String?,
        forkDefaultBranch: String?,
        fallbackBranch: String = BuildConfig.SOURCE_REPO_DEFAULT_BRANCH,
    ): String? {
        return when (normalizeAppUpdateSource(source)) {
            APP_UPDATE_SOURCE_FORK -> forkMetadataUrl(
                forkRepoFullName = forkRepoFullName,
                forkDefaultBranch = forkDefaultBranch,
                fallbackBranch = fallbackBranch,
            )
            else -> upstreamMetadataUrl.trim().takeIf { it.isNotEmpty() }
        }
    }

    fun forkMetadataUrl(
        forkRepoFullName: String?,
        forkDefaultBranch: String?,
        fallbackBranch: String = BuildConfig.SOURCE_REPO_DEFAULT_BRANCH,
    ): String? {
        val repo = forkRepoFullName?.trim().orEmpty()
        if (repo.isBlank()) return null
        val branch = forkDefaultBranch?.trim().takeUnless { it.isNullOrBlank() } ?: fallbackBranch
        return "https://raw.githubusercontent.com/$repo/$branch/version.json"
    }
}
