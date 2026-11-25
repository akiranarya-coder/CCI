package com.example.cci.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cci.core.utils.DateTimeUtils
import com.example.cci.data.local.entity.AlertEntity
import com.example.cci.data.local.entity.CciTestEntity
import com.example.cci.ui.CciUiState
import com.example.cci.ui.components.CciTrendChart
import com.example.cci.ui.components.KpiMetricCard
import com.example.cci.ui.components.SectionHeader
import com.example.cci.ui.components.StatusPill
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.EmberBright
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.FoundryNavy
import com.example.ui.theme.FoundrySteel
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlantEmerald
import com.example.ui.theme.PlantEmeraldDark
import com.example.ui.theme.WhatsAppDeepGreen
import com.example.ui.theme.WhatsAppGreen
import java.util.Locale

@Composable
fun DashboardScreen(
    state: CciUiState,
    onQuickShareDailyWhatsApp: () -> Unit,
    onOpenReportsTab: () -> Unit,
    onOpenChangeover: () -> Unit,
    onOpenNewTest: () -> Unit,
    onOpenTestsTab: () -> Unit,
    onAcknowledgeAlert: (Long) -> Unit,
    onShareTestWhatsApp: (CciTestEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Crusher Plant Control Hero Card
        item {
            CrusherLineHeroCard(
                runningLine = state.runningLine,
                standbyLine = state.standbyLine,
                lastChangeoverAt = state.crusherStatus?.lastChangeoverAt ?: "",
                canChangeover = state.session.role.canChangeover,
                canAddTest = state.session.role.canAddTest,
                onChangeoverClick = onOpenChangeover,
                onNewTestClick = onOpenNewTest
            )
        }

        // 2. One-Tap Daily WhatsApp Report Card
        item {
            DailyWhatsAppQuickCard(
                todayCount = state.todayTests.size,
                todayAvgCci = state.todayAvgCci,
                onShareNow = onQuickShareDailyWhatsApp,
                onCustomizeReport = onOpenReportsTab
            )
        }

        // 3. 2x2 Operational KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val cciVal = state.todayAvgCci ?: state.overallAvgCci
                    val isToday = state.todayAvgCci != null
                    val cciGood = cciVal == null || cciVal >= state.settings.targetMinCci
                    KpiMetricCard(
                        label = if (isToday) "Today Avg CCI" else "Recent Avg CCI",
                        value = if (cciVal != null) "${String.format(Locale.US, "%.2f", cciVal)}%" else "--",
                        caption = "Target >= ${String.format(Locale.US, "%.1f", state.settings.targetMinCci)}%",
                        accentColor = if (cciGood) PlantEmerald else CautionAmber,
                        icon = Icons.Rounded.Speed,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = "Today Tests",
                        value = "${state.todayTests.size}",
                        caption = "Total logged: ${state.tests.size}",
                        accentColor = EmberOrange,
                        icon = Icons.Rounded.Science,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val gainVal = state.todayAvgFinesGain
                    KpiMetricCard(
                        label = "Avg Fines Gain",
                        value = if (gainVal != null) "${String.format(Locale.US, "%+.2f", gainVal)} pp" else "--",
                        caption = "Before -> After (-3mm)",
                        accentColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = "Active Alerts",
                        value = "${state.activeAlerts.size}",
                        caption = if (state.activeAlerts.isEmpty()) "All lines nominal" else "Needs attention",
                        accentColor = if (state.activeAlerts.isEmpty()) PlantEmerald else AlertCrimson,
                        icon = Icons.Rounded.NotificationsActive,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Custom Canvas CCI Performance Trend Chart
        item {
            CciTrendChart(
                tests = state.tests,
                targetMinCci = state.settings.targetMinCci
            )
        }

        // 5. Active Alerts (if any)
        if (state.activeAlerts.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Active Plant Alerts",
                    subtitle = "Unacknowledged quality or crusher threshold notices"
                )
            }
            items(state.activeAlerts, key = { "alert_${it.id}" }) { alert ->
                AlertBannerCard(
                    alert = alert,
                    onAcknowledge = { onAcknowledgeAlert(alert.id) }
                )
            }
        }

        // 6. Recent Sieve Tests
        item {
            SectionHeader(
                title = "Recent Sieve Tests",
                subtitle = "Latest coke crushing index samples",
                actionLabel = "View All",
                onActionClick = onOpenTestsTab
            )
        }

        items(state.tests.take(4), key = { "dash_test_${it.id}" }) { test ->
            RecentTestCompactCard(
                test = test,
                targetMinCci = state.settings.targetMinCci,
                onShareWhatsApp = { onShareTestWhatsApp(test) }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun CrusherLineHeroCard(
    runningLine: String,
    standbyLine: String,
    lastChangeoverAt: String,
    canChangeover: Boolean,
    canAddTest: Boolean,
    onChangeoverClick: () -> Unit,
    onNewTestClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(FoundryNavy, FoundrySteel, Color(0xFF23415C))
                )
            )
    ) {
        // Subtle technical blueprint grid overlay
        Canvas(modifier = Modifier.matchParentSize()) {
            val step = 28.dp.toPx()
            val gridColor = Color.White.copy(alpha = 0.045f)
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PlantEmeraldDark)
                    )
                    Text(
                        text = "ACTIVE CRUSHER LINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = PlantEmeraldDark,
                        letterSpacing = 1.sp
                    )
                }
                StatusPill(
                    text = "SHIFT ${DateTimeUtils.currentShift()}",
                    containerColor = EmberBright.copy(alpha = 0.22f),
                    contentColor = EmberBright
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = runningLine,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Standby: $standbyLine",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFBBD0E6)
                    )
                    if (lastChangeoverAt.isNotBlank()) {
                        Text(
                            text = "Since ${DateTimeUtils.formatDateTimeReadable(lastChangeoverAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8CA8C4)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (canAddTest) {
                    Button(
                        onClick = onNewTestClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmberOrange,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_new_test_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New CCI Test")
                    }
                }
                if (canChangeover) {
                    OutlinedButton(
                        onClick = onChangeoverClick,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_changeover_button")
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Changeover")
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyWhatsAppQuickCard(
    todayCount: Int,
    todayAvgCci: Double?,
    onShareNow: () -> Unit,
    onCustomizeReport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, WhatsAppGreen.copy(alpha = 0.45f))
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Plant Report • WhatsApp",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val summary = if (todayCount > 0 && todayAvgCci != null) {
                        "${DateTimeUtils.formatDisplayDate(DateTimeUtils.todayIso())} • $todayCount samples • Avg CCI ${String.format(Locale.US, "%.2f", todayAvgCci)}%"
                    } else {
                        "${DateTimeUtils.formatDisplayDate(DateTimeUtils.todayIso())} • Ready to share plant status"
                    }
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShareNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("dashboard_whatsapp_share_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share via WhatsApp")
                }
                FilledTonalButton(
                    onClick = onCustomizeReport,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("dashboard_open_reports_button")
                ) {
                    Text("Preview / PDF")
                }
            }
        }
    }
}

