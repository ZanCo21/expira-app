package com.expira.app.data

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.expira.app.ExpiraApplication
import com.expira.app.domain.DeleteScanUseCase
import com.expira.app.domain.ReminderChoice
import com.expira.app.domain.ReminderInfo
import com.expira.app.domain.ReminderOption
import com.expira.app.domain.ReminderStatus
import com.expira.app.domain.ScanRecord
import com.expira.app.domain.ScheduleReminderUseCase
import com.expira.app.domain.SystemTodayProvider
import com.expira.app.extraction.CalendarDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: ExpiraDatabase
    private lateinit var repository: ScanHistoryRepository
    private lateinit var scheduler: WorkManagerReminderScheduler
    private val today = SystemTodayProvider.today()

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        db = Room.inMemoryDatabaseBuilder(context, ExpiraDatabase::class.java).build()
        repository = ScanHistoryRepository(db.scanDao())
        scheduler = WorkManagerReminderScheduler(context)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun saveEntry(expiry: CalendarDate, name: String? = "Susu UHT Cokelat"): Long = repository.save(
        ScanRecord(
            expiry = expiry,
            detectedDate = expiry,
            rawOcrText = "EXP",
            confidence = 0.9,
            confidenceLevel = null,
            wasCorrected = false,
            imagePath = null,
            preprocessConfig = null,
            timings = null,
            createdAt = 1,
            productName = name,
        ),
    )

    private fun useCase(canNotify: Boolean) = ScheduleReminderUseCase(repository, scheduler, { canNotify })

    private fun workState(id: Long): WorkInfo.State? =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(WorkManagerReminderScheduler.workName(id)).get()
            .firstOrNull()?.state

    @Test
    fun reminderIsScheduledAndStoredWithEntry() = runBlocking {
        val expiry = today.plusDays(30)
        val id = saveEntry(expiry)
        val info = useCase(canNotify = true)(id, expiry, ReminderChoice(enabled = true, option = ReminderOption.H7))
        assertEquals(ReminderInfo(ReminderOption.H7, today.plusDays(23), ReminderStatus.SCHEDULED), info)
        assertEquals(info, repository.getById(id)?.reminder)
        assertEquals(WorkInfo.State.ENQUEUED, workState(id))
    }

    @Test
    fun reminderDayInThePastIsNotScheduled() = runBlocking {
        val expiry = today.plusDays(2)
        val id = saveEntry(expiry)
        val info = useCase(canNotify = true)(id, expiry, ReminderChoice(enabled = true, option = ReminderOption.H7))
        assertEquals(ReminderStatus.TOO_LATE, info?.status)
        assertNull(workState(id))
    }

    @Test
    fun deniedPermissionIsRecordedAndNothingIsScheduled() = runBlocking {
        val expiry = today.plusDays(30)
        val id = saveEntry(expiry)
        val info = useCase(canNotify = false)(id, expiry, ReminderChoice(enabled = true))
        assertEquals(ReminderStatus.PERMISSION_DENIED, info?.status)
        assertNull(workState(id))
    }

    @Test
    fun deletingTheEntryCancelsItsReminder() = runBlocking {
        val expiry = today.plusDays(30)
        val id = saveEntry(expiry)
        useCase(canNotify = true)(id, expiry, ReminderChoice(enabled = true))
        DeleteScanUseCase(repository, scheduler)(id)
        assertNull(repository.getById(id))
        assertEquals(WorkInfo.State.CANCELLED, workState(id))
    }

    @Test
    fun disablingTheReminderClearsIt() = runBlocking {
        val expiry = today.plusDays(30)
        val id = saveEntry(expiry)
        useCase(canNotify = true)(id, expiry, ReminderChoice(enabled = true))
        useCase(canNotify = true)(id, expiry, ReminderChoice(enabled = false))
        assertNull(repository.getById(id)?.reminder)
        assertEquals(WorkInfo.State.CANCELLED, workState(id))
    }

    @Test
    fun workerPostsNotificationWithNameDateAndDaysLeft() = runBlocking {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        // The worker reads the app's real repository, so use it here.
        val appRepository = (context.applicationContext as ExpiraApplication).container.historyRepository
        val expiry = today.plusDays(7)
        val id = appRepository.save(
            ScanRecord(
                expiry = expiry, detectedDate = expiry, rawOcrText = "", confidence = null, confidenceLevel = null,
                wasCorrected = false, imagePath = null, preprocessConfig = null, timings = null, createdAt = 1,
                productName = "Biskuit Gandum",
                reminder = ReminderInfo(ReminderOption.H7, today, ReminderStatus.SCHEDULED),
            ),
        )
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context.applicationContext)
            .setInputData(workDataOf(ReminderWorker.KEY_SCAN_ID to id))
            .build()
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertEquals(ReminderStatus.SENT, appRepository.getById(id)?.reminder?.status)

        val manager = context.getSystemService(NotificationManager::class.java)
        val posted = manager.activeNotifications.firstOrNull { it.id == id.toInt() }?.notification
        val text = posted?.extras?.getCharSequence("android.text")?.toString().orEmpty()
        assertEquals("Biskuit Gandum", posted?.extras?.getCharSequence("android.title")?.toString())
        assertTrue(text, text.contains("sisa 7 hari"))
        appRepository.delete(id)
    }

    @Test
    fun workerForDeletedEntryDoesNothing() = runBlocking {
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context.applicationContext)
            .setInputData(workDataOf(ReminderWorker.KEY_SCAN_ID to 987_654L))
            .build()
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }
}
