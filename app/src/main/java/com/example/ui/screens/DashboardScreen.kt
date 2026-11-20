package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FeedingSchedule
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.MilkTrendChart
import com.example.ui.components.SectionHeader
import com.example.ui.state.AlertCategory
import com.example.ui.state.AlertSeverity
import com.example.ui.state.FarmAlertItem
import com.example.ui.state.FarmAnalytics
import com.example.ui.state.ScheduleStatus
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SagePrimary
import com.example.ui.viewmodel.DairyDashboardData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    data: DairyDashboardData,
    onNavigateTab: (Int) -> Unit,
    onQuickLogMilk: () -> Unit,
    onQuickAddCattle: () -> Unit,
    onQuickLogTransaction: () -> Unit,
    onQuickCompleteSchedule: (FeedingSchedule) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { FarmAnalytics.todayDateStr() }
    val nowTime = remember { FarmAnalytics.currentTimeStr() }
    val monthPrefix = remember(today) { today.take(7) }
    val currency = data.profile?.currencySymbol ?: "$"
    val farmName = data.profile?.farmName?.ifBlank { "My Dairy Farm" } ?: "My Dairy Farm"

    val todayMilkRecords = remember(data.milkRecords, today) {
        data.milkRecords.filter { it.date == today }
    }
    val todayMilkTotal = remember(todayMilkRecords) { todayMilkRecords.sumOf { it.totalLitres } }
    val todayMorning = remember(todayMilkRecords) { todayMilkRecords.sumOf { it.morningLitres } }
    val todayEvening = remember(todayMilkRecords) { todayMilkRecords.sumOf { it.eveningLitres } }
    val monthMilkTotal = remember(data.milkRecords, monthPrefix) {
        data.milkRecords.filter { it.date.startsWith(monthPrefix) }.sumOf { it.totalLitres }
    }
    val milk7DaySeries = remember(data.milkRecords) {
        FarmAnalytics.buildLast7DaysMilkSeries(data.milkRecords)
    }

    val activeSchedules = remember(data.feedingSchedules) {
        data.feedingSchedules.filter { it.isActive }
    }
    val completedTodayCount = remember(activeSchedules, today) {
        activeSchedules.count { FarmAnalytics.evaluateScheduleStatus(it, today, nowTime) == ScheduleStatus.COMPLETED }
    }
    val pendingSchedules = remember(activeSchedules, today, nowTime) {
        activeSchedules.filter { FarmAnalytics.evaluateScheduleStatus(it, today, nowTime) != ScheduleStatus.COMPLETED }
    }
    val lowStockFeeds = remember(data.feedItems) { data.feedItems.filter { it.isLowStock } }
    val totalFeedStockKg = remember(data.feedItems) {
        data.feedItems.filter { it.unit == "kg" }.sumOf { it.quantity }
    }

    val todayTx = remember(data.transactions, today) { data.transactions.filter { it.date == today } }
    val todayIncome = remember(todayTx) { todayTx.filter { it.type == "Income" }.sumOf { it.amount } }
    val todayExpense = remember(todayTx) { todayTx.filter { it.type == "Expense" }.sumOf { it.amount } }

    val monthTx = remember(data.transactions, monthPrefix) {
        data.transactions.filter { it.date.startsWith(monthPrefix) }
    }
    val monthIncome = remember(monthTx) { monthTx.filter { it.type == "Income" }.sumOf { it.amount } }
    val monthExpense = remember(monthTx) { monthTx.filter { it.type == "Expense" }.sumOf { it.amount } }
    val monthProfit = monthIncome - monthExpense

    val alerts = remember(data) {
        FarmAnalytics.buildAlerts(
            profile = data.profile,
            schedules = data.feedingSchedules,
            feedItems = data.feedItems,
            cattle = data.cattle,
            milkRecords = data.milkRecords
        )
    }

    var showAlertsSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Editorial Farm Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_farm_hero),
                        contentDescription = "Dairy farm pasture banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        EmeraldDeep.copy(alpha = 0.25f),
                                        EmeraldDeep.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopStart),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = "Synced",
                                    tint = Color(0xFFA7F3D0),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Cloud & Offline Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.92f),
                            shape = CircleShape
                        ) {
                            IconButton(
                                onClick = { showAlertsSheet = true },
                                modifier = Modifier.testTag("dashboard_alerts_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (alerts.isNotEmpty()) {
                                            Badge(
                                                containerColor = if (alerts.any { it.severity == AlertSeverity.CRITICAL }) LossCrimson else AmberWarm
                                            ) {
                                                Text(alerts.size.toString(), color = Color.White)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Notifications,
                                        contentDescription = "Farm notifications",
                                        tint = EmeraldDeep
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date()),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = farmName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${data.cattle.count { it.isActive }} active cattle • ${data.feedItems.size} feed items tracked",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // 2. Quick Actions Strip
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        QuickActionPill(
                            icon = Icons.Rounded.WaterDrop,
                            label = "Log Milk",
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            testTag = "quick_log_milk",
                            onClick = onQuickLogMilk
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = Icons.Rounded.Pets,
                            label = "Add Cattle",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            testTag = "quick_add_cattle",
                            onClick = onQuickAddCattle
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = Icons.Rounded.Restaurant,
                            label = "Feeding",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            testTag = "quick_open_feeding",
                            onClick = { onNavigateTab(2) }
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = Icons.Rounded.Add,
                            label = "Transaction",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            testTag = "quick_add_tx",
                            onClick = onQuickLogTransaction
                        )
                    }
                }
            }

            // 3. Urgent Alerts Callout (if any critical/warning alerts exist)
            if (alerts.isNotEmpty()) {
                item {
                    val topAlert = alerts.first()
                    val isCrit = topAlert.severity == AlertSeverity.CRITICAL
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isCrit) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAlertsSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = "Alert",
                                tint = if (isCrit) MaterialTheme.colorScheme.error else AmberWarm
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${alerts.size} Active Alert${if (alerts.size > 1) "s" else ""}: ${topAlert.title}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCrit) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = topAlert.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCrit) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "View",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isCrit) MaterialTheme.colorScheme.error else AmberWarm
                            )
                        }
                    }
                }
            }

            // 4. Milk Production Overview
            item {
                SectionHeader(
                    title = "Milk Production",
                    actionLabel = "Details",
                    onActionClick = { onNavigateTab(1) }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricCard(
                        title = "Today's Yield",
                        value = "${String.format(Locale.US, "%.1f", todayMilkTotal)} L",
                        subtitle = "AM ${String.format(Locale.US, "%.1f", todayMorning)}L • PM ${String.format(Locale.US, "%.1f", todayEvening)}L",
                        icon = Icons.Rounded.WaterDrop,
                        accentColor = EmeraldDeep,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(1) }
                    )
                    KpiMetricCard(
                        title = "Monthly Output",
                        value = "${String.format(Locale.US, "%.1f", monthMilkTotal)} L",
                        subtitle = "${data.cattle.count { it.isActive }} active cattle",
                        icon = Icons.Rounded.Pets,
                        accentColor = SagePrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(1) }
                    )
                }
            }
            item {
                MilkTrendChart(points = milk7DaySeries)
            }

            // 5. Feeding Overview
            item {
                SectionHeader(
                    title = "Feeding & Inventory",
                    actionLabel = "Manage",
                    onActionClick = { onNavigateTab(2) }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricCard(
                        title = "Today's Schedules",
                        value = "$completedTodayCount / ${activeSchedules.size}",
                        subtitle = "${pendingSchedules.size} pending today",
                        icon = Icons.Rounded.Schedule,
                        accentColor = AmberWarm,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(2) }
                    )
                    KpiMetricCard(
                        title = "Feed Stock (kg)",
                        value = "${String.format(Locale.US, "%.0f", totalFeedStockKg)} kg",
                        subtitle = if (lowStockFeeds.isEmpty()) "All stocks healthy" else "${lowStockFeeds.size} low stock alert(s)",
                        icon = Icons.Rounded.Restaurant,
                        accentColor = if (lowStockFeeds.isEmpty()) ProfitGreen else LossCrimson,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(2) }
                    )
                }
            }
            if (pendingSchedules.isNotEmpty()) {
                item {
                    val nextSchedule = pendingSchedules.first()
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Next: ${nextSchedule.title} (${nextSchedule.feedingTime})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${nextSchedule.quantity} ${nextSchedule.unit} ${nextSchedule.feedName} • ${nextSchedule.targetGroup}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { onQuickCompleteSchedule(nextSchedule) },
                                modifier = Modifier.testTag("dashboard_complete_schedule_button")
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fed")
                            }
                        }
                    }
                }
            }

            // 6. Financial Overview
            item {
                SectionHeader(
                    title = "Financial Performance",
                    actionLabel = "Ledger",
                    onActionClick = { onNavigateTab(3) }
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiMetricCard(
                            title = "Today's Cashflow",
                            value = "+$currency${String.format(Locale.US, "%.0f", todayIncome)}",
                            subtitle = "Expenses: -$currency${String.format(Locale.US, "%.0f", todayExpense)}",
                            icon = Icons.Rounded.AccountBalanceWallet,
                            accentColor = ProfitGreen,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(3) }
                        )
                        KpiMetricCard(
                            title = "Monthly Net P&L",
                            value = "${if (monthProfit >= 0) "+" else "-"}$currency${String.format(Locale.US, "%.0f", kotlin.math.abs(monthProfit))}",
                            subtitle = "In $currency${String.format(Locale.US, "%.0f", monthIncome)} • Out $currency${String.format(Locale.US, "%.0f", monthExpense)}",
                            icon = if (monthProfit >= 0) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                            accentColor = if (monthProfit >= 0) ProfitGreen else LossCrimson,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(3) }
                        )
                    }
                }
            }
        }
    }

    if (showAlertsSheet) {
        ModalBottomSheet(onDismissRequest = { showAlertsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Farm Alerts & Reminders",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (alerts.isEmpty()) {
                    Text(
                        text = "Everything looks healthy! No overdue feedings, low stock warnings, or yield drops detected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(alerts, key = { it.id }) { alert ->
                            AlertRowCard(
                                alert = alert,
                                onClick = {
                                    showAlertsSheet = false
                                    when (alert.category) {
                                        AlertCategory.FEEDING, AlertCategory.LOW_STOCK -> onNavigateTab(2)
                                        AlertCategory.MILK_DROP -> onNavigateTab(1)
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .height(48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun AlertRowCard(alert: FarmAlertItem, onClick: () -> Unit) {
    val bgColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
        AlertSeverity.WARNING -> MaterialTheme.colorScheme.secondaryContainer
        AlertSeverity.INFO -> MaterialTheme.colorScheme.primaryContainer
    }
    val fgColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
        AlertSeverity.WARNING -> MaterialTheme.colorScheme.onSecondaryContainer
        AlertSeverity.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        onClick = onClick,
        color = bgColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (alert.category) {
                    AlertCategory.FEEDING -> Icons.Rounded.Schedule
                    AlertCategory.LOW_STOCK -> Icons.Rounded.WarningAmber
                    AlertCategory.MILK_DROP -> Icons.Rounded.TrendingDown
                },
                contentDescription = null,
                tint = fgColor
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = fgColor
                )
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = fgColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}
