package nz.silentalarms.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nz.silentalarms.app.SilentAlarmsApplication
import nz.silentalarms.app.scheduler.AlarmScheduler
import nz.silentalarms.app.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_ALARM_TRIGGERED) return

        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        Log.d(TAG, "Alarm fired for id=$alarmId")
        val pendingResult = goAsync()
        val app = context.applicationContext as SilentAlarmsApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarmDao = app.database.alarmDao()
                val alarm = alarmDao.getById(alarmId)
                if (alarm != null) {
                    Log.d(
                        "SilentAlarms",
                        "Alarm fired: id=${alarm.id}, label='${alarm.label}', time=${alarm.hour}:${alarm.minute}",
                    )
                    ContextCompat.startForegroundService(
                        context,
                        AlarmService.createStartIntent(context, alarm),
                    )

                    if (alarm.daysOfWeek.isNotEmpty()) {
                        app.alarmScheduler.schedule(alarm)
                    } else {
                        alarmDao.setEnabled(alarm.id, false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing fired alarm id=$alarmId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
