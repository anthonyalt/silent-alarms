package nz.silentalarms.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale
import nz.silentalarms.app.service.AlarmService
import nz.silentalarms.app.ui.theme.SilentAlarmsTheme

class AlarmActivity : ComponentActivity() {
    private var alarmDetails by mutableStateOf(AlarmDetails())

    private val alarmFinishedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AlarmService.ACTION_ALARM_FINISHED) {
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        alarmDetails = intent.toAlarmDetails()

        setContent {
            SilentAlarmsTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.alarm_ringing),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                alarmDetails.hour,
                                alarmDetails.minute,
                            ),
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Text(
                            text = alarmDetails.label.ifBlank {
                                getString(R.string.unlabelled_alarm)
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                startService(
                                    AlarmService.createSnoozeIntent(
                                        this@AlarmActivity,
                                        alarmDetails.id,
                                    ),
                                )
                                finishAndRemoveTask()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.snooze_alarm))
                        }
                        Button(
                            onClick = {
                                startService(
                                    AlarmService.createDismissIntent(this@AlarmActivity),
                                )
                                finishAndRemoveTask()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.dismiss_alarm))
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        alarmDetails = intent.toAlarmDetails()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            alarmFinishedReceiver,
            IntentFilter(AlarmService.ACTION_ALARM_FINISHED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStop() {
        unregisterReceiver(alarmFinishedReceiver)
        super.onStop()
    }

    companion object {
        private const val EXTRA_ALARM_ID = "alarm_id"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_HOUR = "hour"
        private const val EXTRA_MINUTE = "minute"

        fun createIntent(
            context: Context,
            alarmId: Long,
            label: String,
            hour: Int,
            minute: Int,
        ): Intent =
            Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_HOUR, hour)
                putExtra(EXTRA_MINUTE, minute)
            }

        private fun Intent.toAlarmDetails() =
            AlarmDetails(
                id = getLongExtra(EXTRA_ALARM_ID, -1L),
                label = getStringExtra(EXTRA_LABEL).orEmpty(),
                hour = getIntExtra(EXTRA_HOUR, 0),
                minute = getIntExtra(EXTRA_MINUTE, 0),
            )
    }
}

private data class AlarmDetails(
    val id: Long = -1,
    val label: String = "",
    val hour: Int = 0,
    val minute: Int = 0,
)
