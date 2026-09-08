package com.arttvad.worktime.ui.calendar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.EventRepeat
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.domain.exporting.MonthCsvExporter
import com.arttvad.worktime.domain.exporting.MonthReportDataBuilder
import com.arttvad.worktime.domain.exporting.MonthXlsxExporter
import com.arttvad.worktime.domain.model.WorkDayType
import com.arttvad.worktime.domain.pattern.ShiftPatternDay
import com.arttvad.worktime.exporting.MonthPdfExporter
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ExpandedBreakpoint = 840.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkTimeScreen(
    uiState: CalendarUiState,
    snackbarHostState: SnackbarHostState,
    onVisibleMonthChanged: (YearMonth) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onDismissDayEditor: () -> Unit,
    onSaveDay: (LocalDate, WorkDayType, Int, Int, String, Long?) -> Unit,
    onDeleteDay: (LocalDate) -> Unit,
    onUpdatePayment: (Long?, String) -> Unit,
    onApplyPattern: (List<ShiftPatternDay>) -> Unit,
) {
    var paymentSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var patternGeneratorOpen by rememberSaveable { mutableStateOf(false) }
    var statisticsOpen by rememberSaveable { mutableStateOf(false) }
    var pendingCsv by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingXlsx by rememberSaveable { mutableStateOf<ByteArray?>(null) }
    var pendingPdf by rememberSaveable { mutableStateOf<ByteArray?>(null) }
    val initialPage = remember { pageForMonth(uiState.visibleMonth) }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { PagerMonthCount },
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val csvExportSuccess = stringResource(R.string.export_success)
    val csvExportError = stringResource(R.string.export_error)
    val xlsxExportSuccess = stringResource(R.string.export_xlsx_success)
    val xlsxExportError = stringResource(R.string.export_xlsx_error)
    val pdfExportSuccess = stringResource(R.string.export_pdf_success)
    val pdfExportError = stringResource(R.string.export_pdf_error)
    val csvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val content = pendingCsv
        pendingCsv = null
        if (uri != null && content != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)
                            ?.writer(Charsets.UTF_8)
                            ?.use { writer -> writer.write(content) }
                            ?: error("Document provider returned no output stream")
                    }
                }
                snackbarHostState.showSnackbar(
                    message = if (result.isSuccess) csvExportSuccess else csvExportError,
                )
            }
        }
    }
    val xlsxDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ),
    ) { uri ->
        val content = pendingXlsx
        pendingXlsx = null
        if (uri != null && content != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)
                            ?.use { output -> output.write(content) }
                            ?: error("Document provider returned no output stream")
                    }
                }
                snackbarHostState.showSnackbar(
                    message = if (result.isSuccess) xlsxExportSuccess else xlsxExportError,
                )
            }
        }
    }
    val pdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        val content = pendingPdf
        pendingPdf = null
        if (uri != null && content != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)
                            ?.use { output -> output.write(content) }
                            ?: error("Document provider returned no output stream")
                    }
                }
                snackbarHostState.showSnackbar(
                    message = if (result.isSuccess) pdfExportSuccess else pdfExportError,
                )
            }
        }
    }

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
                    IconButton(onClick = { patternGeneratorOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.EventRepeat,
                            contentDescription = stringResource(R.string.pattern_open),
                        )
                    }
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
                    onStatisticsRequested = { statisticsOpen = true },
                    onDismissDayEditor = onDismissDayEditor,
                    onSaveDay = onSaveDay,
                    onDeleteDay = onDeleteDay,
                )
            } else {
                CompactMonthLayout(
                    uiState = uiState,
                    pagerState = pagerState,
                    onDaySelected = onDaySelected,
                    onStatisticsRequested = { statisticsOpen = true },
                )

                uiState.selectedDate?.let { selectedDate ->
                    DayEditorSheet(
                        date = selectedDate,
                        entry = uiState.entries[selectedDate],
                        currencyCode = uiState.preferences.currencyCode,
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

    if (patternGeneratorOpen) {
        PatternGeneratorSheet(
            visibleMonth = uiState.visibleMonth,
            onDismiss = { patternGeneratorOpen = false },
            onApply = onApplyPattern,
        )
    }

    if (statisticsOpen) {
        MonthStatisticsSheet(
            month = uiState.visibleMonth,
            statistics = uiState.detailedStatistics,
            yearStatistics = uiState.detailedYearStatistics,
            preferences = uiState.preferences,
            onDismiss = { statisticsOpen = false },
            onExportCsv = {
                pendingCsv = MonthCsvExporter.export(
                    month = uiState.visibleMonth,
                    days = uiState.entries.values.toList(),
                    hourlyRateMinor = uiState.preferences.hourlyRateMinor,
                    currencyCode = uiState.preferences.currencyCode,
                )
                csvDocumentLauncher.launch("worktime-${uiState.visibleMonth}.csv")
            },
            onExportXlsx = {
                val month = uiState.visibleMonth
                val days = uiState.entries.values.toList()
                val hourlyRateMinor = uiState.preferences.hourlyRateMinor
                val currencyCode = uiState.preferences.currencyCode
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            val report = MonthReportDataBuilder.build(
                                month = month,
                                days = days,
                                hourlyRateMinor = hourlyRateMinor,
                                currencyCode = currencyCode,
                            )
                            ByteArrayOutputStream().use { output ->
                                MonthXlsxExporter.write(output, report)
                                output.toByteArray()
                            }
                        }
                    }
                    result.fold(
                        onSuccess = { bytes ->
                            pendingXlsx = bytes
                            xlsxDocumentLauncher.launch("worktime-$month.xlsx")
                        },
                        onFailure = {
                            snackbarHostState.showSnackbar(xlsxExportError)
                        },
                    )
                }
            },
            onExportPdf = {
                val month = uiState.visibleMonth
                val days = uiState.entries.values.toList()
                val hourlyRateMinor = uiState.preferences.hourlyRateMinor
                val currencyCode = uiState.preferences.currencyCode
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            val report = MonthReportDataBuilder.build(
                                month = month,
                                days = days,
                                hourlyRateMinor = hourlyRateMinor,
                                currencyCode = currencyCode,
                            )
                            ByteArrayOutputStream().use { output ->
                                MonthPdfExporter.write(output, report)
                                output.toByteArray()
                            }
                        }
                    }
                    result.fold(
                        onSuccess = { bytes ->
                            pendingPdf = bytes
                            pdfDocumentLauncher.launch("worktime-$month.pdf")
                        },
                        onFailure = {
                            snackbarHostState.showSnackbar(pdfExportError)
                        },
                    )
                }
            },
        )
    }
}

@Composable
private fun CompactMonthLayout(
    uiState: CalendarUiState,
    pagerState: PagerState,
    onDaySelected: (LocalDate) -> Unit,
    onStatisticsRequested: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        MonthContent(
            uiState = uiState,
            pagerState = pagerState,
            onDaySelected = onDaySelected,
            onStatisticsRequested = onStatisticsRequested,
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
    onStatisticsRequested: () -> Unit,
    onDismissDayEditor: () -> Unit,
    onSaveDay: (LocalDate, WorkDayType, Int, Int, String, Long?) -> Unit,
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
            onStatisticsRequested = onStatisticsRequested,
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
                    currencyCode = uiState.preferences.currencyCode,
                    onDismiss = onDismissDayEditor,
                    onSave = onSaveDay,
                    onDelete = onDeleteDay,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
