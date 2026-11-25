package com.example.cci.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cci.core.constants.AppConstants
import com.example.cci.core.utils.DateTimeUtils
import com.example.cci.data.local.entity.CciTestEntity
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlantEmerald
import java.util.Locale

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun KpiMetricCard(
    label: String,
    value: String,
    caption: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                Text(
                    text = label.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CciTrendChart(
    tests: List<CciTestEntity>,
    targetMinCci: Double,
    modifier: Modifier = Modifier
) {
    val ordered = remember(tests) { tests.take(10).reversed() }
    val primaryColor = MaterialTheme.colorScheme.secondary
    val line2Color = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val targetColor = PlantEmerald
    val surfaceColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CCI % Trend (Recent Samples)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Target >= ${String.format(Locale.US, "%.1f", targetMinCci)}% (-3mm fines)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendDot(color = EmberOrange, label = "L-01")
                    LegendDot(color = line2Color, label = "L-02")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (ordered.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log at least 2 CCI tests to visualize trend",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val values = ordered.map { it.crushingIndex }
                val minVal = (minOf(values.min(), targetMinCci) - 4.0).coerceAtLeast(0.0)
                val maxVal = (maxOf(values.max(), targetMinCci) + 4.0).coerceAtMost(100.0)
                val span = (maxVal - minVal).coerceAtLeast(5.0)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val padLeft = 12.dp.toPx()
                        val padRight = 12.dp.toPx()
                        val padTop = 14.dp.toPx()
                        val padBottom = 16.dp.toPx()
                        val chartW = (size.width - padLeft - padRight).coerceAtLeast(1f)
                        val chartH = (size.height - padTop - padBottom).coerceAtLeast(1f)

                        fun yFor(v: Double): Float {
                            val norm = ((v - minVal) / span).toFloat().coerceIn(0f, 1f)
                            return padTop + chartH * (1f - norm)
                        }

                        // Subtle horizontal guide lines
                        for (i in 0..3) {
                            val y = padTop + (chartH / 3f) * i
                            drawLine(
                                color = gridColor.copy(alpha = 0.6f),
                                start = Offset(padLeft, y),
                                end = Offset(padLeft + chartW, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Target threshold dashed line
                        val targetY = yFor(targetMinCci)
                        drawLine(
                            color = targetColor,
                            start = Offset(padLeft, targetY),
                            end = Offset(padLeft + chartW, targetY),
                            strokeWidth = 1.8.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )

                        val points = ordered.mapIndexed { idx, t ->
                            val x = padLeft + (chartW * idx.toFloat() / (ordered.size - 1).coerceAtLeast(1))
                            val y = yFor(t.crushingIndex)
                            Offset(x, y)
                        }

                        // Area fill under trend
                        val fillPath = Path().apply {
                            moveTo(points.first().x, padTop + chartH)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, padTop + chartH)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.22f),
                                    primaryColor.copy(alpha = 0.0f)
                                ),
                                startY = padTop,
                                endY = padTop + chartH
                            )
                        )

                        // Connecting polyline
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Data points colored by line
                        points.forEachIndexed { idx, pt ->
                            val test = ordered[idx]
                            val dotColor = if (test.crusherLine == AppConstants.LINE_01) EmberOrange else line2Color
                            val ringColor = if (test.crushingIndex < targetMinCci) CautionAmber else surfaceColor
                            drawCircle(color = ringColor, radius = 6.dp.toPx(), center = pt)
                            drawCircle(color = dotColor, radius = 4.dp.toPx(), center = pt)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val first = ordered.first()
                    val mid = ordered[ordered.size / 2]
                    val last = ordered.last()
                    Text(
                        text = "${DateTimeUtils.formatShortDate(first.testDate)} S-${first.shift}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${DateTimeUtils.formatShortDate(mid.testDate)} S-${mid.shift}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${DateTimeUtils.formatShortDate(last.testDate)} S-${last.shift} (${String.format(Locale.US, "%.1f", last.crushingIndex)}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwitchUserDialog(
    currentUsername: String,
    onDismiss: () -> Unit,
    onLogin: (username: String, password: String, onError: (String) -> Unit) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val quickAccounts = listOf(
        "admin" to "admin123",
        "shiftincharge" to "shift123",
        "operator" to "oper123",
        "maintenance" to "maint123",
        "management" to "mgmt123"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Switch Plant User", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Active: $currentUsername",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quick-fill plant role credentials:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickAccounts.forEach { (user, pass) ->
                        SuggestionChip(
                            onClick = {
                                username = user
                                password = pass
                                errorMsg = null
                            },
                            label = { Text(user, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMsg = null },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_username_input")
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMsg = null },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onLogin(username, password) { err -> errorMsg = err }
                },
                modifier = Modifier.testTag("login_submit_button")
            ) {
                Text("Sign In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
