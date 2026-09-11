package nz.silentalarms.app.data

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmTest {
    @Test
    fun newAlarmHasStorageDefaults() {
        val alarm = alarm(hour = 0, minute = 0)

        assertEquals(0L, alarm.id)
        assertEquals(emptySet<DayOfWeek>(), alarm.daysOfWeek)
        assertNull(alarm.soundUri)
        assertTrue(alarm.isEnabled)
        assertTrue(alarm.createdAt > 0)
    }

    @Test
    fun acceptsEndOfDay() {
        val alarm = alarm(hour = 23, minute = 59)
        assertEquals(23, alarm.hour)
        assertEquals(59, alarm.minute)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsHourBelowZero() {
        alarm(hour = -1, minute = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsHourAboveTwentyThree() {
        alarm(hour = 24, minute = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMinuteBelowZero() {
        alarm(hour = 0, minute = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMinuteAboveFiftyNine() {
        alarm(hour = 0, minute = 60)
    }

    private fun alarm(hour: Int, minute: Int) =
        Alarm(label = "", hour = hour, minute = minute, vibrationPatternId = "default")
}
