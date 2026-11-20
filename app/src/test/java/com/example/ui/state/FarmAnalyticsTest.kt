package com.example.ui.state

import com.example.data.model.Cattle
import com.example.data.model.FarmTransaction
import com.example.data.model.FeedItem
import com.example.data.model.FeedingSchedule
import com.example.data.model.MilkRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmAnalyticsTest {

    @Test
    fun detectProductionDrops_flagsAnimalsWithTwentyPercentOrMoreDrop() {
        val cow = Cattle(id = "c1", tagNumber = "COW-101", name = "Bella", isActive = true)
        val records = listOf(
            MilkRecord(id = "r1", cattleId = "c1", date = "2026-11-10", morningLitres = 10.0, eveningLitres = 10.0, totalLitres = 20.0),
            MilkRecord(id = "r2", cattleId = "c1", date = "2026-11-11", morningLitres = 10.0, eveningLitres = 10.0, totalLitres = 20.0),
            MilkRecord(id = "r3", cattleId = "c1", date = "2026-11-12", morningLitres = 6.0, eveningLitres = 6.0, totalLitres = 12.0)
        )
        val drops = FarmAnalytics.detectProductionDrops(listOf(cow), records)
        assertEquals(1, drops.size)
        assertEquals(40, drops.first().dropPercent)
    }

    @Test
    fun filterTransactionsByPeriod_andCalculateProfit() {
        val txs = listOf(
            FarmTransaction(id = "t1", date = "2026-11-13", type = "Income", amount = 250.0),
            FarmTransaction(id = "t2", date = "2026-11-13", type = "Expense", amount = 80.0),
            FarmTransaction(id = "t3", date = "2026-11-01", type = "Income", amount = 500.0),
            FarmTransaction(id = "t4", date = "2025-10-10", type = "Expense", amount = 300.0)
        )

        val daily = FarmAnalytics.filterTransactionsByPeriod(txs, FinancePeriod.DAILY, "2026-11-13")
        assertEquals(2, daily.size)
        val dailyNet = daily.filter { it.type == "Income" }.sumOf { it.amount } -
            daily.filter { it.type == "Expense" }.sumOf { it.amount }
        assertEquals(170.0, dailyNet, 0.01)

        val monthly = FarmAnalytics.filterTransactionsByPeriod(txs, FinancePeriod.MONTHLY, "2026-11-13")
        assertEquals(3, monthly.size)
    }

    @Test
    fun evaluateScheduleStatus_distinguishesCompletedPendingOverdue() {
        val completed = FeedingSchedule(id = "s1", feedingTime = "06:00", lastCompletedDate = "2026-11-13")
        val overdue = FeedingSchedule(id = "s2", feedingTime = "06:00", lastCompletedDate = "")
        val pending = FeedingSchedule(id = "s3", feedingTime = "18:00", lastCompletedDate = "")

        assertEquals(ScheduleStatus.COMPLETED, FarmAnalytics.evaluateScheduleStatus(completed, "2026-11-13", "10:00"))
        assertEquals(ScheduleStatus.MISSED_OR_OVERDUE, FarmAnalytics.evaluateScheduleStatus(overdue, "2026-11-13", "10:00"))
        assertEquals(ScheduleStatus.PENDING, FarmAnalytics.evaluateScheduleStatus(pending, "2026-11-13", "10:00"))
    }

    @Test
    fun buildAlerts_includesLowStockAndOverdueFeed() {
        val feed = FeedItem(id = "f1", name = "Mineral Mix", quantity = 10.0, unit = "kg", minStockLevel = 20.0)
        val sched = FeedingSchedule(id = "s1", title = "Morning Mix", feedId = "f1", feedName = "Mineral Mix", quantity = 5.0, unit = "kg", feedingTime = "06:00", isActive = true)
        val alerts = FarmAnalytics.buildAlerts(null, listOf(sched), listOf(feed), emptyList(), emptyList())
        assertTrue(alerts.any { it.category == AlertCategory.LOW_STOCK })
        assertTrue(alerts.any { it.category == AlertCategory.FEEDING })
    }
}
