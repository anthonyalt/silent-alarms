package nz.silentalarms.app.ui

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import nz.silentalarms.app.data.Alarm
import nz.silentalarms.app.data.AlarmDatabase
import nz.silentalarms.app.scheduler.AlarmScheduling
import nz.silentalarms.app.testing.MainDispatcherRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AlarmViewModelPersistenceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AlarmDatabase
    private lateinit var scheduler: RecordingAlarmScheduler

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AlarmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scheduler = RecordingAlarmScheduler()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createdAlarmPersistsAcrossViewModelRecreation() = runTest {
        val firstStore = ViewModelStore()
        val firstViewModel = createViewModel(firstStore)

        firstViewModel.startAddingAlarm()
        firstViewModel.updateEditorTime(6, 45)
        firstViewModel.updateEditorLabel("Wake up")
        firstViewModel.toggleEditorDay(DayOfWeek.MONDAY)
        firstViewModel.toggleEditorDay(DayOfWeek.FRIDAY)
        firstViewModel.saveEditor()

        val saved = database.alarmDao().observeAll().first { it.size == 1 }.single()
        firstStore.clear()

        val secondStore = ViewModelStore()
        val secondViewModel = createViewModel(secondStore)
        val restored = secondViewModel.uiState.first { it.alarms.size == 1 }.alarms.single()

        assertEquals(saved.id, restored.id)
        assertEquals("Wake up", restored.label)
        assertEquals(6, restored.hour)
        assertEquals(45, restored.minute)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), restored.daysOfWeek)
        assertTrue(restored.isEnabled)
        assertEquals(saved.createdAt, restored.createdAt)
        secondStore.clear()
    }

    @Test
    fun enabledTogglePersistsAcrossViewModelRecreation() = runTest {
        val alarmId = database.alarmDao().upsert(alarm())
        val firstStore = ViewModelStore()
        val firstViewModel = createViewModel(firstStore)

        firstViewModel.setAlarmEnabled(alarmId, false)
        database.alarmDao().observeAll().first { alarms ->
            alarms.singleOrNull()?.isEnabled == false
        }
        firstStore.clear()

        val secondStore = ViewModelStore()
        val secondViewModel = createViewModel(secondStore)
        val disabled = secondViewModel.uiState.first { it.alarms.size == 1 }.alarms.single()
        assertFalse(disabled.isEnabled)

        secondViewModel.setAlarmEnabled(alarmId, true)
        database.alarmDao().observeAll().first { alarms ->
            alarms.singleOrNull()?.isEnabled == true
        }
        secondStore.clear()

        val thirdStore = ViewModelStore()
        val thirdViewModel = createViewModel(thirdStore)
        val enabled = thirdViewModel.uiState.first { it.alarms.size == 1 }.alarms.single()
        assertTrue(enabled.isEnabled)
        thirdStore.clear()
    }

    @Test
    fun deleteThenUndoRestoresAlarm() = runTest {
        val alarmId = database.alarmDao().upsert(alarm())
        val store = ViewModelStore()
        val viewModel = createViewModel(store)

        viewModel.deleteAlarm(alarmId)
        database.alarmDao().observeAll().first { it.isEmpty() }
        viewModel.uiState.first { it.deletedAlarm?.id == alarmId }

        viewModel.undoDelete()
        val restored = database.alarmDao().observeAll().first { it.size == 1 }.single()

        assertEquals(alarmId, restored.id)
        assertEquals("Persisted", restored.label)
        assertTrue(restored.isEnabled)
        assertTrue(alarmId in scheduler.cancelledAlarmIds)
        assertTrue(scheduler.scheduledAlarms.any { it.id == alarmId })
        store.clear()
    }

    private fun createViewModel(store: ViewModelStore): AlarmViewModel =
        AlarmViewModel(database.alarmDao(), scheduler).also {
            store.put("alarm-view-model", it)
        }

    private fun alarm() =
        Alarm(
            label = "Persisted",
            hour = 7,
            minute = 30,
            daysOfWeek = setOf(DayOfWeek.TUESDAY),
            vibrationPatternId = "default",
        )
}

private class RecordingAlarmScheduler : AlarmScheduling {
    val scheduledAlarms = mutableListOf<Alarm>()
    val cancelledAlarmIds = mutableListOf<Long>()

    override fun schedule(alarm: Alarm) {
        scheduledAlarms += alarm
    }

    override fun scheduleAt(alarm: Alarm, triggerMillis: Long) {
        scheduledAlarms += alarm
    }

    override fun cancel(alarm: Alarm) {
        cancelledAlarmIds += alarm.id
    }

    override fun cancel(alarmId: Long) {
        cancelledAlarmIds += alarmId
    }
}
