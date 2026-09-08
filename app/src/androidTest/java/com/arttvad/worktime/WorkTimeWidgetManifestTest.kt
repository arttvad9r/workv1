package com.arttvad.worktime

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arttvad.worktime.widget.WorkTimeWidgetReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkTimeWidgetManifestTest {
    @Test
    fun widgetReceiverIsRegisteredWithProviderMetadata() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val receiverInfo = context.packageManager.getReceiverInfo(
            ComponentName(context, WorkTimeWidgetReceiver::class.java),
            PackageManager.GET_META_DATA,
        )

        assertEquals(true, receiverInfo.exported)
        assertNotNull(receiverInfo.metaData)
        assertEquals(
            R.xml.work_time_widget_info,
            receiverInfo.metaData.getInt("android.appwidget.provider"),
        )
    }
}
