package nz.silentalarms.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val vibrationPatternId: String,
    val soundUri: String? = null,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
) {
    init {
        require(hour in 0..23) { "Hour must be between 0 and 23." }
        require(minute in 0..59) { "Minute must be between 0 and 59." }
    }
}
