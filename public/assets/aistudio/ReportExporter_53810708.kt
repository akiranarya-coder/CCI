package com.evonith.cci.core.utils

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.evonith.cci.core.constants.AppConstants
import com.evonith.cci.data.local.entity.CciTestEntity
import com.evonith.cci.data.local.entity.ChangeoverEntity
import com.evonith.cci.data.local.entity.MaintenanceEntity
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

enum class ReportType(val title: String) {
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

    fun exportAndShare(
        context: Context,
        reportType: ReportType,
        format: ExportFormat,
        startDate: String,
        endDate: String,
        tests: List<CciTestEntity>,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val slug = reportType.name.lowercase(Locale.US)
        val file = File(dir, "evonith_${slug}.${format.ext}")

        when (format) {
            ExportFormat.PDF -> writePdf(file, reportType, startDate, endDate, tests, changeovers, maintenance)
            ExportFormat.CSV -> writeCsv(file, reportType, tests, changeovers, maintenance)
            ExportFormat.EXCEL -> writeExcel(file, reportType, tests, changeovers, maintenance)
        }
        return file
    }

    fun openOrShareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export ${file.name}"))
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
            ReportType.CCI_TESTS -> {
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

    private fun writeExcel(
        file: File,
        type: ReportType,
        tests: List<CciTestEntity>,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>
    ) {
        val workbook = XSSFWorkbook()
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            }
            setFont(font)
        }

        fun addSheet(name: String, headers: List<String>, rows: List<List<String>>) {
            val sheet = workbook.createSheet(name)
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { idx, h ->
                val cell = headerRow.createCell(idx)
                cell.setCellValue(h)
                cell.cellStyle = headerStyle
                sheet.setColumnWidth(idx, 18 * 256)
            }
            rows.forEachIndexed { rIdx, rowVals ->
                val row = sheet.createRow(rIdx + 1)
                rowVals.forEachIndexed { cIdx, v ->
                    row.createCell(cIdx).setCellValue(v)
                }
            }
        }

