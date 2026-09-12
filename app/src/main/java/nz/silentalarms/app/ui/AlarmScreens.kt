package nz.silentalarms.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.util.Locale
import nz.silentalarms.app.R
import nz.silentalarms.app.data.Alarm
import nz.silentalarms.app.ui.theme.SilentAlarmsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmApp(
    uiState: AlarmUiState,
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onDismissEditor: () -> Unit,
    onEditorTimeChange: (hour: Int, minute: Int) -> Unit,
    onEditorLabelChange: (String) -> Unit,
    onEditorDayToggle: (DayOfWeek) -> Unit,
    onEditorEnabledChange: (Boolean) -> Unit,
    onSaveEditor: () -> Unit,
    onToggleAlarm: (Long, Boolean) -> Unit,
    onDeleteAlarm: (Long) -> Unit,
    onUndoDelete: () -> Unit,
    onDeleteSnackbarDismissed: () -> Unit,
) {
    if (uiState.editor != null) {
        AlarmEditorScreen(
            editor = uiState.editor,
            onDismiss = onDismissEditor,
            onTimeChange = onEditorTimeChange,
            onLabelChange = onEditorLabelChange,
            onDayToggle = onEditorDayToggle,
            onEnabledChange = onEditorEnabledChange,
            onSave = onSaveEditor,
        )
    } else {
        AlarmListScreen(
            uiState = uiState,
            onAddAlarm = onAddAlarm,
            onEditAlarm = onEditAlarm,
            onToggleAlarm = onToggleAlarm,
            onDeleteAlarm = onDeleteAlarm,
            onUndoDelete = onUndoDelete,
            onDeleteSnackbarDismissed = onDeleteSnackbarDismissed,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmListScreen(
    uiState: AlarmUiState,
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onToggleAlarm: (Long, Boolean) -> Unit,
    onDeleteAlarm: (Long) -> Unit,
    onUndoDelete: () -> Unit,
    onDeleteSnackbarDismissed: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = stringResource(R.string.alarm_deleted)
    val undoLabel = stringResource(R.string.undo_delete)

    LaunchedEffect(uiState.deletedAlarm?.id) {
        if (uiState.deletedAlarm != null) {
            val result = snackbarHostState.showSnackbar(
                message = deletedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onUndoDelete()
            } else {
                onDeleteSnackbarDismissed()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarm) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.add_alarm),
                )
            }
        },
    ) { contentPadding ->
        AlarmListContent(
            alarms = uiState.alarms,
            contentPadding = contentPadding,
            onAlarmClick = onEditAlarm,
            onToggleAlarm = onToggleAlarm,
            onDeleteAlarm = onDeleteAlarm,
        )
    }
}

@Composable
private fun AlarmListContent(
    alarms: List<Alarm>,
    contentPadding: PaddingValues,
    onAlarmClick: (Long) -> Unit,
    onToggleAlarm: (Long, Boolean) -> Unit,
    onDeleteAlarm: (Long) -> Unit,
) {
    if (alarms.isEmpty()) {
        EmptyAlarmList(contentPadding)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = alarms,
                key = { it.id },
            ) { alarm ->
                SwipeToDeleteAlarmRow(
                    alarm = alarm,
                    onAlarmClick = onAlarmClick,
                    onToggleAlarm = onToggleAlarm,
                    onDeleteAlarm = onDeleteAlarm,
                )
            }
        }
    }
}

