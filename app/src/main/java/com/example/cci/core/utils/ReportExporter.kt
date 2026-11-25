package com.example.cci.core.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.cci.core.constants.AppConstants
import com.example.cci.data.datastore.PlantSettings
import com.example.cci.data.local.entity.CciTestEntity
import com.example.cci.data.local.entity.ChangeoverEntity
import com.example.cci.data.local.entity.CrusherStatusEntity
import com.example.cci.data.local.entity.DailyReport
import com.example.cci.data.local.entity.MaintenanceEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ReportType(val title: String) {
    DAILY_SUMMARY("Daily Plant CCI Report"),
    CCI_TESTS("CCI Test Report"),
    CRUSHER_PERFORMANCE("Crusher Performance Report"),
    CHANGEOVERS("Changeover Report"),
    MAINTENANCE("Maintenance Report")
}

enum class ExportFormat(val label: String, val ext: String, val mime: String) {
    PDF("PDF", "pdf", "application/pdf"),
    CSV("CSV", "csv", "text/csv"),
    EXCEL("Excel (.xlsx)", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
}

object ReportExporter {

    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

    /**
     * Generates a concise, executive-ready summary text of the Daily CCI Report
     * tailored for quick sharing in WhatsApp plant operations groups.
     */
    fun generateDailyReportSummaryText(
        settings: PlantSettings,
        reportDateLabel: String,
        shiftFilter: String = "ALL",
        tests: List<CciTestEntity>,
        crusherStatus: CrusherStatusEntity?,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("*${settings.companyName.uppercase(Locale.US)} — DAILY CCI SUMMARY*")
        sb.appendLine("${settings.departmentName}")
        sb.appendLine("📅 $reportDateLabel${if (shiftFilter != "ALL") " (Shift $shiftFilter)" else " (All Shifts)"}")
        sb.appendLine("────────────────────")

        if (crusherStatus != null) {
            sb.appendLine("⚙️ Running: *${crusherStatus.runningLine}* | Standby: ${crusherStatus.standbyLine}")
        }

        if (tests.isEmpty()) {
            sb.appendLine("🧪 Samples: *0* (No CCI sieve tests logged)")
        } else {
            val avgCci = tests.map { it.crushingIndex }.average()
            val minCci = tests.minOf { it.crushingIndex }
            val maxCci = tests.maxOf { it.crushingIndex }
            val avgBefore = tests.map { it.beforeFinesPct }.average()
            val avgAfter = tests.map { it.afterFinesPct }.average()
            val avgGain = tests.map { it.finesIncreasePp }.average()
            val inTarget = tests.count { it.crushingIndex >= settings.targetMinCci }
            val statusIcon = if (avgCci >= settings.targetMinCci) "✅ ON TARGET" else "⚠️ BELOW TARGET"

            sb.appendLine("📊 *Avg CCI:* *${fmt(avgCci)}%* ($statusIcon ≥ ${fmt1(settings.targetMinCci)}%)")
            sb.appendLine("📈 Range: ${fmt(minCci)}% – ${fmt(maxCci)}% | Samples: *${tests.size}* ($inTarget in spec)")
            sb.appendLine("🔬 Fines (-3mm): ${fmt(avgBefore)}% → *${fmt(avgAfter)}%* (${fmtSigned(avgGain)} pp)")

            val shiftSummary = AppConstants.SHIFTS.mapNotNull { shift ->
                val list = tests.filter { it.shift == shift }
                if (list.isEmpty()) null
                else "S-$shift: *${fmt(list.map { it.crushingIndex }.average())}%* (${list.size})"
            }.joinToString(" | ")
            if (shiftSummary.isNotBlank()) {
                sb.appendLine("🏭 $shiftSummary")
            }
        }

        sb.appendLine("🔄 Changeovers: ${changeovers.size} | 🔧 Maintenance: ${maintenance.size}")
        sb.appendLine("────────────────────")
        sb.append("✍️ ${settings.whatsappSignOff}")
        return sb.toString()
    }

    /**
     * Generates the daily report summary text and launches an Android Intent
     * to share the content directly via WhatsApp (with fallback share sheet).
     */
    fun shareDailyReportSummaryViaWhatsApp(
        context: Context,
        settings: PlantSettings,
        reportDateLabel: String,
        shiftFilter: String = "ALL",
        tests: List<CciTestEntity>,
        crusherStatus: CrusherStatusEntity?,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>
    ): Boolean {
        val summaryText = generateDailyReportSummaryText(
            settings = settings,
            reportDateLabel = reportDateLabel,
            shiftFilter = shiftFilter,
            tests = tests,
            crusherStatus = crusherStatus,
            changeovers = changeovers,
            maintenance = maintenance
        )
        return shareTextToWhatsApp(context, summaryText)
    }

