package nz.silentalarms.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import nz.silentalarms.app.data.Alarm
import nz.silentalarms.app.ui.theme.SilentAlarmsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AlarmScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listScreenShowsSavedAlarmAndRoutesActions() {
        var editedAlarmId: Long? = null
        var toggledAlarm: Pair<Long, Boolean>? = null
        composeRule.setContent {
            SilentAlarmsTheme {
                AlarmApp(
                    uiState = AlarmUiState(alarms = listOf(alarm())),
                    onAddAlarm = {},
                    onEditAlarm = { editedAlarmId = it },
                    onDismissEditor = {},
                    onEditorTimeChange = { _, _ -> },
                    onEditorLabelChange = {},
                    onEditorDayToggle = {},
                    onEditorEnabledChange = {},
                    onSaveEditor = {},
                    onToggleAlarm = { id, enabled -> toggledAlarm = id to enabled },
                    onDeleteAlarm = {},
                    onUndoDelete = {},
                    onDeleteSnackbarDismissed = {},
                )
            }
        }

        composeRule.onNodeWithText("07:30").assertIsDisplayed()
        composeRule.onNodeWithText("Morning").assertIsDisplayed()
        composeRule.onNodeWithText("Mon, Fri").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add alarm").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-toggle-42").assertIsOn().performClick()
        assertEquals(42L to false, toggledAlarm)

        composeRule.onNodeWithTag("alarm-row-42").performClick()
        assertEquals(42L, editedAlarmId)
    }

    @Test
    fun listScreenSwipeRoutesDelete() {
        var deletedAlarmId: Long? = null
        composeRule.setContent {
            SilentAlarmsTheme {
                AlarmApp(
                    uiState = AlarmUiState(alarms = listOf(alarm())),
                    onAddAlarm = {},
                    onEditAlarm = {},
                    onDismissEditor = {},
                    onEditorTimeChange = { _, _ -> },
                    onEditorLabelChange = {},
                    onEditorDayToggle = {},
                    onEditorEnabledChange = {},
                    onSaveEditor = {},
                    onToggleAlarm = { _, _ -> },
                    onDeleteAlarm = { deletedAlarmId = it },
                    onUndoDelete = {},
                    onDeleteSnackbarDismissed = {},
                )
            }
        }

        composeRule.onNodeWithTag("alarm-row-42").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertEquals(42L, deletedAlarmId)
    }

    @Test
    fun addScreenShowsEditableFieldsAndRoutesChanges() {
        var label = ""
        var toggledDay: DayOfWeek? = null
        var saved = false
        composeRule.setContent {
            SilentAlarmsTheme {
                AlarmApp(
                    uiState = AlarmUiState(editor = AlarmEditorState(hour = 8, minute = 15)),
                    onAddAlarm = {},
                    onEditAlarm = {},
                    onDismissEditor = {},
                    onEditorTimeChange = { _, _ -> },
                    onEditorLabelChange = { label = it },
                    onEditorDayToggle = { toggledDay = it },
                    onEditorEnabledChange = {},
                    onSaveEditor = { saved = true },
                    onToggleAlarm = { _, _ -> },
                    onDeleteAlarm = {},
                    onUndoDelete = {},
                    onDeleteSnackbarDismissed = {},
                )
            }
        }

        composeRule.onNodeWithText("Add alarm").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-label").performTextInput("Early")
        assertEquals("Early", label)
        composeRule.onNodeWithText("Does not repeat").assertExists()
        composeRule.onNodeWithTag("repeat-MONDAY").performScrollTo().performClick()
        assertEquals(DayOfWeek.MONDAY, toggledDay)
        composeRule.onNodeWithTag("save-alarm").performScrollTo().performClick()
        assertEquals(true, saved)
    }

    @Test
    fun editScreenShowsExistingAlarmValues() {
        composeRule.setContent {
            SilentAlarmsTheme {
                AlarmApp(
                    uiState = AlarmUiState(editor = alarm().toEditorStateForTest()),
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

        composeRule.onNodeWithText("Edit alarm").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-label").assertTextContains("Morning")
        composeRule.onNodeWithText("Mon, Fri").assertExists()
        composeRule.onNodeWithTag("save-alarm").performScrollTo().assertIsDisplayed()
    }

    private fun alarm() =
        Alarm(
            id = 42,
            label = "Morning",
            hour = 7,
            minute = 30,
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            vibrationPatternId = "default",
        )
}

private fun Alarm.toEditorStateForTest() =
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