@Composable
private fun EmptyAlarmList(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_alarm),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.no_alarms),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.empty_alarm_list_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteAlarmRow(
    alarm: Alarm,
    onAlarmClick: (Long) -> Unit,
    onToggleAlarm: (Long, Boolean) -> Unit,
    onDeleteAlarm: (Long) -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = stringResource(R.string.delete_alarm),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        enableDismissFromStartToEnd = false,
        onDismiss = { direction ->
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                onDeleteAlarm(alarm.id)
            }
        },
    ) {
        AlarmRow(
            alarm = alarm,
            onClick = { onAlarmClick(alarm.id) },
            onToggle = { onToggleAlarm(alarm.id, it) },
        )
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = formatAlarmTime(alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.displaySmall,
                )
                val label = alarm.label.ifBlank { stringResource(R.string.unlabelled_alarm) }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = repeatSummary(alarm.daysOfWeek),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggle,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AlarmEditorScreen(
    editor: AlarmEditorState,
    onDismiss: () -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    onLabelChange: (String) -> Unit,
    onDayToggle: (DayOfWeek) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = editor.hour,
        initialMinute = editor.minute,
        is24Hour = true,
    )

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        onTimeChange(timePickerState.hour, timePickerState.minute)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editor.id == 0L) {
                            stringResource(R.string.add_alarm)
                        } else {
                            stringResource(R.string.edit_alarm)
                        },
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                OutlinedTextField(
                    value = editor.label,
                    onValueChange = onLabelChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.alarm_label)) },
                    placeholder = { Text(stringResource(R.string.alarm_label_placeholder)) },
                    singleLine = true,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.repeat_days),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DayOfWeek.values().forEach { day ->
                            FilterChip(
                                selected = day in editor.daysOfWeek,
                                onClick = { onDayToggle(day) },
                                label = { Text(dayShortLabel(day)) },
                            )
                        }
                    }
                    Text(
                        text = repeatSummary(editor.daysOfWeek),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.alarm_enabled))
                    Switch(
                        checked = editor.isEnabled,
                        onCheckedChange = onEnabledChange,
                    )
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.save_alarm))
                }
            }
        }
    }
}

@Composable
private fun repeatSummary(days: Set<DayOfWeek>): String {
    if (days.isEmpty()) {
        return stringResource(R.string.repeat_once)
    }
    if (days.size == DayOfWeek.values().size) {
        return stringResource(R.string.repeat_every_day)
    }
    val labels = mutableListOf<String>()
    for (day in DayOfWeek.values()) {
        if (day in days) {
            labels += dayShortLabel(day)
        }
    }
    return labels.joinToString(separator = ", ")
}

@Composable
private fun dayShortLabel(day: DayOfWeek): String =
    when (day) {
        DayOfWeek.MONDAY -> stringResource(R.string.monday_short)
        DayOfWeek.TUESDAY -> stringResource(R.string.tuesday_short)
        DayOfWeek.WEDNESDAY -> stringResource(R.string.wednesday_short)
        DayOfWeek.THURSDAY -> stringResource(R.string.thursday_short)
        DayOfWeek.FRIDAY -> stringResource(R.string.friday_short)
        DayOfWeek.SATURDAY -> stringResource(R.string.saturday_short)
        DayOfWeek.SUNDAY -> stringResource(R.string.sunday_short)
    }

private fun formatAlarmTime(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

@Preview(showBackground = true)
@Composable
private fun EmptyAlarmListPreview() {
    SilentAlarmsTheme {
        AlarmApp(
            uiState = AlarmUiState(),
            onAddAlarm = {},
            onEditAlarm = {},
            onDismissEditor = {},
            onEditorTimeChange = { _, _ -> },
            onEditorLabelChange = {},
            onEditorDayToggle = {},
            onEditorEnabledChange = {},
            onSaveEditor = {},
            onToggleAlarm = { _, _ -> },
            onDeleteAlarm = {},
            onUndoDelete = {},
            onDeleteSnackbarDismissed = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmListPreview() {
    SilentAlarmsTheme {
        AlarmApp(
            uiState = AlarmUiState(
                alarms = listOf(
                    Alarm(
                        id = 1,
                        label = "Morning",
                        hour = 7,
                        minute = 30,
                        daysOfWeek = setOf(
                            DayOfWeek.MONDAY,
                            DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY,
                        ),
                        vibrationPatternId = "default",
                    ),
                    Alarm(
                        id = 2,
                        label = "",
                        hour = 9,
                        minute = 0,
                        vibrationPatternId = "default",
                        isEnabled = false,
                    ),
                ),
            ),
            onAddAlarm = {},
            onEditAlarm = {},
            onDismissEditor = {},
            onEditorTimeChange = { _, _ -> },
            onEditorLabelChange = {},
            onEditorDayToggle = {},
            onEditorEnabledChange = {},
            onSaveEditor = {},
            onToggleAlarm = { _, _ -> },
            onDeleteAlarm = {},
            onUndoDelete = {},
            onDeleteSnackbarDismissed = {},
        )
    }
}
