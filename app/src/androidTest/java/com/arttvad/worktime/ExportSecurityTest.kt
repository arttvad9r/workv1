package com.arttvad.worktime

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportSecurityTest {
    @Test
    fun appDoesNotRequestBroadStoragePermissions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
        )
        val requested = packageInfo.requestedPermissions.orEmpty().toSet()

        assertFalse("android.permission.READ_EXTERNAL_STORAGE" in requested)
        assertFalse("android.permission.WRITE_EXTERNAL_STORAGE" in requested)
        assertFalse("android.permission.MANAGE_EXTERNAL_STORAGE" in requested)
    }
}
