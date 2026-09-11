package nz.silentalarms.app

import android.app.Application
import androidx.room.Room
import nz.silentalarms.app.data.AlarmDatabase
import nz.silentalarms.app.data.settingsDataStore

class SilentAlarmsApplication : Application() {
    val database: AlarmDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AlarmDatabase::class.java,
            "alarms.db",
        ).build()
    }

    val settings by lazy { applicationContext.settingsDataStore }
}
