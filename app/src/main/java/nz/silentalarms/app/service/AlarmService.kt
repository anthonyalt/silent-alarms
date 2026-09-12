package nz.silentalarms.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import nz.silentalarms.app.AlarmActivity
import nz.silentalarms.app.R
import nz.silentalarms.app.SilentAlarmsApplication
import nz.silentalarms.app.data.Alarm
import nz.silentalarms.app.scheduler.AlarmScheduler

class AlarmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vibrator: Vibrator? = null
    private var currentAlarmId = -1L

    private val autoDismiss = Runnable {
        Log.d(TAG, "Auto-dismissing alarm id=$currentAlarmId after five minutes")
        finishAlarm()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startAlarm(intent)
            ACTION_DISMISS -> {
                Log.d(TAG, "Alarm dismissed by user, id=$currentAlarmId")
                finishAlarm()
            }
            ACTION_SNOOZE -> snoozeAlarm(intent.getLongExtra(EXTRA_ALARM_ID, currentAlarmId), startId)
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopAlerting()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startAlarm(intent: Intent) {
        stopAlerting()

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) {
            stopSelf()
            return
        }

        currentAlarmId = alarmId
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        val hour = intent.getIntExtra(EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildAlarmNotification(alarmId, label, hour, minute),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )

        startVibration()
        handler.postDelayed(autoDismiss, AUTO_DISMISS_MILLIS)
        Log.d(TAG, "Started foreground alarm id=$alarmId")
    }

    private fun snoozeAlarm(alarmId: Long, startId: Int) {
        if (alarmId == -1L) {
            finishAlarm()
            return
        }

        Log.d(TAG, "Snoozing alarm id=$alarmId for nine minutes")
        stopVibrationAndTimeout()
        sendAlarmFinished()

        val app = application as SilentAlarmsApplication
        serviceScope.launch {
            try {
                val alarm = app.database.alarmDao().getById(alarmId)
                if (alarm != null) {
                    app.database.alarmDao().setEnabled(alarm.id, true)
                    app.alarmScheduler.scheduleAt(
                        alarm.copy(isEnabled = true),
                        System.currentTimeMillis() + SNOOZE_MILLIS,
                    )
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Unable to snooze alarm id=$alarmId", exception)
            } finally {
                stopAlerting()
                stopSelf(startId)
            }
        }
    }

    private fun buildAlarmNotification(
        alarmId: Long,
        label: String,
        hour: Int,
        minute: Int,
    ): Notification {
        ensureNotificationChannel(this)

        val fullScreenIntent = PendingIntent.getActivity(
            this,
            AlarmScheduler.getPendingIntentRequestCode(alarmId),
            AlarmActivity.createIntent(this, alarmId, label, hour, minute),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissIntent = PendingIntent.getService(
            this,
            AlarmScheduler.getPendingIntentRequestCode(alarmId),
            createDismissIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozeIntent = PendingIntent.getService(
            this,
            AlarmScheduler.getPendingIntentRequestCode(alarmId),
            createSnoozeIntent(this, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        val canUseFullScreenIntent =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                notificationManager.canUseFullScreenIntent()

        if (!canUseFullScreenIntent) {
            Log.w(TAG, "Full-screen intent permission unavailable; using heads-up notification")
        }

        val title = label.ifBlank { getString(R.string.unlabelled_alarm) }
        val time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(time)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenIntent)
            .addAction(0, getString(R.string.snooze_alarm), snoozeIntent)
            .addAction(0, getString(R.string.dismiss_alarm), dismissIntent)

        if (canUseFullScreenIntent) {
            builder.setFullScreenIntent(fullScreenIntent, true)
        }

        return builder.build()
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

        val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 1)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        vibrator?.vibrate(effect, attributes)
    }

    private fun finishAlarm() {
        stopAlerting()
        sendAlarmFinished()
        stopSelf()
    }

    private fun stopAlerting() {
        stopVibrationAndTimeout()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    private fun stopVibrationAndTimeout() {
        handler.removeCallbacks(autoDismiss)
        vibrator?.cancel()
        vibrator = null
    }

    private fun sendAlarmFinished() {
        sendBroadcast(
            Intent(ACTION_ALARM_FINISHED)
                .setPackage(packageName),
        )
    }

    companion object {
        private const val TAG = "AlarmService"
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 1_001
        private const val ACTION_START = "nz.silentalarms.app.action.START_ALARM"
        private const val ACTION_DISMISS = "nz.silentalarms.app.action.DISMISS_ALARM"
        private const val ACTION_SNOOZE = "nz.silentalarms.app.action.SNOOZE_ALARM"
        const val ACTION_ALARM_FINISHED = "nz.silentalarms.app.action.ALARM_FINISHED"
        private const val EXTRA_ALARM_ID = "alarm_id"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_HOUR = "hour"
        private const val EXTRA_MINUTE = "minute"
        const val AUTO_DISMISS_MILLIS = 5 * 60 * 1_000L
        const val SNOOZE_MILLIS = 9 * 60 * 1_000L
        private val VIBRATION_PATTERN = longArrayOf(0, 1_000, 500, 1_000, 1_000)

        fun createStartIntent(context: Context, alarm: Alarm): Intent =
            Intent(context, AlarmService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ALARM_ID, alarm.id)
                putExtra(EXTRA_LABEL, alarm.label)
                putExtra(EXTRA_HOUR, alarm.hour)
                putExtra(EXTRA_MINUTE, alarm.minute)
            }

        fun createDismissIntent(context: Context): Intent =
            Intent(context, AlarmService::class.java).apply {
                action = ACTION_DISMISS
            }

        fun createSnoozeIntent(context: Context, alarmId: Long): Intent =
            Intent(context, AlarmService::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_ALARM_ID, alarmId)
            }

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(NotificationManager::class.java) ?: return
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.notification_channel_description)
                    setSound(null, attributes)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
