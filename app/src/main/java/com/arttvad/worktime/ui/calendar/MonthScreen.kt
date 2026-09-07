package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.distinctUntilChanged

private val ExpandedBreakpoint = 840.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkTimeScreen(
    uiState: CalendarUiState,
    snackbarHostState: SnackbarHostState,
    onVisibleMonthChanged: (YearMonth) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onDismissDayEditor: () -> Unit,
    onSaveDay: (LocalDate, Int, Int, String) -> Unit,
    onDeleteDay: (LocalDate) -> Unit,
    onUpdatePayment: (Long?, String) -> Unit,
) {
    var paymentSettingsOpen by rememberSaveable { mutableStateOf(false) }
    val initialPage = remember { pageForMonth(uiState.visibleMonth) }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { PagerMonthCount },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page -> onVisibleMonthChanged(monthForPage(page)) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { paymentSettingsOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            if (maxWidth >= ExpandedBreakpoint) {
                ExpandedMonthLayout(
                    uiState = uiState,
                    pagerState = pagerState,
                    onDaySelected = onDaySelected,
                    onDismissDayEditor = onDismissDayEditor,
                    onSaveDay = onSaveDay,
                    onDeleteDay = onDeleteDay,
                )
            } else {
                CompactMonthLayout(
                    uiState = uiState,
                    pagerState = pagerState,
                    onDaySelected = onDaySelected,
                )

                uiState.selectedDate?.let { selectedDate ->
                    DayEditorSheet(
                        date = selectedDate,
                        entry = uiState.entries[selectedDate],
                        onDismiss = onDismissDayEditor,
                        onSave = onSaveDay,
                        onDelete = onDeleteDay,
                    )
                }
            }
        }
    }

    if (paymentSettingsOpen) {
        PaymentSettingsSheet(
            preferences = uiState.preferences,
            onDismiss = { paymentSettingsOpen = false },
            onSave = { rate, currency ->
                onUpdatePayment(rate, currency)
                paymentSettingsOpen = false
            },
        )
    }
}

@Composable
private fun CompactMonthLayout(
    uiState: CalendarUiState,
    pagerState: PagerState,
    onDaySelected: (LocalDate) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        MonthContent(
            uiState = uiState,
            pagerState = pagerState,
            onDaySelected = onDaySelected,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
        )
    }
}

@Composable
private fun ExpandedMonthLayout(
    uiState: CalendarUiState,
    pagerState: PagerState,
    onDaySelected: (LocalDate) -> Unit,
    onDismissDayEditor: () -> Unit,
    onSaveDay: (LocalDate, Int, Int, String) -> Unit,
    onDeleteDay: (LocalDate) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        MonthContent(
            uiState = uiState,
            pagerState = pagerState,
            onDaySelected = onDaySelected,
            modifier = Modifier
                .weight(1.55f)
                .widthIn(max = 760.dp),
        )

        VerticalDivider(modifier = Modifier.fillMaxHeight())

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .widthIn(max = 520.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 1.dp,
        ) {
            val selectedDate = uiState.selectedDate
            if (selectedDate == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.tap_day_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                DayEditorContent(
                    date = selectedDate,
                    entry = uiState.entries[selectedDate],
                    onDismiss = onDismissDayEditor,
                    onSave = onSaveDay,
                    onDelete = onDeleteDay,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
