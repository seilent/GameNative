package app.gamenative.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.gamenative.data.SteamCollection
import kotlinx.coroutines.flow.Flow

@Dao
interface SteamCollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collections: List<SteamCollection>)

    @Query("SELECT * FROM steam_collection")
    suspend fun getAll(): List<SteamCollection>

    @Query("SELECT * FROM steam_collection")
    fun getAllFlow(): Flow<List<SteamCollection>>

    @Query("DELETE FROM steam_collection")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(collections: List<SteamCollection>) {
        deleteAll()
        insertAll(collections)
    }
}
