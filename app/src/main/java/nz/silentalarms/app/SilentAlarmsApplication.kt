package nz.silentalarms.app

import android.app.Application
import androidx.room.Room
import nz.silentalarms.app.data.AlarmDatabase
import nz.silentalarms.app.data.settingsDataStore
import nz.silentalarms.app.scheduler.AlarmScheduler
import nz.silentalarms.app.service.AlarmService

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
        AlarmService.ensureNotificationChannel(this)
    }
}
