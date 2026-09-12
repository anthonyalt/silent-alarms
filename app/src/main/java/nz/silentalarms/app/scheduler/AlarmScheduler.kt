package nz.silentalarms.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.ZoneId
import java.time.ZonedDateTime
import nz.silentalarms.app.MainActivity
import nz.silentalarms.app.data.Alarm
import nz.silentalarms.app.receiver.AlarmReceiver

class AlarmScheduler(private val context: Context) : AlarmScheduling {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }

        scheduleAt(alarm, calculateNextTriggerMillis(alarm))
    }

    override fun scheduleAt(alarm: Alarm, triggerMillis: Long) {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager service not available")
            return
        }

        if (!canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarm: exact alarm permission missing")
            return
        }

        val operationIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGERED
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val operationPendingIntent = PendingIntent.getBroadcast(
            context,
            getPendingIntentRequestCode(alarm.id),
            operationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            getPendingIntentRequestCode(alarm.id),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, operationPendingIntent)

        Log.d(TAG, "Scheduled alarm id=${alarm.id} at $triggerMillis")
    }

    override fun cancel(alarm: Alarm) {
        cancel(alarm.id)
    }

    override fun cancel(alarmId: Long) {
        if (alarmManager == null) return

        val operationIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGERED
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val operationPendingIntent = PendingIntent.getBroadcast(
            context,
            getPendingIntentRequestCode(alarmId),
            operationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.cancel(operationPendingIntent)
        operationPendingIntent.cancel()

        Log.d(TAG, "Cancelled alarm id=$alarmId")
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        const val ACTION_ALARM_TRIGGERED = "nz.silentalarms.app.ALARM_TRIGGERED"
        const val EXTRA_ALARM_ID = "nz.silentalarms.app.EXTRA_ALARM_ID"

        fun getPendingIntentRequestCode(alarmId: Long): Int = (alarmId % Int.MAX_VALUE).toInt()

        fun calculateNextTriggerMillis(
            alarm: Alarm,
            now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault()),
        ): Long {
            val targetToday = now
                .withHour(alarm.hour)
                .withMinute(alarm.minute)
                .withSecond(0)
                .withNano(0)

            if (alarm.daysOfWeek.isEmpty()) {
                val trigger = if (targetToday.isAfter(now)) targetToday else targetToday.plusDays(1)
                return trigger.toInstant().toEpochMilli()
            } else {
                for (i in 0..7) {
                    val candidate = targetToday.plusDays(i.toLong())
                    if (candidate.isAfter(now) && candidate.dayOfWeek in alarm.daysOfWeek) {
                        return candidate.toInstant().toEpochMilli()
                    }
                }
                return targetToday.plusDays(1).toInstant().toEpochMilli()
            }
        }
    }
}
