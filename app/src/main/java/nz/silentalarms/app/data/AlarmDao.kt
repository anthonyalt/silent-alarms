package nz.silentalarms.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<Alarm>>

    @Upsert
    suspend fun upsert(alarm: Alarm)

    @Delete
    suspend fun delete(alarm: Alarm)
}
