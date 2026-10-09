package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoreSourceManagerTest {
    @Test
    fun normalizeFallsBackToOfficialForUnknown() {
        assertEquals(AppConfig.CORE_SOURCE_OFFICIAL, CoreSourceManager.normalizeSource(null))
        assertEquals(AppConfig.CORE_SOURCE_OFFICIAL, CoreSourceManager.normalizeSource(""))
        assertEquals(AppConfig.CORE_SOURCE_OFFICIAL, CoreSourceManager.normalizeSource("someone/else"))
    }

    @Test
    fun normalizeKeepsKnownSourcesCaseInsensitive() {
        assertEquals(
            AppConfig.CORE_SOURCE_AHMAD_LIB,
            CoreSourceManager.normalizeSource("ahmad-2213/androidlibxraylite")
        )
        assertEquals(
            AppConfig.CORE_SOURCE_OFFICIAL,
            CoreSourceManager.normalizeSource(" 2dust/AndroidLibXrayLite ")
        )
    }

    @Test
    fun urlBuildersUseNormalizedSource() {
        val tag = "v1.2.3"
        assertEquals(
            "https://github.com/Ahmad-2213/AndroidLibXrayLite/releases/download/v1.2.3/libv2ray.aar",
            CoreSourceManager.libDownloadUrl(AppConfig.CORE_SOURCE_AHMAD_LIB, tag)
        )
        assertEquals(
            "https://api.github.com/repos/2dust/AndroidLibXrayLite/releases/latest",
            CoreSourceManager.latestReleaseApiUrl("unknown/repo")
        )
        assertEquals(
            "https://github.com/Ahmad-2213/Xray-core/releases",
            CoreSourceManager.xrayCoreReleasesUrl(AppConfig.CORE_SOURCE_AHMAD_LIB)
        )
        assertEquals(
            "https://github.com/XTLS/Xray-core/releases",
            CoreSourceManager.xrayCoreReleasesUrl(AppConfig.CORE_SOURCE_OFFICIAL)
        )
    }

    @Test
    fun parseLatestTagHandlesValidAndInvalidJson() {
        val json = """{"tag_name":"v1.2.3","body":"","assets":[],"prerelease":false}"""
        assertEquals("v1.2.3", CoreSourceManager.parseLatestTag(json))
        assertNull(CoreSourceManager.parseLatestTag(null))
        assertNull(CoreSourceManager.parseLatestTag(""))
        assertNull(CoreSourceManager.parseLatestTag("{not json"))
    }

    @Test
    fun describeBuiltSourceIncludesVersion() {
        assertEquals(
            "${AppConfig.CORE_SOURCE_OFFICIAL} (Xray 1.2.3)",
            CoreSourceManager.describeBuiltSource(AppConfig.CORE_SOURCE_OFFICIAL, "Xray 1.2.3")
        )
        assertTrue(CoreSourceManager.isAhmadSource(AppConfig.CORE_SOURCE_AHMAD_LIB))
        assertFalse(CoreSourceManager.isAhmadSource(AppConfig.CORE_SOURCE_OFFICIAL))
    }

    @Test
    fun fetchLatestTagUsesInjectedFetcher(): Unit = runBlocking {
        val json = """{"tag_name":"v9.9.9","body":"","assets":[],"prerelease":false}"""
        val tag = CoreSourceManager.fetchLatestTag(AppConfig.CORE_SOURCE_OFFICIAL) { json }
        assertEquals("v9.9.9", tag)
    }
}
