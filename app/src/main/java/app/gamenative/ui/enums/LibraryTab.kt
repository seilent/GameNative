package app.gamenative.ui.enums

import android.content.Context
import androidx.annotation.StringRes
import app.gamenative.BuildConfig
import app.gamenative.R
import app.gamenative.service.amazon.AmazonService
import app.gamenative.service.epic.EpicService
import app.gamenative.service.gog.GOGService

enum class LibraryTab(
    @get:StringRes val labelResId: Int,
    val showCustom: Boolean,
    val showSteam: Boolean,
    val showGoG: Boolean,
    val showEpic: Boolean,
    val showAmazon: Boolean,
    val installedOnly: Boolean,
) {
    ALL(
        labelResId = R.string.tab_all,
        showCustom = true,
        showSteam = true,
        showGoG = true,
        showEpic = true,
        showAmazon = true,
        installedOnly = false,
    ),
    INSTALLED(
        labelResId = R.string.tab_installed,
        showCustom = true,
        showSteam = true,
        showGoG = true,
        showEpic = true,
        showAmazon = true,
        installedOnly = true,
    ),
    STEAM(
        labelResId = R.string.tab_steam,
        showCustom = false,
        showSteam = true,
        showGoG = false,
        showEpic = false,
        showAmazon = false,
        installedOnly = false,
    ),
    GOG(
        labelResId = R.string.tab_gog,
        showCustom = false,
        showSteam = false,
        showGoG = true,
        showEpic = false,
        showAmazon = false,
        installedOnly = false,
    ),
    EPIC(
        labelResId = R.string.tab_epic,
        showCustom = false,
        showSteam = false,
        showGoG = false,
        showEpic = true,
        showAmazon = false,
        installedOnly = false,
    ),
    AMAZON(
        labelResId = R.string.tab_amazon,
        showCustom = false,
        showSteam = false,
        showGoG = false,
        showEpic = false,
        showAmazon = true,
        installedOnly = false,
    ),
    LOCAL(
        labelResId = R.string.tab_local,
        showCustom = true,
        showSteam = false,
        showGoG = false,
        showEpic = false,
        showAmazon = false,
        installedOnly = false,
    );

    companion object {
        val visibleEntries: List<LibraryTab>
            get() = if (BuildConfig.MODERN_ANDROID) entries.filter { it != LOCAL } else entries

        fun visibleEntries(context: Context): List<LibraryTab> = entries.filter { tab ->
            when (tab) {
                LOCAL -> !BuildConfig.MODERN_ANDROID
                GOG -> GOGService.hasStoredCredentials(context)
                EPIC -> EpicService.hasStoredCredentials(context)
                AMAZON -> AmazonService.hasStoredCredentials(context)
                else -> true
            }
        }

        fun LibraryTab.next(visible: List<LibraryTab> = visibleEntries): LibraryTab {
            val index = visible.indexOf(this).coerceAtLeast(0)
            return visible[(index + 1) % visible.size]
        }

        fun LibraryTab.previous(visible: List<LibraryTab> = visibleEntries): LibraryTab {
            val index = visible.indexOf(this).coerceAtLeast(0)
            return visible[if (index == 0) visible.size - 1 else index - 1]
        }
    }
}
