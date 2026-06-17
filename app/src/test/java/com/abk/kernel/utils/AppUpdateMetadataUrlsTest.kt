package com.abk.kernel.utils

import com.abk.kernel.data.model.APP_UPDATE_SOURCE_FORK
import com.abk.kernel.data.model.APP_UPDATE_SOURCE_UPSTREAM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateMetadataUrlsTest {
    @Test
    fun resolvesUpstreamMetadataUrl() {
        val url = AppUpdateMetadataUrls.resolve(
            source = APP_UPDATE_SOURCE_UPSTREAM,
            upstreamMetadataUrl = "https://gist.githubusercontent.com/upstream/id/raw/version.json",
            forkRepoFullName = "fork/ABK",
            forkDefaultBranch = "dev",
        )

        assertEquals("https://gist.githubusercontent.com/upstream/id/raw/version.json", url)
    }

    @Test
    fun resolvesForkMetadataUrlFromForkRepo() {
        val url = AppUpdateMetadataUrls.resolve(
            source = APP_UPDATE_SOURCE_FORK,
            upstreamMetadataUrl = "https://gist.githubusercontent.com/upstream/id/raw/version.json",
            forkRepoFullName = "fork/ABK",
            forkDefaultBranch = "dev",
        )

        assertEquals("https://raw.githubusercontent.com/fork/ABK/dev/version.json", url)
    }

    @Test
    fun forkMetadataUrlMissingWhenForkRepoUnavailable() {
        val url = AppUpdateMetadataUrls.resolve(
            source = APP_UPDATE_SOURCE_FORK,
            upstreamMetadataUrl = "https://example.com/version.json",
            forkRepoFullName = null,
            forkDefaultBranch = null,
        )

        assertNull(url)
    }
}
