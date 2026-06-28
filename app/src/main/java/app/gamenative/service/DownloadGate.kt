package app.gamenative.service

import app.gamenative.PrefManager
import app.gamenative.data.DownloadInfo
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object DownloadGate {
    private const val EXTERNAL_MAX_CONCURRENT = 1

    private val semaphore = Semaphore(EXTERNAL_MAX_CONCURRENT)

    suspend fun <T> withSlot(downloadInfo: DownloadInfo, block: suspend () -> T): T {
        if (!PrefManager.useExternalStorage) {
            return block()
        }
        if (semaphore.availablePermits == 0) {
            downloadInfo.updateStatusMessage("Queued")
            downloadInfo.emitProgressChange()
        }
        return semaphore.withPermit {
            downloadInfo.updateStatusMessage(null)
            downloadInfo.emitProgressChange()
            block()
        }
    }
}
