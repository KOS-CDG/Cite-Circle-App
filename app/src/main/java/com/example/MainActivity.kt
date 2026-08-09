package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.prefs.ThemeMode
import com.example.ui.navigation.CiteCircleApp
import com.example.ui.theme.CiteCircleTheme

/**
 * This file used to be 735 lines: the activity, the whole app shell, the NavHost, five screens
 * and six shared composables. Everything else now lives under ui/ -- see ui/navigation for the
 * shell and route table.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val application = context.applicationContext as MyApplication
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(application.repository, application.settingsStore),
            )

            val themeMode by homeViewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CiteCircleTheme(darkTheme = darkTheme) {
                CiteCircleApp(homeViewModel)
            }
        }
    }
}
