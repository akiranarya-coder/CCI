package com.example.cci.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cci.core.constants.AppConstants
import com.example.cci.core.utils.DateTimeUtils
import com.example.cci.data.local.entity.ChangeoverEntity
import com.example.cci.data.local.entity.MaintenanceEntity
import com.example.cci.ui.CciUiState
import com.example.cci.ui.components.EmptyStateCard
import com.example.cci.ui.components.SectionHeader
import com.example.cci.ui.components.StatusPill
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.PlantEmerald

@Composable
fun OperationsScreen(
    state: CciUiState,
    showChangeoverDialog: Boolean,
    onOpenChangeoverDialog: () -> Unit,
    onDismissChangeoverDialog: () -> Unit,
    onExecuteChangeover: (newRunning: String, newStandby: String, reason: String, manualOverride: Boolean, remarks: String) -> Unit,
    onAddMaintenance: (line: String, date: String, type: String, details: String, measurements: String, condition: String, nextDue: String) -> Unit,
    onApproveMaintenance: (MaintenanceEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddMaintDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Line Changeover") },
                icon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("ops_tab_changeover")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Maintenance") },
                icon = { Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("ops_tab_maintenance")
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedTab == 0) {
                ChangeoverTabContent(
                    state = state,
                    onExecuteClick = onOpenChangeoverDialog
                )
            } else {
                MaintenanceTabContent(
                    state = state,
                    onApprove = onApproveMaintenance
                )
                if (state.session.role.canLogMaintenance) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddMaintDialog = true },
                        containerColor = EmberOrange,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Rounded.Add, contentDescription = "Log Maintenance") },
                        text = { Text("Log Maintenance") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("add_maintenance_fab")
                    )
                }
            }
        }
    }

    if (showChangeoverDialog) {
        ExecuteChangeoverDialog(
            currentRunning = state.runningLine,
            currentStandby = state.standbyLine,
            onDismiss = onDismissChangeoverDialog,
            onConfirm = onExecuteChangeover
        )
    }

    if (showAddMaintDialog) {
        AddMaintenanceDialog(
            defaultLine = state.standbyLine,
            onDismiss = { showAddMaintDialog = false },
            onSave = { line, date, type, details, measurements, condition, nextDue ->
                onAddMaintenance(line, date, type, details, measurements, condition, nextDue)
                showAddMaintDialog = false
            }
        )
    }
}

@Composable
private fun ChangeoverTabContent(
    state: CciUiState,
    onExecuteClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    Text("Twin Crusher Line Allocation", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LineStatusTile(
                            title = state.runningLine,
                            status = "RUNNING",
                            color = PlantEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        LineStatusTile(
                            title = state.standbyLine,
                            status = "STANDBY",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    state.crusherStatus?.let { st ->
                        Text(
                            text = "Last Changeover: ${DateTimeUtils.formatDateTimeReadable(st.lastChangeoverAt)} by ${st.updatedBy}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.session.role.canChangeover) {
                        Button(
                            onClick = onExecuteClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("execute_changeover_button")
                        ) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch Running Line to ${state.standbyLine}")
                        }
                    } else {
                        Text(
                            text = "Role '${state.session.role.displayName}' has view-only access to line changeovers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Changeover Register",
                subtitle = "${state.changeovers.size} recorded rotations"
            )
        }

        if (state.changeovers.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.SwapHoriz,
                    title = "No changeovers logged",
                    subtitle = "Switch crusher lines above to start building changeover history."
                )
            }
        } else {
            items(state.changeovers, key = { it.id }) { item ->
                ChangeoverHistoryCard(item)
            }
        }
    }
}

@Composable
private fun LineStatusTile(
    title: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(status, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ChangeoverHistoryCard(item: ChangeoverEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.previousLine.replace("Crusher ", "")} → ${item.newLine.replace("Crusher ", "")}",
                    style = MaterialTheme.typography.titleMedium
                )
                if (item.manualOverride) {
                    StatusPill(
                        text = "MANUAL OVERRIDE",
                        containerColor = CautionAmber.copy(alpha = 0.16f),
                        contentColor = CautionAmber
                    )
                }
            }
            Text(
                text = item.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${DateTimeUtils.formatDateTimeReadable(item.eventDateTime)} • Authorized by ${item.authorizedBy}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.remarks.isNotBlank()) {
                Text(
                    text = item.remarks,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MaintenanceTabContent(
    state: CciUiState,
    onApprove: (MaintenanceEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "Crusher Maintenance Log",
                subtitle = "Roller turning, gap calibration & preventive checks"
            )
        }

        if (state.maintenance.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Build,
                    title = "No maintenance logs yet",
                    subtitle = "Tap Log Maintenance to record roller gap calibrations or inspections."
                )
            }
        } else {
            items(state.maintenance, key = { it.id }) { record ->
                MaintenanceRecordCard(
                    record = record,
                    canApprove = state.session.role.canApproveMaintenance,
                    onApprove = { onApprove(record) }
                )
            }
        }
    }
}

