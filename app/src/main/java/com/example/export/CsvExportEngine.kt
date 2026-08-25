package com.example.export

import android.content.Context
import com.example.data.BankAccount
import com.example.data.CreditCard
import com.example.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

object CsvExportEngine {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val fileDateFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun generateCsv(
        transactions: List<Transaction>,
        bankAccounts: List<BankAccount>,
        creditCards: List<CreditCard>,
        filterDescription: String = "All Transactions"
    ): String {
        val sb = StringBuilder()
        val bankMap = bankAccounts.associateBy { it.id }
        val cardMap = creditCards.associateBy { it.id }

        // CSV Header Metadata
        sb.append("# Expense Manager - Financial Data Export\n")
        sb.append("# Generated Date: ${dateFormatter.format(Date())} ${timeFormatter.format(Date())}\n")
        sb.append("# Scope / Filter: ${escapeCsv(filterDescription)}\n")
        sb.append("# Total Records: ${transactions.size}\n\n")

        // CSV Column Headers
        sb.append("Transaction ID,Date,Time,Type,Amount (INR),Category,Subcategory,Merchant / Payee,Payment Mode,Bank Account,Credit Card,Project & Tax Tags,Notes\n")

        transactions.sortedByDescending { it.date }.forEach { tx ->
            val dateStr = dateFormatter.format(Date(tx.date))
            val timeStr = timeFormatter.format(Date(tx.date))
            val bankName = tx.bankAccountId?.let { bankMap[it]?.let { b -> "${b.bankName} (${b.accountNickname})" } } ?: ""
            val cardName = tx.creditCardId?.let { cardMap[it]?.cardName } ?: ""

            sb.append("${tx.id},")
            sb.append("$dateStr,")
            sb.append("$timeStr,")
            sb.append("${escapeCsv(tx.type)},")
            sb.append(String.format(Locale.US, "%.2f", tx.amount)).append(",")
            sb.append("${escapeCsv(tx.category)},")
            sb.append("${escapeCsv(tx.subcategory)},")
            sb.append("${escapeCsv(tx.merchant)},")
            sb.append("${escapeCsv(tx.paymentMethod)},")
            sb.append("${escapeCsv(bankName)},")
            sb.append("${escapeCsv(cardName)},")
            sb.append("${escapeCsv(tx.tagsString)},")
            sb.append("${escapeCsv(tx.notes)}\n")
        }

        return sb.toString()
    }

    fun exportAndShareCsv(
        context: Context,
        transactions: List<Transaction>,
        bankAccounts: List<BankAccount>,
        creditCards: List<CreditCard>,
        filterDescription: String = "All Transactions"
    ): Boolean {
        val csvContent = generateCsv(transactions, bankAccounts, creditCards, filterDescription)
        val fileName = "ExpenseManager_Report_${fileDateFormatter.format(Date())}.csv"
        return FileShareUtils.saveAndShareFile(
            context = context,
            fileName = fileName,
            mimeType = "text/csv",
            contentBytes = csvContent.toByteArray(Charsets.UTF_8),
            chooserTitle = "Export Detailed Excel/CSV Report"
        )
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
