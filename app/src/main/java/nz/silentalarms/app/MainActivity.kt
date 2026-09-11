package nz.silentalarms.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import nz.silentalarms.app.ui.AlarmListScreen
import nz.silentalarms.app.ui.theme.SilentAlarmsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SilentAlarmsTheme {
                AlarmListScreen()
            }
        }
    }
}
