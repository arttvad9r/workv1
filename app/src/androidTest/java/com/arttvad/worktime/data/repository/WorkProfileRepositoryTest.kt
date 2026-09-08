package com.arttvad.worktime.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arttvad.worktime.data.local.DEFAULT_PROFILE_ID
import com.arttvad.worktime.data.local.DEFAULT_PROFILE_NAME
import com.arttvad.worktime.data.local.WorkTimeDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkProfileRepositoryTest {
    @Test
    fun repositorySeedsDefaultProfileAndCreatesStableAdditionalProfile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(
            context,
            WorkTimeDatabase::class.java,
        ).build()

        try {
            val repository = RoomWorkProfileRepository(database.workProfileDao())

            val initial = runBlocking { repository.observeProfiles().first() }
            assertEquals(1, initial.size)
            assertEquals(DEFAULT_PROFILE_ID, initial.single().id)
            assertEquals(DEFAULT_PROFILE_NAME, initial.single().name)

            val created = runBlocking { repository.create("  Подработка  ") }
            assertEquals(2L, created.id)
            assertEquals("Подработка", created.name)

            val profiles = runBlocking { repository.observeProfiles().first() }
            assertEquals(listOf(DEFAULT_PROFILE_ID, 2L), profiles.map { it.id })
            assertEquals(listOf(DEFAULT_PROFILE_NAME, "Подработка"), profiles.map { it.name })
        } finally {
            database.close()
        }
    }
}
