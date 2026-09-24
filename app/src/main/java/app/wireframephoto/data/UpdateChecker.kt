package app.wireframephoto.data

import android.content.Context
import app.wireframephoto.core.Versions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A newer release published on GitHub. [apkUrl] downloads the APK directly. */
data class AvailableUpdate(val version: String, val apkUrl: String, val pageUrl: String)

/** Outcome of a check the user asked for ("업데이트 확인"). */
sealed interface UpdateCheck {
    data class Available(val update: AvailableUpdate) : UpdateCheck
    data object UpToDate : UpdateCheck
    data object Failed : UpdateCheck
}

/**
 * Sideloaded builds get no store updates, so the app asks GitHub for the latest release.
 * One anonymous GET to api.github.com each time the app comes to the front, at most every
 * [CHECK_INTERVAL_MS]. Requests carry the last ETag, so an unchanged answer is a 304 that GitHub
 * doesn't count against its rate limit. The answer is cached so the banner still shows offline.
 * Versions the user dismissed are not offered again, unless they check by hand.
 */
class UpdateChecker(private val context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    val installedVersion: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    /** Last known newer release, without touching the network. */
    fun cached(): AvailableUpdate? {
        val version = prefs.getString(KEY_VERSION, null) ?: return null
        val apk = prefs.getString(KEY_APK, null) ?: return null
        val page = prefs.getString(KEY_PAGE, null) ?: return null
        return AvailableUpdate(version, apk, page).takeIf(::offerable)
    }

    /** Refreshes from GitHub when the cache is stale; network failures just keep the cache. */
    suspend fun check(): AvailableUpdate? {
        if (System.currentTimeMillis() - prefs.getLong(KEY_CHECKED_AT, 0) < CHECK_INTERVAL_MS) return cached()
        return refresh()?.takeIf(::offerable) ?: cached()
    }

    /** "업데이트 확인": always asks GitHub, and shows a version even if it was dismissed before. */
    suspend fun checkNow(): UpdateCheck {
        val latest = refresh() ?: return UpdateCheck.Failed
        if (!Versions.isNewer(latest.version, installedVersion)) return UpdateCheck.UpToDate
        prefs.edit().remove(KEY_DISMISSED).apply()
        return UpdateCheck.Available(latest)
    }

    /** Fetches the latest release and caches it; null if GitHub couldn't be reached. */
    private suspend fun refresh(): AvailableUpdate? {
        val latest = withContext(Dispatchers.IO) { runCatching { fetchLatest() }.getOrNull() } ?: return null
        prefs.edit()
            .putLong(KEY_CHECKED_AT, System.currentTimeMillis())
            .putString(KEY_VERSION, latest.version)
            .putString(KEY_APK, latest.apkUrl)
            .putString(KEY_PAGE, latest.pageUrl)
            .apply()
        return latest
    }

    /** The cached release without the "newer / not dismissed" filter (a 304 means it's current). */
    private fun cachedRelease(): AvailableUpdate? {
        val version = prefs.getString(KEY_VERSION, null) ?: return null
        val apk = prefs.getString(KEY_APK, null) ?: return null
        val page = prefs.getString(KEY_PAGE, null) ?: return null
        return AvailableUpdate(version, apk, page)
    }

    /** "나중에": hide this version; a later release will be offered again. */
    fun dismiss(update: AvailableUpdate) {
        prefs.edit().putString(KEY_DISMISSED, update.version).apply()
    }

    private fun offerable(update: AvailableUpdate) =
        Versions.isNewer(update.version, installedVersion) && update.version != prefs.getString(KEY_DISMISSED, null)

    private fun fetchLatest(): AvailableUpdate? {
        val conn = URL(LATEST_URL).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "oh-my-photogrid/$installedVersion")
            prefs.getString(KEY_ETAG, null)?.let { conn.setRequestProperty("If-None-Match", it) }
            if (conn.responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) return cachedRelease()
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            conn.getHeaderField("ETag")?.let { prefs.edit().putString(KEY_ETAG, it).apply() }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val assets = json.getJSONArray("assets")
            val apk = (0 until assets.length()).map { assets.getJSONObject(it).getString("browser_download_url") }
                .firstOrNull { it.endsWith(".apk") } ?: return null
            return AvailableUpdate(json.getString("tag_name").removePrefix("v"), apk, json.getString("html_url"))
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val LATEST_URL = "https://api.github.com/repos/Canine89/oh-my-photogrid/releases/latest"
        // GitHub caches this answer for 60 s, so checking more often can't see anything newer.
        // Unchanged answers are 304s, which don't count against the 60/hour anonymous limit.
        const val CHECK_INTERVAL_MS = 60 * 1000L
        const val KEY_CHECKED_AT = "checked_at"
        const val KEY_VERSION = "latest_version"
        const val KEY_APK = "latest_apk"
        const val KEY_PAGE = "latest_page"
        const val KEY_DISMISSED = "dismissed_version"
        const val KEY_ETAG = "latest_etag"
    }
}
