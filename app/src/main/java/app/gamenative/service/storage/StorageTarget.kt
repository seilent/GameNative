package app.gamenative.service.storage

data class StorageTarget(
    val id: String,
    val label: String,
    val rootPath: String,
    val isInternal: Boolean,
    val isRemovable: Boolean,
    val isMounted: Boolean,
)
