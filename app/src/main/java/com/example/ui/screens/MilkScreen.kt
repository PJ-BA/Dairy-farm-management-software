package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Cattle
import com.example.data.model.FarmConstants
import com.example.data.model.MilkRecord
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.EmptyFarmState
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.MilkTrendChart
import com.example.ui.state.FarmAnalytics
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.DairyDashboardData
import java.util.Locale

@Composable
fun MilkScreen(
    data: DairyDashboardData,
    initialSubTab: Int = 0,
    openAddMilkTrigger: Int = 0,
    openAddCattleTrigger: Int = 0,
    onSaveCattle: (Cattle) -> Unit,
    onDeleteCattle: (String) -> Unit,
    onSaveMilkRecord: (MilkRecord) -> Unit,
    onDeleteMilkRecord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    var editingCattle by remember { mutableStateOf<Cattle?>(null) }
    var showCattleSheet by remember { mutableStateOf(false) }
    var deletingCattle by remember { mutableStateOf<Cattle?>(null) }

    var editingMilkRecord by remember { mutableStateOf<MilkRecord?>(null) }
    var showMilkSheet by remember { mutableStateOf(false) }
    var deletingMilkRecord by remember { mutableStateOf<MilkRecord?>(null) }

    LaunchedEffect(openAddMilkTrigger) {
        if (openAddMilkTrigger > 0) {
            selectedSubTab = 0
            editingMilkRecord = null
            showMilkSheet = true
        }
    }
    LaunchedEffect(openAddCattleTrigger) {
        if (openAddCattleTrigger > 0) {
            selectedSubTab = 1
            editingCattle = null
            showCattleSheet = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedSubTab == 0) {
                FloatingActionButton(
                    onClick = {
                        if (data.cattle.isEmpty()) {
                            selectedSubTab = 1
                            editingCattle = null
                            showCattleSheet = true
                        } else {
                            editingMilkRecord = null
                            showMilkSheet = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("add_milk_record_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Log milk yield")
                }
            } else if (selectedSubTab == 1) {
                FloatingActionButton(
                    onClick = {
                        editingCattle = null
                        showCattleSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("add_cattle_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Register cattle")
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
                val tabs = listOf("Daily Entry", "Herd", "Analytics")
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    tabs.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                            selected = selectedSubTab == index,
                            onClick = { selectedSubTab = index },
                            modifier = Modifier.testTag("milk_tab_${label.lowercase().replace(" ", "_")}")
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }

                when (selectedSubTab) {
                    0 -> DailyMilkEntrySection(
                        cattle = data.cattle,
                        records = data.milkRecords,
                        onAddMilk = {
                            if (data.cattle.isEmpty()) {
                                selectedSubTab = 1
                                editingCattle = null
                                showCattleSheet = true
                            } else {
                                editingMilkRecord = null
                                showMilkSheet = true
                            }
                        },
                        onEditRecord = {
                            editingMilkRecord = it
                            showMilkSheet = true
                        },
                        onDeleteRecord = { deletingMilkRecord = it }
                    )
                    1 -> CattleHerdSection(
                        cattle = data.cattle,
                        onAddCattle = {
                            editingCattle = null
                            showCattleSheet = true
                        },
                        onEditCattle = {
                            editingCattle = it
                            showCattleSheet = true
                        },
                        onToggleActive = { animal ->
                            onSaveCattle(animal.copy(isActive = !animal.isActive))
                        },
                        onDeleteCattle = { deletingCattle = it }
                    )
                    2 -> MilkAnalyticsSection(
                        cattle = data.cattle,
                        records = data.milkRecords
                    )
                }
            }
        }
    }

    if (showCattleSheet) {
        CattleFormSheet(
            existing = editingCattle,
            onDismiss = { showCattleSheet = false },
            onSave = { cattle ->
                onSaveCattle(cattle)
                showCattleSheet = false
            }
        )
    }

    if (showMilkSheet) {
        MilkRecordFormSheet(
            cattle = data.cattle,
            existing = editingMilkRecord,
            onDismiss = { showMilkSheet = false },
            onSave = { rec ->
                onSaveMilkRecord(rec)
                showMilkSheet = false
            }
        )
    }

    deletingCattle?.let { target ->
        ConfirmDeleteDialog(
            title = "Remove ${target.name}?",
            message = "This will delete cattle ${target.tagNumber} from your herd registry.",
            onConfirm = {
                onDeleteCattle(target.id)
                deletingCattle = null
            },
            onDismiss = { deletingCattle = null }
        )
    }

    deletingMilkRecord?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Milk Entry?",
            message = "Remove ${String.format(Locale.US, "%.1f", target.totalLitres)} L entry for ${target.cattleName} on ${target.date}?",
            onConfirm = {
                onDeleteMilkRecord(target.id)
                deletingMilkRecord = null
            },
            onDismiss = { deletingMilkRecord = null }
        )
    }
}

