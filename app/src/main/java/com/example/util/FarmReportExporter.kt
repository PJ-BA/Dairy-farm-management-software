package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.FarmTransaction
import com.example.data.model.FeedingRecord
import com.example.data.model.MilkRecord
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportType(val label: String, val fileSlug: String) {
    FARM_SUMMARY("Complete Farm Summary", "farm_summary"),
    MILK_PRODUCTION("Milk Production Report", "milk_production"),
    FEEDING_LOG("Feeding Consumption Report", "feeding_consumption"),
    FINANCIAL_PL("Financial P&L Statement", "financial_statement")
}

object FarmReportExporter {

    private fun csvEscape(value: String): String {
        val cleaned = value.replace("\"", "\"\"")
        return "\"$cleaned\""
    }

    fun buildCsvContent(
        type: ReportType,
        farmName: String,
        currency: String,
        milkRecords: List<MilkRecord>,
        feedingRecords: List<FeedingRecord>,
        transactions: List<FarmTransaction>
    ): String {
        val sb = StringBuilder()
        val generatedOn = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        sb.appendLine("# DairyLedger Export - $farmName - Generated $generatedOn")
        when (type) {
            ReportType.MILK_PRODUCTION -> {
                sb.appendLine("Date,Animal Tag,Animal Name,Morning (L),Evening (L),Total (L),Notes")
                milkRecords.forEach { r ->
                    sb.appendLine(
                        listOf(
                            csvEscape(r.date),
                            csvEscape(r.cattleTag),
                            csvEscape(r.cattleName),
                            String.format(Locale.US, "%.2f", r.morningLitres),
                            String.format(Locale.US, "%.2f", r.eveningLitres),
                            String.format(Locale.US, "%.2f", r.totalLitres),
                            csvEscape(r.notes)
                        ).joinToString(",")
                    )
                }
            }
            ReportType.FEEDING_LOG -> {
                sb.appendLine("Date,Time,Schedule,Feed Name,Quantity Consumed,Unit,Status,Notes")
                feedingRecords.forEach { f ->
                    sb.appendLine(
                        listOf(
                            csvEscape(f.date),
                            csvEscape(f.time),
                            csvEscape(f.scheduleTitle),
                            csvEscape(f.feedName),
                            String.format(Locale.US, "%.2f", f.quantityConsumed),
                            csvEscape(f.unit),
                            csvEscape(f.status),
                            csvEscape(f.notes)
                        ).joinToString(",")
                    )
                }
            }
            ReportType.FINANCIAL_PL -> {
                sb.appendLine("Date,Type,Category,Description,Payment Method,Qty (L),Price/L,Amount ($currency)")
                transactions.forEach { t ->
                    sb.appendLine(
                        listOf(
                            csvEscape(t.date),
                            csvEscape(t.type),
                            csvEscape(t.category),
                            csvEscape(t.description),
                            csvEscape(t.paymentMethod),
                            String.format(Locale.US, "%.2f", t.quantityLitres),
                            String.format(Locale.US, "%.2f", t.pricePerLitre),
                            String.format(Locale.US, "%.2f", t.amount)
                        ).joinToString(",")
                    )
                }
            }
            ReportType.FARM_SUMMARY -> {
                val totalMilk = milkRecords.sumOf { it.totalLitres }
                val totalIncome = transactions.filter { it.type == "Income" }.sumOf { it.amount }
                val totalExpense = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
                val netProfit = totalIncome - totalExpense
                sb.appendLine("Section,Metric,Value")
                sb.appendLine("Milk,Total Recorded Production (L),${String.format(Locale.US, "%.2f", totalMilk)}")
                sb.appendLine("Milk,Total Records Count,${milkRecords.size}")
                sb.appendLine("Feeding,Total Feeding Activities,${feedingRecords.size}")
                sb.appendLine("Finance,Total Income ($currency),${String.format(Locale.US, "%.2f", totalIncome)}")
                sb.appendLine("Finance,Total Expenses ($currency),${String.format(Locale.US, "%.2f", totalExpense)}")
                sb.appendLine("Finance,Net Profit ($currency),${String.format(Locale.US, "%.2f", netProfit)}")
            }
        }
        return sb.toString()
    }

