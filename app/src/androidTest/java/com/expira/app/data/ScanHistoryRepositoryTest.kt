package com.expira.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.expira.app.domain.ScanRecord
import com.expira.app.domain.ScanTimings
import com.expira.app.extraction.CalendarDate
import com.expira.app.extraction.ConfidenceLevel
import com.expira.app.imaging.PreprocessConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanHistoryRepositoryTest {

    private lateinit var db: ExpiraDatabase
    private lateinit var repository: ScanHistoryRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, ExpiraDatabase::class.java).build()
        repository = ScanHistoryRepository(db.scanDao())
    }

    @After
    fun tearDown() = db.close()

    private fun record(day: Int, createdAt: Long, corrected: Boolean = false) = ScanRecord(
        expiry = checkNotNull(CalendarDate.of(day, 8, 2027)),
        detectedDate = CalendarDate.of(day, 8, 2027),
        rawOcrText = "EXP $day.08.27",
        confidence = 0.94,
        confidenceLevel = ConfidenceLevel.HIGH,
        wasCorrected = corrected,
        imagePath = "/data/scan.jpg",
        preprocessConfig = PreprocessConfig.P3,
        timings = ScanTimings(120, 480, 10, 2, 612),
        createdAt = createdAt,
    )

    @Test
    fun savedRecordRoundTripsUnchanged() = runBlocking {
        val original = record(12, createdAt = 1000)
        val id = repository.save(original)
        assertEquals(original.copy(id = id), repository.observeAll().first().single())
    }

    @Test
    fun recordsAreNewestFirstAndRecentIsLimited() = runBlocking {
        repository.save(record(1, createdAt = 100))
        repository.save(record(2, createdAt = 300))
        repository.save(record(3, createdAt = 200))
        assertEquals(listOf(2, 3, 1), repository.observeAll().first().map { it.expiry.day })
        assertEquals(listOf(2, 3), repository.observeRecent(2).first().map { it.expiry.day })
    }

    @Test
    fun manualEntryWithoutDetectionIsStored() = runBlocking {
        val manual = record(5, createdAt = 1).copy(
            detectedDate = null,
            confidence = null,
            confidenceLevel = null,
            wasCorrected = true,
            preprocessConfig = null,
            timings = null,
        )
        val id = repository.save(manual)
        assertEquals(manual.copy(id = id), repository.observeAll().first().single())
    }
}
