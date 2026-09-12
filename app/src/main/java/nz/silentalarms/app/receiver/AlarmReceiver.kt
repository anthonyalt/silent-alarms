package nz.silentalarms.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nz.silentalarms.app.MainActivity
import nz.silentalarms.app.R
import nz.silentalarms.app.SilentAlarmsApplication
import nz.silentalarms.app.data.Alarm
import nz.silentalarms.app.scheduler.AlarmScheduler

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
                    showNotification(context, alarm)

                    if (alarm.daysOfWeek.isNotEmpty()) {
                        // Reschedule next occurrence for repeating alarm
                        app.alarmScheduler.schedule(alarm)
                    } else {
                        // Non-repeating alarm: disable in database after firing
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

    private fun showNotification(context: Context, alarm: Alarm) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        ensureNotificationChannel(context, notificationManager)

        val contentIntent = PendingIntent.getActivity(
            context,
            AlarmScheduler.getPendingIntentRequestCode(alarm.id),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = alarm.label.ifBlank { context.getString(R.string.unlabelled_alarm) }
        val timeText = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(timeText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        notificationManager.notify(AlarmScheduler.getPendingIntentRequestCode(alarm.id), notification)
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val CHANNEL_ID = "alarm_channel"

        fun ensureNotificationChannel(context: Context, notificationManager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.notification_channel_description)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
