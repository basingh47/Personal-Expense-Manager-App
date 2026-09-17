package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val amount: Double,
    val category: String,
    val subcategory: String = "",
    val paymentMethod: String, // Cash, UPI, Credit Card, Bank
    val merchant: String = "",
    val notes: String = "",
    val tagsString: String = "", // Comma-separated tags
    val type: String, // "EXPENSE", "INCOME", "TRANSFER", "REFUND"
    val assetId: Long? = null, // For linking to a vehicle/asset
    val creditCardId: Long? = null, // For linking to a credit card
    val bankAccountId: Long? = null, // For linking to source bank account
    val toBankAccountId: Long? = null, // For inter-account transfers (destination bank account)
    val toCreditCardId: Long? = null // For credit card bill payments (destination card paid)
) {
    val tags: List<String>
        get() = if (tagsString.isBlank()) emptyList() else tagsString.split(",").map { it.trim() }
}

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. "Ola Scooter"
    val type: String, // "VEHICLE", "ELECTRONICS", "OTHER"
    val purchaseDate: Long,
    val purchasePrice: Double,
    val insuranceDetails: String = "",
    val warrantyDetails: String = "",
    val notes: String = ""
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val limitAmount: Double,
    val monthYear: String // e.g. "2026-07"
)

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. "Netflix"
    val cost: Double,
    val renewalDate: Long,
    val billingCycle: String, // "MONTHLY", "YEARLY", "2_MONTHS", "3_MONTHS", "6_MONTHS", etc.
    val isActive: Boolean = true,
    val category: String = "",
    val notes: String = "",
    val isAutoRenew: Boolean = true, // Auto-Renew ON / OFF
    val reminderDaysInAdvance: Int = 2, // How many days prior to notify (0, 1, 2, 3, 5, 7, 14 days)
    val paymentAccountId: Long? = null, // Linked bank account
    val paymentCardId: Long? = null, // Linked credit card
    val paymentMethodName: String = "", // e.g. "HDFC Bank", "ICICI Amazon Pay", "UPI AutoPay"
    val lastPaidDate: Long? = null // Timestamp of last payment
)

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. "Laptop"
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long,
    val notes: String = ""
)

@Entity(tableName = "borrow_lend")
data class BorrowLend(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val amount: Double,
    val type: String, // "BORROWED" or "LENT"
    val dueDate: Long,
    val isPaid: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "wishlist")
data class Wishlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double,
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val targetDate: Long,
    val notes: String = "",
    val isPurchased: Boolean = false,
    val purchasedTransactionId: Long? = null
)

@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String, // Emoji
    val type: String, // "EXPENSE" or "INCOME"
    val parentCategory: String? = null // If it's a subcategory, this is the parent category name; if null, it's a main category!
)

@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardName: String,
    val billingDay: Int, // 1 to 31
    val dueDay: Int, // 1 to 31
    val cardLimit: Double,
    val lastFourDigits: String = ""
)

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankName: String, // e.g. "HDFC Bank", "State Bank of India", "ICICI Bank", "Axis Bank", "Kotak", "Custom"
    val accountNickname: String, // e.g. "Salary Account", "Savings", "Emergency Fund"
    val accountNumberLast4: String = "", // e.g. "4589" for SMS pattern matching
    val accountType: String = "SAVINGS", // "SAVINGS", "CURRENT", "SALARY"
    val initialBalance: Double = 0.0,
    val isSmsDetectionEnabled: Boolean = true, // Whether to track SMS for this specific bank
    val isHiddenFromSummary: Boolean = false // Whether to hide/exclude from global totals & analytics
)

@Entity(tableName = "pending_sms_transactions")
data class PendingSmsTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val amount: Double,
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val merchant: String = "",
    val rawSender: String = "",
    val rawBody: String = "",
    val suggestedCategory: String = "Food & Drinks",
    val paymentMethod: String = "UPI", // Cash, UPI, Credit Card, Bank
    val creditCardId: Long? = null,
    val bankAccountId: Long? = null,
    val lastFourDigits: String = ""
)

