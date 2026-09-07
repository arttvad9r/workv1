package com.arttvad.worktime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arttvad.worktime.ui.calendar.CalendarViewModel
import com.arttvad.worktime.ui.calendar.WorkTimeRoot
import com.arttvad.worktime.ui.theme.WorkTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as WorkTimeApplication).container

        setContent {
            WorkTimeTheme {
                val viewModel: CalendarViewModel = viewModel(
                    factory = CalendarViewModel.factory(
                        workDayRepository = container.workDayRepository,
                        preferencesRepository = container.preferencesRepository,
                    ),
                )
                WorkTimeRoot(viewModel)
            }
        }
    }
}
