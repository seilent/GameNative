package app.gamenative.service.storage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager as AndroidStorageManager
import androidx.core.content.ContextCompat
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

    fun setDefaultTarget(target: StorageTarget) {
        PrefManager.defaultStorageTargetId = target.id
        if (target.isInternal) {
            PrefManager.useExternalStorage = false
        } else {
            PrefManager.useExternalStorage = true
            PrefManager.externalStoragePath = target.rootPath
        }
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

    fun registerVolumeCallback(context: Context, onChange: () -> Unit) {
        val onEvent = {
            DownloadService.populateDownloadService(context)
            DownloadService.invalidateCache()
            onChange()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val sm = context.getSystemService(AndroidStorageManager::class.java)
            sm?.registerStorageVolumeCallback(
                ContextCompat.getMainExecutor(context),
                object : AndroidStorageManager.StorageVolumeCallback() {
                    override fun onStateChanged(volume: android.os.storage.StorageVolume) {
                        onEvent()
                    }
                },
            )
        } else {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(Intent.ACTION_MEDIA_EJECT)
                addAction(Intent.ACTION_MEDIA_REMOVED)
                addDataScheme("file")
            }
            context.registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        onEvent()
                    }
                },
                filter,
            )
        }
    }
}
