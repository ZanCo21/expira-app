package com.expira.app.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Saved history must survive every schema upgrade. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ExpiraDatabase::class.java)

    @Test
    fun historyFromVersion1SurvivesUpgradeToLatest() = runBlocking {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO scans (expiry_epoch_day, expiry_day, expiry_month, expiry_year, detected_date,
                    raw_ocr_text, confidence, confidence_level, was_corrected, image_path, preprocess_config,
                    preprocess_ms, ocr_ms, extract_ms, validate_ms, total_ms, created_at)
                VALUES (739840, 12, 8, 2027, '12-08-2027', 'EXP 12.08.27', 0.94, 'HIGH', 0, NULL, 'P0',
                    1, 2, 3, 4, 10, 1000)
                """.trimIndent(),
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, ExpiraDatabase::class.java, dbName).build()
        try {
            val records = ScanHistoryRepository(db.scanDao()).getAll()
            val record = records.single()
            assertEquals("12-08-2027", record.expiry.format())
            // Columns added in v3 (product name, reminder) start empty for old entries.
            assertEquals(null, record.productName)
            assertEquals(null, record.reminder)
            assertEquals(emptyList<Any>(), EvaluationRepository(db.evaluationDao()).observeLatestRun().first())
        } finally {
            db.close()
        }
    }
}
