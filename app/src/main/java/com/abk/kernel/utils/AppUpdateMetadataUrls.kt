package com.abk.kernel.utils

import com.abk.kernel.BuildConfig
import com.abk.kernel.data.model.APP_UPDATE_SOURCE_FORK
import com.abk.kernel.data.model.normalizeAppUpdateSource

object AppUpdateMetadataUrls {
    private val gistMetadataOwnerRegex = Regex(
        """^https://gist\.githubusercontent\.com/([^/]+)/.+$""",
        RegexOption.IGNORE_CASE,
    )

    fun resolve(
        source: String,
        upstreamMetadataUrl: String = BuildConfig.APP_UPDATE_METADATA_URL,
        forkRepoFullName: String?,
        forkDefaultBranch: String?,
        fallbackBranch: String = BuildConfig.SOURCE_REPO_DEFAULT_BRANCH,
    ): String? {
        val bakedUrl = upstreamMetadataUrl.trim()
        return when (normalizeAppUpdateSource(source)) {
            APP_UPDATE_SOURCE_FORK -> resolveForkMetadataUrl(
                bakedMetadataUrl = bakedUrl,
                forkRepoFullName = forkRepoFullName,
                forkDefaultBranch = forkDefaultBranch,
                fallbackBranch = fallbackBranch,
            )
            else -> bakedUrl.takeIf { it.isNotEmpty() }
        }
    }

    internal fun resolveForkMetadataUrl(
        bakedMetadataUrl: String,
        forkRepoFullName: String?,
        forkDefaultBranch: String?,
        fallbackBranch: String = BuildConfig.SOURCE_REPO_DEFAULT_BRANCH,
    ): String? {
        val baked = bakedMetadataUrl.trim()
        if (baked.isNotEmpty() && isGistMetadataUrl(baked)) {
            val forkOwner = forkRepoOwner(forkRepoFullName)
            val gistOwner = gistMetadataOwner(baked)
            if (forkOwner.isNullOrBlank() || gistOwner.equals(forkOwner, ignoreCase = true)) {
                return baked
            }
        }
        return forkMetadataUrl(
            forkRepoFullName = forkRepoFullName,
            forkDefaultBranch = forkDefaultBranch,
            fallbackBranch = fallbackBranch,
        )
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

    internal fun isGistMetadataUrl(url: String): Boolean =
        gistMetadataOwnerRegex.matches(url.trim())

    internal fun gistMetadataOwner(url: String): String? =
        gistMetadataOwnerRegex.matchEntire(url.trim())?.groupValues?.getOrNull(1)

    private fun forkRepoOwner(forkRepoFullName: String?): String? =
        forkRepoFullName?.trim()?.substringBefore('/')?.takeUnless { it.isBlank() }
}
