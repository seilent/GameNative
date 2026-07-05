package app.gamenative.utils

import android.content.Context
import app.gamenative.BuildConfig
import app.gamenative.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

@Serializable
data class UpdateInfo(
    val updateAvailable: Boolean,
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String? = null
)

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val assets: List<GithubAsset> = emptyList()
)

@Serializable
private data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)

object UpdateChecker {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(Constants.Misc.UPDATE_CHECK_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GameNative-UpdateChecker")
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val release = json.decodeFromString<GithubRelease>(responseBody)
                    val latestVersionName = release.tagName.removePrefix("v").trim()
                    val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                        ?: return@withContext null

                    val updateAvailable = isVersionNewer(latestVersionName, BuildConfig.VERSION_NAME)
                    Timber.i("Update check: updateAvailable=$updateAvailable, latest=$latestVersionName, current=${BuildConfig.VERSION_NAME}")

                    return@withContext UpdateInfo(
                        updateAvailable = updateAvailable,
                        versionCode = 0,
                        versionName = latestVersionName,
                        downloadUrl = apkAsset.browserDownloadUrl,
                        releaseNotes = release.body
                    )
                }
            } else {
                Timber.w("Update check failed: HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking for updates")
        }
        return@withContext null
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
