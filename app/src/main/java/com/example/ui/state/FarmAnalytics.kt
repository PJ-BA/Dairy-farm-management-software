package com.example.ui.state

import com.example.data.model.Cattle
import com.example.data.model.FarmTransaction
import com.example.data.model.FeedItem
import com.example.data.model.FeedingRecord
import com.example.data.model.FeedingSchedule
import com.example.data.model.MilkRecord
import com.example.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class FinancePeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

enum class ScheduleStatus {
    COMPLETED,
    PENDING,
    MISSED_OR_OVERDUE
}

data class DailyMilkPoint(
    val date: String,
    val shortLabel: String,
    val morningLitres: Double,
    val eveningLitres: Double,
    val totalLitres: Double
)

data class PeriodFinancePoint(
    val label: String,
    val income: Double,
    val expense: Double
)

data class ProductionDropAlert(
    val cattleId: String,
    val cattleTag: String,
    val cattleName: String,
    val latestLitres: Double,
    val averageLitres: Double,
    val dropPercent: Int
)

data class FarmAlertItem(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val category: AlertCategory
)

enum class AlertSeverity { INFO, WARNING, CRITICAL }
enum class AlertCategory { FEEDING, LOW_STOCK, MILK_DROP }

object FarmAnalytics {

    fun todayDateStr(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun currentTimeStr(): String =
        SimpleDateFormat("HH:mm", Locale.US).format(Date())

    fun dateDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun shortDayLabel(dateStr: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
            if (parsed != null) SimpleDateFormat("EEE", Locale.US).format(parsed) else dateStr.takeLast(2)
        } catch (_: Exception) {
            dateStr.takeLast(2)
        }
    }

    fun buildLast7DaysMilkSeries(records: List<MilkRecord>): List<DailyMilkPoint> {
        val byDate = records.groupBy { it.date }
        return (6 downTo 0).map { offset ->
            val d = dateDaysAgo(offset)
            val dayRecords = byDate[d].orEmpty()
            DailyMilkPoint(
                date = d,
                shortLabel = shortDayLabel(d),
                morningLitres = dayRecords.sumOf { it.morningLitres },
                eveningLitres = dayRecords.sumOf { it.eveningLitres },
                totalLitres = dayRecords.sumOf { it.totalLitres }
            )
        }
    }

    fun detectProductionDrops(
        cattle: List<Cattle>,
        records: List<MilkRecord>
    ): List<ProductionDropAlert> {
        val activeCattle = cattle.filter { it.isActive }
        val byCattle = records.groupBy { it.cattleId }
        val alerts = mutableListOf<ProductionDropAlert>()

        activeCattle.forEach { animal ->
            val sorted = byCattle[animal.id].orEmpty().sortedByDescending { it.date }
            if (sorted.size >= 2) {
                val latest = sorted.first()
                val prior = sorted.drop(1).take(7)
                val avg = prior.map { it.totalLitres }.average()
                if (avg > 1.0 && latest.totalLitres < avg * 0.80) {
                    val dropPct = (((avg - latest.totalLitres) / avg) * 100.0).toInt().coerceIn(1, 99)
                    alerts.add(
                        ProductionDropAlert(
                            cattleId = animal.id,
                            cattleTag = animal.tagNumber,
                            cattleName = animal.name,
                            latestLitres = latest.totalLitres,
                            averageLitres = avg,
                            dropPercent = dropPct
                        )
                    )
                }
            }
        }
        return alerts.sortedByDescending { it.dropPercent }
    }

    fun evaluateScheduleStatus(
        schedule: FeedingSchedule,
        today: String = todayDateStr(),
        nowTime: String = currentTimeStr()
    ): ScheduleStatus {
        if (schedule.lastCompletedDate == today) return ScheduleStatus.COMPLETED
        return if (schedule.feedingTime < nowTime) ScheduleStatus.MISSED_OR_OVERDUE else ScheduleStatus.PENDING
    }