@Composable
private fun DailyMilkEntrySection(
    cattle: List<Cattle>,
    records: List<MilkRecord>,
    onAddMilk: () -> Unit,
    onEditRecord: (MilkRecord) -> Unit,
    onDeleteRecord: (MilkRecord) -> Unit
) {
    var dateFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val today = remember { FarmAnalytics.todayDateStr() }
    val weekStart = remember { FarmAnalytics.dateDaysAgo(6) }
    val monthPrefix = remember(today) { today.take(7) }

    val filtered = remember(records, dateFilter, searchQuery, today, weekStart, monthPrefix) {
        records.filter { rec ->
            val matchesDate = when (dateFilter) {
                "Today" -> rec.date == today
                "7 Days" -> rec.date in weekStart..today
                "Month" -> rec.date.startsWith(monthPrefix)
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                rec.cattleName.contains(searchQuery, ignoreCase = true) ||
                rec.cattleTag.contains(searchQuery, ignoreCase = true) ||
                rec.notes.contains(searchQuery, ignoreCase = true)
            matchesDate && matchesQuery
        }
    }

    val filteredTotal = remember(filtered) { filtered.sumOf { it.totalLitres } }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Search by cow tag, name, or notes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Today", "7 Days", "Month")) { filter ->
                    FilterChip(
                        selected = dateFilter == filter,
                        onClick = { dateFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
        }
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
                    Text(
                        text = "${filtered.size} Record${if (filtered.size == 1) "" else "s"} ($dateFilter)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", filteredTotal)} L",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                val emptyDesc = if (cattle.isEmpty()) {
                    "Register your cows or buffaloes first, then start logging morning & evening milk yields."
                } else {
                    "No milk records match this filter. Tap below to record today's morning or evening yield."
                }
                EmptyFarmState(
                    icon = Icons.Rounded.WaterDrop,
                    title = if (cattle.isEmpty()) "No Cattle Registered" else "No Milk Entries Yet",
                    description = emptyDesc,
                    actionText = if (cattle.isEmpty()) "Register First Animal" else "Log Milk Yield",
                    onAction = onAddMilk
                )
            }
        } else {
            items(filtered, key = { it.id }) { rec ->
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
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", rec.totalLitres)}L",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${rec.cattleName} (${rec.cattleTag})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${rec.date}  •  AM ${String.format(Locale.US, "%.1f", rec.morningLitres)}L  •  PM ${String.format(Locale.US, "%.1f", rec.eveningLitres)}L",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (rec.notes.isNotBlank()) {
                                Text(
                                    text = rec.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = { onEditRecord(rec) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit milk record")
                        }
                        IconButton(onClick = { onDeleteRecord(rec) }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete milk record",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CattleHerdSection(
    cattle: List<Cattle>,
    onAddCattle: () -> Unit,
    onEditCattle: (Cattle) -> Unit,
    onToggleActive: (Cattle) -> Unit,
    onDeleteCattle: (Cattle) -> Unit
) {
    var statusFilter by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }

    val filtered = remember(cattle, statusFilter, search) {
        cattle.filter { animal ->
            val matchesStatus = when (statusFilter) {
                "Active" -> animal.isActive
                "Inactive" -> !animal.isActive
                else -> true
            }
            val matchesSearch = search.isBlank() ||
                animal.name.contains(search, ignoreCase = true) ||
                animal.tagNumber.contains(search, ignoreCase = true) ||
                animal.breed.contains(search, ignoreCase = true)
            matchesStatus && matchesSearch
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Search tag number, name, or breed") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Active", "Inactive").forEach { label ->
                    FilterChip(
                        selected = statusFilter == label,
                        onClick = { statusFilter = label },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyFarmState(
                    icon = Icons.Rounded.Pets,
                    title = "No Cattle Found",
                    description = "Add cows or buffaloes with unique tag numbers and breeds to track individual milk yields.",
                    actionText = "Register Animal",
                    onAction = onAddCattle
                )
            }
        } else {
            items(filtered, key = { it.id }) { animal ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = if (animal.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = animal.tagNumber,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (animal.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = animal.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${animal.species} • ${animal.breed} • Reg ${animal.registrationDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = animal.isActive,
                                onCheckedChange = { onToggleActive(animal) }
                            )
                        }
                        if (animal.notes.isNotBlank()) {
                            Text(
                                text = animal.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = { onEditCattle(animal) }) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = { onDeleteCattle(animal) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilkAnalyticsSection(
    cattle: List<Cattle>,
    records: List<MilkRecord>
) {
    var selectedAnimalId by remember { mutableStateOf("ALL") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val scopedRecords = remember(records, selectedAnimalId) {
        if (selectedAnimalId == "ALL") records else records.filter { it.cattleId == selectedAnimalId }
    }

    val today = remember { FarmAnalytics.todayDateStr() }
    val weekStart = remember { FarmAnalytics.dateDaysAgo(6) }
    val monthPrefix = remember(today) { today.take(7) }

    val dailyTotal = remember(scopedRecords, today) { scopedRecords.filter { it.date == today }.sumOf { it.totalLitres } }
    val weeklyTotal = remember(scopedRecords, weekStart, today) { scopedRecords.filter { it.date in weekStart..today }.sumOf { it.totalLitres } }
    val monthlyTotal = remember(scopedRecords, monthPrefix) { scopedRecords.filter { it.date.startsWith(monthPrefix) }.sumOf { it.totalLitres } }
    val avgPerEntry = remember(scopedRecords) {
        if (scopedRecords.isEmpty()) 0.0 else scopedRecords.map { it.totalLitres }.average()
    }

    val series7Day = remember(scopedRecords) { FarmAnalytics.buildLast7DaysMilkSeries(scopedRecords) }
    val dropAlerts = remember(cattle, records) { FarmAnalytics.detectProductionDrops(cattle, records) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                val selectedLabel = if (selectedAnimalId == "ALL") {
                    "All Herd Animals (${cattle.size})"
                } else {
                    cattle.firstOrNull { it.id == selectedAnimalId }?.let { "${it.name} (${it.tagNumber})" } ?: "All Herd"
                }
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filter Analytics by Animal") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Herd Animals") },
                        onClick = {
                            selectedAnimalId = "ALL"
                            dropdownExpanded = false
                        }
                    )
                    cattle.forEach { animal ->
                        DropdownMenuItem(
                            text = { Text("${animal.name} (${animal.tagNumber})") },
                            onClick = {
                                selectedAnimalId = animal.id
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiMetricCard(
                    title = "Daily / Weekly",
                    value = "${String.format(Locale.US, "%.1f", weeklyTotal)} L",
                    subtitle = "Today: ${String.format(Locale.US, "%.1f", dailyTotal)} L",
                    icon = Icons.Rounded.WaterDrop,
                    accentColor = EmeraldDeep,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Monthly / Avg",
                    value = "${String.format(Locale.US, "%.1f", monthlyTotal)} L",
                    subtitle = "Avg/entry: ${String.format(Locale.US, "%.1f", avgPerEntry)} L",
                    icon = Icons.Rounded.Pets,
                    accentColor = ProfitGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            MilkTrendChart(points = series7Day)
        }

        item {
            Text(
                text = "Declining Production Watchlist",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (dropAlerts.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No unusual yield drops detected across active cattle (threshold: >=20% below 7-day average).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(dropAlerts, key = { it.cattleId }) { alert ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TrendingDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${alert.cattleName} (${alert.cattleTag}) • -${alert.dropPercent}%",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Latest ${String.format(Locale.US, "%.1f", alert.latestLitres)} L vs 7-day avg ${String.format(Locale.US, "%.1f", alert.averageLitres)} L",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CattleFormSheet(
    existing: Cattle?,
    onDismiss: () -> Unit,
    onSave: (Cattle) -> Unit
) {
    var tagNumber by remember(existing) { mutableStateOf(existing?.tagNumber ?: "") }
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var species by remember(existing) { mutableStateOf(existing?.species ?: "Cow") }
    var breed by remember(existing) { mutableStateOf(existing?.breed ?: "Holstein") }
    var regDate by remember(existing) { mutableStateOf(existing?.registrationDate ?: FarmAnalytics.todayDateStr()) }
    var isActive by remember(existing) { mutableStateOf(existing?.isActive ?: true) }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }
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
                text = if (existing == null) "Register Cattle" else "Edit ${existing.tagNumber}",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = tagNumber,
                onValueChange = { tagNumber = it },
                label = { Text("Unique Animal ID / Tag (e.g. COW-104)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cattle_tag_input")
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Animal Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cattle_name_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FarmConstants.SPECIES_OPTIONS.forEach { sp ->
                    FilterChip(
                        selected = species == sp,
                        onClick = { species = sp },
                        label = { Text(sp) }
                    )
                }
            }
            OutlinedTextField(
                value = breed,
                onValueChange = { breed = it },
                label = { Text("Breed") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(FarmConstants.BREED_SUGGESTIONS) { suggestion ->
                    FilterChip(
                        selected = breed == suggestion,
                        onClick = { breed = suggestion },
                        label = { Text(suggestion) }
                    )
                }
            }
            DatePickerField(
                label = "Registration Date",
                dateValue = regDate,
                onDateSelected = { regDate = it },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Active in Milking Herd", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    if (tagNumber.isBlank() || name.isBlank() || breed.isBlank()) {
                        error = "Please enter Tag Number, Name, and Breed."
                        return@Button
                    }
                    onSave(
                        (existing ?: Cattle()).copy(
                            tagNumber = tagNumber.trim(),
                            name = name.trim(),
                            species = species,
                            breed = breed.trim(),
                            registrationDate = regDate,
                            isActive = isActive,
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_cattle_button")
            ) {
                Text(if (existing == null) "Save Cattle" else "Update Cattle")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilkRecordFormSheet(
    cattle: List<Cattle>,
    existing: MilkRecord?,
    onDismiss: () -> Unit,
    onSave: (MilkRecord) -> Unit
) {
    val activeOrAll = remember(cattle) {
        cattle.filter { it.isActive }.ifEmpty { cattle }
    }
    var selectedCattle by remember(existing, activeOrAll) {
        mutableStateOf(
            if (existing != null) cattle.firstOrNull { it.id == existing.cattleId } ?: activeOrAll.firstOrNull()
            else activeOrAll.firstOrNull()
        )
    }
    var cattleDropdown by remember { mutableStateOf(false) }
    var date by remember(existing) { mutableStateOf(existing?.date ?: FarmAnalytics.todayDateStr()) }
    var morningStr by remember(existing) { mutableStateOf(existing?.morningLitres?.toString() ?: "") }
    var eveningStr by remember(existing) { mutableStateOf(existing?.eveningLitres?.toString() ?: "") }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val morningVal = morningStr.toDoubleOrNull() ?: 0.0
    val eveningVal = eveningStr.toDoubleOrNull() ?: 0.0
    val totalVal = morningVal + eveningVal

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existing == null) "Log Daily Milk Yield" else "Edit Milk Entry",
                style = MaterialTheme.typography.headlineSmall
            )
            ExposedDropdownMenuBox(
                expanded = cattleDropdown,
                onExpandedChange = { cattleDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedCattle?.let { "${it.name} (${it.tagNumber})" } ?: "Select Animal",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Animal") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cattleDropdown) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = cattleDropdown,
                    onDismissRequest = { cattleDropdown = false }
                ) {
                    activeOrAll.forEach { animal ->
                        DropdownMenuItem(
                            text = { Text("${animal.name} (${animal.tagNumber}) • ${animal.breed}") },
                            onClick = {
                                selectedCattle = animal
                                cattleDropdown = false
                            }
                        )
                    }
                }
            }

            DatePickerField(
                label = "Production Date",
                dateValue = date,
                onDateSelected = { date = it },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = morningStr,
                    onValueChange = { morningStr = it },
                    label = { Text("Morning (L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("morning_milk_input")
                )
                OutlinedTextField(
                    value = eveningStr,
                    onValueChange = { eveningStr = it },
                    label = { Text("Evening (L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("evening_milk_input")
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-Calculated Total",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", totalVal)} L",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Optional Remarks") },
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val animal = selectedCattle
                    if (animal == null) {
                        error = "Please select an animal."
                        return@Button
                    }
                    if (totalVal <= 0.0) {
                        error = "Enter morning and/or evening milk quantity greater than 0."
                        return@Button
                    }
                    onSave(
                        (existing ?: MilkRecord()).copy(
                            cattleId = animal.id,
                            cattleTag = animal.tagNumber,
                            cattleName = animal.name,
                            date = date,
                            morningLitres = morningVal,
                            eveningLitres = eveningVal,
                            totalLitres = totalVal,
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_milk_record_button")
            ) {
                Text(if (existing == null) "Save Milk Record" else "Update Milk Record")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
