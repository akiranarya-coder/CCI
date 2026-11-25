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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cci.core.constants.AppConstants
import com.example.cci.core.utils.DateTimeUtils
import com.example.cci.data.local.entity.CciTestEntity
import com.example.cci.ui.CciUiState
import com.example.cci.ui.components.EmptyStateCard
import com.example.cci.ui.components.StatusPill
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlantEmerald
import com.example.ui.theme.WhatsAppGreen
import java.util.Locale

@Composable
fun TestsScreen(
    state: CciUiState,
    suggestedSampleId: String,
    showAddDialog: Boolean,
    onOpenAddDialog: () -> Unit,
    onDismissAddDialog: () -> Unit,
    onRefreshSampleId: (String) -> Unit,
    onAddTest: (
        sampleId: String,
        testDate: String,
        shift: String,
        crusherLine: String,
        cokeType: String,
        cokeSource: String,
        operatorName: String,
        before050: Double,
        before100: Double,
        before300: Double,
        beforePlus3: Double,
        after050: Double,
        after100: Double,
        after300: Double,
        afterPlus3: Double,
        remarks: String
    ) -> Unit,
    onDeleteTest: (CciTestEntity) -> Unit,
    onShareTestWhatsApp: (CciTestEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLine by remember { mutableStateOf("ALL") }
    var selectedShift by remember { mutableStateOf("ALL") }
    var inspectingTest by remember { mutableStateOf<CciTestEntity?>(null) }

    val filtered = remember(state.tests, searchQuery, selectedLine, selectedShift) {
        state.tests.filter { t ->
            val matchLine = selectedLine == "ALL" || t.crusherLine == selectedLine
            val matchShift = selectedShift == "ALL" || t.shift == selectedShift
            val q = searchQuery.trim().lowercase(Locale.US)
            val matchSearch = q.isEmpty() ||
                t.sampleId.lowercase(Locale.US).contains(q) ||
                t.cokeSource.lowercase(Locale.US).contains(q) ||
                t.operatorName.lowercase(Locale.US).contains(q) ||
                t.remarks.lowercase(Locale.US).contains(q)
            matchLine && matchShift && matchSearch
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search sample ID, coke source, remarks...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tests_search_input")
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "All Lines", AppConstants.LINE_01 to "Line-01", AppConstants.LINE_02 to "Line-02").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedLine == key,
                            onClick = { selectedLine = key },
                            label = { Text(label) }
                        )
                    }
                    listOf("ALL" to "All Shifts", "A" to "Shift A", "B" to "Shift B", "C" to "Shift C").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedShift == key,
                            onClick = { selectedShift = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Showing ${filtered.size} of ${state.tests.size} sieve test records",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Science,
                        title = "No matching CCI tests",
                        subtitle = "Adjust filters or log a new sieve analysis sample."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { test ->
                    CciTestCard(
                        test = test,
                        targetMinCci = state.settings.targetMinCci,
                        onClick = { inspectingTest = test },
                        onShareWhatsApp = { onShareTestWhatsApp(test) }
                    )
                }
            }
        }

        if (state.session.role.canAddTest) {
            ExtendedFloatingActionButton(
                onClick = {
                    onRefreshSampleId(DateTimeUtils.todayIso())
                    onOpenAddDialog()
                },
                containerColor = EmberOrange,
                contentColor = Color.White,
                icon = { Icon(Icons.Rounded.Add, contentDescription = "New CCI Test") },
                text = { Text("New CCI Test") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_cci_test_fab")
            )
        }
    }

    if (showAddDialog) {
        AddCciTestDialog(
            suggestedSampleId = suggestedSampleId,
            defaultLine = state.runningLine,
            defaultOperator = state.session.username,
            targetMinCci = state.settings.targetMinCci,
            onDismiss = onDismissAddDialog,
            onSave = onAddTest
        )
    }

    inspectingTest?.let { test ->
        TestDetailDialog(
            test = test,
            targetMinCci = state.settings.targetMinCci,
            canDelete = state.session.role.canDeleteTest,
            onDismiss = { inspectingTest = null },
            onShareWhatsApp = {
                onShareTestWhatsApp(test)
            },
            onDelete = {
                onDeleteTest(test)
                inspectingTest = null
            }
        )
    }
}

