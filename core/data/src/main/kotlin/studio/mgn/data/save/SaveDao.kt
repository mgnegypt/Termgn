package studio.mgn.data.save

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SaveDao {
    @Query("SELECT * FROM saves WHERE id = :id")
    suspend fun get(id: Int): SaveSlot?

    @Query("SELECT * FROM saves")
    suspend fun getAll(): List<SaveSlot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(slot: SaveSlot)

    @Query("DELETE FROM saves WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM saves")
    suspend fun deleteAll()
}
