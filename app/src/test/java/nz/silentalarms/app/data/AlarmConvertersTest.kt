package nz.silentalarms.app.data

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmConvertersTest {
    private val converters = AlarmConverters()

    @Test
    fun everyWeekdayCombinationRoundTrips() {
        for (mask in 0..127) {
            assertEquals(mask, converters.encodeDays(converters.decodeDays(mask)))
        }
    }

    @Test
    fun weekdaysUseIsoOrderAndEmptySetMeansNoRepeat() {
        assertEquals(0, converters.encodeDays(emptySet()))
        assertEquals(emptySet<DayOfWeek>(), converters.decodeDays(0))
        assertEquals(1, converters.encodeDays(setOf(DayOfWeek.MONDAY)))
        assertEquals(64, converters.encodeDays(setOf(DayOfWeek.SUNDAY)))
        assertEquals(DayOfWeek.values().toSet(), converters.decodeDays(127))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeBits() {
        converters.decodeDays(128)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeMasks() {
        converters.decodeDays(-1)
    }
}