@Composable
private fun CciTestCard(
    test: CciTestEntity,
    targetMinCci: Double,
    onClick: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    val meetsTarget = test.crushingIndex >= targetMinCci
    val statusColor = if (meetsTarget) PlantEmerald else CautionAmber

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("cci_test_card_${test.id}"),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = test.sampleId,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        StatusPill(
                            text = "Shift ${test.shift}",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "${DateTimeUtils.formatDateTimeReadable(test.testDateTime)} • ${test.crusherLine}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "CCI",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", test.crushingIndex)}%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = statusColor
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricMini(label = "Before (-3mm)", value = "${String.format(Locale.US, "%.2f", test.beforeFinesPct)}%")
                MetricMini(label = "After (-3mm)", value = "${String.format(Locale.US, "%.2f", test.afterFinesPct)}%")
                MetricMini(label = "Gain", value = "${String.format(Locale.US, "%+.2f", test.finesIncreasePp)} pp")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${test.cokeType} • ${test.cokeSource}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onShareWhatsApp,
                    modifier = Modifier.testTag("share_test_whatsapp_${test.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Share sample via WhatsApp",
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricMini(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AddCciTestDialog(
    suggestedSampleId: String,
    defaultLine: String,
    defaultOperator: String,
    targetMinCci: Double,
    onDismiss: () -> Unit,
    onSave: (
        sampleId: String,
        testDate: String,
        shift: String,
        crusherLine: String,
        cokeType: String,
        cokeSource: String,
        operatorName: String,
        before050: Double,
        before100: Double,
        before300: Double,
        beforePlus3: Double,
        after050: Double,
        after100: Double,
        after300: Double,
        afterPlus3: Double,
        remarks: String
    ) -> Unit
) {
    var sampleId by remember(suggestedSampleId) { mutableStateOf(suggestedSampleId) }
    var testDate by remember { mutableStateOf(DateTimeUtils.todayIso()) }
    var shift by remember { mutableStateOf(DateTimeUtils.currentShift()) }
    var crusherLine by remember { mutableStateOf(defaultLine) }
    var cokeType by remember { mutableStateOf(AppConstants.COKE_TYPES.first()) }
    var cokeSource by remember { mutableStateOf(AppConstants.COKE_SOURCES.first()) }
    var operatorName by remember { mutableStateOf(defaultOperator) }

    var b05 by remember { mutableStateOf("2.20") }
    var b10 by remember { mutableStateOf("1.90") }
    var b30 by remember { mutableStateOf("1.50") }
    var bPlus by remember { mutableStateOf("14.40") }

    var a05 by remember { mutableStateOf("4.10") }
    var a10 by remember { mutableStateOf("2.50") }
    var a30 by remember { mutableStateOf("2.15") }
    var aPlus by remember { mutableStateOf("11.25") }

    var remarks by remember { mutableStateOf("") }

    val bv05 = b05.toDoubleOrNull() ?: 0.0
    val bv10 = b10.toDoubleOrNull() ?: 0.0
    val bv30 = b30.toDoubleOrNull() ?: 0.0
    val bvPlus = bPlus.toDoubleOrNull() ?: 0.0
    val bTotal = bv05 + bv10 + bv30 + bvPlus

    val av05 = a05.toDoubleOrNull() ?: 0.0
    val av10 = a10.toDoubleOrNull() ?: 0.0
    val av30 = a30.toDoubleOrNull() ?: 0.0
    val avPlus = aPlus.toDoubleOrNull() ?: 0.0
    val aTotal = av05 + av10 + av30 + avPlus

    val beforeFines = if (bTotal > 0.0) ((bv05 + bv10 + bv30) / bTotal) * 100.0 else 0.0
    val afterFines = if (aTotal > 0.0) ((av05 + av10 + av30) / aTotal) * 100.0 else 0.0
    val cci = afterFines
    val gain = afterFines - beforeFines
    val isValid = bTotal > 0.0 && aTotal > 0.0
    val meetsTarget = cci >= targetMinCci

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New CCI Sieve Analysis", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                // Live Calculated Preview Strip
                val statusColor = if (meetsTarget) PlantEmerald else CautionAmber
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LIVE CRUSHING INDEX (CCI)", style = MaterialTheme.typography.labelSmall, color = statusColor)
                            Text(
                                text = "${String.format(Locale.US, "%.2f", cci)}%",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = statusColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Before ${String.format(Locale.US, "%.1f", beforeFines)}% → After ${String.format(Locale.US, "%.1f", afterFines)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Gain: ${String.format(Locale.US, "%+.2f", gain)} pp | ${if (meetsTarget) "OK" else "BELOW TARGET"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sampleId,
                        onValueChange = { sampleId = it },
                        label = { Text("Sample ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = testDate,
                        onValueChange = { testDate = it },
                        label = { Text("Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Crusher Line & Shift", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppConstants.CRUSHER_LINES.forEach { line ->
                        FilterChip(
                            selected = crusherLine == line,
                            onClick = { crusherLine = line },
                            label = { Text(line.replace("Crusher ", "")) }
                        )
                    }
                    AppConstants.SHIFTS.forEach { s ->
                        FilterChip(
                            selected = shift == s,
                            onClick = { shift = s },
                            label = { Text("Shift $s") }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cokeSource,
                        onValueChange = { cokeSource = it },
                        label = { Text("Coke Source") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = operatorName,
                        onValueChange = { operatorName = it },
                        label = { Text("Operator") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                HorizontalDivider()
                Text(
                    "Before Crushing Sieve Weights (kg) — Total: ${String.format(Locale.US, "%.2f", bTotal)} kg",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeightField("-0.5 mm", b05, { b05 = it }, Modifier.weight(1f))
                    WeightField("0.5-1.0", b10, { b10 = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeightField("1.0-3.0", b30, { b30 = it }, Modifier.weight(1f))
                    WeightField("+3.0 mm", bPlus, { bPlus = it }, Modifier.weight(1f))
                }

                HorizontalDivider()
                Text(
                    "After Crushing Sieve Weights (kg) — Total: ${String.format(Locale.US, "%.2f", aTotal)} kg",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeightField("-0.5 mm", a05, { a05 = it }, Modifier.weight(1f))
                    WeightField("0.5-1.0", a10, { a10 = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeightField("1.0-3.0", a30, { a30 = it }, Modifier.weight(1f))
                    WeightField("+3.0 mm", aPlus, { aPlus = it }, Modifier.weight(1f))
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks / Moisture Observation") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = isValid,
                        onClick = {
                            onSave(
                                sampleId, testDate, shift, crusherLine, cokeType, cokeSource,
                                operatorName, bv05, bv10, bv30, bvPlus, av05, av10, av30, avPlus, remarks
                            )
                        },
                        modifier = Modifier.testTag("save_cci_test_button")
                    ) {
                        Text("Save Test Record")
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightField(
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
private fun TestDetailDialog(
    test: CciTestEntity,
    targetMinCci: Double,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onDelete: () -> Unit
) {
    val meetsTarget = test.crushingIndex >= targetMinCci
    val badgeColor = if (meetsTarget) PlantEmerald else CautionAmber

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(test.sampleId, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                    Text(
                        "${test.crusherLine} • Shift ${test.shift}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = "CCI ${String.format(Locale.US, "%.2f", test.crushingIndex)}%",
                    containerColor = badgeColor.copy(alpha = 0.16f),
                    contentColor = badgeColor
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Timestamp: ${DateTimeUtils.formatDateTimeReadable(test.testDateTime)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Coke: ${test.cokeType} (${test.cokeSource}) • Operator: ${test.operatorName}",
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider()
                Text("Sieve Fractions (kg)", style = MaterialTheme.typography.titleSmall)
                SieveCompareRow("-0.5 mm", test.beforeFrac050, test.afterFrac050)
                SieveCompareRow("0.5–1.0 mm", test.beforeFrac100, test.afterFrac100)
                SieveCompareRow("1.0–3.0 mm", test.beforeFrac300, test.afterFrac300)
                SieveCompareRow("+3.0 mm", test.beforeFracPlus3, test.afterFracPlus3)
                HorizontalDivider()
                SieveCompareRow("Total (kg)", test.beforeTotal, test.afterTotal, bold = true)
                Text(
                    text = "Before Fines: ${String.format(Locale.US, "%.2f", test.beforeFinesPct)}%  →  After Fines: ${String.format(Locale.US, "%.2f", test.afterFinesPct)}%  (${String.format(Locale.US, "%+.2f", test.finesIncreasePp)} pp)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (test.remarks.isNotBlank()) {
                    Text(
                        text = "Remarks: ${test.remarks}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onShareWhatsApp,
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("WhatsApp")
            }
        },
        dismissButton = {
            Row {
                if (canDelete) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
private fun SieveCompareRow(label: String, before: Double, after: Double, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "B: ${String.format(Locale.US, "%.2f", before)}",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(84.dp)
        )
        Text(
            text = "A: ${String.format(Locale.US, "%.2f", after)}",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(84.dp)
        )
    }
}
