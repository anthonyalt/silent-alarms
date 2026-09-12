package nz.silentalarms.app

import android.app.Application
import android.app.NotificationManager
import androidx.room.Room
import nz.silentalarms.app.data.AlarmDatabase
import nz.silentalarms.app.data.settingsDataStore
import nz.silentalarms.app.receiver.AlarmReceiver
import nz.silentalarms.app.scheduler.AlarmScheduler

class SilentAlarmsApplication : Application() {
    val database: AlarmDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AlarmDatabase::class.java,
            "alarms.db",
        ).build()
    }

    val settings by lazy { applicationContext.settingsDataStore }

    val alarmScheduler by lazy { AlarmScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)?.let { notificationManager ->
            AlarmReceiver.ensureNotificationChannel(this, notificationManager)
        }
    }
}