    fun filterTransactionsByPeriod(
        transactions: List<FarmTransaction>,
        period: FinancePeriod,
        today: String = todayDateStr()
    ): List<FarmTransaction> {
        val monthPrefix = today.take(7)
        val yearPrefix = today.take(4)
        val weekStart = dateDaysAgo(6)
        return when (period) {
            FinancePeriod.DAILY -> transactions.filter { it.date == today }
            FinancePeriod.WEEKLY -> transactions.filter { it.date in weekStart..today }
            FinancePeriod.MONTHLY -> transactions.filter { it.date.startsWith(monthPrefix) }
            FinancePeriod.YEARLY -> transactions.filter { it.date.startsWith(yearPrefix) }
        }
    }

    fun buildLast7DaysFinanceSeries(transactions: List<FarmTransaction>): List<PeriodFinancePoint> {
        val byDate = transactions.groupBy { it.date }
        return (6 downTo 0).map { offset ->
            val d = dateDaysAgo(offset)
            val items = byDate[d].orEmpty()
            PeriodFinancePoint(
                label = shortDayLabel(d),
                income = items.filter { it.type == "Income" }.sumOf { it.amount },
                expense = items.filter { it.type == "Expense" }.sumOf { it.amount }
            )
        }
    }

    fun buildAlerts(
        profile: UserProfile?,
        schedules: List<FeedingSchedule>,
        feedItems: List<FeedItem>,
        cattle: List<Cattle>,
        milkRecords: List<MilkRecord>
    ): List<FarmAlertItem> {
        val alerts = mutableListOf<FarmAlertItem>()
        val today = todayDateStr()
        val nowTime = currentTimeStr()

        if (profile?.feedingRemindersEnabled != false) {
            schedules.filter { it.isActive }.forEach { schedule ->
                val status = evaluateScheduleStatus(schedule, today, nowTime)
                when (status) {
                    ScheduleStatus.MISSED_OR_OVERDUE -> alerts.add(
                        FarmAlertItem(
                            id = "feed_overdue_${schedule.id}",
                            title = "Due Feeding: ${schedule.title}",
                            message = "Scheduled for ${schedule.feedingTime} (${schedule.quantity} ${schedule.unit} ${schedule.feedName}). Tap Feeding to log.",
                            severity = AlertSeverity.WARNING,
                            category = AlertCategory.FEEDING
                        )
                    )
                    ScheduleStatus.PENDING -> alerts.add(
                        FarmAlertItem(
                            id = "feed_upcoming_${schedule.id}",
                            title = "Upcoming Feeding at ${schedule.feedingTime}",
                            message = "${schedule.title} • ${schedule.quantity} ${schedule.unit} ${schedule.feedName} for ${schedule.targetGroup}.",
                            severity = AlertSeverity.INFO,
                            category = AlertCategory.FEEDING
                        )
                    )
                    ScheduleStatus.COMPLETED -> Unit
                }
            }
        }

        if (profile?.lowStockAlertsEnabled != false) {
            feedItems.filter { it.isLowStock }.forEach { item ->
                val severity = if (item.quantity <= 0.0) AlertSeverity.CRITICAL else AlertSeverity.WARNING
                val label = if (item.quantity <= 0.0) "Out of Stock" else "Low Feed Stock"
                alerts.add(
                    FarmAlertItem(
                        id = "stock_${item.id}",
                        title = "$label: ${item.name}",
                        message = "Remaining: ${String.format(Locale.US, "%.1f", item.quantity)} ${item.unit} (Min threshold: ${String.format(Locale.US, "%.1f", item.minStockLevel)} ${item.unit}).",
                        severity = severity,
                        category = AlertCategory.LOW_STOCK
                    )
                )
            }
        }

        if (profile?.productionDropAlertsEnabled != false) {
            detectProductionDrops(cattle, milkRecords).forEach { drop ->
                alerts.add(
                    FarmAlertItem(
                        id = "drop_${drop.cattleId}",
                        title = "Yield Drop: ${drop.cattleName} (${drop.cattleTag})",
                        message = "Latest ${String.format(Locale.US, "%.1f", drop.latestLitres)} L is ${drop.dropPercent}% below 7-day average (${String.format(Locale.US, "%.1f", drop.averageLitres)} L).",
                        severity = AlertSeverity.CRITICAL,
                        category = AlertCategory.MILK_DROP
                    )
                )
            }
        }

        return alerts
    }
}
