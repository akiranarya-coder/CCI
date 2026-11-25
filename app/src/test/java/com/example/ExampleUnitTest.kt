package com.example

import com.example.cci.core.constants.AppConstants
import com.example.cci.core.security.PasswordHasher
import com.example.cci.core.utils.DateTimeUtils
import com.example.cci.core.utils.ReportExporter
import com.example.cci.data.datastore.PlantSettings
import com.example.cci.data.local.entity.CciTestEntity
import com.example.cci.data.local.entity.CrusherStatusEntity
import com.example.cci.data.local.entity.DailyReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun dailyWhatsAppReport_formatsSummaryMetricsAndSamples() {
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
    fun dailyReportSummaryText_formatsCompactWhatsAppSummary() {
        val sample = CciTestEntity(
            id = 2,
            sampleId = "CCI-20261124-02",
            testDateTime = "2026-11-24T15:30",
            testDate = "2026-11-24",
            shift = "B",
            crusherLine = AppConstants.LINE_01,
            cokeType = "Breeze Coke",
            cokeSource = "BF Stock House Bay-2",
            operatorName = "operator",
            beforeFrac050 = 2.25,
            beforeFrac100 = 1.95,
            beforeFrac300 = 1.50,
            beforeFracPlus3 = 14.30,
            beforeTotal = 20.0,
            afterFrac050 = 3.95,
            afterFrac100 = 2.45,
            afterFrac300 = 2.15,
            afterFracPlus3 = 11.45,
            afterTotal = 20.0,
            beforeFinesPct = 28.50,
            afterFinesPct = 42.75,
            crushingIndex = 42.75,
            finesIncreasePp = 14.25,
            remarks = "Steady feed",
            createdAt = "2026-11-24T15:30",
            createdBy = "operator",
            updatedAt = "2026-11-24T15:30",
            updatedBy = "operator"
        )
        val status = CrusherStatusEntity(
            runningLine = AppConstants.LINE_01,
            standbyLine = AppConstants.LINE_02,
            lastChangeoverAt = "2026-11-19T08:00",
            updatedAt = "2026-11-19T08:00",
            updatedBy = "shiftincharge"
        )

        val summary = ReportExporter.generateDailyReportSummaryText(
            settings = PlantSettings(),
            reportDateLabel = "24 Nov 2026",
            tests = listOf(sample),
            crusherStatus = status,
            changeovers = emptyList(),
            maintenance = emptyList()
        )

        assertTrue(summary.contains("DAILY CCI SUMMARY"))
        assertTrue(summary.contains("42.75%"))
        assertTrue(summary.contains("S-B:"))
        assertTrue(summary.contains("Crusher Line-01"))
    }

    @Test
    fun savedDailyReport_formatsCrusherParametersAndSieveResults() {
        val saved = DailyReport(
            id = 10,
            reportDate = "2026-11-24",
            shiftScope = "ALL",
            runningLine = AppConstants.LINE_01,
            standbyLine = AppConstants.LINE_02,
            rollerGapMm = 2.9,
            feedRateTph = 44.5,
            rollerPressureBar = 148.0,
            cokeMoisturePct = 5.1,
            sampleCount = 3,
            avgBeforeFrac050 = 2.20,
            avgBeforeFrac100 = 1.90,
            avgBeforeFrac300 = 1.55,
            avgBeforeFracPlus3 = 14.35,
            avgAfterFrac050 = 4.10,
            avgAfterFrac100 = 2.55,
            avgAfterFrac300 = 2.15,
            avgAfterFracPlus3 = 11.20,
            avgBeforeFinesPct = 28.25,
            avgAfterFinesPct = 44.00,
            avgCrushingIndex = 44.00,
            minCrushingIndex = 42.75,
            maxCrushingIndex = 45.00,
            avgFinesIncreasePp = 15.75,
            targetCompliancePct = 100.0,
            changeoverCount = 1,
            maintenanceCount = 1,
            remarks = "Smooth shift handover",
            summaryText = "",
            createdAt = "2026-11-24T22:00",
            createdBy = "shiftincharge"
        )

        val formatted = ReportExporter.formatSavedDailyReportForWhatsApp(PlantSettings(), saved)
        assertTrue(formatted.contains("CRUSHER PARAMETERS"))
        assertTrue(formatted.contains("2.9 mm"))
        assertTrue(formatted.contains("44.5 TPH"))
        assertTrue(formatted.contains("148.0 bar"))
        assertTrue(formatted.contains("SIEVE ANALYSIS RESULTS"))
        assertTrue(formatted.contains("44.00%"))
        assertTrue(formatted.contains("-0.5mm=4.10"))
    }

    @Test
    fun dateTimeUtils_datePickerUtcRoundTripAndValidation() {
        val iso = "2026-11-24"
        val utcMillis = DateTimeUtils.isoDateToUtcMillis(iso)
        assertNotNull(utcMillis)
        assertEquals(iso, DateTimeUtils.utcMillisToIsoDate(utcMillis!!))
        assertTrue(DateTimeUtils.isValidIsoDate("2026-11-24"))
        assertFalse(DateTimeUtils.isValidIsoDate("2026-02-30"))
        assertFalse(DateTimeUtils.isValidIsoDate("24-11-2026"))
    }

    @Test
    fun passwordHasher_verifiesMatchingAndRejectsWrongPassword() {
        val hash = PasswordHasher.hashPassword("shift123")
        assertTrue(PasswordHasher.verifyPassword("shift123", hash))
        assertFalse(PasswordHasher.verifyPassword("wrongpass", hash))
    }
}
