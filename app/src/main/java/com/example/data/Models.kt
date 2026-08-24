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
    val type: String, // "EXPENSE" or "INCOME"
    val assetId: Long? = null, // For linking to a vehicle/asset
    val creditCardId: Long? = null // For linking to a credit card
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
    val billingCycle: String, // "MONTHLY", "YEARLY"
    val isActive: Boolean = true,
    val category: String = "",
    val notes: String = ""
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
    val isPurchased: Boolean = false
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
    val lastFourDigits: String = ""
)

