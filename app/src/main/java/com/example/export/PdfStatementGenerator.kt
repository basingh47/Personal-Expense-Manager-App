package com.example.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.BankAccount
import com.example.data.CreditCard
import com.example.data.Transaction
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class StatementFilter(
    val title: String,
    val periodLabel: String,
    val startDate: Long?,
    val endDate: Long?,
    val bankAccountId: Long? = null,
    val creditCardId: Long? = null,
    val tagFilter: String? = null,
    val categoryFilter: String? = null
)

object PdfStatementGenerator {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun generatePdfStatementBytes(
        context: Context,
        allTransactions: List<Transaction>,
        bankAccounts: List<BankAccount>,
        creditCards: List<CreditCard>,
        filter: StatementFilter
    ): ByteArray {
        val filteredTx = allTransactions.filter { tx ->
            val matchStart = filter.startDate == null || tx.date >= filter.startDate
            val matchEnd = filter.endDate == null || tx.date <= filter.endDate
            val matchBank = filter.bankAccountId == null || tx.bankAccountId == filter.bankAccountId || tx.toBankAccountId == filter.bankAccountId
            val matchCard = filter.creditCardId == null || tx.creditCardId == filter.creditCardId || tx.toCreditCardId == filter.creditCardId
            val matchTag = filter.tagFilter.isNullOrBlank() || tx.tags.any { it.equals(filter.tagFilter, ignoreCase = true) }
            val matchCat = filter.categoryFilter.isNullOrBlank() || tx.category.equals(filter.categoryFilter, ignoreCase = true)
            matchStart && matchEnd && matchBank && matchCard && matchTag && matchCat
        }.sortedByDescending { it.date }

        val totalIncome = filteredTx.filter { it.type == "INCOME" || it.type == "REFUND" }.sumOf { it.amount }
        val totalExpense = filteredTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netCashFlow = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome * 100.0).coerceAtLeast(0.0) else 0.0

        val taxDeductibleTx = filteredTx.filter { it.tags.any { tag -> tag.contains("tax", ignoreCase = true) } }
        val taxDeductibleTotal = taxDeductibleTx.sumOf { it.amount }

        val bankMap = bankAccounts.associateBy { it.id }
        val cardMap = creditCards.associateBy { it.id }

        // Category breakdown
        val expenseCategoryMap = filteredTx.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val pdfDoc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - (margin * 2)

