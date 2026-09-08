package com.arttvad.worktime

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arttvad.worktime.reminder.WorkReminderReceiver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderManifestTest {
    @Test
    fun reminderUsesNotificationAndBootPermissionsWithoutExactAlarmAccess() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val permissions = packageInfo.requestedPermissions?.toSet().orEmpty()

        assertTrue(Manifest.permission.POST_NOTIFICATIONS in permissions)
        assertTrue(Manifest.permission.RECEIVE_BOOT_COMPLETED in permissions)
        assertFalse("android.permission.SCHEDULE_EXACT_ALARM" in permissions)
        assertFalse("android.permission.USE_EXACT_ALARM" in permissions)
    }

    @Test
    fun reminderReceiverIsPrivate() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val receiverInfo = context.packageManager.getReceiverInfo(
            ComponentName(context, WorkReminderReceiver::class.java),
            0,
        )

        assertFalse(receiverInfo.exported)
        assertTrue(receiverInfo.enabled)
    }
}
