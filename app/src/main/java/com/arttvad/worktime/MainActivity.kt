package com.arttvad.worktime

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arttvad.worktime.ui.calendar.CalendarViewModel
import com.arttvad.worktime.ui.calendar.WorkTimeRoot
import com.arttvad.worktime.ui.theme.WorkTimeTheme

class MainActivity : ComponentActivity() {
    private var openTodayRequestToken by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.action == ACTION_ADD_TODAY) {
            openTodayRequestToken++
        }
        publishQuickAddShortcut()

        val container = (application as WorkTimeApplication).container

        setContent {
            WorkTimeTheme {
                val viewModel: CalendarViewModel = viewModel(
                    factory = CalendarViewModel.factory(
                        workDayRepository = container.workDayRepository,
                        preferencesRepository = container.preferencesRepository,
                        profileBackupRepository = container.profileBackupRepository,
                    ),
                )
                WorkTimeRoot(
                    viewModel = viewModel,
                    openTodayRequestToken = openTodayRequestToken,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_ADD_TODAY) {
            openTodayRequestToken++
        }
    }

    private fun publishQuickAddShortcut() {
        runCatching {
            val shortcutManager = getSystemService(ShortcutManager::class.java)
            val shortcutIntent = Intent(this, MainActivity::class.java).apply {
                action = ACTION_ADD_TODAY
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val shortcut = ShortcutInfo.Builder(this, QUICK_ADD_SHORTCUT_ID)
                .setShortLabel(getString(R.string.shortcut_add_today_short))
                .setLongLabel(getString(R.string.shortcut_add_today_long))
                .setIntent(shortcutIntent)
                .build()
            shortcutManager.setDynamicShortcuts(listOf(shortcut))
        }
    }

    companion object {
        const val ACTION_ADD_TODAY = "com.arttvad.worktime.action.ADD_TODAY"
        const val QUICK_ADD_SHORTCUT_ID = "add-today"
    }
}
