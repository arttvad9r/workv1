package com.arttvad.worktime.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.arttvad.worktime.MainActivity
import com.arttvad.worktime.R
import com.arttvad.worktime.WorkTimeApplication
import com.arttvad.worktime.data.preferences.WorkPreferences
import java.time.YearMonth
import kotlinx.coroutines.flow.first

class WorkTimeWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val application = context.applicationContext as WorkTimeApplication
        val month = YearMonth.now()
        val days = runCatching {
            application.container.workDayRepository.observeMonth(month).first()
        }.getOrDefault(emptyList())
        val preferences = runCatching {
            application.container.preferencesRepository.preferences.first()
        }.getOrDefault(WorkPreferences())
        val snapshot = WorkTimeWidgetSnapshotFactory.create(
            month = month,
            days = days,
            hourlyRateMinor = preferences.hourlyRateMinor,
            currencyCode = preferences.currencyCode,
        )

        provideContent {
            GlanceTheme {
                WorkTimeWidgetContent(
                    snapshot = snapshot,
                    workedLabel = context.getString(R.string.worked),
                    earnedLabel = context.getString(R.string.earned),
                    overtimeLabel = context.getString(R.string.overtime),
                )
            }
        }
    }
}

class WorkTimeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WorkTimeWidget()
}

@Composable
internal fun WorkTimeWidgetContent(
    snapshot: WorkTimeWidgetSnapshot,
    workedLabel: String,
    earnedLabel: String,
    overtimeLabel: String,
) {
    val size = LocalSize.current
    Scaffold(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>())
            .semantics { testTag = "widget-root" },
        backgroundColor = GlanceTheme.colors.widgetBackground,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = snapshot.monthTitle,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                modifier = GlanceModifier.semantics { testTag = "widget-month" },
            )
            Spacer(GlanceModifier.height(6.dp))
            MetricLine(
                text = "$workedLabel: ${snapshot.worked}",
                tag = "widget-worked",
            )
            MetricLine(
                text = "$earnedLabel: ${snapshot.earned}",
                tag = "widget-earned",
            )
            if (size.width >= 240.dp || size.height >= 140.dp) {
                MetricLine(
                    text = "$overtimeLabel: ${snapshot.overtime}",
                    tag = "widget-overtime",
                )
            }
        }
    }
}

@Composable
private fun MetricLine(text: String, tag: String) {
    Text(
        text = text,
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 14.sp,
        ),
        maxLines = 1,
        modifier = GlanceModifier
            .padding(vertical = 2.dp)
            .semantics { testTag = tag },
    )
}
