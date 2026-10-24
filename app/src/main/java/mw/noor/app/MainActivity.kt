package mw.noor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import mw.noor.app.ui.NoorApp
import mw.noor.app.ui.theme.NoorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoorTheme {
                NoorApp()
            }
        }
    }
}
