package nz.silentalarms.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nz.silentalarms.app.SilentAlarmsApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d(TAG, "Received ${intent.action}, rescheduling enabled alarms...")
            val pendingResult = goAsync()
            val app = context.applicationContext as SilentAlarmsApplication

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val enabledAlarms = app.database.alarmDao().getEnabledAlarms()
                    val scheduler = app.alarmScheduler
                    enabledAlarms.forEach { alarm ->
                        scheduler.schedule(alarm)
                    }
                    Log.d(TAG, "Rescheduled ${enabledAlarms.size} alarms after boot.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