@Composable
private fun MaintenanceRecordCard(
    record: MaintenanceEntity,
    canApprove: Boolean,
    onApprove: () -> Unit
) {
    val isApproved = record.approvedBy.isNotBlank()
    val statusColor = if (isApproved) PlantEmerald else CautionAmber

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.activityType, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${record.crusherLine} • ${DateTimeUtils.formatDisplayDate(record.activityDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = if (isApproved) "APPROVED" else "PENDING",
                    containerColor = statusColor.copy(alpha = 0.14f),
                    contentColor = statusColor
                )
            }

            Text(record.details, style = MaterialTheme.typography.bodyMedium)

            if (record.measurements.isNotBlank()) {
                Text(
                    text = "Measurements: ${record.measurements}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (record.rollerCondition.isNotBlank()) {
                Text(
                    text = "Roller Condition: ${record.rollerCondition}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append("By ${record.completedBy}")
                        if (isApproved) append(" • Approved: ${record.approvedBy}")
                        if (record.nextDueDate.isNotBlank()) append(" • Due: ${DateTimeUtils.formatShortDate(record.nextDueDate)}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (!isApproved && canApprove) {
                    OutlinedButton(
                        onClick = onApprove,
                        modifier = Modifier.testTag("approve_maint_${record.id}")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecuteChangeoverDialog(
    currentRunning: String,
    currentStandby: String,
    onDismiss: () -> Unit,
    onConfirm: (newRunning: String, newStandby: String, reason: String, manualOverride: Boolean, remarks: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(AppConstants.CHANGEOVER_REASONS.first()) }
    var manualOverride by remember { mutableStateOf(false) }
    var remarks by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Execute Line Changeover", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Switching running line from $currentRunning to $currentStandby.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Reason", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppConstants.CHANGEOVER_REASONS.forEach { r ->
                        FilterChip(
                            selected = selectedReason == r,
                            onClick = { selectedReason = r },
                            label = { Text(r) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manual Override", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Mark if switching ahead of scheduled interval",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = manualOverride, onCheckedChange = { manualOverride = it })
                }
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Handover Remarks") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentStandby, currentRunning, selectedReason, manualOverride, remarks) },
                colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_changeover_button")
            ) {
                Text("Confirm Changeover")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddMaintenanceDialog(
    defaultLine: String,
    onDismiss: () -> Unit,
    onSave: (line: String, date: String, type: String, details: String, measurements: String, condition: String, nextDue: String) -> Unit
) {
    var crusherLine by remember { mutableStateOf(defaultLine) }
    var activityDate by remember { mutableStateOf(DateTimeUtils.todayIso()) }
    var activityType by remember { mutableStateOf(AppConstants.MAINTENANCE_TYPES.first()) }
    var details by remember { mutableStateOf("") }
    var measurements by remember { mutableStateOf("") }
    var rollerCondition by remember { mutableStateOf(AppConstants.ROLLER_CONDITIONS.first()) }
    var nextDue by remember { mutableStateOf(DateTimeUtils.daysFromNowIso(30)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Maintenance Activity", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppConstants.CRUSHER_LINES.forEach { line ->
                        FilterChip(
                            selected = crusherLine == line,
                            onClick = { crusherLine = line },
                            label = { Text(line.replace("Crusher ", "")) }
                        )
                    }
                }
                Text("Activity Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppConstants.MAINTENANCE_TYPES.forEach { t ->
                        FilterChip(
                            selected = activityType == t,
                            onClick = { activityType = t },
                            label = { Text(t) }
                        )
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Work Details *") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = measurements,
                    onValueChange = { measurements = it },
                    label = { Text("Measurements (e.g., Gap: 3.0 mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Roller Condition", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppConstants.ROLLER_CONDITIONS.forEach { cond ->
                        FilterChip(
                            selected = rollerCondition == cond,
                            onClick = { rollerCondition = cond },
                            label = { Text(cond) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = activityDate,
                        onValueChange = { activityDate = it },
                        label = { Text("Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = nextDue,
                        onValueChange = { nextDue = it },
                        label = { Text("Next Due") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = details.isNotBlank(),
                onClick = { onSave(crusherLine, activityDate, activityType, details, measurements, rollerCondition, nextDue) },
                modifier = Modifier.testTag("save_maintenance_button")
            ) {
                Text("Save Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
