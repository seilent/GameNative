package app.gamenative.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("steam_collection")
data class SteamCollection(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo("app_ids")
    val appIds: List<Int>,
    @ColumnInfo("is_hidden")
    val isHidden: Boolean = false,
    @ColumnInfo("sort_order")
    val sortOrder: Int = 0,
)
