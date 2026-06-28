package app.gamenative.service.storage

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager as AndroidStorageManager
import app.gamenative.PrefManager
import app.gamenative.service.DownloadService
import app.gamenative.utils.StorageUtils
import timber.log.Timber

object StorageManager {

    const val INTERNAL_ID = "internal"

    fun internalTarget(): StorageTarget = StorageTarget(
        id = INTERNAL_ID,
        label = "Internal storage",
        rootPath = DownloadService.baseDataDirPath,
        isInternal = true,
        isRemovable = false,
        isMounted = true,
    )

    fun externalTargets(context: Context): List<StorageTarget> {
        val sm = context.getSystemService(AndroidStorageManager::class.java)
        val seen = mutableSetOf<String>()
        val targets = mutableListOf<StorageTarget>()
        for (dir in StorageUtils.getAllExternalFilesDirs(context)) {
            val volume = try {
                sm?.getStorageVolume(dir)
            } catch (e: Exception) {
                null
            } ?: continue
            if (volume.isPrimary) continue
            val rootPath = dir.absolutePath
            if (!seen.add(rootPath)) continue
            val id = volume.uuid ?: rootPath.hashCode().toString()
            val label = volume.getDescription(context)
                ?: if (volume.isRemovable) "SD card" else "External storage"
            targets += StorageTarget(
                id = id,
                label = label,
                rootPath = rootPath,
                isInternal = false,
                isRemovable = volume.isRemovable,
                isMounted = Environment.getExternalStorageState(dir) == Environment.MEDIA_MOUNTED,
            )
        }
        return targets
    }

    fun allTargets(context: Context): List<StorageTarget> =
        listOf(internalTarget()) + externalTargets(context)

    fun targetById(context: Context, id: String?): StorageTarget? {
        if (id.isNullOrBlank()) return null
        return allTargets(context).firstOrNull { it.id == id }
    }

    fun defaultInstallTarget(context: Context): StorageTarget {
        val chosen = targetById(context, PrefManager.defaultStorageTargetId)
        if (chosen != null && chosen.isMounted) return chosen
        return internalTarget()
    }

    fun freeBytes(target: StorageTarget): Long = try {
        StorageUtils.getAvailableSpaceForUncreatedPath(target.rootPath)
    } catch (e: Exception) {
        Timber.w(e, "free space failed for %s", target.rootPath)
        0L
    }

    fun totalBytes(target: StorageTarget): Long = try {
        StorageUtils.getTotalSpace(target.rootPath)
    } catch (e: Exception) {
        0L
    }
}