    /**
     * Formats a persisted Room DailyReport entity (date, crusher parameters, and
     * sieve analysis results) into a structured WhatsApp message.
     */
    fun formatSavedDailyReportForWhatsApp(
        settings: PlantSettings,
        report: DailyReport
    ): String {
        val statusIcon = if (report.avgCrushingIndex >= settings.targetMinCci) "✅ ON TARGET" else "⚠️ BELOW TARGET"
        return buildString {
            appendLine("*${settings.companyName.uppercase(Locale.US)} — DAILY REPORT*")
            appendLine("${settings.departmentName}")
            appendLine("📅 Date: *${DateTimeUtils.formatDisplayDate(report.reportDate)}* | Shift: ${report.shiftScope}")
            appendLine("────────────────────")
            appendLine("⚙️ *CRUSHER PARAMETERS*")
            appendLine("• Line: *${report.runningLine}* (Standby: ${report.standbyLine})")
            appendLine("• Roller Gap: *${fmt1(report.rollerGapMm)} mm* | Pressure: *${fmt1(report.rollerPressureBar)} bar*")
            appendLine("• Feed Rate: *${fmt1(report.feedRateTph)} TPH* | Moisture: *${fmt1(report.cokeMoisturePct)}%*")
            appendLine()
            appendLine("🧪 *SIEVE ANALYSIS RESULTS (${report.sampleCount} Samples)*")
            appendLine("• *Avg CCI (-3mm): ${fmt(report.avgCrushingIndex)}%* ($statusIcon)")
            appendLine("• Min / Max CCI: ${fmt(report.minCrushingIndex)}% / ${fmt(report.maxCrushingIndex)}%")
            appendLine("• Fines (-3mm): ${fmt(report.avgBeforeFinesPct)}% → *${fmt(report.avgAfterFinesPct)}%* (${fmtSigned(report.avgFinesIncreasePp)} pp)")
            appendLine("• After Sieve Avg (kg): -0.5mm=${fmt(report.avgAfterFrac050)} | 0.5–1mm=${fmt(report.avgAfterFrac100)} | 1–3mm=${fmt(report.avgAfterFrac300)} | +3mm=${fmt(report.avgAfterFracPlus3)}")
            appendLine("• Target Compliance: *${fmt1(report.targetCompliancePct)}%*")
            appendLine("🔄 Changeovers: ${report.changeoverCount} | 🔧 Maintenance: ${report.maintenanceCount}")
            if (report.remarks.isNotBlank()) {
                appendLine("📝 _${report.remarks}_")
            }
            appendLine("────────────────────")
            append("✍️ ${settings.whatsappSignOff} (Saved by ${report.createdBy})")
        }
    }

