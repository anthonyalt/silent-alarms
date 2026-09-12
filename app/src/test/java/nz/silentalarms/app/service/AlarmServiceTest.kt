package nz.silentalarms.app.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmServiceTest {
    @Test
    fun autoDismissIsExactlyFiveMinutes() {
        assertEquals(300_000L, AlarmService.AUTO_DISMISS_MILLIS)
    }

    @Test
    fun snoozeIsExactlyNineMinutes() {
        assertEquals(540_000L, AlarmService.SNOOZE_MILLIS)
    }
}
