package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.GitHubRelease
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves and describes selectable Xray core (libv2ray AAR) sources.
 *
 * The native core is baked into the APK at build time via
 * `.github/workflows/build.yml` (`core_source` input). This manager only
 * stores the user's preferred source, builds release URLs, and formats the
 * built-in source for display. It never hot-swaps native code at runtime,
 * which keeps the change compatible with upstream v2rayNG merges.
 */
object CoreSourceManager {
    fun normalizeSource(repo: String?): String {
        val trimmed = repo?.trim().orEmpty()
        return if (AppConfig.CORE_SOURCES.any { it.equals(trimmed, ignoreCase = true) }) {
            AppConfig.CORE_SOURCES.first { it.equals(trimmed, ignoreCase = true) }
        } else {
            AppConfig.DEFAULT_CORE_SOURCE
        }
    }

    fun isKnownSource(repo: String?): Boolean {
        val trimmed = repo?.trim().orEmpty()
        return AppConfig.CORE_SOURCES.any { it.equals(trimmed, ignoreCase = true) }
    }

    fun isAhmadSource(repo: String?): Boolean {
        return normalizeSource(repo) == AppConfig.CORE_SOURCE_AHMAD_LIB
    }

    fun latestReleaseApiUrl(repo: String): String {
        return "https://api.github.com/repos/${normalizeSource(repo)}/releases/latest"
    }

    fun releasesPageUrl(repo: String): String {
        return "${AppConfig.GITHUB_URL}/${normalizeSource(repo)}/releases"
    }

    fun libDownloadUrl(repo: String, tag: String): String {
        val cleanTag = tag.trim()
        return "${AppConfig.GITHUB_URL}/${normalizeSource(repo)}/releases/download/$cleanTag/libv2ray.aar"
    }

    fun xrayCoreReleasesUrl(repo: String): String {
        return if (isAhmadSource(repo)) {
            "${AppConfig.GITHUB_URL}/${AppConfig.CORE_XRAY_AHMAD}/releases"
        } else {
            "${AppConfig.GITHUB_URL}/${AppConfig.CORE_XRAY_OFFICIAL}/releases"
        }
    }

    fun describeBuiltSource(builtSource: String?, libVersion: String?): String {
        val source = normalizeSource(builtSource)
        val version = libVersion?.trim().orEmpty().ifEmpty { "unknown" }
        return "$source ($version)"
    }

    fun parseLatestTag(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return JsonUtil.fromJsonSafe(json, GitHubRelease::class.java)?.tagName?.trim()?.ifEmpty { null }
    }

    suspend fun fetchLatestTag(
        repo: String,
        fetcher: suspend (UrlContentRequest) -> String? = { request -> HttpUtil.getUrlContent(request) }
    ): String? = withContext(Dispatchers.IO) {
        val response = try {
            fetcher(UrlContentRequest(url = latestReleaseApiUrl(repo), timeout = 8000))
        } catch (_: Exception) {
            null
        }
        parseLatestTag(response)
    }
}
