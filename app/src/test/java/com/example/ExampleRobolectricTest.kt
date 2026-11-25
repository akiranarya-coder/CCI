package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cci.core.constants.AppConstants
import com.example.cci.core.security.PasswordHasher
import com.example.cci.core.utils.ReportExporter
import com.example.cci.data.datastore.PlantSettings
import com.example.cci.data.local.entity.CciTestEntity
import com.example.cci.data.local.entity.CrusherStatusEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Evonith CCI", appName)
    }

    @Test
    fun `daily whatsapp report formats summary metrics and samples`() {
        val sample = CciTestEntity(
            id = 1,
            sampleId = "CCI-20261124-01",
            testDateTime = "2026-11-24T08:45",
            testDate = "2026-11-24",
            shift = "A",
            crusherLine = AppConstants.LINE_01,
            cokeType = "Breeze Coke",
            cokeSource = "BF Stock House Bay-1",
            operatorName = "operator",
            beforeFrac050 = 2.15,
            beforeFrac100 = 1.85,
            beforeFrac300 = 1.55,
            beforeFracPlus3 = 14.45,
            beforeTotal = 20.0,
            afterFrac050 = 4.15,
            afterFrac100 = 2.60,
            afterFrac300 = 2.25,
            afterFracPlus3 = 11.00,
            afterTotal = 20.0,
            beforeFinesPct = 27.75,
            afterFinesPct = 45.00,
            crushingIndex = 45.00,
            finesIncreasePp = 17.25,
            remarks = "Within target",
            createdAt = "2026-11-24T08:45",
            createdBy = "operator",
            updatedAt = "2026-11-24T08:45",
            updatedBy = "operator"
        )
        val status = CrusherStatusEntity(
            runningLine = AppConstants.LINE_01,
            standbyLine = AppConstants.LINE_02,
            lastChangeoverAt = "2026-11-19T08:00",
            updatedAt = "2026-11-19T08:00",
            updatedBy = "shiftincharge"
        )

        val report = ReportExporter.buildDailyWhatsAppReport(
            settings = PlantSettings(),
            reportDateLabel = "24 Nov 2026",
            shiftFilter = "ALL",
            tests = listOf(sample),
            crusherStatus = status,
            changeovers = emptyList(),
            maintenance = emptyList()
        )

        assertTrue(report.contains("DAILY COKE CRUSHING INDEX (CCI) REPORT"))
        assertTrue(report.contains("CCI-20261124-01"))
        assertTrue(report.contains("45.00%"))
        assertTrue(report.contains("Crusher Line-01"))
    }

    @Test
    fun `password hasher verifies matching and rejects wrong password`() {
        val hash = PasswordHasher.hashPassword("shift123")
        assertTrue(PasswordHasher.verifyPassword("shift123", hash))
        assertFalse(PasswordHasher.verifyPassword("wrongpass", hash))
    }
}
