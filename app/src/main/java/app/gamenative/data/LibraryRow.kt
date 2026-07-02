package app.gamenative.data

data class LibraryRow(
    val id: Int,
    val name: String,
    val nameLower: String,
    val clientIconHash: String,
    val capsuleUrl: String,
    val heroUrl: String,
    val headerUrlCached: String,
    val sizeBytes: Long,
    val isInstalled: Boolean,
    val isShared: Boolean,
    val type: Int,
)
