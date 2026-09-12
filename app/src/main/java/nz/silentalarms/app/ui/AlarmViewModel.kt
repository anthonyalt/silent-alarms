package nz.silentalarms.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nz.silentalarms.app.data.Alarm
import nz.silentalarms.app.data.AlarmDao
import nz.silentalarms.app.scheduler.AlarmScheduling

class AlarmViewModel(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduling,
) : ViewModel() {
    private val editorState = MutableStateFlow<AlarmEditorState?>(null)
    private val deletedAlarm = MutableStateFlow<Alarm?>(null)

    val uiState: StateFlow<AlarmUiState> =
        combine(alarmDao.observeAll(), editorState, deletedAlarm) { alarms, editor, deleted ->
            AlarmUiState(
                alarms = alarms,
                editor = editor,
                deletedAlarm = deleted,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AlarmUiState(),
        )

    fun startAddingAlarm() {
        val now = LocalTime.now()
        editorState.value = AlarmEditorState(hour = now.hour, minute = now.minute)
    }

    fun startEditingAlarm(id: Long) {
        viewModelScope.launch {
            alarmDao.getById(id)?.let { alarm ->
                editorState.value = alarm.toEditorState()
            }
        }
    }

    fun dismissEditor() {
        editorState.value = null
    }

    fun updateEditorTime(hour: Int, minute: Int) {
        editorState.update { editor ->
            editor?.copy(hour = hour.coerceIn(0, 23), minute = minute.coerceIn(0, 59))
        }
    }

    fun updateEditorLabel(label: String) {
        editorState.update { editor -> editor?.copy(label = label) }
    }

    fun toggleEditorDay(day: DayOfWeek) {
        editorState.update { editor ->
            editor?.let {
                val days = it.daysOfWeek.toMutableSet()
                if (!days.add(day)) {
                    days.remove(day)
                }
                it.copy(daysOfWeek = days.toSet())
            }
        }
    }

    fun updateEditorEnabled(isEnabled: Boolean) {
        editorState.update { editor -> editor?.copy(isEnabled = isEnabled) }
    }

    fun saveEditor() {
        val editor = editorState.value ?: return
        editorState.value = null
        viewModelScope.launch {
            val alarmToSave = editor.toAlarm()
            val id = alarmDao.upsert(alarmToSave)
            val savedAlarm = alarmToSave.copy(id = id)
            if (savedAlarm.isEnabled) {
                alarmScheduler.schedule(savedAlarm)
            } else {
                alarmScheduler.cancel(savedAlarm)
            }
        }
    }

    fun setAlarmEnabled(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            alarmDao.setEnabled(id, isEnabled)
            val alarm = alarmDao.getById(id)
            if (alarm != null) {
                if (isEnabled) {
                    alarmScheduler.schedule(alarm)
                } else {
                    alarmScheduler.cancel(alarm)
                }
            }
        }
    }

    fun deleteAlarm(id: Long) {
        viewModelScope.launch {
            alarmDao.getById(id)?.let { alarm ->
                alarmDao.delete(alarm)
                alarmScheduler.cancel(alarm)
                deletedAlarm.value = alarm
            }
        }
    }

    fun undoDelete() {
        val alarm = deletedAlarm.value ?: return
        deletedAlarm.value = null
        viewModelScope.launch {
            alarmDao.upsert(alarm)
            if (alarm.isEnabled) {
                alarmScheduler.schedule(alarm)
            }
        }
    }

    fun clearDeletedAlarm() {
        deletedAlarm.value = null
    }

    class Factory(
        private val alarmDao: AlarmDao,
        private val alarmScheduler: AlarmScheduling,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
                return AlarmViewModel(alarmDao, alarmScheduler) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

data class AlarmUiState(
    val alarms: List<Alarm> = emptyList(),
    val editor: AlarmEditorState? = null,
    val deletedAlarm: Alarm? = null,
)

data class AlarmEditorState(
    val id: Long = 0,
    val label: String = "",
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val vibrationPatternId: String = "default",
    val soundUri: String? = null,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

private fun Alarm.toEditorState() =
    AlarmEditorState(
        id = id,
        label = label,
        hour = hour,
        minute = minute,
        daysOfWeek = daysOfWeek,
        vibrationPatternId = vibrationPatternId,
        soundUri = soundUri,
        isEnabled = isEnabled,
        createdAt = createdAt,
    )

private fun AlarmEditorState.toAlarm() =
    Alarm(
        id = id,
        label = label.trim(),
        hour = hour,
        minute = minute,
        daysOfWeek = daysOfWeek,
        vibrationPatternId = vibrationPatternId,
        soundUri = soundUri,
        isEnabled = isEnabled,
        createdAt = createdAt,
    )