        when (type) {
            ReportType.CCI_TESTS -> {
                addSheet(
                    "CCI Tests",
                    listOf("Sample ID", "Date", "Shift", "Line", "Coke Type", "Source", "Operator", "Before Fines %", "After Fines %", "CCI %", "Increase pp", "Remarks"),
                    tests.map { t ->
                        listOf(
                            t.sampleId, t.testDate, t.shift, t.crusherLine, t.cokeType, t.cokeSource,
                            t.operatorName, fmt(t.beforeFinesPct), fmt(t.afterFinesPct),
                            fmt(t.crushingIndex), fmt(t.finesIncreasePp), t.remarks
                        )
                    }
                )
            }
            ReportType.CRUSHER_PERFORMANCE -> {
                addSheet(
                    "Line & Shift Summary",
                    listOf("Crusher Line", "Shift", "Test Count", "Avg CCI %", "Avg Before Fines %", "Avg After Fines %", "Avg Increase pp"),
                    tests.groupBy { it.crusherLine to it.shift }.map { (k, list) ->
                        listOf(
                            k.first, k.second, list.size.toString(),
                            fmt(list.map { it.crushingIndex }.average()),
                            fmt(list.map { it.beforeFinesPct }.average()),
                            fmt(list.map { it.afterFinesPct }.average()),
                            fmt(list.map { it.finesIncreasePp }.average())
                        )
                    }
                )
                addSheet(
                    "Monthly Summary",
                    listOf("Month", "Test Count", "Avg CCI %"),
                    tests.groupBy { it.testDate.take(7) }.map { (month, list) ->
                        listOf(month, list.size.toString(), fmt(list.map { it.crushingIndex }.average()))
                    }
                )
            }
            ReportType.CHANGEOVERS -> {
                addSheet(
                    "Changeovers",
                    listOf("DateTime", "Previous", "New Running", "New Standby", "Reason", "Manual Override", "Authorized By", "Remarks"),
                    changeovers.map { c ->
                        listOf(
                            c.eventDateTime, c.previousLine, c.newLine, c.newStandbyLine,
                            c.reason, if (c.manualOverride) "Yes" else "No", c.authorizedBy, c.remarks
                        )
                    }
                )
            }
            ReportType.MAINTENANCE -> {
                addSheet(
                    "Maintenance",
                    listOf("Crusher", "Date", "Activity", "Details", "Measurements", "Roller Condition", "Completed By", "Approved By"),
                    maintenance.map { m ->
                        listOf(
                            m.crusherLine, m.activityDate, m.activityType, m.details,
                            m.measurements, m.rollerCondition, m.completedBy, m.approvedBy.ifBlank { "Pending" }
                        )
                    }
                )
            }
        }

        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
    }

    private fun writePdf(
        file: File,
        type: ReportType,
        startDate: String,
        endDate: String,
        tests: List<CciTestEntity>,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>
    ) {
        val pdf = PdfDocument()
        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val subPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 10f }

        val lines = mutableListOf<String>()
        when (type) {
            ReportType.CCI_TESTS -> {
                tests.forEach { t ->
                    lines.add("${t.sampleId} | ${t.testDate} S-${t.shift} | ${t.crusherLine} | ${t.cokeSource.ifBlank { t.cokeType }}")
                    lines.add("   Before: ${fmt(t.beforeFinesPct)}% | After: ${fmt(t.afterFinesPct)}% | CCI: ${fmt(t.crushingIndex)}% | Inc: ${fmtSigned(t.finesIncreasePp)} pp | Tech: ${t.operatorName}")
                    if (t.remarks.isNotBlank()) lines.add("   Remarks: ${t.remarks}")
                }
            }
            ReportType.CRUSHER_PERFORMANCE -> {
                AppConstants.CRUSHER_LINES.forEach { line ->
                    val sub = tests.filter { it.crusherLine == line }
                    val avg = if (sub.isNotEmpty()) fmt(sub.map { it.crushingIndex }.average()) else "0.00"
                    lines.add("$line: Tests=${sub.size}, Avg CCI=$avg%")
                }
                lines.add("Shift-wise Summary:")
                tests.groupBy { it.crusherLine to it.shift }.forEach { (k, list) ->
                    lines.add("  ${k.first} Shift ${k.second}: Tests=${list.size}, Avg CCI=${fmt(list.map { it.crushingIndex }.average())}%")
                }
                lines.add("Monthly Summary:")
                tests.groupBy { it.testDate.take(7) }.forEach { (m, list) ->
                    lines.add("  $m: Tests=${list.size}, Avg CCI=${fmt(list.map { it.crushingIndex }.average())}%")
                }
            }
            ReportType.CHANGEOVERS -> {
                changeovers.forEach { c ->
                    lines.add("${c.eventDateTime} | ${c.previousLine} -> ${c.newLine} (Standby: ${c.newStandbyLine})")
                    lines.add("   Reason: ${c.reason} | Override: ${if (c.manualOverride) "Yes" else "No"} | Auth: ${c.authorizedBy}")
                }
            }
            ReportType.MAINTENANCE -> {
                maintenance.forEach { m ->
                    lines.add("${m.activityDate} | ${m.crusherLine} | ${m.activityType}")
                    lines.add("   ${m.details} | By: ${m.completedBy} | Approved: ${m.approvedBy.ifBlank { "Pending" }}")
                }
            }
        }
        if (lines.isEmpty()) lines.add("No records found for selected date filter.")

        var pageNum = 1
        var index = 0
        while (index < lines.size) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            var y = 45f
            canvas.drawText("${AppConstants.COMPANY_NAME} — ${AppConstants.DEPARTMENT_NAME}", 36f, y, titlePaint)
            y += 20f
            canvas.drawText("${type.title} (Range: ${startDate.ifBlank { "All" }} to ${endDate.ifBlank { "All" }}) — Page $pageNum", 36f, y, subPaint)
            y += 24f

            while (index < lines.size && y < 790f) {
                canvas.drawText(lines[index].take(95), 36f, y, bodyPaint)
                y += 16f
                index++
            }
            pdf.finishPage(page)
            pageNum++
        }

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
    }

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
    private fun fmtSigned(v: Double): String = String.format(Locale.US, "%+.2f", v)
    private fun csvEsc(s: String): String {
        val clean = s.replace("\"", "\"\"")
        return "\"$clean\""
    }
}
