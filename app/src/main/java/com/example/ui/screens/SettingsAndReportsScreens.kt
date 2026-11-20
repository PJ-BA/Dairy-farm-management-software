package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.GoogleAuthHelper
import com.example.data.model.FarmConstants
import com.example.data.model.UserProfile
import com.example.notifications.FeedingReminderManager
import com.example.ui.viewmodel.DairyDashboardData
import com.example.util.FarmReportExporter
import com.example.util.ReportType

@Composable
fun SettingsScreen(
    data: DairyDashboardData,
    userEmail: String,
    userName: String,
    onSaveProfile: (UserProfile) -> Unit,
    onOpenReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val profile = data.profile ?: UserProfile(
        displayName = userName,
        email = userEmail,
        farmName = "${userName.ifBlank { "My" }}'s Dairy Farm"
    )

    var farmName by remember(profile.farmName) { mutableStateOf(profile.farmName) }
    var currency by remember(profile.currencySymbol) { mutableStateOf(profile.currencySymbol) }
    var milkPriceStr by remember(profile.defaultMilkPrice) { mutableStateOf(profile.defaultMilkPrice.toString()) }
    var feedingReminders by remember(profile.feedingRemindersEnabled) { mutableStateOf(profile.feedingRemindersEnabled) }
    var lowStockAlerts by remember(profile.lowStockAlertsEnabled) { mutableStateOf(profile.lowStockAlertsEnabled) }
    var dropAlerts by remember(profile.productionDropAlertsEnabled) { mutableStateOf(profile.productionDropAlertsEnabled) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                feedingReminders = true
                onSaveProfile(profile.copy(feedingRemindersEnabled = true))
            }
        }
    )

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Settings & Farm Preferences",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            // 1. Account Card
            item {
                SettingsCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (userName.firstOrNull() ?: userEmail.firstOrNull() ?: 'D').uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName.ifBlank { "Farm Owner" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = userEmail.ifBlank { "Google Account" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                GoogleAuthHelper.signOut(credentialManager, scope)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }

            // 2. Farm Configuration
            item {
                SettingsCard {
                    Text("Farm Profile", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = farmName,
                        onValueChange = { farmName = it },
                        label = { Text("Dairy Farm Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Currency Symbol", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FarmConstants.CURRENCY_OPTIONS.forEach { sym ->
                            FilterChip(
                                selected = currency == sym,
                                onClick = { currency = sym },
                                label = { Text(sym) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = milkPriceStr,
                        onValueChange = { milkPriceStr = it },
                        label = { Text("Default Milk Price per Litre ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onSaveProfile(
                                profile.copy(
                                    farmName = farmName.ifBlank { "My Dairy Farm" },
                                    currencySymbol = currency,
                                    defaultMilkPrice = (milkPriceStr.toDoubleOrNull() ?: 1.50).coerceAtLeast(0.0),
                                    feedingRemindersEnabled = feedingReminders,
                                    lowStockAlertsEnabled = lowStockAlerts,
                                    productionDropAlertsEnabled = dropAlerts
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_farm_settings_button")
                    ) {
                        Text("Save Farm Preferences")
                    }
                }
            }

            // 3. Notifications & Alerts
            item {
                SettingsCard {
                    Text("Notifications & Alerts", style = MaterialTheme.typography.titleMedium)
                    ToggleSettingRow(
                        title = "Feeding Schedule Reminders",
                        subtitle = "Notify daily at scheduled cattle feeding times",
                        checked = feedingReminders,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !FeedingReminderManager.hasNotificationPermission(context)
                            ) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                feedingReminders = enabled
                                onSaveProfile(profile.copy(feedingRemindersEnabled = enabled))
                            }
                        }
                    )
                    ToggleSettingRow(
                        title = "Low Feed Stock Warnings",
                        subtitle = "Highlight feeds below minimum stock threshold",
                        checked = lowStockAlerts,
                        onCheckedChange = { enabled ->
                            lowStockAlerts = enabled
                            onSaveProfile(profile.copy(lowStockAlertsEnabled = enabled))
                        }
                    )
                    ToggleSettingRow(
                        title = "Unusual Milk Drop Alerts",
                        subtitle = "Alert when an animal drops >= 20% below 7-day average",
                        checked = dropAlerts,
                        onCheckedChange = { enabled ->
                            dropAlerts = enabled
                            onSaveProfile(profile.copy(productionDropAlertsEnabled = enabled))
                        }
                    )
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !FeedingReminderManager.hasNotificationPermission(context)
                            ) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                FeedingReminderManager.postNotification(
                                    context,
                                    999,
                                    "DairyLedger Test Alert",
                                    "Feeding reminders are active for ${farmName.ifBlank { "your farm" }}."
                                )
                                Toast.makeText(context, "Test notification sent", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Test Reminder Notification")
                    }
                }
            }

            // 4. Reports & Data Export
            item {
                SettingsCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export Farm Reports (PDF / CSV)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Generate printable PDF statements or spreadsheet CSV files for milk, feeding, and P&L.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = onOpenReports,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_reports_sheet_button")
                    ) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Report Exporter")
                    }
                }
            }

            // 5. Offline Resilience Info
            item {
                SettingsCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Offline Barn Entry & Cloud Sync", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Cloud Firestore persistence is enabled locally. You can log milk yields, feedings, and expenses offline; changes synchronize automatically when connectivity returns.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsBottomSheet(
    data: DairyDashboardData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(ReportType.FARM_SUMMARY) }
    val farmName = data.profile?.farmName?.ifBlank { "My Dairy Farm" } ?: "My Dairy Farm"
    val currency = data.profile?.currencySymbol ?: "$"

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FarmReportExporter.writePdfReport(
                        outputStream = out,
                        type = selectedType,
                        farmName = farmName,
                        currency = currency,
                        milkRecords = data.milkRecords,
                        feedingRecords = data.feedingRecords,
                        transactions = data.transactions
                    )
                }
                Toast.makeText(context, "PDF report saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = FarmReportExporter.buildCsvContent(
                    type = selectedType,
                    farmName = farmName,
                    currency = currency,
                    milkRecords = data.milkRecords,
                    feedingRecords = data.feedingRecords,
                    transactions = data.transactions
                )
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "CSV report saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Export & Share Farm Reports",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Choose a report dataset to save to your device or share directly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ReportType.entries.forEach { reportType ->
                val selected = selectedType == reportType
                Surface(
                    onClick = { selectedType = reportType },
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = reportType.label,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selected) {
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { createPdfLauncher.launch("dairy_${selectedType.fileSlug}.pdf") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_pdf_button")
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save PDF")
                }
                Button(
                    onClick = { createCsvLauncher.launch("dairy_${selectedType.fileSlug}.csv") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_csv_button")
                ) {
                    Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save CSV")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val intent = FarmReportExporter.shareTempReport(
                            context = context,
                            isPdf = true,
                            type = selectedType,
                            farmName = farmName,
                            currency = currency,
                            milkRecords = data.milkRecords,
                            feedingRecords = data.feedingRecords,
                            transactions = data.transactions
                        )
                        context.startActivity(Intent.createChooser(intent, "Share PDF Report"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share PDF")
                }
                OutlinedButton(
                    onClick = {
                        val intent = FarmReportExporter.shareTempReport(
                            context = context,
                            isPdf = false,
                            type = selectedType,
                            farmName = farmName,
                            currency = currency,
                            milkRecords = data.milkRecords,
                            feedingRecords = data.feedingRecords,
                            transactions = data.transactions
                        )
                        context.startActivity(Intent.createChooser(intent, "Share CSV Report"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share CSV")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
