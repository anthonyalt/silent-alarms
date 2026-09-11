package nz.silentalarms.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nz.silentalarms.app.ui.AlarmApp
import nz.silentalarms.app.ui.AlarmViewModel
import nz.silentalarms.app.ui.theme.SilentAlarmsTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AlarmViewModel by viewModels {
        AlarmViewModel.Factory((application as SilentAlarmsApplication).database.alarmDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle()
            SilentAlarmsTheme {
                AlarmApp(
                    uiState = uiState.value,
                    onAddAlarm = viewModel::startAddingAlarm,
                    onEditAlarm = viewModel::startEditingAlarm,
                    onDismissEditor = viewModel::dismissEditor,
                    onEditorTimeChange = viewModel::updateEditorTime,
                    onEditorLabelChange = viewModel::updateEditorLabel,
                    onEditorDayToggle = viewModel::toggleEditorDay,
                    onEditorEnabledChange = viewModel::updateEditorEnabled,
                    onSaveEditor = viewModel::saveEditor,
                    onToggleAlarm = viewModel::setAlarmEnabled,
                    onDeleteAlarm = viewModel::deleteAlarm,
                    onUndoDelete = viewModel::undoDelete,
                    onDeleteSnackbarDismissed = viewModel::clearDeletedAlarm,
                )
            }
        }
    }
}
