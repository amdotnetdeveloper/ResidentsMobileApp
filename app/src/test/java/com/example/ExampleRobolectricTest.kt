package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.Resident
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Resident Directory", appName)
    }

    @Test
    fun insertAndReadResident() = runBlocking {
        val dao = db.residentDao()
        val resident = Resident(
            userId = 1L,
            buildingNumber = "202",
            block = "A",
            name = "John Connor",
            phoneNumber = "1234567890",
            familySize = 3
        )
        
        dao.insertResident(resident)

        val residentsList = dao.getAllResidents(1L).first()
        assertEquals(1, residentsList.size)
        
        val saved = residentsList[0]
        assertEquals("202", saved.buildingNumber)
        assertEquals("A", saved.block)
        assertEquals("John Connor", saved.name)
        assertEquals(3, saved.familySize)
    }

    @Test
    fun getResidentByUniqueComposite_findsDuplicate() = runBlocking {
        val dao = db.residentDao()
        val resident = Resident(
            userId = 1L,
            buildingNumber = "304",
            block = "B",
            name = "Jane Doe",
            phoneNumber = "9876543210",
            familySize = 2
        )
        dao.insertResident(resident)

        // Exact match
        val match = dao.getResidentByUniqueComposite(1L, "B", "304", "Jane Doe")
        assertTrue(match != null)
        assertEquals("Jane Doe", match?.name)

        // Case-insensitive check
        val matchCaseInsensitive = dao.getResidentByUniqueComposite(1L, "b", "304", "jane doe")
        assertTrue(matchCaseInsensitive != null)
        assertEquals("Jane Doe", matchCaseInsensitive?.name)

        // Non-matching Block/Room/Name
        val nonMatch = dao.getResidentByUniqueComposite(1L, "A", "304", "Jane Doe")
        assertTrue(nonMatch == null)
    }
}
