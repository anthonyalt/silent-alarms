package nz.silentalarms.app.scheduler

import nz.silentalarms.app.data.Alarm

interface AlarmScheduling {
    fun schedule(alarm: Alarm)

    fun scheduleAt(alarm: Alarm, triggerMillis: Long)

    fun cancel(alarm: Alarm)

    fun cancel(alarmId: Long)
}
