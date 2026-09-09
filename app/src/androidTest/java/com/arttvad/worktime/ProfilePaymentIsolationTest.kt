package com.arttvad.worktime

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arttvad.worktime.data.local.WorkTimeDatabase
import com.arttvad.worktime.data.repository.RoomWorkProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilePaymentIsolationTest {
    @Test
    fun profilePaymentsRemainIndependent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, WorkTimeDatabase::class.java).build()
        try {
            val repository = RoomWorkProfileRepository(database.workProfileDao())
            repository.observeProfiles().first()
            val second = repository.create("Подработка")

            repository.updatePayment(1L, 1_500L, "EUR")
            repository.updatePayment(second.id, 2_500L, "USD")

            var profiles = repository.observeProfiles().first().associateBy { profile -> profile.id }
            assertEquals(1_500L, profiles.getValue(1L).hourlyRateMinor)
            assertEquals("EUR", profiles.getValue(1L).currencyCode)
            assertEquals(2_500L, profiles.getValue(second.id).hourlyRateMinor)
            assertEquals("USD", profiles.getValue(second.id).currencyCode)

            repository.updatePayment(1L, null, "EUR")
            profiles = repository.observeProfiles().first().associateBy { profile -> profile.id }
            assertNull(profiles.getValue(1L).hourlyRateMinor)
            assertEquals("EUR", profiles.getValue(1L).currencyCode)
            assertEquals(2_500L, profiles.getValue(second.id).hourlyRateMinor)
        } finally {
            database.close()
        }
    }
}
