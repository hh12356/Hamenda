package io.github.hh12356.hamenda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.hh12356.hamenda.data.Store
import io.github.hh12356.hamenda.ui.SettingsScreen
import io.github.hh12356.hamenda.ui.WeekScreen
import io.github.hh12356.hamenda.ui.theme.HamendaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val store = remember { Store(context) }
    HamendaTheme(mode = store.data.settings.themeMode) {
        var showSettings by remember { mutableStateOf(false) }
        Surface(Modifier.fillMaxSize()) {
            if (showSettings) {
                SettingsScreen(store, onBack = { showSettings = false })
            } else {
                WeekScreen(store, onOpenSettings = { showSettings = true })
            }
        }
    }
}