        // Paints
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 41, 59) }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(5, 150, 105) // Primary Emerald
            style = Paint.Style.FILL
        }

        // Layout measurements
        // First page has header banner, KPI cards, category breakdown, tax notes, then starts transaction ledger.
        // Subsequent pages have compact header and transaction ledger continuation.
        var currentPageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        // --- PAGE 1: HEADER & SUMMARY ---
        // Header Emerald Banner
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, headerPaint)

        // Banner Text
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText("FINANCIAL STATEMENT & AUDIT SUMMARY", margin, 40f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(209, 250, 229)
        canvas.drawText("Expense Manager • Official Client & Tax Filing Record", margin, 58f, paint)
        canvas.drawText("Generated On: ${dateFormat.format(Date())} at ${timeFormat.format(Date())}", margin, 74f, paint)

        var yCursor = 115f

        // Metadata box
        paint.color = Color.rgb(241, 245, 249)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(margin, yCursor, margin + contentWidth, yCursor + 50f), 8f, 8f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("STATEMENT PERIOD:", margin + 14f, yCursor + 20f, paint)
        canvas.drawText("SCOPE / FILTER:", margin + 200f, yCursor + 20f, paint)
        canvas.drawText("TOTAL ENTRIES:", margin + 400f, yCursor + 20f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10.5f
        canvas.drawText(filter.periodLabel, margin + 14f, yCursor + 38f, paint)
        val scopeText = if (filter.title.length > 28) filter.title.substring(0, 25) + "..." else filter.title
        canvas.drawText(scopeText, margin + 200f, yCursor + 38f, paint)
        canvas.drawText("${filteredTx.size} Transactions", margin + 400f, yCursor + 38f, paint)

        yCursor += 66f

        // 4 KPI Summary Cards in a 2x2 grid or 4 columns
        val kpiWidth = (contentWidth - 24f) / 4f
        val kpiHeight = 56f

        drawKpiCard(canvas, margin, yCursor, kpiWidth, kpiHeight, "TOTAL INCOME", String.format(Locale.US, "₹%,.2f", totalIncome), Color.rgb(16, 185, 129), Color.rgb(236, 253, 245))
        drawKpiCard(canvas, margin + kpiWidth + 8f, yCursor, kpiWidth, kpiHeight, "TOTAL EXPENSE", String.format(Locale.US, "₹%,.2f", totalExpense), Color.rgb(239, 68, 68), Color.rgb(254, 242, 242))
        drawKpiCard(canvas, margin + (kpiWidth + 8f) * 2, yCursor, kpiWidth, kpiHeight, "NET CASH FLOW", String.format(Locale.US, "₹%,.2f", netCashFlow), if (netCashFlow >= 0) Color.rgb(5, 150, 105) else Color.rgb(220, 38, 38), Color.rgb(241, 245, 249))
        drawKpiCard(canvas, margin + (kpiWidth + 8f) * 3, yCursor, kpiWidth, kpiHeight, "SAVINGS RATE", String.format(Locale.US, "%.1f%%", savingsRate), Color.rgb(217, 119, 6), Color.rgb(254, 243, 199))

        yCursor += kpiHeight + 16f

        // Category Summary & Tax Highlights (Side-by-Side mini tables)
        if (expenseCategoryMap.isNotEmpty() || taxDeductibleTotal > 0) {
            val halfWidth = (contentWidth - 12f) / 2f

            // Left Box: Top Spending Categories
            paint.color = Color.rgb(248, 250, 252)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(margin, yCursor, margin + halfWidth, yCursor + 105f), 8f, 8f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TOP EXPENSE CATEGORIES", margin + 12f, yCursor + 18f, paint)

            var catY = yCursor + 36f
            val topCats = expenseCategoryMap.take(3)
            topCats.forEach { (catName, amt) ->
                val pct = if (totalExpense > 0) (amt / totalExpense * 100.0) else 0.0
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.rgb(51, 65, 85)
                paint.textSize = 9.5f
                canvas.drawText(catName, margin + 12f, catY, paint)

                val amtStr = String.format(Locale.US, "₹%,.2f (%.0f%%)", amt, pct)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(amtStr, margin + halfWidth - 12f - paint.measureText(amtStr), catY, paint)
                catY += 20f
            }

            // Right Box: Tax Deductible & Bank Snapshot Summary
            paint.color = Color.rgb(248, 250, 252)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(margin + halfWidth + 12f, yCursor, margin + contentWidth, yCursor + 105f), 8f, 8f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TAX & ACCOUNT SNAPSHOT", margin + halfWidth + 24f, yCursor + 18f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 9.5f
            canvas.drawText("Tax-Deductible Spending:", margin + halfWidth + 24f, yCursor + 38f, paint)
            val taxStr = String.format(Locale.US, "₹%,.2f (%d items)", taxDeductibleTotal, taxDeductibleTx.size)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.rgb(217, 119, 6)
            canvas.drawText(taxStr, margin + contentWidth - 12f - paint.measureText(taxStr), yCursor + 38f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.rgb(51, 65, 85)
            canvas.drawText("Tracked Bank Accounts:", margin + halfWidth + 24f, yCursor + 58f, paint)
            val bankCountStr = "${bankAccounts.size} Accounts active"
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.rgb(15, 23, 42)
            canvas.drawText(bankCountStr, margin + contentWidth - 12f - paint.measureText(bankCountStr), yCursor + 58f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.rgb(51, 65, 85)
            canvas.drawText("Tracked Credit Cards:", margin + halfWidth + 24f, yCursor + 78f, paint)
            val cardCountStr = "${creditCards.size} Cards active"
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(cardCountStr, margin + contentWidth - 12f - paint.measureText(cardCountStr), yCursor + 78f, paint)

            yCursor += 120f
        }

        // Section Title: Itemized Ledger
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ITEMIZED TRANSACTION LEDGER", margin, yCursor + 12f, paint)
        yCursor += 22f

        // Table Header
        drawTableHeader(canvas, margin, yCursor, contentWidth)
        yCursor += 24f

        val rowHeight = 22f
        val footerMargin = 40f
        val txDateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        filteredTx.forEachIndexed { index, tx ->
            // Check page overflow
            if (yCursor + rowHeight > pageHeight - footerMargin) {
                // Draw footer for current page
                drawPageFooter(canvas, margin, pageHeight - 20f, contentWidth, currentPageNum)
                pdfDoc.finishPage(page)

                currentPageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
                page = pdfDoc.startPage(pageInfo)
                canvas = page.canvas

                // Compact continuation header
                paint.color = Color.rgb(5, 150, 105)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 36f, paint)

                paint.color = Color.WHITE
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Expense Manager Financial Statement (Continued)", margin, 22f, paint)

                yCursor = 50f
                drawTableHeader(canvas, margin, yCursor, contentWidth)
                yCursor += 24f
            }

            // Draw alternating row background
            if (index % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                paint.style = Paint.Style.FILL
                canvas.drawRect(margin, yCursor - 14f, margin + contentWidth, yCursor + 6f, paint)
            }

            // Columns positions:
            // Col 1: Date (55)
            // Col 2: Merchant / Note (140)
            // Col 3: Category (100)
            // Col 4: Payment / Account (85)
            // Col 5: Tags (65)
            // Col 6: Amount (78)
            val dateStr = txDateFormat.format(Date(tx.date))
            val merchantStr = if (tx.merchant.isNotBlank()) tx.merchant else (if (tx.notes.isNotBlank()) tx.notes else tx.category)
            val shortMerchant = if (merchantStr.length > 22) merchantStr.substring(0, 20) + ".." else merchantStr

            val catStr = if (tx.subcategory.isNotBlank()) "${tx.category} > ${tx.subcategory}" else tx.category
            val shortCat = if (catStr.length > 17) catStr.substring(0, 15) + ".." else catStr

            val payStr = when {
                tx.creditCardId != null -> cardMap[tx.creditCardId]?.cardName ?: "Credit Card"
                tx.bankAccountId != null -> bankMap[tx.bankAccountId]?.bankName ?: tx.paymentMethod
                else -> tx.paymentMethod
            }
            val shortPay = if (payStr.length > 14) payStr.substring(0, 12) + ".." else payStr

            val shortTag = if (tx.tagsString.isNotBlank()) {
                val first = tx.tags.firstOrNull() ?: ""
                if (first.length > 11) first.substring(0, 9) + ".." else first
            } else "-"

            val isIncome = tx.type == "INCOME" || tx.type == "REFUND"
            val amtPrefix = if (isIncome) "+₹" else "-₹"
            val amtStr = amtPrefix + String.format(Locale.US, "%,.2f", tx.amount)

            // Draw text cells
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f
            paint.color = Color.rgb(71, 85, 105)
            canvas.drawText(dateStr, margin + 4f, yCursor, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(shortMerchant, margin + 60f, yCursor, paint)

            paint.color = Color.rgb(51, 65, 85)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(shortCat, margin + 195f, yCursor, paint)
            canvas.drawText(shortPay, margin + 295f, yCursor, paint)

            // Tag badge style
            if (shortTag != "-") {
                paint.color = Color.rgb(217, 119, 6)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            } else {
                paint.color = Color.rgb(148, 163, 184)
            }
            canvas.drawText(shortTag, margin + 380f, yCursor, paint)

            // Amount
            paint.color = if (isIncome) Color.rgb(16, 185, 129) else Color.rgb(220, 38, 38)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val amtWidth = paint.measureText(amtStr)
            canvas.drawText(amtStr, margin + contentWidth - 4f - amtWidth, yCursor, paint)

            yCursor += rowHeight
        }

        // Draw page footer on last page
        drawPageFooter(canvas, margin, pageHeight - 20f, contentWidth, currentPageNum)
        pdfDoc.finishPage(page)

        val outputStream = ByteArrayOutputStream()
        pdfDoc.writeTo(outputStream)
        pdfDoc.close()

        return outputStream.toByteArray()
    }

    private fun drawTableHeader(canvas: Canvas, margin: Float, y: Float, contentWidth: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(margin, y - 14f, margin + contentWidth, y + 8f), 4f, 4f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("DATE", margin + 4f, y, paint)
        canvas.drawText("MERCHANT / NOTE", margin + 60f, y, paint)
        canvas.drawText("CATEGORY", margin + 195f, y, paint)
        canvas.drawText("ACCOUNT / MODE", margin + 295f, y, paint)
        canvas.drawText("TAGS", margin + 380f, y, paint)

        val amtHeader = "AMOUNT (INR)"
        val amtW = paint.measureText(amtHeader)
        canvas.drawText(amtHeader, margin + contentWidth - 4f - amtW, y, paint)
    }

    private fun drawKpiCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        title: String,
        amount: String,
        amountColor: Int,
        bgFillColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgFillColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, paint)

        // Title
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, x + 8f, y + 18f, paint)

        // Amount
        paint.color = amountColor
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(amount, x + 8f, y + 38f, paint)
    }

    private fun drawPageFooter(canvas: Canvas, margin: Float, y: Float, contentWidth: Float, pageNum: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Confidential • Official Financial Statement", margin, y, paint)
        val pageStr = "Page $pageNum"
        val pw = paint.measureText(pageStr)
        canvas.drawText(pageStr, margin + contentWidth - pw, y, paint)
    }

    fun exportAndSharePdfStatement(
        context: Context,
        allTransactions: List<Transaction>,
        bankAccounts: List<BankAccount>,
        creditCards: List<CreditCard>,
        filter: StatementFilter
    ): Boolean {
        val pdfBytes = generatePdfStatementBytes(context, allTransactions, bankAccounts, creditCards, filter)
        val safePeriod = filter.periodLabel.replace(" ", "_").replace("/", "-")
        val fileName = "ExpenseManager_Statement_${safePeriod}_${fileDateFormat.format(Date())}.pdf"
        return FileShareUtils.saveAndShareFile(
            context = context,
            fileName = fileName,
            mimeType = "application/pdf",
            contentBytes = pdfBytes,
            chooserTitle = "Export Financial Statement (PDF)"
        )
    }
}