    /**
     * Generates a detailed, structured WhatsApp-formatted Daily Plant CCI Report
     * including individual sample logs and remarks.
     */
    fun buildDailyWhatsAppReport(
        settings: PlantSettings,
        reportDateLabel: String,
        shiftFilter: String,
        tests: List<CciTestEntity>,
        crusherStatus: CrusherStatusEntity?,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("*${settings.companyName.uppercase(Locale.US)}*")
        sb.appendLine("*${settings.departmentName}*")
        sb.appendLine("📋 *DAILY COKE CRUSHING INDEX (CCI) REPORT*")
        sb.appendLine("📅 Period: $reportDateLabel${if (shiftFilter != "ALL") " | Shift $shiftFilter" else " | All Shifts"}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        if (crusherStatus != null) {
            sb.appendLine("⚙️ *CRUSHER LINE STATUS*")
            sb.appendLine("• Running: *${crusherStatus.runningLine}*")
            sb.appendLine("• Standby: ${crusherStatus.standbyLine}")
            sb.appendLine("• Last Changeover: ${DateTimeUtils.formatDateTimeReadable(crusherStatus.lastChangeoverAt)}")
            sb.appendLine()
        }

        sb.appendLine("📊 *SUMMARY METRICS*")
        if (tests.isEmpty()) {
            sb.appendLine("• No CCI sieve tests recorded for this selection.")
        } else {
            val avgCci = tests.map { it.crushingIndex }.average()
            val avgBefore = tests.map { it.beforeFinesPct }.average()
            val avgAfter = tests.map { it.afterFinesPct }.average()
            val avgInc = tests.map { it.finesIncreasePp }.average()
            val minCci = tests.minOf { it.crushingIndex }
            val maxCci = tests.maxOf { it.crushingIndex }
            val inSpec = tests.count { it.crushingIndex >= settings.targetMinCci }
            val compliancePct = (inSpec.toDouble() / tests.size) * 100.0
            val statusIcon = if (avgCci >= settings.targetMinCci) "✅" else "⚠️"

            sb.appendLine("• Total Samples: *${tests.size}* ($inSpec/${tests.size} ≥ ${fmt1(settings.targetMinCci)}% target)")
            sb.appendLine("• Avg CCI (-3mm): *${fmt(avgCci)}%* $statusIcon (Min ${fmt(minCci)}% | Max ${fmt(maxCci)}%)")
            sb.appendLine("• Avg Fines: ${fmt(avgBefore)}% → *${fmt(avgAfter)}%* (${fmtSigned(avgInc)} pp)")
            sb.appendLine("• Target Compliance: *${fmt1(compliancePct)}%*")
            sb.appendLine()

            sb.appendLine("🏭 *SHIFT-WISE BREAKDOWN*")
            AppConstants.SHIFTS.forEach { shift ->
                val shiftList = tests.filter { it.shift == shift }
                if (shiftList.isNotEmpty()) {
                    val sAvg = shiftList.map { it.crushingIndex }.average()
                    val sInc = shiftList.map { it.finesIncreasePp }.average()
                    val flag = if (sAvg >= settings.targetMinCci) "✅" else "⚠️"
                    sb.appendLine("• Shift $shift (${shiftList.size}): Avg CCI *${fmt(sAvg)}%* (${fmtSigned(sInc)} pp) $flag")
                }
            }
            sb.appendLine()

            sb.appendLine("🧪 *SAMPLE LOGS*")
            tests.take(10).forEach { t ->
                val tag = if (t.crushingIndex >= settings.targetMinCci) "✅" else "⚠️"
                val timePart = t.testDateTime.substringAfter("T", "")
                sb.appendLine("$tag *${t.sampleId}* | S-${t.shift} $timePart | ${t.crusherLine.replace("Crusher ", "")}")
                sb.appendLine("   Before: ${fmt(t.beforeFinesPct)}% → After: *${fmt(t.afterFinesPct)}%* (${fmtSigned(t.finesIncreasePp)} pp)")
                if (t.remarks.isNotBlank()) {
                    sb.appendLine("   _${t.remarks}_")
                }
            }
            if (tests.size > 10) {
                sb.appendLine("_...and ${tests.size - 10} more samples (see attached PDF/Excel)._")
            }
        }

        if (changeovers.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("🔄 *LINE CHANGEOVERS (${changeovers.size})*")
            changeovers.take(3).forEach { c ->
                sb.appendLine("• ${c.previousLine} → *${c.newLine}* (${c.reason}) by ${c.authorizedBy}")
            }
        }

        if (maintenance.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("🔧 *MAINTENANCE ACTIVITIES (${maintenance.size})*")
            maintenance.take(3).forEach { m ->
                val approval = if (m.approvedBy.isNotBlank()) "Approved" else "Pending Approval"
                sb.appendLine("• ${m.crusherLine}: *${m.activityType}* ($approval)")
                if (m.measurements.isNotBlank()) {
                    sb.appendLine("   ${m.measurements}")
                }
            }
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("✍️ ${settings.whatsappSignOff}")
        sb.append("_Generated ${DateTimeUtils.formatDateTimeReadable(DateTimeUtils.nowDateTimeIso())}_")
        return sb.toString()
    }

    fun buildSingleTestWhatsAppMessage(
        settings: PlantSettings,
        test: CciTestEntity
    ): String {
        val status = if (test.crushingIndex >= settings.targetMinCci) "✅ WITHIN TARGET" else "⚠️ BELOW TARGET (${fmt1(settings.targetMinCci)}%)"
        return buildString {
            appendLine("*${settings.companyName} — CCI Sample Result*")
            appendLine("🧪 Sample: *${test.sampleId}*")
            appendLine("📅 ${DateTimeUtils.formatDateTimeReadable(test.testDateTime)} | Shift *${test.shift}*")
            appendLine("⚙️ ${test.crusherLine} | ${test.cokeType} (${test.cokeSource})")
            appendLine("━━━━━━━━━━━━━━━━")
            appendLine("• Before Fines (-3mm): *${fmt(test.beforeFinesPct)}%* (${fmt(test.beforeTotal)} kg)")
            appendLine("• After Fines (-3mm): *${fmt(test.afterFinesPct)}%* (${fmt(test.afterTotal)} kg)")
            appendLine("• *Crushing Index (CCI): ${fmt(test.crushingIndex)}%*")
            appendLine("• Fines Gain: *${fmtSigned(test.finesIncreasePp)} pp*")
            appendLine("• Status: $status")
            if (test.remarks.isNotBlank()) {
                appendLine("• Remarks: _${test.remarks}_")
            }
            appendLine("👷 Operator: ${test.operatorName}")
        }
    }

    /**
     * Shares formatted text directly to WhatsApp if installed (or WhatsApp Business),
     * otherwise seamlessly falls back to Android's system share sheet.
     */
    fun shareTextToWhatsApp(context: Context, message: String): Boolean {
        val targetPackage = resolveWhatsAppPackage(context)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            if (targetPackage != null) {
                setPackage(targetPackage)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            if (targetPackage != null) {
                context.startActivity(intent)
                true
            } else {
                val chooser = Intent.createChooser(intent, "Share Daily CCI Report").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                false
            }
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(
                Intent.createChooser(fallback, "Share Daily CCI Report").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            false
        }
    }

    /**
     * Shares an exported PDF/Excel/CSV report directly to WhatsApp (with optional summary caption),
     * falling back to the system share sheet if WhatsApp is not installed.
     */
    fun shareFileToWhatsApp(
        context: Context,
        file: File,
        mimeType: String,
        caption: String? = null
    ): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val targetPackage = resolveWhatsAppPackage(context)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            if (!caption.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (targetPackage != null) {
                setPackage(targetPackage)
            }
        }
        return try {
            if (targetPackage != null) {
                context.startActivity(intent)
                true
            } else {
                context.startActivity(
                    Intent.createChooser(intent, "Share ${file.name}").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                false
            }
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                if (!caption.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(fallback, "Share ${file.name}").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            false
        }
    }

    fun openOrShareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Export ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun resolveWhatsAppPackage(context: Context): String? {
        val pm = context.packageManager
        return listOf(WHATSAPP_PACKAGE, WHATSAPP_BUSINESS_PACKAGE).firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun generateReportFile(
        context: Context,
        settings: PlantSettings,
        reportType: ReportType,
        format: ExportFormat,
        startDate: String,
        endDate: String,
        tests: List<CciTestEntity>,
        crusherStatus: CrusherStatusEntity?,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val slug = reportType.name.lowercase(Locale.US)
        val dateStamp = endDate.replace("-", "")
        val file = File(dir, "evonith_${slug}_${dateStamp}.${format.ext}")

        when (format) {
            ExportFormat.PDF -> writePdf(file, settings, reportType, startDate, endDate, tests, crusherStatus, changeovers, maintenance)
            ExportFormat.CSV -> writeCsv(file, reportType, tests, changeovers, maintenance)
            ExportFormat.EXCEL -> writeExcelXlsx(file, reportType, tests, changeovers, maintenance)
        }
        return file
    }

    fun exportJsonBackup(
        context: Context,
        settings: PlantSettings,
        tests: List<CciTestEntity>,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "evonith_cci_backup_${DateTimeUtils.todayIso().replace("-", "")}.json")

        val root = JSONObject().apply {
            put("exportedAt", DateTimeUtils.nowDateTimeIso())
            put("company", settings.companyName)
            put("department", settings.departmentName)
            put("targetMinCci", settings.targetMinCci)
            put("tests", JSONArray().apply {
                tests.forEach { t ->
                    put(JSONObject().apply {
                        put("sampleId", t.sampleId)
                        put("testDateTime", t.testDateTime)
                        put("shift", t.shift)
                        put("crusherLine", t.crusherLine)
                        put("cokeType", t.cokeType)
                        put("cokeSource", t.cokeSource)
                        put("operatorName", t.operatorName)
                        put("beforeFinesPct", t.beforeFinesPct)
                        put("afterFinesPct", t.afterFinesPct)
                        put("crushingIndex", t.crushingIndex)
                        put("finesIncreasePp", t.finesIncreasePp)
                        put("remarks", t.remarks)
                    })
                }
            })
            put("changeovers", JSONArray().apply {
                changeovers.forEach { c ->
                    put(JSONObject().apply {
                        put("eventDateTime", c.eventDateTime)
                        put("previousLine", c.previousLine)
                        put("newLine", c.newLine)
                        put("reason", c.reason)
                        put("authorizedBy", c.authorizedBy)
                        put("remarks", c.remarks)
                    })
                }
            })
            put("maintenance", JSONArray().apply {
                maintenance.forEach { m ->
                    put(JSONObject().apply {
                        put("crusherLine", m.crusherLine)
                        put("activityDate", m.activityDate)
                        put("activityType", m.activityType)
                        put("details", m.details)
                        put("measurements", m.measurements)
                        put("completedBy", m.completedBy)
                        put("approvedBy", m.approvedBy)
                    })
                }
            })
        }
        file.writeText(root.toString(2), Charsets.UTF_8)
        return file
    }

    private fun writeCsv(
        file: File,
        type: ReportType,
        tests: List<CciTestEntity>,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>
    ) {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        when (type) {
            ReportType.DAILY_SUMMARY, ReportType.CCI_TESTS -> {
                sb.appendLine("Sample ID,Date,Time,Shift,Crusher Line,Coke Type,Coke Source,Operator,Before Total (kg),After Total (kg),Before Fines (%),After Fines (%),Crushing Index (%),Fines Increase (pp),Remarks")
                tests.forEach { t ->
                    sb.appendLine(
                        listOf(
                            t.sampleId,
                            t.testDate,
                            t.testDateTime.substringAfter("T", ""),
                            t.shift,
                            t.crusherLine,
                            t.cokeType,
                            csvEsc(t.cokeSource),
                            csvEsc(t.operatorName),
                            fmt(t.beforeTotal),
                            fmt(t.afterTotal),
                            fmt(t.beforeFinesPct),
                            fmt(t.afterFinesPct),
                            fmt(t.crushingIndex),
                            fmt(t.finesIncreasePp),
                            csvEsc(t.remarks)
                        ).joinToString(",")
                    )
                }
            }
            ReportType.CRUSHER_PERFORMANCE -> {
                sb.appendLine("Crusher Line,Shift,Test Count,Avg CCI (%),Avg Initial Fines (%),Avg Final Fines (%),Avg Fines Increase (pp)")
                tests.groupBy { it.crusherLine to it.shift }.forEach { (key, list) ->
                    sb.appendLine(
                        listOf(
                            key.first,
                            key.second,
                            list.size.toString(),
                            fmt(list.map { it.crushingIndex }.average()),
                            fmt(list.map { it.beforeFinesPct }.average()),
                            fmt(list.map { it.afterFinesPct }.average()),
                            fmt(list.map { it.finesIncreasePp }.average())
                        ).joinToString(",")
                    )
                }
            }
            ReportType.CHANGEOVERS -> {
                sb.appendLine("DateTime,Previous Line,New Running Line,New Standby Line,Reason,Manual Override,Authorized By,Remarks")
                changeovers.forEach { c ->
                    sb.appendLine(
                        listOf(
                            c.eventDateTime,
                            c.previousLine,
                            c.newLine,
                            c.newStandbyLine,
                            csvEsc(c.reason),
                            if (c.manualOverride) "Yes" else "No",
                            csvEsc(c.authorizedBy),
                            csvEsc(c.remarks)
                        ).joinToString(",")
                    )
                }
            }
            ReportType.MAINTENANCE -> {
                sb.appendLine("Crusher Line,Activity Date,Activity,Details,Measurements,Roller Condition,Completed By,Approved By")
                maintenance.forEach { m ->
                    sb.appendLine(
                        listOf(
                            m.crusherLine,
                            m.activityDate,
                            csvEsc(m.activityType),
                            csvEsc(m.details),
                            csvEsc(m.measurements),
                            csvEsc(m.rollerCondition),
                            csvEsc(m.completedBy),
                            csvEsc(m.approvedBy.ifBlank { "Pending" })
                        ).joinToString(",")
                    )
                }
            }
        }
        file.writeText(sb.toString(), Charsets.UTF_8)
    }

    /**
     * Lightweight, zero-dependency standards-compliant OpenXML (.xlsx) writer
     * compatible with Microsoft Excel, Google Sheets, and WPS Office on Android.
     */
    private fun writeExcelXlsx(
        file: File,
        type: ReportType,
        tests: List<CciTestEntity>,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>
    ) {
        val headers: List<String>
        val rows: List<List<String>>
        val sheetName: String

        when (type) {
            ReportType.DAILY_SUMMARY, ReportType.CCI_TESTS -> {
                sheetName = "CCI Tests"
                headers = listOf("Sample ID", "Date", "Shift", "Line", "Coke Type", "Source", "Operator", "Before Fines %", "After Fines %", "CCI %", "Increase pp", "Remarks")
                rows = tests.map { t ->
                    listOf(
                        t.sampleId, t.testDate, t.shift, t.crusherLine, t.cokeType, t.cokeSource,
                        t.operatorName, fmt(t.beforeFinesPct), fmt(t.afterFinesPct),
                        fmt(t.crushingIndex), fmt(t.finesIncreasePp), t.remarks
                    )
                }
            }
            ReportType.CRUSHER_PERFORMANCE -> {
                sheetName = "Crusher Performance"
                headers = listOf("Crusher Line", "Shift", "Test Count", "Avg CCI %", "Avg Before Fines %", "Avg After Fines %", "Avg Increase pp")
                rows = tests.groupBy { it.crusherLine to it.shift }.map { (k, list) ->
                    listOf(
                        k.first, k.second, list.size.toString(),
                        fmt(list.map { it.crushingIndex }.average()),
                        fmt(list.map { it.beforeFinesPct }.average()),
                        fmt(list.map { it.afterFinesPct }.average()),
                        fmt(list.map { it.finesIncreasePp }.average())
                    )
                }
            }
            ReportType.CHANGEOVERS -> {
                sheetName = "Changeovers"
                headers = listOf("DateTime", "Previous", "New Running", "New Standby", "Reason", "Manual Override", "Authorized By", "Remarks")
                rows = changeovers.map { c ->
                    listOf(
                        c.eventDateTime, c.previousLine, c.newLine, c.newStandbyLine,
                        c.reason, if (c.manualOverride) "Yes" else "No", c.authorizedBy, c.remarks
                    )
                }
            }
            ReportType.MAINTENANCE -> {
                sheetName = "Maintenance"
                headers = listOf("Crusher", "Date", "Activity", "Details", "Measurements", "Roller Condition", "Completed By", "Approved By")
                rows = maintenance.map { m ->
                    listOf(
                        m.crusherLine, m.activityDate, m.activityType, m.details,
                        m.measurements, m.rollerCondition, m.completedBy, m.approvedBy.ifBlank { "Pending" }
                    )
                }
            }
        }

        val allRows = listOf(headers) + rows
        val sheetXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            allRows.forEachIndexed { rIdx, cols ->
                val rowNum = rIdx + 1
                append("""<row r="$rowNum">""")
                cols.forEachIndexed { cIdx, value ->
                    val colRef = colName(cIdx) + rowNum
                    val styleAttr = if (rIdx == 0) """ s="1"""" else ""
                    append("""<c r="$colRef" t="inlineStr"$styleAttr><is><t>${xmlEsc(value)}</t></is></c>""")
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

        val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="${xmlEsc(sheetName)}" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

        val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

        val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF132232"/></patternFill></fill>
  </fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
  </cellXfs>
</styleSheet>"""

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            fun putEntry(path: String, content: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            putEntry("[Content_Types].xml", contentTypes)
            putEntry("_rels/.rels", rootRels)
            putEntry("xl/workbook.xml", workbookXml)
            putEntry("xl/_rels/workbook.xml.rels", workbookRels)
            putEntry("xl/styles.xml", stylesXml)
            putEntry("xl/worksheets/sheet1.xml", sheetXml)
        }
    }

    private fun writePdf(
        file: File,
        settings: PlantSettings,
        type: ReportType,
        startDate: String,
        endDate: String,
        tests: List<CciTestEntity>,
        crusherStatus: CrusherStatusEntity?,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>
    ) {
        val pdf = PdfDocument()
        val headerBgPaint = Paint().apply { color = Color.rgb(19, 34, 50) }
        val accentPaint = Paint().apply { color = Color.rgb(217, 91, 22) }
        val kpiBgPaint = Paint().apply { color = Color.rgb(241, 245, 249) }
        val whiteTitlePaint = Paint().apply { color = Color.WHITE; textSize = 15f; isFakeBoldText = true; isAntiAlias = true }
        val whiteSubPaint = Paint().apply { color = Color.rgb(203, 213, 225); textSize = 10f; isAntiAlias = true }
        val sectionPaint = Paint().apply { color = Color.rgb(19, 34, 50); textSize = 11.5f; isFakeBoldText = true; isAntiAlias = true }
        val bodyPaint = Paint().apply { color = Color.rgb(30, 41, 59); textSize = 9.5f; isAntiAlias = true }
        val mutedPaint = Paint().apply { color = Color.rgb(100, 116, 139); textSize = 8.5f; isAntiAlias = true }
        val dividerPaint = Paint().apply { color = Color.rgb(226, 232, 240); strokeWidth = 1f }

        val lines = mutableListOf<Pair<Boolean, String>>() // isHeader to text
        if (type == ReportType.DAILY_SUMMARY) {
            if (crusherStatus != null) {
                lines.add(true to "CRUSHER LINE STATUS")
                lines.add(false to "Running: ${crusherStatus.runningLine}   |   Standby: ${crusherStatus.standbyLine}")
                lines.add(false to "Last Changeover: ${DateTimeUtils.formatDateTimeReadable(crusherStatus.lastChangeoverAt)} (by ${crusherStatus.updatedBy})")
                lines.add(false to "")
            }
            lines.add(true to "CCI SIEVE TEST RESULTS (${tests.size} Samples)")
            if (tests.isEmpty()) {
                lines.add(false to "No sieve test records logged in selected period.")
            } else {
                tests.forEach { t ->
                    val flag = if (t.crushingIndex >= settings.targetMinCci) "[OK]" else "[LOW]"
                    lines.add(false to "$flag ${t.sampleId} | ${t.testDate} S-${t.shift} | ${t.crusherLine} | ${t.cokeSource}")
                    lines.add(false to "     Before: ${fmt(t.beforeFinesPct)}%  ->  After: ${fmt(t.afterFinesPct)}%  |  CCI: ${fmt(t.crushingIndex)}%  |  Gain: ${fmtSigned(t.finesIncreasePp)} pp")
                    if (t.remarks.isNotBlank()) lines.add(false to "     Remarks: ${t.remarks}")
                }
            }
            if (changeovers.isNotEmpty()) {
                lines.add(false to "")
                lines.add(true to "LINE CHANGEOVERS (${changeovers.size})")
                changeovers.forEach { c ->
                    lines.add(false to "${DateTimeUtils.formatDateTimeReadable(c.eventDateTime)} | ${c.previousLine} -> ${c.newLine} (${c.reason})")
                }
            }
            if (maintenance.isNotEmpty()) {
                lines.add(false to "")
                lines.add(true to "MAINTENANCE LOGS (${maintenance.size})")
                maintenance.forEach { m ->
                    lines.add(false to "${m.activityDate} | ${m.crusherLine} | ${m.activityType} (${m.approvedBy.ifBlank { "Pending" }})")
                    lines.add(false to "     ${m.details} ${if (m.measurements.isNotBlank()) "| ${m.measurements}" else ""}")
                }
            }
        } else {
            when (type) {
                ReportType.CCI_TESTS -> {
                    lines.add(true to "DETAILED CCI TEST LOG")
                    tests.forEach { t ->
                        lines.add(false to "${t.sampleId} | ${t.testDate} S-${t.shift} | ${t.crusherLine} | ${t.cokeSource.ifBlank { t.cokeType }}")
                        lines.add(false to "   Before: ${fmt(t.beforeFinesPct)}% | After: ${fmt(t.afterFinesPct)}% | CCI: ${fmt(t.crushingIndex)}% | Inc: ${fmtSigned(t.finesIncreasePp)} pp | Tech: ${t.operatorName}")
                        if (t.remarks.isNotBlank()) lines.add(false to "   Remarks: ${t.remarks}")
                    }
                }
                ReportType.CRUSHER_PERFORMANCE -> {
                    lines.add(true to "LINE-WISE PERFORMANCE")
                    AppConstants.CRUSHER_LINES.forEach { line ->
                        val sub = tests.filter { it.crusherLine == line }
                        val avg = if (sub.isNotEmpty()) fmt(sub.map { it.crushingIndex }.average()) else "0.00"
                        lines.add(false to "$line: Tests=${sub.size}, Avg CCI=$avg%")
                    }
                    lines.add(false to "")
                    lines.add(true to "SHIFT-WISE BREAKDOWN")
                    tests.groupBy { it.crusherLine to it.shift }.forEach { (k, list) ->
                        lines.add(false to "${k.first} Shift ${k.second}: Tests=${list.size}, Avg CCI=${fmt(list.map { it.crushingIndex }.average())}%")
                    }
                }
                ReportType.CHANGEOVERS -> {
                    lines.add(true to "CRUSHER LINE CHANGEOVER REGISTER")
                    changeovers.forEach { c ->
                        lines.add(false to "${c.eventDateTime} | ${c.previousLine} -> ${c.newLine} (Standby: ${c.newStandbyLine})")
                        lines.add(false to "   Reason: ${c.reason} | Override: ${if (c.manualOverride) "Yes" else "No"} | Auth: ${c.authorizedBy}")
                    }
                }
                ReportType.MAINTENANCE -> {
                    lines.add(true to "CRUSHER MAINTENANCE REGISTER")
                    maintenance.forEach { m ->
                        lines.add(false to "${m.activityDate} | ${m.crusherLine} | ${m.activityType}")
                        lines.add(false to "   ${m.details} | By: ${m.completedBy} | Approved: ${m.approvedBy.ifBlank { "Pending" }}")
                    }
                }
                else -> Unit
            }
        }

        if (lines.isEmpty()) lines.add(false to "No records found for selected date range.")

        var pageNum = 1
        var index = 0
        while (index < lines.size) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            // Top Foundry Navy Header Banner
            canvas.drawRect(0f, 0f, 595f, 78f, headerBgPaint)
            canvas.drawRect(0f, 74f, 595f, 78f, accentPaint)
            canvas.drawText("${settings.companyName.uppercase(Locale.US)} — ${settings.departmentName}", 32f, 34f, whiteTitlePaint)
            val rangeLabel = if (startDate == endDate) DateTimeUtils.formatDisplayDate(startDate)
            else "${DateTimeUtils.formatDisplayDate(startDate)} to ${DateTimeUtils.formatDisplayDate(endDate)}"
            canvas.drawText("${type.title}  |  Period: $rangeLabel  |  Page $pageNum", 32f, 56f, whiteSubPaint)

            var y = 100f

            // Page 1 KPI Summary Strip
            if (pageNum == 1 && tests.isNotEmpty()) {
                val avgCci = tests.map { it.crushingIndex }.average()
                val avgInc = tests.map { it.finesIncreasePp }.average()
                val inSpec = tests.count { it.crushingIndex >= settings.targetMinCci }
                val boxes = listOf(
                    "AVG CCI (-3mm)" to "${fmt(avgCci)}%",
                    "SAMPLES" to "${tests.size}",
                    "AVG FINES GAIN" to "${fmtSigned(avgInc)} pp",
                    "IN TARGET (>=${fmt1(settings.targetMinCci)}%)" to "$inSpec / ${tests.size}"
                )
                val boxW = 124f
                boxes.forEachIndexed { idx, (label, value) ->
                    val left = 32f + idx * (boxW + 11f)
                    canvas.drawRoundRect(RectF(left, y, left + boxW, y + 48f), 6f, 6f, kpiBgPaint)
                    canvas.drawText(label, left + 8f, y + 17f, mutedPaint)
                    canvas.drawText(value, left + 8f, y + 36f, sectionPaint)
                }
                y += 68f
            }

            while (index < lines.size && y < 782f) {
                val (isHeader, text) = lines[index]
                if (text.isEmpty()) {
                    y += 8f
                } else if (isHeader) {
                    canvas.drawText(text.take(86), 32f, y, sectionPaint)
                    canvas.drawLine(32f, y + 4f, 563f, y + 4f, dividerPaint)
                    y += 18f
                } else {
                    canvas.drawText(text.take(94), 32f, y, bodyPaint)
                    y += 15f
                }
                index++
            }

            // Footer
            canvas.drawLine(32f, 804f, 563f, 804f, dividerPaint)
            canvas.drawText("Sign-off: ${settings.whatsappSignOff}", 32f, 820f, mutedPaint)
            canvas.drawText("Generated: ${DateTimeUtils.formatDateTimeReadable(DateTimeUtils.nowDateTimeIso())}", 360f, 820f, mutedPaint)

            pdf.finishPage(page)
            pageNum++
        }

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
    }

    private fun colName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.insert(0, ('A' + (n % 26)))
            n = (n / 26) - 1
        }
        return sb.toString()
    }

    private fun xmlEsc(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
    private fun fmt1(v: Double): String = String.format(Locale.US, "%.1f", v)
    private fun fmtSigned(v: Double): String = String.format(Locale.US, "%+.2f", v)
    private fun csvEsc(s: String): String = "\"${s.replace("\"", "\"\"")}\""
}
