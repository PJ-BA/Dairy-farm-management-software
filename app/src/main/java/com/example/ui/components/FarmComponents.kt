package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.state.DailyMilkPoint
import com.example.ui.state.PeriodFinancePoint
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmptyFarmState(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onAction) {
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
fun MilkTrendChart(
    points: List<DailyMilkPoint>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }
    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(points) { animateTrigger = true }
    val animProgress by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 550),
        label = "milkChartAnim"
    )

    val maxVal = max(10.0, points.maxOfOrNull { it.totalLitres } ?: 10.0)
    val amColor = EmeraldDeep
    val pmColor = AmberGold
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val lineColor = EmeraldLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "7-Day Production Trend",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val selected = points.getOrNull(selectedIndex)
                if (selected != null) {
                    Text(
                        text = "${selected.date}: ${String.format(Locale.US, "%.1f", selected.totalLitres)} L (AM ${String.format(Locale.US, "%.1f", selected.morningLitres)} / PM ${String.format(Locale.US, "%.1f", selected.eveningLitres)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(color = amColor, label = "AM")
                LegendDot(color = pmColor, label = "PM")
            }
        }

        if (points.all { it.totalLitres == 0.0 }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Log daily milk yields to visualize your 7-day trend curve.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isNotEmpty()) {
                                val slot = size.width / points.size.toFloat()
                                val idx = (offset.x / slot).toInt().coerceIn(0, points.lastIndex)
                                selectedIndex = idx
                            }
                        }
                    }
            ) {
                val count = points.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.44f).coerceAtMost(38.dp.toPx())
                val chartHeight = size.height - 16.dp.toPx()
                val topPad = 8.dp.toPx()

                // Horizontal reference gridlines
                for (i in 0..3) {
                    val y = topPad + (chartHeight / 3f) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val trendPath = Path()
                points.forEachIndexed { idx, pt ->
                    val centerX = slotWidth * idx + slotWidth / 2f
                    val totalH = ((pt.totalLitres / maxVal).toFloat() * chartHeight * animProgress)
                    val pmH = ((pt.eveningLitres / maxVal).toFloat() * chartHeight * animProgress)
                    val amH = (totalH - pmH).coerceAtLeast(0f)
                    val baseY = topPad + chartHeight
                    val left = centerX - barWidth / 2f

                    if (idx == selectedIndex) {
                        drawRoundRect(
                            color = amColor.copy(alpha = 0.08f),
                            topLeft = Offset(centerX - slotWidth * 0.42f, 0f),
                            size = Size(slotWidth * 0.84f, baseY + 6.dp.toPx()),
                            cornerRadius = CornerRadius(10.dp.toPx())
                        )
                    }

                    if (amH > 0f) {
                        drawRoundRect(
                            color = amColor,
                            topLeft = Offset(left, baseY - amH),
                            size = Size(barWidth, amH),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                    }
                    if (pmH > 0f) {
                        drawRoundRect(
                            color = pmColor,
                            topLeft = Offset(left, baseY - totalH),
                            size = Size(barWidth, pmH),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                    }

                    val dotY = baseY - totalH
                    if (idx == 0) {
                        trendPath.moveTo(centerX, dotY)
                    } else {
                        trendPath.lineTo(centerX, dotY)
                    }
                }

                drawPath(
                    path = trendPath,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEachIndexed { idx, pt ->
                    val centerX = slotWidth * idx + slotWidth / 2f
                    val totalH = ((pt.totalLitres / maxVal).toFloat() * chartHeight * animProgress)
                    val dotY = topPad + chartHeight - totalH
                    drawCircle(
                        color = if (idx == selectedIndex) AmberGold else EmeraldDeep,
                        radius = if (idx == selectedIndex) 5.dp.toPx() else 3.5.dp.toPx(),
                        center = Offset(centerX, dotY)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                points.forEachIndexed { idx, pt ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedIndex = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pt.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (idx == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FinanceComparisonChart(
    points: List<PeriodFinancePoint>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val maxVal = max(10.0, points.maxOfOrNull { max(it.income, it.expense) } ?: 10.0)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "7-Day Income vs Expense ($currency)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendDot(color = ProfitGreen, label = "Income")
                LegendDot(color = LossCrimson, label = "Expense")
            }
        }

        if (points.all { it.income == 0.0 && it.expense == 0.0 }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add income or expense entries to compare daily cashflow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val count = points.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val singleBarWidth = (slotWidth * 0.28f).coerceAtMost(20.dp.toPx())
                val chartHeight = size.height - 12.dp.toPx()
                val topPad = 6.dp.toPx()

                for (i in 0..2) {
                    val y = topPad + (chartHeight / 2f) * i
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }

                points.forEachIndexed { idx, pt ->
                    val centerX = slotWidth * idx + slotWidth / 2f
                    val baseY = topPad + chartHeight
                    val incH = ((pt.income / maxVal).toFloat() * chartHeight).coerceAtLeast(if (pt.income > 0) 4.dp.toPx() else 0f)
                    val expH = ((pt.expense / maxVal).toFloat() * chartHeight).coerceAtLeast(if (pt.expense > 0) 4.dp.toPx() else 0f)

                    if (incH > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(ProfitGreen, ProfitGreen.copy(alpha = 0.7f))),
                            topLeft = Offset(centerX - singleBarWidth - 2.dp.toPx(), baseY - incH),
                            size = Size(singleBarWidth, incH),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
                    if (expH > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(LossCrimson, LossCrimson.copy(alpha = 0.7f))),
                            topLeft = Offset(centerX + 2.dp.toPx(), baseY - expH),
                            size = Size(singleBarWidth, expH),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                points.forEach { pt ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
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
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    dateValue: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = dateValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = "Pick date")
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        val initialMillis = remember(dateValue) {
            try {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                fmt.parse(dateValue)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        onDateSelected(fmt.format(Date(millis)))
                    }
                    showDialog = false
                }) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    timeValue: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = timeValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Rounded.Schedule, contentDescription = "Pick time")
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        val parts = timeValue.split(":")
        val initHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 6
        val initMin = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val timeState = rememberTimePickerState(initialHour = initHour, initialMinute = initMin, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Feeding Time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(String.format(Locale.US, "%02d:%02d", timeState.hour, timeState.minute))
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
