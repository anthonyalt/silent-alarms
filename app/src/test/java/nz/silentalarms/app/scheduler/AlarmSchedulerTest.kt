package nz.silentalarms.app.scheduler

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import nz.silentalarms.app.data.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmSchedulerTest {
    private val zoneId = ZoneId.of("Pacific/Auckland")

    @Test
    fun calculateNextTriggerMillis_oneOffAlarm_futureToday() {
        val now = ZonedDateTime.of(LocalDateTime.of(2026, 9, 12, 10, 0), zoneId)
        val alarm = Alarm(id = 1, label = "Test", hour = 11, minute = 30, vibrationPatternId = "default")

        val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(alarm, now)
        val expected = ZonedDateTime.of(LocalDateTime.of(2026, 9, 12, 11, 30), zoneId).toInstant().toEpochMilli()

        assertEquals(expected, triggerMillis)
    }

    @Test
    fun calculateNextTriggerMillis_oneOffAlarm_pastToday() {
        val now = ZonedDateTime.of(LocalDateTime.of(2026, 9, 12, 12, 0), zoneId)
        val alarm = Alarm(id = 1, label = "Test", hour = 11, minute = 30, vibrationPatternId = "default")

        val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(alarm, now)
        val expected = ZonedDateTime.of(LocalDateTime.of(2026, 9, 13, 11, 30), zoneId).toInstant().toEpochMilli()

        assertEquals(expected, triggerMillis)
    }

    @Test
    fun calculateNextTriggerMillis_repeatingAlarm_laterToday() {
        // Saturday 2026-09-12 09:00
        val now = ZonedDateTime.of(LocalDateTime.of(2026, 9, 12, 9, 0), zoneId)
        val alarm = Alarm(
            id = 1,
            label = "Weekend",
            hour = 10,
            minute = 0,
            daysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            vibrationPatternId = "default",
        )

        val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(alarm, now)
        val expected = ZonedDateTime.of(LocalDateTime.of(2026, 9, 12, 10, 0), zoneId).toInstant().toEpochMilli()

        assertEquals(expected, triggerMillis)
    }

    @Test
    fun calculateNextTriggerMillis_repeatingAlarm_nextMatchingDay() {
        // Saturday 2026-09-12 11:00
        val now = ZonedDateTime.of(LocalDateTime.of(2026, 9, 12, 11, 0), zoneId)
        val alarm = Alarm(
            id = 1,
            label = "Weekday",
            hour = 8,
            minute = 0,
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            vibrationPatternId = "default",
        )

        val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(alarm, now)
        // Next matching day is Monday 2026-09-14 08:00
        val expected = ZonedDateTime.of(LocalDateTime.of(2026, 9, 14, 8, 0), zoneId).toInstant().toEpochMilli()

        assertEquals(expected, triggerMillis)
    }

    @Test
    fun pendingIntentRequestCode_derivedFromId() {
        assertEquals(42, AlarmScheduler.getPendingIntentRequestCode(42L))
        assertEquals(0, AlarmScheduler.getPendingIntentRequestCode(Int.MAX_VALUE.toLong() * 2L + 0L))
    }
}
