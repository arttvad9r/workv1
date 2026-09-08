package com.arttvad.worktime

import android.content.Intent
import android.content.pm.ShortcutManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runEmptyComposeUiTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class QuickAddShortcutFlowTest {
    @Test
    fun dynamicShortcutIsPublishedAndOpensTodayEditor() = runEmptyComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntil(timeoutMillis = 5_000) {
                shortcutManager.dynamicShortcuts.any { shortcut ->
                    shortcut.id == MainActivity.QUICK_ADD_SHORTCUT_ID
                }
            }

            val shortcut = shortcutManager.dynamicShortcuts.single { item ->
                item.id == MainActivity.QUICK_ADD_SHORTCUT_ID
            }
            val shortcutIntent = requireNotNull(shortcut.intent)
            assertEquals(MainActivity.ACTION_ADD_TODAY, shortcutIntent.action)
            assertTrue(shortcutIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
            assertTrue(shortcutIntent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)

            context.startActivity(
                Intent(shortcutIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )

            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-worked-hours").assertExists()
                    true
                }.getOrDefault(false)
            }
        }
    }
}
