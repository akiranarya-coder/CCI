package com.example.cci.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cci.core.utils.DateTimeUtils
import com.example.cci.core.utils.ExportFormat
import com.example.cci.core.utils.ReportExporter
import com.example.cci.core.utils.ReportType
import com.example.cci.data.local.entity.CciTestEntity
import com.example.cci.data.local.entity.ChangeoverEntity
import com.example.cci.data.local.entity.DailyReport
import com.example.cci.data.local.entity.MaintenanceEntity
import com.example.cci.ui.CciUiState
import com.example.cci.ui.components.EmptyStateCard
import com.example.cci.ui.components.SectionHeader
import com.example.cci.ui.components.StatusPill
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlantEmerald
import com.example.ui.theme.WhatsAppDeepGreen
import com.example.ui.theme.WhatsAppGreen
import java.util.Locale

private enum class DateRangePreset(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    CUSTOM_DATE("Pick Date"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    ALL_TIME("All Records")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: CciUiState,
    onShareWhatsAppText: (String) -> Unit,
    onCopyReportText: (String) -> Unit,
    onSaveDailyReport: (
        reportDate: String,
        shiftScope: String,
        rollerGapMm: Double,
        feedRateTph: Double,
        rollerPressureBar: Double,
        cokeMoisturePct: Double,
        tests: List<CciTestEntity>,
        changeoverCount: Int,
        maintenanceCount: Int,
        remarks: String,
        summaryText: String
    ) -> Unit,
    onDeleteDailyReport: (DailyReport) -> Unit,
    onShareSavedDailyReport: (DailyReport) -> Unit,
    onExportReport: (
        reportType: ReportType,
        format: ExportFormat,
        startDate: String,
        endDate: String,
        tests: List<CciTestEntity>,
        changeovers: List<ChangeoverEntity>,
        maintenance: List<MaintenanceEntity>,
        viaWhatsApp: Boolean,
        caption: String?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPreset by remember { mutableStateOf(DateRangePreset.TODAY) }
    var customIsoDate by remember { mutableStateOf(DateTimeUtils.todayIso()) }
    var showFilterDatePicker by remember { mutableStateOf(false) }
    var selectedShift by remember { mutableStateOf("ALL") }
    var useCompactSummary by remember { mutableStateOf(true) }
    var selectedReportType by remember { mutableStateOf(ReportType.DAILY_SUMMARY) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var showSaveSnapshotDialog by remember { mutableStateOf(false) }

    val (startDate, endDate, periodLabel) = remember(selectedPreset, customIsoDate) {
        val today = DateTimeUtils.todayIso()
        when (selectedPreset) {
            DateRangePreset.TODAY -> Triple(today, today, DateTimeUtils.formatDisplayDate(today))
            DateRangePreset.YESTERDAY -> {
                val y = DateTimeUtils.daysAgoIso(1)
                Triple(y, y, DateTimeUtils.formatDisplayDate(y))
            }
            DateRangePreset.CUSTOM_DATE -> Triple(
                customIsoDate,
                customIsoDate,
                DateTimeUtils.formatDisplayDate(customIsoDate)
            )
            DateRangePreset.LAST_7_DAYS -> {
                val s = DateTimeUtils.daysAgoIso(6)
                Triple(s, today, "${DateTimeUtils.formatShortDate(s)} – ${DateTimeUtils.formatDisplayDate(today)}")
            }
            DateRangePreset.LAST_30_DAYS -> {
                val s = DateTimeUtils.daysAgoIso(29)
                Triple(s, today, "${DateTimeUtils.formatShortDate(s)} – ${DateTimeUtils.formatDisplayDate(today)}")
            }
            DateRangePreset.ALL_TIME -> Triple("2000-01-01", "2099-12-31", "All Recorded Dates")
        }
    }

    val filteredTests = remember(state.tests, startDate, endDate, selectedShift) {
        state.tests.filter { t ->
            t.testDate in startDate..endDate && (selectedShift == "ALL" || t.shift == selectedShift)
        }
    }

    val filteredChangeovers = remember(state.changeovers, startDate, endDate) {
        state.changeovers.filter { c ->
            val d = c.eventDateTime.substringBefore("T")
            d in startDate..endDate
        }
    }

    val filteredMaintenance = remember(state.maintenance, startDate, endDate) {
        state.maintenance.filter { m -> m.activityDate in startDate..endDate }
    }

    val whatsappPreviewText = remember(
        useCompactSummary, state.settings, periodLabel, selectedShift, filteredTests, state.crusherStatus, filteredChangeovers, filteredMaintenance
    ) {
        if (useCompactSummary) {
            ReportExporter.generateDailyReportSummaryText(
                settings = state.settings,
                reportDateLabel = periodLabel,
                shiftFilter = selectedShift,
                tests = filteredTests,
                crusherStatus = state.crusherStatus,
                changeovers = filteredChangeovers,
                maintenance = filteredMaintenance
            )
        } else {
            ReportExporter.buildDailyWhatsAppReport(
                settings = state.settings,
                reportDateLabel = periodLabel,
                shiftFilter = selectedShift,
                tests = filteredTests,
                crusherStatus = state.crusherStatus,
                changeovers = filteredChangeovers,
                maintenance = filteredMaintenance
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Period & Shift Filter Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Report Period & Shift", style = MaterialTheme.typography.titleMedium)
                        StatusPill(
                            text = "${filteredTests.size} SAMPLES",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DateRangePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = selectedPreset == preset,
                                onClick = {
                                    if (preset == DateRangePreset.CUSTOM_DATE) {
                                        showFilterDatePicker = true
                                    } else {
                                        selectedPreset = preset
                                    }
                                },
                                leadingIcon = if (preset == DateRangePreset.CUSTOM_DATE) {
                                    { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                label = {
                                    Text(
                                        if (preset == DateRangePreset.CUSTOM_DATE && selectedPreset == DateRangePreset.CUSTOM_DATE) {
                                            DateTimeUtils.formatShortDate(customIsoDate)
                                        } else preset.label
                                    )
                                },
                                modifier = Modifier.testTag("report_preset_${preset.name.lowercase(Locale.US)}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL" to "All Shifts", "A" to "Shift A", "B" to "Shift B", "C" to "Shift C").forEach { (key, label) ->
                            FilterChip(
                                selected = selectedShift == key,
                                onClick = { selectedShift = key },
                                label = { Text(label) }
                            )
                        }
                    }

                    val avgCci = filteredTests.takeIf { it.isNotEmpty() }?.map { it.crushingIndex }?.average()
                    val inSpec = filteredTests.count { it.crushingIndex >= state.settings.targetMinCci }
                    val cciColor = if (avgCci == null || avgCci >= state.settings.targetMinCci) PlantEmerald else CautionAmber

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryStat(
                            label = "Avg CCI",
                            value = if (avgCci != null) "${String.format(Locale.US, "%.2f", avgCci)}%" else "--",
                            valueColor = cciColor
                        )
                        SummaryStat(
                            label = "In Target",
                            value = if (filteredTests.isNotEmpty()) "$inSpec/${filteredTests.size}" else "--",
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )
                        SummaryStat(
                            label = "Changeovers",
                            value = "${filteredChangeovers.size}",
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )
                        SummaryStat(
                            label = "Maint.",
                            value = "${filteredMaintenance.size}",
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    FilledTonalButton(
                        onClick = { showSaveSnapshotDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_save_daily_report_button")
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Daily Report Snapshot to Database")
                    }
                }
            }
        }

        // 2. Daily WhatsApp Report Center
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, WhatsAppGreen.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Report — WhatsApp Share",
                                style = MaterialTheme.typography.titleLarge,
                                color = WhatsAppDeepGreen
                            )
                            Text(
                                text = "Formatted for Plant Operations & Management groups",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            text = "WHATSAPP READY",
                            containerColor = WhatsAppGreen.copy(alpha = 0.14f),
                            contentColor = WhatsAppDeepGreen
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = useCompactSummary,
                            onClick = { useCompactSummary = true },
                            label = { Text("Summary Text") },
                            modifier = Modifier.testTag("whatsapp_mode_summary")
                        )
                        FilterChip(
                            selected = !useCompactSummary,
                            onClick = { useCompactSummary = false },
                            label = { Text("Detailed Report") },
                            modifier = Modifier.testTag("whatsapp_mode_detailed")
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = whatsappPreviewText,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.5.sp,
                            lineHeight = 16.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(12.dp)
                                .testTag("whatsapp_report_preview")
                        )
                    }

                    Button(
                        onClick = { onShareWhatsAppText(whatsappPreviewText) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("whatsapp_share_text_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (useCompactSummary) "Share Daily Summary via WhatsApp" else "Share Detailed Report via WhatsApp",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                onExportReport(
                                    ReportType.DAILY_SUMMARY,
                                    ExportFormat.PDF,
                                    startDate,
                                    endDate,
                                    filteredTests,
                                    filteredChangeovers,
                                    filteredMaintenance,
                                    true,
                                    "Evonith Daily CCI Report ($periodLabel)"
                                )
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("whatsapp_share_pdf_button")
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF on WhatsApp")
                        }

                        OutlinedButton(
                            onClick = { onCopyReportText(whatsappPreviewText) },
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("copy_daily_report_button")
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy")
                        }
                    }
                }
            }
        }

        // 3. Saved Daily Reports Archive (Room DailyReport entity)
        item {
            SectionHeader(
                title = "Saved Daily Reports",
                subtitle = "${state.dailyReports.size} persisted snapshots with crusher parameters & sieve results",
                actionLabel = "+ Save New",
                onActionClick = { showSaveSnapshotDialog = true }
            )
        }

        if (state.dailyReports.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Description,
                    title = "No saved daily reports yet",
                    subtitle = "Save a daily report snapshot above to archive crusher parameters and sieve analysis results."
                )
            }
        } else {
            items(state.dailyReports, key = { "saved_dr_${it.id}" }) { report ->
                SavedDailyReportCard(
                    report = report,
                    targetMinCci = state.settings.targetMinCci,
                    canDelete = state.session.role.canDeleteTest,
                    onShareWhatsApp = { onShareSavedDailyReport(report) },
                    onDelete = { onDeleteDailyReport(report) }
                )
            }
        }

        // 4. Detailed File Export Section (PDF, Excel .xlsx, CSV)
        item {
            SectionHeader(
                title = "Export Registers & Spreadsheets",
                subtitle = "Generate PDF, Excel (.xlsx), or CSV files for plant archives"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Report Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReportType.entries.forEach { rt ->
                            FilterChip(
                                selected = selectedReportType == rt,
                                onClick = { selectedReportType = rt },
                                label = { Text(rt.title.replace(" Report", "")) }
                            )
                        }
                    }

                    Text("File Format", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                leadingIcon = {
                                    val icon = when (fmt) {
                                        ExportFormat.PDF -> Icons.Rounded.PictureAsPdf
                                        ExportFormat.EXCEL -> Icons.Rounded.TableChart
                                        ExportFormat.CSV -> Icons.Rounded.Description
                                    }
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text(fmt.label) }
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onExportReport(
                                    selectedReportType,
                                    selectedFormat,
                                    startDate,
                                    endDate,
                                    filteredTests,
                                    filteredChangeovers,
                                    filteredMaintenance,
                                    false,
                                    null
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_share_file_button")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export ${selectedFormat.ext.uppercase(Locale.US)}")
                        }
                        OutlinedButton(
                            onClick = {
                                onExportReport(
                                    selectedReportType,
                                    selectedFormat,
                                    startDate,
                                    endDate,
                                    filteredTests,
                                    filteredChangeovers,
                                    filteredMaintenance,
                                    true,
                                    "${selectedReportType.title} ($periodLabel)"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_whatsapp_file_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send to WhatsApp")
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showFilterDatePicker) {
        PlantDatePickerModal(
            initialIsoDate = customIsoDate,
            onDateSelected = { pickedIso ->
                customIsoDate = pickedIso
                selectedPreset = DateRangePreset.CUSTOM_DATE
                showFilterDatePicker = false
            },
            onDismiss = { showFilterDatePicker = false }
        )
    }

    if (showSaveSnapshotDialog) {
        SaveDailyReportDialog(
            defaultDate = if (endDate == "2099-12-31") DateTimeUtils.todayIso() else endDate,
            shiftScope = selectedShift,
            runningLine = state.runningLine,
            sampleCount = filteredTests.size,
            avgCci = filteredTests.takeIf { it.isNotEmpty() }?.map { it.crushingIndex }?.average() ?: 0.0,
            onDismiss = { showSaveSnapshotDialog = false },
            onSave = { date, gap, feed, pressure, moisture, remarks ->
                onSaveDailyReport(
                    date,
                    selectedShift,
                    gap,
                    feed,
                    pressure,
                    moisture,
                    filteredTests,
                    filteredChangeovers.size,
                    filteredMaintenance.size,
                    remarks,
                    whatsappPreviewText
                )
                showSaveSnapshotDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantDatePickerModal(
    initialIsoDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val maxAllowedUtc = remember { DateTimeUtils.todayEndOfDayUtcMillis() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateTimeUtils.isoDateToUtcMillis(initialIsoDate),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= maxAllowedUtc
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = datePickerState.selectedDateMillis != null,
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(DateTimeUtils.utcMillisToIsoDate(millis))
                    }
                },
                modifier = Modifier.testTag("date_picker_confirm_button")
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("date_picker_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = "Select Daily Report Date",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            }
        )
    }
}

@Composable
private fun SavedDailyReportCard(
    report: DailyReport,
    targetMinCci: Double,
    canDelete: Boolean,
    onShareWhatsApp: () -> Unit,
    onDelete: () -> Unit
) {
    val meetsTarget = report.avgCrushingIndex >= targetMinCci
    val badgeColor = if (meetsTarget) PlantEmerald else CautionAmber

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_daily_report_card_${report.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateTimeUtils.formatDisplayDate(report.reportDate),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${report.runningLine} • Shift: ${report.shiftScope} • ${report.sampleCount} samples",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = "CCI ${String.format(Locale.US, "%.2f", report.avgCrushingIndex)}%",
                    containerColor = badgeColor.copy(alpha = 0.14f),
                    contentColor = badgeColor
                )
            }

            // Crusher Parameters Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ParamMini("Gap", "${String.format(Locale.US, "%.1f", report.rollerGapMm)}mm")
                ParamMini("Feed", "${String.format(Locale.US, "%.1f", report.feedRateTph)}TPH")
                ParamMini("Press.", "${String.format(Locale.US, "%.0f", report.rollerPressureBar)}bar")
                ParamMini("Moist.", "${String.format(Locale.US, "%.1f", report.cokeMoisturePct)}%")
            }

            // Sieve Analysis Summary Line
            Text(
                text = "Sieve (-3mm): ${String.format(Locale.US, "%.1f", report.avgBeforeFinesPct)}% → ${String.format(Locale.US, "%.1f", report.avgAfterFinesPct)}% (${String.format(Locale.US, "%+.2f", report.avgFinesIncreasePp)} pp) • After: -0.5=${String.format(Locale.US, "%.2f", report.avgAfterFrac050)}kg, +3=${String.format(Locale.US, "%.2f", report.avgAfterFracPlus3)}kg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (report.remarks.isNotBlank()) {
                Text(
                    text = report.remarks,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved by ${report.createdBy}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_saved_report_${report.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete saved daily report",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = onShareWhatsApp,
                        modifier = Modifier.testTag("share_saved_report_whatsapp_${report.id}")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp")
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveDailyReportDialog(
    defaultDate: String,
    shiftScope: String,
    runningLine: String,
    sampleCount: Int,
    avgCci: Double,
    onDismiss: () -> Unit,
    onSave: (
        reportDate: String,
        rollerGapMm: Double,
        feedRateTph: Double,
        rollerPressureBar: Double,
        cokeMoisturePct: Double,
        remarks: String
    ) -> Unit
) {
    var reportDate by remember { mutableStateOf(defaultDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var rollerGap by remember { mutableStateOf("2.9") }
    var feedRate by remember { mutableStateOf("44.0") }
    var rollerPressure by remember { mutableStateOf("145.0") }
    var cokeMoisture by remember { mutableStateOf("5.2") }
    var remarks by remember { mutableStateOf("") }
    val isDateValid = DateTimeUtils.isValidIsoDate(reportDate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Save Daily Report", style = MaterialTheme.typography.titleLarge)
                Text(
                    "$runningLine • Shift $shiftScope • $sampleCount samples (Avg CCI ${String.format(Locale.US, "%.2f", avgCci)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = "${DateTimeUtils.formatDisplayDate(reportDate)} ($reportDate)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Report Date") },
                        supportingText = {
                            Text(
                                if (isDateValid) "Validated ISO date — tap calendar to change"
                                else "Please select a valid report date"
                            )
                        },
                        isError = !isDateValid,
                        trailingIcon = {
                            IconButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.testTag("report_date_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = "Choose report date"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_date_field")
                    )
                    // Full-field click overlay so tapping anywhere on the date input opens DatePicker
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

                Text("Crusher Operating Parameters", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DecimalInput("Roller Gap (mm)", rollerGap, { rollerGap = it }, Modifier.weight(1f))
                    DecimalInput("Feed Rate (TPH)", feedRate, { feedRate = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DecimalInput("Pressure (bar)", rollerPressure, { rollerPressure = it }, Modifier.weight(1f))
                    DecimalInput("Moisture (%)", cokeMoisture, { cokeMoisture = it }, Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Shift Remarks") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = isDateValid,
                onClick = {
                    onSave(
                        reportDate,
                        rollerGap.toDoubleOrNull() ?: 2.9,
                        feedRate.toDoubleOrNull() ?: 44.0,
                        rollerPressure.toDoubleOrNull() ?: 145.0,
                        cokeMoisture.toDoubleOrNull() ?: 5.2,
                        remarks
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_save_daily_report_button")
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        PlantDatePickerModal(
            initialIsoDate = reportDate,
            onDateSelected = { selectedIso ->
                reportDate = selectedIso
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun DecimalInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

@Composable
private fun ParamMini(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SummaryStat(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = valueColor
        )
    }
}
