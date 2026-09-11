package nz.silentalarms.app.data

import androidx.room.TypeConverter
import java.time.DayOfWeek

class AlarmConverters {
    // Bits 0..6 represent Monday..Sunday; zero represents a non-repeating alarm.
    @TypeConverter
    fun encodeDays(days: Set<DayOfWeek>): Int =
        days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    @TypeConverter
    fun decodeDays(mask: Int): Set<DayOfWeek> {
        require(mask in 0..127) { "Days of week must use only the lowest seven bits." }
        return DayOfWeek.values().filterTo(mutableSetOf()) { day ->
            mask and (1 shl (day.value - 1)) != 0
        }
    }
}
