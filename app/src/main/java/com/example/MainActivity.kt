package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cci.CciApplication
import com.example.cci.ui.CciViewModel
import com.example.cci.ui.MainDestination
import com.example.cci.ui.components.SwitchUserDialog
import com.example.cci.ui.screens.DashboardScreen
import com.example.cci.ui.screens.OperationsScreen
import com.example.cci.ui.screens.ReportsScreen
import com.example.cci.ui.screens.SettingsScreen
import com.example.cci.ui.screens.TestsScreen
import com.example.ui.theme.EvonithCciTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CciApplication
        setContent {
            val viewModel: CciViewModel = viewModel(
                factory = CciViewModel.factory(app, app.container)
            )
            EvonithCciApp(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvonithCciApp(viewModel: CciViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val suggestedSampleId by viewModel.suggestedSampleId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var currentDestination by rememberSaveable { mutableStateOf(MainDestination.DASHBOARD) }
    var showSwitchUserDialog by remember { mutableStateOf(false) }
    var showAddTestDialog by remember { mutableStateOf(false) }
    var showChangeoverDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Return to Dashboard on system back press from any secondary tab
    BackHandler(enabled = currentDestination != MainDestination.DASHBOARD) {
        currentDestination = MainDestination.DASHBOARD
    }

    EvonithCciTheme(themeMode = state.settings.themeMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 640.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_evonith_cci_logo),
                                    contentDescription = "Evonith CCI logo",
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Column {
                                    Text(
                                        text = "Evonith CCI",
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = state.settings.departmentName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        actions = {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showSwitchUserDialog = true }
                                    .testTag("topbar_user_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = "Switch user",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = state.session.username,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp
                        ) {
                            MainDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = destinationIcon(dest, selected),
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            MainDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = destinationIcon(dest, selected),
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("rail_${dest.route}")
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val contentModifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)

                        when (currentDestination) {
                            MainDestination.DASHBOARD -> DashboardScreen(
                                state = state,
                                onQuickShareDailyWhatsApp = { viewModel.shareTodayQuickWhatsApp(context) },
                                onOpenReportsTab = { currentDestination = MainDestination.REPORTS },
                                onOpenChangeover = {
                                    currentDestination = MainDestination.OPERATIONS
                                    showChangeoverDialog = true
                                },
                                onOpenNewTest = {
                                    viewModel.refreshNextSampleId()
                                    currentDestination = MainDestination.TESTS
                                    showAddTestDialog = true
                                },
                                onOpenTestsTab = { currentDestination = MainDestination.TESTS },
                                onAcknowledgeAlert = { viewModel.acknowledgeAlert(it) },
                                onShareTestWhatsApp = { viewModel.shareSingleTestViaWhatsApp(context, it) },
                                modifier = contentModifier
                            )

                            MainDestination.TESTS -> TestsScreen(
                                state = state,
                                suggestedSampleId = suggestedSampleId,
                                showAddDialog = showAddTestDialog,
                                onOpenAddDialog = { showAddTestDialog = true },
                                onDismissAddDialog = { showAddTestDialog = false },
                                onRefreshSampleId = { viewModel.refreshNextSampleId(it) },
                                onAddTest = { sId, d, sh, ln, ct, cs, op, b05, b10, b30, bp, a05, a10, a30, ap, rem ->
                                    viewModel.addCciTest(
                                        sampleId = sId, testDate = d, shift = sh, crusherLine = ln,
                                        cokeType = ct, cokeSource = cs, operatorName = op,
                                        before050 = b05, before100 = b10, before300 = b30, beforePlus3 = bp,
                                        after050 = a05, after100 = a10, after300 = a30, afterPlus3 = ap,
                                        remarks = rem,
                                        onSuccess = { showAddTestDialog = false }
                                    )
                                },
                                onDeleteTest = { viewModel.deleteTest(it) },
                                onShareTestWhatsApp = { viewModel.shareSingleTestViaWhatsApp(context, it) },
                                modifier = contentModifier
                            )

                            MainDestination.OPERATIONS -> OperationsScreen(
                                state = state,
                                showChangeoverDialog = showChangeoverDialog,
                                onOpenChangeoverDialog = { showChangeoverDialog = true },
                                onDismissChangeoverDialog = { showChangeoverDialog = false },
                                onExecuteChangeover = { newRun, newStdby, reason, manual, rem ->
                                    viewModel.executeChangeover(
                                        newRunningLine = newRun,
                                        newStandbyLine = newStdby,
                                        reason = reason,
                                        manualOverride = manual,
                                        remarks = rem,
                                        onSuccess = { showChangeoverDialog = false }
                                    )
                                },
                                onAddMaintenance = { ln, dt, tp, det, meas, cond, due ->
                                    viewModel.addMaintenance(ln, dt, tp, det, meas, cond, due) {}
                                },
                                onApproveMaintenance = { viewModel.approveMaintenance(it) },
                                modifier = contentModifier
                            )

                            MainDestination.REPORTS -> ReportsScreen(
                                state = state,
                                onShareWhatsAppText = { viewModel.shareTextReportViaWhatsApp(context, it) },
                                onCopyReportText = { viewModel.copyReportText(context, it) },
                                onSaveDailyReport = { date, shiftScope, gap, feed, pressure, moisture, tests, chgCount, mntCount, rem, summary ->
                                    viewModel.saveDailyReport(
                                        reportDate = date,
                                        shiftScope = shiftScope,
                                        rollerGapMm = gap,
                                        feedRateTph = feed,
                                        rollerPressureBar = pressure,
                                        cokeMoisturePct = moisture,
                                        tests = tests,
                                        changeoverCount = chgCount,
                                        maintenanceCount = mntCount,
                                        remarks = rem,
                                        summaryText = summary
                                    )
                                },
                                onDeleteDailyReport = { viewModel.deleteDailyReport(it) },
                                onShareSavedDailyReport = { viewModel.shareSavedDailyReportViaWhatsApp(context, it) },
                                onExportReport = { rt, fmt, start, end, tests, chg, mnt, viaWa, cap ->
                                    viewModel.exportAndShareReport(
                                        context = context,
                                        reportType = rt,
                                        format = fmt,
                                        startDate = start,
                                        endDate = end,
                                        filteredTests = tests,
                                        filteredChangeovers = chg,
                                        filteredMaintenance = mnt,
                                        viaWhatsApp = viaWa,
                                        whatsappCaption = cap
                                    )
                                },
                                modifier = contentModifier
                            )

                            MainDestination.SETTINGS -> SettingsScreen(
                                state = state,
                                onSwitchUserClick = { showSwitchUserDialog = true },
                                onSavePlantConfig = { co, dept, minCci, maxGain, signOff ->
                                    viewModel.savePlantConfig(co, dept, minCci, maxGain, signOff)
                                },
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onCreateUser = { u, fn, pw, role ->
                                    viewModel.createUser(u, fn, pw, role) {}
                                },
                                onToggleUserActive = { viewModel.toggleUserActive(it) },
                                onExportJsonBackup = { viewModel.exportJsonBackup(context) },
                                modifier = contentModifier
                            )
                        }
                    }
                }
            }

            if (showSwitchUserDialog) {
                SwitchUserDialog(
                    currentUsername = state.session.username,
                    onDismiss = { showSwitchUserDialog = false },
                    onLogin = { u, p, onError ->
                        viewModel.login(
                            username = u,
                            password = p,
                            onSuccess = { showSwitchUserDialog = false },
                            onError = onError
                        )
                    }
                )
            }
        }
    }
}

private fun destinationIcon(dest: MainDestination, selected: Boolean): ImageVector = when (dest) {
    MainDestination.DASHBOARD -> if (selected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard
    MainDestination.TESTS -> if (selected) Icons.Filled.Science else Icons.Outlined.Science
    MainDestination.OPERATIONS -> if (selected) Icons.Filled.Build else Icons.Outlined.Build
    MainDestination.REPORTS -> if (selected) Icons.Filled.Description else Icons.Outlined.Description
    MainDestination.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}