    fun writePdfReport(
        outputStream: OutputStream,
        type: ReportType,
        farmName: String,
        currency: String,
        milkRecords: List<MilkRecord>,
        feedingRecords: List<FeedingRecord>,
        transactions: List<FarmTransaction>
    ) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.rgb(27, 67, 50)
            style = Paint.Style.FILL
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(216, 243, 220)
            textSize = 11f
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(27, 67, 50)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 35)
            textSize = 10.5f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.rgb(90, 105, 96)
            textSize = 9.5f
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(220, 215, 200)
            strokeWidth = 1f
        }

        // Top editorial banner
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 88f, headerPaint)
        canvas.drawText("DairyLedger • ${type.label}", 36f, 38f, titlePaint)
        val generatedOn = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(Date())
        canvas.drawText("$farmName   |   Generated $generatedOn", 36f, 62f, subtitlePaint)

        var y = 120f

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - 48f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 48f
            }
        }

        val totalMilk = milkRecords.sumOf { it.totalLitres }
        val totalIncome = transactions.filter { it.type == "Income" }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val netProfit = totalIncome - totalExpenses

        if (type == ReportType.FARM_SUMMARY || type == ReportType.FINANCIAL_PL) {
            canvas.drawText("Financial Overview", 36f, y, sectionPaint)
            y += 20f
            canvas.drawText(
                "Total Income: $currency${String.format(Locale.US, "%.2f", totalIncome)}   |   " +
                    "Total Expenses: $currency${String.format(Locale.US, "%.2f", totalExpenses)}   |   " +
                    "Net Profit: $currency${String.format(Locale.US, "%.2f", netProfit)}",
                36f, y, bodyPaint
            )
            y += 14f
            canvas.drawLine(36f, y, pageWidth - 36f, y, dividerPaint)
            y += 22f
        }

        if (type == ReportType.FARM_SUMMARY || type == ReportType.MILK_PRODUCTION) {
            ensureSpace(50f)
            canvas.drawText("Milk Production (${milkRecords.size} records • ${String.format(Locale.US, "%.1f", totalMilk)} L total)", 36f, y, sectionPaint)
            y += 18f
            milkRecords.take(35).forEach { r ->
                ensureSpace(18f)
                canvas.drawText(
                    "${r.date}  |  ${r.cattleTag} (${r.cattleName})  |  AM: ${String.format(Locale.US, "%.1f", r.morningLitres)}L  PM: ${String.format(Locale.US, "%.1f", r.eveningLitres)}L  Total: ${String.format(Locale.US, "%.1f", r.totalLitres)}L",
                    36f, y, bodyPaint
                )
                y += 16f
            }
            y += 10f
        }

        if (type == ReportType.FARM_SUMMARY || type == ReportType.FEEDING_LOG) {
            ensureSpace(50f)
            canvas.drawText("Feeding Consumption (${feedingRecords.size} activities)", 36f, y, sectionPaint)
            y += 18f
            feedingRecords.take(35).forEach { f ->
                ensureSpace(18f)
                canvas.drawText(
                    "${f.date} ${f.time}  |  ${f.scheduleTitle}  |  ${f.feedName}: ${String.format(Locale.US, "%.1f", f.quantityConsumed)} ${f.unit} [${f.status}]",
                    36f, y, bodyPaint
                )
                y += 16f
            }
            y += 10f
        }

        if (type == ReportType.FARM_SUMMARY || type == ReportType.FINANCIAL_PL) {
            ensureSpace(50f)
            canvas.drawText("Recent Transactions (${transactions.size} total)", 36f, y, sectionPaint)
            y += 18f
            transactions.take(40).forEach { t ->
                ensureSpace(18f)
                val sign = if (t.type == "Income") "+" else "-"
                canvas.drawText(
                    "${t.date}  |  ${t.type} (${t.category})  |  ${t.paymentMethod}  |  $sign$currency${String.format(Locale.US, "%.2f", t.amount)}  ${t.description.take(28)}",
                    36f, y, bodyPaint
                )
                y += 16f
            }
        }

        ensureSpace(24f)
        canvas.drawText("Generated offline-ready by DairyLedger Farm Management", 36f, pageHeight - 24f, mutedPaint)
        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }

    fun shareTempReport(
        context: Context,
        isPdf: Boolean,
        type: ReportType,
        farmName: String,
        currency: String,
        milkRecords: List<MilkRecord>,
        feedingRecords: List<FeedingRecord>,
        transactions: List<FarmTransaction>
    ): Intent {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val ext = if (isPdf) "pdf" else "csv"
        val file = File(reportsDir, "dairy_${type.fileSlug}.$ext")
        FileOutputStream(file).use { out ->
            if (isPdf) {
                writePdfReport(out, type, farmName, currency, milkRecords, feedingRecords, transactions)
            } else {
                val csv = buildCsvContent(type, farmName, currency, milkRecords, feedingRecords, transactions)
                out.write(csv.toByteArray(Charsets.UTF_8))
            }
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            this.type = if (isPdf) "application/pdf" else "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${type.label} - $farmName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