@Composable
private fun AlertBannerCard(
    alert: AlertEntity,
    onAcknowledge: () -> Unit
) {
    val isCritical = alert.severity == "CRITICAL"
    val tint = if (isCritical) AlertCrimson else CautionAmber
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = alert.severity,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = DateTimeUtils.formatDateTimeReadable(alert.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onAcknowledge,
                modifier = Modifier.testTag("acknowledge_alert_${alert.id}")
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Acknowledge", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ack")
            }
        }
    }
}

@Composable
private fun RecentTestCompactCard(
    test: CciTestEntity,
    targetMinCci: Double,
    onShareWhatsApp: () -> Unit
) {
    val ok = test.crushingIndex >= targetMinCci
    val badgeColor = if (ok) PlantEmerald else CautionAmber

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = test.sampleId,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusPill(
                        text = "S-${test.shift}",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${test.crusherLine} • ${DateTimeUtils.formatDateTimeReadable(test.testDateTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Before ${String.format(Locale.US, "%.1f", test.beforeFinesPct)}% → After ${String.format(Locale.US, "%.1f", test.afterFinesPct)}% (${String.format(Locale.US, "%+.2f", test.finesIncreasePp)} pp)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", test.crushingIndex)}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = "WhatsApp",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhatsAppDeepGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onShareWhatsApp)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
