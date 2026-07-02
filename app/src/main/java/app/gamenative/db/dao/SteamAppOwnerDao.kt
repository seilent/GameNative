package app.gamenative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.gamenative.data.SteamAppOwner

@Dao
interface SteamAppOwnerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(owners: List<SteamAppOwner>)

    @Query("DELETE FROM steam_app_owner WHERE app_id = :appId")
    suspend fun deleteForApp(appId: Int)

    @Query("DELETE FROM steam_app_owner")
    suspend fun deleteAll()
}
