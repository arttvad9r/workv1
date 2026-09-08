package com.arttvad.worktime.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.testing.unit.assertHasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasText
import androidx.glance.testing.unit.hasTestTag
import com.arttvad.worktime.MainActivity
import org.junit.Test

class WorkTimeWidgetContentTest {
    private val snapshot = WorkTimeWidgetSnapshot(
        monthTitle = "Сентябрь 2026",
        worked = "168 ч",
        earned = "2 520 €",
        overtime = "4 ч",
    )

    @Test
    fun compactWidgetShowsPrimaryMetricsAndOpensApp() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(180.dp, 110.dp))
        provideComposable {
            GlanceTheme {
                WorkTimeWidgetContent(
                    snapshot = snapshot,
                    workedLabel = "Отработано",
                    earnedLabel = "Заработано",
                    overtimeLabel = "Переработка",
                )
            }
        }

        onNode(hasTestTag("widget-month")).assertHasText("Сентябрь 2026")
        onNode(hasTestTag("widget-worked")).assertHasText("Отработано: 168 ч")
        onNode(hasTestTag("widget-earned")).assertHasText("Заработано: 2 520 €")
        onNode(hasTestTag("widget-root")).assertHasStartActivityClickAction<MainActivity>()
    }

    @Test
    fun widerWidgetShowsOvertime() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(280.dp, 140.dp))
        provideComposable {
            GlanceTheme {
                WorkTimeWidgetContent(
                    snapshot = snapshot,
                    workedLabel = "Отработано",
                    earnedLabel = "Заработано",
                    overtimeLabel = "Переработка",
                )
            }
        }

        onNode(hasTestTag("widget-overtime")).assertHasText("Переработка: 4 ч")
    }
}
