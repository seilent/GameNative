package app.gamenative.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "steam_app_owner",
    primaryKeys = ["app_id", "account_id"],
    indices = [Index(value = ["account_id"])]
)
data class SteamAppOwner(
    @ColumnInfo("app_id") val appId: Int,
    @ColumnInfo("account_id") val accountId: Int,
)
