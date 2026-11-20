package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FarmConstants
import com.example.data.model.FeedItem
import com.example.data.model.FeedingRecord
import com.example.data.model.FeedingSchedule
import com.example.notifications.FeedingReminderManager
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyFarmState
import com.example.ui.components.TimePickerField
import com.example.ui.state.FarmAnalytics
import com.example.ui.state.ScheduleStatus
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.DairyDashboardData
import java.util.Locale

@Composable
fun FeedingScreen(
    data: DairyDashboardData,
    onSaveFeedItem: (FeedItem) -> Unit,
    onRestockFeedItem: (FeedItem, Double, Boolean, Double, String, String) -> Unit,
    onDeleteFeedItem: (String) -> Unit,
    onSaveSchedule: (FeedingSchedule) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onCompleteFeeding: (FeedingSchedule?, FeedItem, Double, String, String, String) -> Unit,
    onMarkMissed: (FeedingSchedule, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var hasNotifPerm by remember { mutableStateOf(FeedingReminderManager.hasNotificationPermission(context)) }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasNotifPerm = granted }
    )

    var showFeedSheet by remember { mutableStateOf(false) }
    var editingFeed by remember { mutableStateOf<FeedItem?>(null) }
    var restockingFeed by remember { mutableStateOf<FeedItem?>(null) }
    var deletingFeed by remember { mutableStateOf<FeedItem?>(null) }

    var showScheduleSheet by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<FeedingSchedule?>(null) }
    var deletingSchedule by remember { mutableStateOf<FeedingSchedule?>(null) }

    var completingSchedule by remember { mutableStateOf<FeedingSchedule?>(null) }

    val currency = data.profile?.currencySymbol ?: "$"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedSubTab == 0) {
                FloatingActionButton(
                    onClick = {
                        if (data.feedItems.isEmpty()) {
                            selectedSubTab = 1
                            editingFeed = null
                            showFeedSheet = true
                        } else {
                            editingSchedule = null
                            showScheduleSheet = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("add_schedule_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add feeding schedule")
                }
            } else if (selectedSubTab == 1) {
                FloatingActionButton(
                    onClick = {
                        editingFeed = null
                        showFeedSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("add_feed_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add feed item")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
            ) {
                val tabs = listOf("Schedules", "Feed Stock", "History")
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    tabs.forEachIndexed { idx, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                            selected = selectedSubTab == idx,
                            onClick = { selectedSubTab = idx }
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }

                when (selectedSubTab) {
                    0 -> SchedulesSection(
                        schedules = data.feedingSchedules,
                        feedItems = data.feedItems,
                        hasNotificationPermission = hasNotifPerm,
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onCreateSchedule = {
                            if (data.feedItems.isEmpty()) {
                                selectedSubTab = 1
                                editingFeed = null
                                showFeedSheet = true
                            } else {
                                editingSchedule = null
                                showScheduleSheet = true
                            }
                        },
                        onEditSchedule = {
                            editingSchedule = it
                            showScheduleSheet = true
                        },
                        onToggleSchedule = { s -> onSaveSchedule(s.copy(isActive = !s.isActive)) },
                        onDeleteSchedule = { deletingSchedule = it },
                        onLogCompleted = { completingSchedule = it },
                        onMarkMissed = { s ->
                            onMarkMissed(s, FarmAnalytics.todayDateStr(), FarmAnalytics.currentTimeStr())
                        }
                    )
                    1 -> FeedStockSection(
                        feedItems = data.feedItems,
                        currency = currency,
                        onAddFeed = {
                            editingFeed = null
                            showFeedSheet = true
                        },
                        onEditFeed = {
                            editingFeed = it
                            showFeedSheet = true
                        },
                        onRestock = { restockingFeed = it },
                        onDeleteFeed = { deletingFeed = it }
                    )
                    2 -> FeedingHistorySection(records = data.feedingRecords)
                }
            }
        }
    }

    if (showFeedSheet) {
        FeedItemFormSheet(
            existing = editingFeed,
            currency = currency,
            onDismiss = { showFeedSheet = false },
            onSave = { item ->
                onSaveFeedItem(item)
                showFeedSheet = false
            }
        )
    }

    if (showScheduleSheet) {
        FeedingScheduleFormSheet(
            existing = editingSchedule,
            feedItems = data.feedItems,
            onDismiss = { showScheduleSheet = false },
            onSave = { sched ->
                onSaveSchedule(sched)
                showScheduleSheet = false
            }
        )
    }

    restockingFeed?.let { item ->
        RestockFeedDialog(
            item = item,
            currency = currency,
            onDismiss = { restockingFeed = null },
            onConfirm = { added, recordExpense, totalCost, method ->
                onRestockFeedItem(item, added, recordExpense, totalCost, FarmAnalytics.todayDateStr(), method)
                restockingFeed = null
            }
        )
    }

    completingSchedule?.let { schedule ->
        val feedItem = data.feedItems.firstOrNull { it.id == schedule.feedId }
        if (feedItem != null) {
            CompleteFeedingDialog(
                schedule = schedule,
                feedItem = feedItem,
                onDismiss = { completingSchedule = null },
                onConfirm = { qtyConsumed, notes ->
                    onCompleteFeeding(
                        schedule,
                        feedItem,
                        qtyConsumed,
                        FarmAnalytics.todayDateStr(),
                        FarmAnalytics.currentTimeStr(),
                        notes
                    )
                    completingSchedule = null
                }
            )
        } else {
            completingSchedule = null
        }
    }

    deletingFeed?.let { item ->
        ConfirmDeleteDialog(
            title = "Delete ${item.name}?",
            message = "Remove this feed item from your inventory?",
            onConfirm = {
                onDeleteFeedItem(item.id)
                deletingFeed = null
            },
            onDismiss = { deletingFeed = null }
        )
    }

    deletingSchedule?.let { sched ->
        ConfirmDeleteDialog(
            title = "Delete Schedule?",
            message = "Remove '${sched.title}' at ${sched.feedingTime}?",
            onConfirm = {
                onDeleteSchedule(sched.id)
                deletingSchedule = null
            },
            onDismiss = { deletingSchedule = null }
        )
    }
}

@Composable
private fun SchedulesSection(
    schedules: List<FeedingSchedule>,
    feedItems: List<FeedItem>,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onCreateSchedule: () -> Unit,
    onEditSchedule: (FeedingSchedule) -> Unit,
    onToggleSchedule: (FeedingSchedule) -> Unit,
    onDeleteSchedule: (FeedingSchedule) -> Unit,
    onLogCompleted: (FeedingSchedule) -> Unit,
    onMarkMissed: (FeedingSchedule) -> Unit
) {
    val today = remember { FarmAnalytics.todayDateStr() }
    val nowTime = remember { FarmAnalytics.currentTimeStr() }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Feeding Reminders",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Allow notifications so DairyLedger alerts you at scheduled feeding times.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        FilledTonalButton(onClick = onRequestNotificationPermission) {
                            Text("Allow")
                        }
                    }
                }
            }
        }

        if (schedules.isEmpty()) {
            item {
                EmptyFarmState(
                    icon = Icons.Rounded.Schedule,
                    title = if (feedItems.isEmpty()) "Add Feed Inventory First" else "No Feeding Schedules",
                    description = if (feedItems.isEmpty()) {
                        "Add at least one feed item in Feed Stock so you can link schedules and auto-deduct stock."
                    } else {
                        "Create daily feeding schedules for milking cows, calves, or buffaloes with automatic reminders."
                    },
                    actionText = if (feedItems.isEmpty()) "Add Feed Item" else "Create Schedule",
                    onAction = onCreateSchedule
                )
            }
        } else {
            items(schedules, key = { it.id }) { schedule ->
                val status = FarmAnalytics.evaluateScheduleStatus(schedule, today, nowTime)
                val (badgeText, badgeBg, badgeFg) = when {
                    !schedule.isActive -> Triple("Paused", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    status == ScheduleStatus.COMPLETED -> Triple("Completed Today", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    status == ScheduleStatus.MISSED_OR_OVERDUE -> Triple("Due / Overdue", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                    else -> Triple("Upcoming", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
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
                            Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "${schedule.feedingTime} • $badgeText",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = badgeFg,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Switch(
                                checked = schedule.isActive,
                                onCheckedChange = { onToggleSchedule(schedule) }
                            )
                        }
                        Text(
                            text = schedule.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${schedule.quantity} ${schedule.unit} of ${schedule.feedName}  •  Group: ${schedule.targetGroup}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (schedule.isActive && status != ScheduleStatus.COMPLETED) {
                                Button(
                                    onClick = { onLogCompleted(schedule) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark Fed")
                                }
                                if (status == ScheduleStatus.MISSED_OR_OVERDUE) {
                                    OutlinedButton(onClick = { onMarkMissed(schedule) }) {
                                        Text("Missed")
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            IconButton(onClick = { onEditSchedule(schedule) }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit schedule")
                            }
                            IconButton(onClick = { onDeleteSchedule(schedule) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete schedule",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedStockSection(
    feedItems: List<FeedItem>,
    currency: String,
    onAddFeed: () -> Unit,
    onEditFeed: (FeedItem) -> Unit,
    onRestock: (FeedItem) -> Unit,
    onDeleteFeed: (FeedItem) -> Unit
) {
    var lowStockOnly by remember { mutableStateOf(false) }
    val filtered = remember(feedItems, lowStockOnly) {
        if (lowStockOnly) feedItems.filter { it.isLowStock } else feedItems
    }
    val lowCount = remember(feedItems) { feedItems.count { it.isLowStock } }
    val totalValue = remember(feedItems) { feedItems.sumOf { it.quantity * it.costPerUnit } }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Inventory Value",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$currency${String.format(Locale.US, "%.2f", totalValue)}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    FilterChip(
                        selected = lowStockOnly,
                        onClick = { lowStockOnly = !lowStockOnly },
                        label = { Text("Low Stock ($lowCount)") },
                        leadingIcon = if (lowCount > 0) {
                            { Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyFarmState(
                    icon = Icons.Rounded.Inventory2,
                    title = if (lowStockOnly) "No Low-Stock Feeds" else "No Feed Items Added",
                    description = if (lowStockOnly) "All feed items are currently above their minimum stock threshold." else "Track green fodder, dry hay, concentrates, and mineral mixes with low-stock alerts.",
                    actionText = if (lowStockOnly) null else "Add Feed Item",
                    onAction = if (lowStockOnly) null else onAddFeed
                )
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                val isLow = item.isLowStock
                val ratio = if (item.minStockLevel > 0) (item.quantity / (item.minStockLevel * 2.0)).toFloat().coerceIn(0f, 1f) else 1f
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isLow) LossCrimson.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
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
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.category} • $currency${String.format(Locale.US, "%.2f", item.costPerUnit)}/${item.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isLow) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (item.quantity <= 0) "OUT" else "LOW STOCK",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Available: ${String.format(Locale.US, "%.1f", item.quantity)} ${item.unit}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isLow) LossCrimson else ProfitGreen
                            )
                            Text(
                                text = "Min Alert: ${String.format(Locale.US, "%.1f", item.minStockLevel)} ${item.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LinearProgressIndicator(
                            progress = { ratio },
                            color = if (isLow) LossCrimson else ProfitGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onRestock(item) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restock")
                            }
                            IconButton(onClick = { onEditFeed(item) }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit feed")
                            }
                            IconButton(onClick = { onDeleteFeed(item) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete feed",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedingHistorySection(records: List<FeedingRecord>) {
    val today = remember { FarmAnalytics.todayDateStr() }
    val weekStart = remember { FarmAnalytics.dateDaysAgo(6) }
    val monthPrefix = remember(today) { today.take(7) }

    val todayQty = remember(records, today) {
        records.filter { it.date == today && it.status == "Completed" }.sumOf { it.quantityConsumed }
    }
    val weekQty = remember(records, weekStart, today) {
        records.filter { it.date in weekStart..today && it.status == "Completed" }.sumOf { it.quantityConsumed }
    }
    val monthQty = remember(records, monthPrefix) {
        records.filter { it.date.startsWith(monthPrefix) && it.status == "Completed" }.sumOf { it.quantityConsumed }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ConsumptionStat("Today", "${String.format(Locale.US, "%.1f", todayQty)} units")
                    ConsumptionStat("7 Days", "${String.format(Locale.US, "%.1f", weekQty)} units")
                    ConsumptionStat("This Month", "${String.format(Locale.US, "%.1f", monthQty)} units")
                }
            }
        }

        if (records.isEmpty()) {
            item {
                EmptyFarmState(
                    icon = Icons.Rounded.History,
                    title = "No Feeding History Yet",
                    description = "Completed and missed feeding activities appear here automatically with deducted stock amounts."
                )
            }
        } else {
            items(records, key = { it.id }) { rec ->
                val isCompleted = rec.status == "Completed"
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Rounded.Restaurant else Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = if (isCompleted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rec.scheduleTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${rec.date} ${rec.time} • ${rec.feedName}: ${String.format(Locale.US, "%.1f", rec.quantityConsumed)} ${rec.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (rec.notes.isNotBlank()) {
                                Text(
                                    text = rec.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = rec.status,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isCompleted) ProfitGreen else LossCrimson
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsumptionStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedItemFormSheet(
    existing: FeedItem?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (FeedItem) -> Unit
) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: FarmConstants.FEED_CATEGORIES.first()) }
    var qtyStr by remember(existing) { mutableStateOf(existing?.quantity?.toString() ?: "") }
    var unit by remember(existing) { mutableStateOf(existing?.unit ?: "kg") }
    var costStr by remember(existing) { mutableStateOf(existing?.costPerUnit?.toString() ?: "") }
    var minStockStr by remember(existing) { mutableStateOf(existing?.minStockLevel?.toString() ?: "50") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existing == null) "Add Feed Item" else "Edit ${existing.name}",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Feed Name (e.g. Alfalfa Hay, Dairy Pellets)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feed_name_input")
            )
            Text("Category", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FarmConstants.FEED_CATEGORIES) { cat ->
                    FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                }
            }
            Text("Unit", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FarmConstants.FEED_UNITS.forEach { u ->
                    FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("Current Stock ($unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = minStockStr,
                    onValueChange = { minStockStr = it },
                    label = { Text("Min Alert ($unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = costStr,
                onValueChange = { costStr = it },
                label = { Text("Cost per $unit ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    val qty = qtyStr.toDoubleOrNull()
                    val min = minStockStr.toDoubleOrNull() ?: 0.0
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    if (name.isBlank() || qty == null || qty < 0.0) {
                        error = "Enter a valid feed name and non-negative quantity."
                        return@Button
                    }
                    onSave(
                        (existing ?: FeedItem()).copy(
                            name = name.trim(),
                            category = category,
                            quantity = qty,
                            unit = unit,
                            costPerUnit = cost.coerceAtLeast(0.0),
                            minStockLevel = min.coerceAtLeast(0.0)
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_feed_button")
            ) {
                Text(if (existing == null) "Save Feed Item" else "Update Feed Item")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedingScheduleFormSheet(
    existing: FeedingSchedule?,
    feedItems: List<FeedItem>,
    onDismiss: () -> Unit,
    onSave: (FeedingSchedule) -> Unit
) {
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var targetGroup by remember(existing) { mutableStateOf(existing?.targetGroup ?: "Milking Herd") }
    var selectedFeed by remember(existing, feedItems) {
        mutableStateOf(
            if (existing != null) feedItems.firstOrNull { it.id == existing.feedId } ?: feedItems.firstOrNull()
            else feedItems.firstOrNull()
        )
    }
    var feedDropdown by remember { mutableStateOf(false) }
    var qtyStr by remember(existing) { mutableStateOf(existing?.quantity?.toString() ?: "") }
    var timeStr by remember(existing) { mutableStateOf(existing?.feedingTime ?: "06:30") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existing == null) "Create Feeding Schedule" else "Edit Schedule",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Schedule Name (e.g. Morning Concentrate)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = targetGroup,
                onValueChange = { targetGroup = it },
                label = { Text("Cattle / Herd Group (e.g. All Milking Cows)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(
                expanded = feedDropdown,
                onExpandedChange = { feedDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedFeed?.let { "${it.name} (${String.format(Locale.US, "%.1f", it.quantity)} ${it.unit} avail)" } ?: "Select Feed",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Feed Item") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = feedDropdown) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = feedDropdown,
                    onDismissRequest = { feedDropdown = false }
                ) {
                    feedItems.forEach { item ->
                        DropdownMenuItem(
                            text = { Text("${item.name} (${String.format(Locale.US, "%.1f", item.quantity)} ${item.unit})") },
                            onClick = {
                                selectedFeed = item
                                feedDropdown = false
                            }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("Qty (${selectedFeed?.unit ?: "kg"})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                TimePickerField(
                    label = "Feeding Time",
                    timeValue = timeStr,
                    onTimeSelected = { timeStr = it },
                    modifier = Modifier.weight(1f)
                )
            }
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    val feed = selectedFeed
                    val qty = qtyStr.toDoubleOrNull()
                    if (title.isBlank() || targetGroup.isBlank() || feed == null || qty == null || qty <= 0.0) {
                        error = "Please enter title, group, feed item, and quantity > 0."
                        return@Button
                    }
                    onSave(
                        (existing ?: FeedingSchedule()).copy(
                            title = title.trim(),
                            targetGroup = targetGroup.trim(),
                            feedId = feed.id,
                            feedName = feed.name,
                            quantity = qty,
                            unit = feed.unit,
                            feedingTime = timeStr,
                            isActive = existing?.isActive ?: true
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (existing == null) "Save Schedule" else "Update Schedule")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CompleteFeedingDialog(
    schedule: FeedingSchedule,
    feedItem: FeedItem,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var qtyStr by remember { mutableStateOf(schedule.quantity.toString()) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Feeding Completed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${schedule.title} • ${feedItem.name} (Available: ${String.format(Locale.US, "%.1f", feedItem.quantity)} ${feedItem.unit})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("Quantity Consumed (${feedItem.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val qty = qtyStr.toDoubleOrNull()
                if (qty == null || qty <= 0.0) {
                    error = "Quantity must be greater than 0."
                    return@Button
                }
                onConfirm(qty, notes)
            }) {
                Text("Confirm & Deduct")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RestockFeedDialog(
    item: FeedItem,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean, Double, String) -> Unit
) {
    var addedStr by remember { mutableStateOf("") }
    var recordExpense by remember { mutableStateOf(true) }
    var paymentMethod by remember { mutableStateOf("Cash") }
    val addedVal = addedStr.toDoubleOrNull() ?: 0.0
    var costOverrideStr by remember { mutableStateOf("") }
    val calculatedCost = costOverrideStr.toDoubleOrNull() ?: (addedVal * item.costPerUnit)
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restock ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = addedStr,
                    onValueChange = { addedStr = it },
                    label = { Text("Quantity Added (${item.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Record Cattle Feed Expense", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = recordExpense, onCheckedChange = { recordExpense = it })
                }
                if (recordExpense) {
                    OutlinedTextField(
                        value = costOverrideStr,
                        onValueChange = { costOverrideStr = it },
                        label = { Text("Total Cost ($currency) — Est: ${String.format(Locale.US, "%.2f", addedVal * item.costPerUnit)}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(FarmConstants.PAYMENT_METHODS) { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (addedVal <= 0.0) {
                    error = "Enter quantity > 0."
                    return@Button
                }
                onConfirm(addedVal, recordExpense, calculatedCost, paymentMethod)
            }) {
                Text("Restock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
