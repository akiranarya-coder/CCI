package com.example.cci.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.cci.core.constants.UserRole
import com.example.cci.core.utils.DateTimeUtils
import com.example.cci.data.local.entity.UserEntity
import com.example.cci.ui.CciUiState
import com.example.cci.ui.components.SectionHeader
import com.example.cci.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlantEmerald
import java.util.Locale

@Composable
fun SettingsScreen(
    state: CciUiState,
    onSwitchUserClick: () -> Unit,
    onSyncToCloud: () -> Unit,
    onSignOut: () -> Unit,
    onSavePlantConfig: (company: String, dept: String, minCci: Double, maxGain: Double, signOff: String) -> Unit,
    onSetThemeMode: (String) -> Unit,
    onCreateUser: (username: String, fullName: String, password: String, role: UserRole) -> Unit,
    onToggleUserActive: (UserEntity) -> Unit,
    onExportJsonBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var companyName by remember(state.settings.companyName) { mutableStateOf(state.settings.companyName) }
    var departmentName by remember(state.settings.departmentName) { mutableStateOf(state.settings.departmentName) }
    var targetMinCci by remember(state.settings.targetMinCci) { mutableStateOf(String.format(Locale.US, "%.1f", state.settings.targetMinCci)) }
    var maxFinesGain by remember(state.settings.maxFinesIncreasePp) { mutableStateOf(String.format(Locale.US, "%.1f", state.settings.maxFinesIncreasePp)) }
    var signOff by remember(state.settings.whatsappSignOff) { mutableStateOf(state.settings.whatsappSignOff) }
    var showCreateUserDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Firebase Cloud Account & Real-Time Firestore Sync
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (state.cloudSync.isConnected) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                                contentDescription = "Cloud sync status",
                                tint = if (state.cloudSync.isConnected) PlantEmerald else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = state.cloudUserName.ifBlank { "Google Account" },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = state.cloudUserEmail.ifBlank { "Signed in with Google" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusPill(
                            text = if (state.cloudSync.isConnected) "Cloud Live" else "Offline",
                            containerColor = if (state.cloudSync.isConnected) {
                                PlantEmerald.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            contentColor = if (state.cloudSync.isConnected) {
                                PlantEmerald
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }

                    Text(
                        text = "Cloud Firestore: ${state.cloudSync.syncedTestCount} tests • ${state.cloudSync.syncedReportCount} reports • ${state.cloudSync.syncedChangeoverCount} changeovers • ${state.cloudSync.syncedMaintenanceCount} maintenance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    state.cloudSync.lastError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSyncToCloud,
                            enabled = !state.cloudSync.isSyncing,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_to_cloud_button")
                        ) {
                            if (state.cloudSync.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.cloudSync.isSyncing) "Syncing..." else "Sync to Cloud")
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("firebase_sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }

        // 1. Active User Profile & Role Switch
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = "Active user",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Column {
                            Text(state.session.fullName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "@${state.session.username} • ${state.session.role.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onSwitchUserClick,
                        modifier = Modifier.testTag("settings_switch_user_button")
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Switch")
                    }
                }
            }
        }

        // 2. Display Theme Mode
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Display Appearance", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (mode, label) ->
                            FilterChip(
                                selected = state.settings.themeMode.equals(mode, ignoreCase = true),
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Plant Configuration & WhatsApp Sign-Off
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Plant & Report Configuration", style = MaterialTheme.typography.titleMedium)
                    val editable = state.session.role.canEditSettings

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company Name") },
                        enabled = editable,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = departmentName,
                        onValueChange = { departmentName = it },
                        label = { Text("Plant / Department") },
                        enabled = editable,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetMinCci,
                            onValueChange = { targetMinCci = it },
                            label = { Text("Target Min CCI (%)") },
                            enabled = editable,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxFinesGain,
                            onValueChange = { maxFinesGain = it },
                            label = { Text("Max Fines Gain (pp)") },
                            enabled = editable,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = signOff,
                        onValueChange = { signOff = it },
                        label = { Text("WhatsApp Report Sign-Off") },
                        enabled = editable,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (editable) {
                        Button(
                            onClick = {
                                onSavePlantConfig(
                                    companyName,
                                    departmentName,
                                    targetMinCci.toDoubleOrNull() ?: 40.0,
                                    maxFinesGain.toDoubleOrNull() ?: 22.0,
                                    signOff
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_plant_settings_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Configuration")
                        }
                    } else {
                        Text(
                            text = "Sign in as Shift In-Charge or Admin to modify thresholds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Local Backup Export
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Offline Data Backup", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "All CCI tests, changeovers, and maintenance logs are stored locally on this device. Export a JSON snapshot regularly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onExportJsonBackup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_json_backup_button")
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Local Backup (JSON)")
                    }
                }
            }
        }

        // 5. User Accounts (Admin Management)
        if (state.session.role.canManageUsers) {
            item {
                SectionHeader(
                    title = "Plant User Accounts",
                    subtitle = "Manage role-based device access",
                    actionLabel = "+ Add User",
                    onActionClick = { showCreateUserDialog = true }
                )
            }
            items(state.users, key = { "usr_${it.id}" }) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.fullName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "@${user.username} • ${UserRole.fromName(user.role).displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = user.isActive,
                            onCheckedChange = { onToggleUserActive(user) }
                        )
                    }
                }
            }
        }

        // 6. Recent Audit Trail
        item {
            SectionHeader(
                title = "Audit Trail",
                subtitle = "Latest ${state.auditLogs.take(12).size} recorded plant actions"
            )
        }
        items(state.auditLogs.take(12), key = { "audit_${it.id}" }) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${log.action} • ${log.username}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = log.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = DateTimeUtils.formatShortDate(log.timestamp.substringBefore("T")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showCreateUserDialog) {
        CreateUserDialog(
            onDismiss = { showCreateUserDialog = false },
            onCreate = { u, fn, pw, role ->
                onCreateUser(u, fn, pw, role)
                showCreateUserDialog = false
            }
        )
    }
}

@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreate: (username: String, fullName: String, password: String, role: UserRole) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.OPERATOR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                Text("Add Plant User", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min 4 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Role", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UserRole.entries.forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = username.trim().length >= 3 && password.length >= 4,
                onClick = { onCreate(username, fullName, password, role) }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
