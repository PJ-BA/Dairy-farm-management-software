package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.data.model.FarmConstants
import com.example.data.model.FarmTransaction
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.EmptyFarmState
import com.example.ui.components.FinanceComparisonChart
import com.example.ui.components.KpiMetricCard
import com.example.ui.state.FarmAnalytics
import com.example.ui.state.FinancePeriod
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.DairyDashboardData
import java.util.Locale
import kotlin.math.abs

@Composable
fun FinanceScreen(
    data: DairyDashboardData,
    openAddTxTrigger: Int = 0,
    onSaveTransaction: (FarmTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onOpenReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(FinancePeriod.MONTHLY) }
    var typeFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    var showTxSheet by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<FarmTransaction?>(null) }
    var deletingTx by remember { mutableStateOf<FarmTransaction?>(null) }

    LaunchedEffect(openAddTxTrigger) {
        if (openAddTxTrigger > 0) {
            editingTx = null
            showTxSheet = true
        }
    }

    val currency = data.profile?.currencySymbol ?: "$"
    val defaultMilkPrice = data.profile?.defaultMilkPrice ?: 1.50

    val periodTransactions = remember(data.transactions, selectedPeriod) {
        FarmAnalytics.filterTransactionsByPeriod(data.transactions, selectedPeriod)
    }
    val totalIncome = remember(periodTransactions) {
        periodTransactions.filter { it.type == "Income" }.sumOf { it.amount }
    }
    val totalExpense = remember(periodTransactions) {
        periodTransactions.filter { it.type == "Expense" }.sumOf { it.amount }
    }
    val netProfit = totalIncome - totalExpense

    val expenseByCategory = remember(periodTransactions) {
        periodTransactions
            .filter { it.type == "Expense" }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
    }

    val chartPoints = remember(data.transactions) {
        FarmAnalytics.buildLast7DaysFinanceSeries(data.transactions)
    }

    val visibleTransactions = remember(periodTransactions, typeFilter, searchQuery) {
        periodTransactions.filter { tx ->
            val matchesType = typeFilter == "All" || tx.type == typeFilter
            val matchesSearch = searchQuery.isBlank() ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.description.contains(searchQuery, ignoreCase = true) ||
                tx.paymentMethod.contains(searchQuery, ignoreCase = true)
            matchesType && matchesSearch
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTx = null
                    showTxSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_transaction_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add financial transaction")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Financial Ledger",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        FilledTonalButton(
                            onClick = onOpenReports,
                            modifier = Modifier.testTag("finance_export_reports_button")
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF/CSV")
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FinancePeriod.entries) { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(period.label) }
                            )
                        }
                    }
                }

                // Net Profit / Loss Summary Card
                item {
                    val isProfit = netProfit >= 0
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = if (isProfit) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${selectedPeriod.label} Net ${if (isProfit) "Profit" else "Loss"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isProfit) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "${if (isProfit) "+" else "-"}$currency${String.format(Locale.US, "%.2f", abs(netProfit))}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = if (isProfit) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Formula: Income - Expenses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = (if (isProfit) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer).copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = if (isProfit) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                contentDescription = null,
                                tint = if (isProfit) ProfitGreen else LossCrimson,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiMetricCard(
                            title = "${selectedPeriod.label} Income",
                            value = "$currency${String.format(Locale.US, "%.2f", totalIncome)}",
                            subtitle = "${periodTransactions.count { it.type == "Income" }} entries",
                            icon = Icons.Rounded.ArrowUpward,
                            accentColor = ProfitGreen,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "${selectedPeriod.label} Expenses",
                            value = "$currency${String.format(Locale.US, "%.2f", totalExpense)}",
                            subtitle = "${periodTransactions.count { it.type == "Expense" }} entries",
                            icon = Icons.Rounded.ArrowDownward,
                            accentColor = LossCrimson,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    FinanceComparisonChart(points = chartPoints, currency = currency)
                }

                if (expenseByCategory.isNotEmpty() && totalExpense > 0.0) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Expense Breakdown (${selectedPeriod.label})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                expenseByCategory.forEach { (category, amount) ->
                                    val fraction = (amount / totalExpense).toFloat().coerceIn(0f, 1f)
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(category, style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "$currency${String.format(Locale.US, "%.2f", amount)} (${(fraction * 100).toInt()}%)",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            color = LossCrimson.copy(alpha = 0.85f),
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        placeholder = { Text("Filter by category, description, or method") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Income", "Expense").forEach { type ->
                            FilterChip(
                                selected = typeFilter == type,
                                onClick = { typeFilter = type },
                                label = { Text(type) }
                            )
                        }
                    }
                }

                if (visibleTransactions.isEmpty()) {
                    item {
                        EmptyFarmState(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            title = "No Transactions in ${selectedPeriod.label}",
                            description = "Record milk sales income or cattle feed, vet, labour, and utility expenses.",
                            actionText = "Add Transaction",
                            onAction = {
                                editingTx = null
                                showTxSheet = true
                            }
                        )
                    }
                } else {
                    items(visibleTransactions, key = { it.id }) { tx ->
                        val isInc = tx.type == "Income"
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
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isInc) ProfitGreen.copy(alpha = 0.14f) else LossCrimson.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isInc) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                        contentDescription = tx.type,
                                        tint = if (isInc) ProfitGreen else LossCrimson
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = tx.category,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${tx.date} • ${tx.paymentMethod}${if (tx.quantityLitres > 0) " • ${String.format(Locale.US, "%.1f", tx.quantityLitres)}L @ $currency${String.format(Locale.US, "%.2f", tx.pricePerLitre)}/L" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (tx.description.isNotBlank()) {
                                        Text(
                                            text = tx.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (isInc) "+" else "-"}$currency${String.format(Locale.US, "%.2f", tx.amount)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isInc) ProfitGreen else LossCrimson
                                    )
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingTx = tx
                                                showTxSheet = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "Edit transaction", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { deletingTx = tx },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete transaction",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTxSheet) {
        TransactionFormSheet(
            existing = editingTx,
            currency = currency,
            defaultMilkPrice = defaultMilkPrice,
            onDismiss = { showTxSheet = false },
            onSave = { tx ->
                onSaveTransaction(tx)
                showTxSheet = false
            }
        )
    }

    deletingTx?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Transaction?",
            message = "Delete ${target.type} (${target.category}) of $currency${String.format(Locale.US, "%.2f", target.amount)} on ${target.date}?",
            onConfirm = {
                onDeleteTransaction(target.id)
                deletingTx = null
            },
            onDismiss = { deletingTx = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionFormSheet(
    existing: FarmTransaction?,
    currency: String,
    defaultMilkPrice: Double,
    onDismiss: () -> Unit,
    onSave: (FarmTransaction) -> Unit
) {
    var txType by remember(existing) { mutableStateOf(existing?.type ?: "Income") }
    var category by remember(existing) {
        mutableStateOf(
            existing?.category ?: if (txType == "Income") FarmConstants.INCOME_CATEGORIES.first() else FarmConstants.EXPENSE_CATEGORIES.first()
        )
    }
    var date by remember(existing) { mutableStateOf(existing?.date ?: FarmAnalytics.todayDateStr()) }
    var qtyLitresStr by remember(existing) { mutableStateOf(if ((existing?.quantityLitres ?: 0.0) > 0) existing!!.quantityLitres.toString() else "") }
    var pricePerLStr by remember(existing) { mutableStateOf(if ((existing?.pricePerLitre ?: 0.0) > 0) existing!!.pricePerLitre.toString() else defaultMilkPrice.toString()) }
    var amountStr by remember(existing) { mutableStateOf(existing?.amount?.toString() ?: "") }
    var description by remember(existing) { mutableStateOf(existing?.description ?: "") }
    var paymentMethod by remember(existing) { mutableStateOf(existing?.paymentMethod ?: "Cash") }
    var error by remember { mutableStateOf<String?>(null) }

    val isMilkSale = txType == "Income" && category == "Milk Sales"
    val qtyVal = qtyLitresStr.toDoubleOrNull() ?: 0.0
    val priceVal = pricePerLStr.toDoubleOrNull() ?: 0.0
    val autoMilkTotal = qtyVal * priceVal

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existing == null) "Record Transaction" else "Edit Transaction",
                style = MaterialTheme.typography.headlineSmall
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Income", "Expense").forEachIndexed { idx, t ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2),
                        selected = txType == t,
                        onClick = {
                            txType = t
                            category = if (t == "Income") FarmConstants.INCOME_CATEGORIES.first() else FarmConstants.EXPENSE_CATEGORIES.first()
                        }
                    ) {
                        Text(t)
                    }
                }
            }

            Text("Category", style = MaterialTheme.typography.labelLarge)
            val catOptions = if (txType == "Income") FarmConstants.INCOME_CATEGORIES else FarmConstants.EXPENSE_CATEGORIES
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(catOptions) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            DatePickerField(
                label = "Transaction Date",
                dateValue = date,
                onDateSelected = { date = it },
                modifier = Modifier.fillMaxWidth()
            )

            if (isMilkSale) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = qtyLitresStr,
                        onValueChange = {
                            qtyLitresStr = it
                            val q = it.toDoubleOrNull() ?: 0.0
                            val p = pricePerLStr.toDoubleOrNull() ?: 0.0
                            if (q > 0 && p > 0) {
                                amountStr = String.format(Locale.US, "%.2f", q * p)
                            }
                        },
                        label = { Text("Milk Sold (L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pricePerLStr,
                        onValueChange = {
                            pricePerLStr = it
                            val q = qtyLitresStr.toDoubleOrNull() ?: 0.0
                            val p = it.toDoubleOrNull() ?: 0.0
                            if (q > 0 && p > 0) {
                                amountStr = String.format(Locale.US, "%.2f", q * p)
                            }
                        },
                        label = { Text("Price / L ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = {
                    Text(
                        if (isMilkSale && autoMilkTotal > 0)
                            "Total Amount ($currency) — Calc: ${String.format(Locale.US, "%.2f", autoMilkTotal)}"
                        else "Amount ($currency)"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            Text("Payment Method", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FarmConstants.PAYMENT_METHODS) { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method) }
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Notes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val finalAmount = amountStr.toDoubleOrNull() ?: if (isMilkSale) autoMilkTotal else 0.0
                    if (finalAmount <= 0.0) {
                        error = "Enter an amount greater than 0."
                        return@Button
                    }
                    onSave(
                        (existing ?: FarmTransaction()).copy(
                            date = date,
                            type = txType,
                            category = category,
                            description = description.trim(),
                            amount = finalAmount,
                            quantityLitres = if (isMilkSale) qtyVal else 0.0,
                            pricePerLitre = if (isMilkSale) priceVal else 0.0,
                            paymentMethod = paymentMethod
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_tx_button")
            ) {
                Text(if (existing == null) "Save $txType" else "Update Transaction")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
