package com.example.export

import android.content.Context
import com.example.data.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class UserPreferencesBackup(
    val cashOnlyMode: Boolean = false,
    val numberFormatPreference: String = "AUTO",
    val smsDetectionEnabled: Boolean = true,
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val isDarkTheme: Boolean? = null
)

data class FullBackupData(
    val transactions: List<Transaction>,
    val bankAccounts: List<BankAccount>,
    val creditCards: List<CreditCard>,
    val customCategories: List<CustomCategory>,
    val budgets: List<Budget>,
    val assets: List<Asset>,
    val subscriptions: List<Subscription>,
    val savingsGoals: List<SavingsGoal>,
    val borrowLends: List<BorrowLend>,
    val wishlists: List<Wishlist>,
    val userPreferences: UserPreferencesBackup? = null
)

data class BackupStats(
    val appVersion: String,
    val backupTimestamp: Long,
    val backupDateStr: String,
    val totalTransactions: Int,
    val totalBankAccounts: Int,
    val totalCreditCards: Int,
    val totalCategories: Int,
    val totalBudgets: Int,
    val totalAssets: Int,
    val totalSubscriptions: Int,
    val totalSavingsGoals: Int,
    val totalBorrowLends: Int,
    val totalWishlist: Int,
    val hasUserPreferences: Boolean = false
)

object BackupEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun buildBackupJsonString(data: FullBackupData): String {
        val root = JSONObject()
        val now = System.currentTimeMillis()

        root.put("app_name", "Expense Manager")
        root.put("schema_version", 1)
        root.put("export_timestamp", now)
        root.put("export_date", dateFormat.format(Date(now)))

        // 1. Transactions
        val txArray = JSONArray()
        data.transactions.forEach { tx ->
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("date", tx.date)
                put("amount", tx.amount)
                put("category", tx.category)
                put("subcategory", tx.subcategory)
                put("paymentMethod", tx.paymentMethod)
                put("merchant", tx.merchant)
                put("notes", tx.notes)
                put("tagsString", tx.tagsString)
                put("type", tx.type)
                tx.assetId?.let { put("assetId", it) }
                tx.creditCardId?.let { put("creditCardId", it) }
                tx.bankAccountId?.let { put("bankAccountId", it) }
                tx.toBankAccountId?.let { put("toBankAccountId", it) }
                tx.toCreditCardId?.let { put("toCreditCardId", it) }
            }
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // 2. Bank Accounts
        val bankArray = JSONArray()
        data.bankAccounts.forEach { b ->
            val obj = JSONObject().apply {
                put("id", b.id)
                put("bankName", b.bankName)
                put("accountNickname", b.accountNickname)
                put("accountNumberLast4", b.accountNumberLast4)
                put("accountType", b.accountType)
                put("initialBalance", b.initialBalance)
                put("isSmsDetectionEnabled", b.isSmsDetectionEnabled)
                put("isHiddenFromSummary", b.isHiddenFromSummary)
            }
            bankArray.put(obj)
        }
        root.put("bank_accounts", bankArray)

        // 3. Credit Cards
        val cardArray = JSONArray()
        data.creditCards.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("cardName", c.cardName)
                put("billingDay", c.billingDay)
                put("dueDay", c.dueDay)
                put("cardLimit", c.cardLimit)
                put("lastFourDigits", c.lastFourDigits)
            }
            cardArray.put(obj)
        }
        root.put("credit_cards", cardArray)

        // 4. Custom Categories
        val catArray = JSONArray()
        data.customCategories.forEach { cat ->
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("icon", cat.icon)
                put("type", cat.type)
                cat.parentCategory?.let { put("parentCategory", it) }
            }
            catArray.put(obj)
        }
        root.put("custom_categories", catArray)

        // 5. Budgets
        val budgetArray = JSONArray()
        data.budgets.forEach { bg ->
            val obj = JSONObject().apply {
                put("id", bg.id)
                put("category", bg.category)
                put("limitAmount", bg.limitAmount)
                put("monthYear", bg.monthYear)
            }
            budgetArray.put(obj)
        }
        root.put("budgets", budgetArray)

        // 6. Assets
        val assetArray = JSONArray()
        data.assets.forEach { a ->
            val obj = JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("type", a.type)
                put("purchaseDate", a.purchaseDate)
                put("purchasePrice", a.purchasePrice)
                put("insuranceDetails", a.insuranceDetails)
                put("warrantyDetails", a.warrantyDetails)
                put("notes", a.notes)
            }
            assetArray.put(obj)
        }
        root.put("assets", assetArray)

        // 7. Subscriptions
        val subArray = JSONArray()
        data.subscriptions.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("cost", s.cost)
                put("renewalDate", s.renewalDate)
                put("billingCycle", s.billingCycle)
                put("isActive", s.isActive)
                put("category", s.category)
                put("notes", s.notes)
                put("isAutoRenew", s.isAutoRenew)
                put("reminderDaysInAdvance", s.reminderDaysInAdvance)
                s.paymentAccountId?.let { put("paymentAccountId", it) }
                s.paymentCardId?.let { put("paymentCardId", it) }
                put("paymentMethodName", s.paymentMethodName)
                s.lastPaidDate?.let { put("lastPaidDate", it) }
            }
            subArray.put(obj)
        }
        root.put("subscriptions", subArray)

        // 8. Savings Goals
        val goalArray = JSONArray()
        data.savingsGoals.forEach { g ->
            val obj = JSONObject().apply {
                put("id", g.id)
                put("name", g.name)
                put("targetAmount", g.targetAmount)
                put("currentAmount", g.currentAmount)
                put("targetDate", g.targetDate)
                put("notes", g.notes)
            }
            goalArray.put(obj)
        }
        root.put("savings_goals", goalArray)

        // 9. Borrow & Lend
        val blArray = JSONArray()
        data.borrowLends.forEach { bl ->
            val obj = JSONObject().apply {
                put("id", bl.id)
                put("contactName", bl.contactName)
                put("amount", bl.amount)
                put("type", bl.type)
                put("dueDate", bl.dueDate)
                put("isPaid", bl.isPaid)
                put("notes", bl.notes)
            }
            blArray.put(obj)
        }
        root.put("borrow_lend", blArray)

        // 10. Wishlist
        val wishArray = JSONArray()
        data.wishlists.forEach { w ->
            val obj = JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                put("price", w.price)
                put("priority", w.priority)
                put("targetDate", w.targetDate)
                put("notes", w.notes)
                put("isPurchased", w.isPurchased)
                if (w.purchasedTransactionId != null) {
                    put("purchasedTransactionId", w.purchasedTransactionId)
                }
            }
            wishArray.put(obj)
        }
        root.put("wishlist", wishArray)

        // 11. User Preferences (Optional)
        data.userPreferences?.let { prefs ->
            val prefObj = JSONObject().apply {
                put("cash_only_mode", prefs.cashOnlyMode)
                put("number_format_preference", prefs.numberFormatPreference)
                put("sms_detection_enabled", prefs.smsDetectionEnabled)
                put("currency_symbol", prefs.currencySymbol)
                put("currency_code", prefs.currencyCode)
                prefs.isDarkTheme?.let { put("is_dark_theme", it) }
            }
            root.put("user_preferences", prefObj)
        }

        return root.toString(2)
    }

    fun exportAndShareBackup(context: Context, data: FullBackupData): Boolean {
        val jsonStr = buildBackupJsonString(data)
        val fileName = "ExpenseManager_Backup_${fileDateFormat.format(Date())}.json"
        return FileShareUtils.saveAndShareFile(
            context = context,
            fileName = fileName,
            mimeType = "application/json",
            contentBytes = jsonStr.toByteArray(Charsets.UTF_8),
            chooserTitle = "Export Full Backup (JSON)"
        )
    }

    fun parseBackupJson(jsonString: String): Pair<FullBackupData, BackupStats>? {
        return try {
            val root = JSONObject(jsonString)
            val appName = root.optString("app_name", "Expense Manager")
            val timestamp = root.optLong("export_timestamp", System.currentTimeMillis())
            val dateStr = root.optString("export_date", dateFormat.format(Date(timestamp)))

            // Parse transactions
            val txList = mutableListOf<Transaction>()
            val txArray = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until txArray.length()) {
                val o = txArray.getJSONObject(i)
                txList.add(
                    Transaction(
                        id = o.optLong("id", 0L),
                        date = o.optLong("date", System.currentTimeMillis()),
                        amount = o.optDouble("amount", 0.0),
                        category = o.optString("category", "Others"),
                        subcategory = o.optString("subcategory", ""),
                        paymentMethod = o.optString("paymentMethod", "UPI"),
                        merchant = o.optString("merchant", ""),
                        notes = o.optString("notes", ""),
                        tagsString = o.optString("tagsString", ""),
                        type = o.optString("type", "EXPENSE"),
                        assetId = if (o.has("assetId")) o.optLong("assetId") else null,
                        creditCardId = if (o.has("creditCardId")) o.optLong("creditCardId") else null,
                        bankAccountId = if (o.has("bankAccountId")) o.optLong("bankAccountId") else null,
                        toBankAccountId = if (o.has("toBankAccountId")) o.optLong("toBankAccountId") else null,
                        toCreditCardId = if (o.has("toCreditCardId")) o.optLong("toCreditCardId") else null
                    )
                )
            }

            // Parse Bank Accounts
            val bankList = mutableListOf<BankAccount>()
            val bankArray = root.optJSONArray("bank_accounts") ?: JSONArray()
            for (i in 0 until bankArray.length()) {
                val o = bankArray.getJSONObject(i)
                bankList.add(
                    BankAccount(
                        id = o.optLong("id", 0L),
                        bankName = o.optString("bankName", ""),
                        accountNickname = o.optString("accountNickname", ""),
                        accountNumberLast4 = o.optString("accountNumberLast4", ""),
                        accountType = o.optString("accountType", "SAVINGS"),
                        initialBalance = o.optDouble("initialBalance", 0.0),
                        isSmsDetectionEnabled = o.optBoolean("isSmsDetectionEnabled", true),
                        isHiddenFromSummary = o.optBoolean("isHiddenFromSummary", false)
                    )
                )
            }

            // Parse Credit Cards
            val cardList = mutableListOf<CreditCard>()
            val cardArray = root.optJSONArray("credit_cards") ?: JSONArray()
            for (i in 0 until cardArray.length()) {
                val o = cardArray.getJSONObject(i)
                cardList.add(
                    CreditCard(
                        id = o.optLong("id", 0L),
                        cardName = o.optString("cardName", ""),
                        billingDay = o.optInt("billingDay", 20),
                        dueDay = o.optInt("dueDay", 9),
                        cardLimit = o.optDouble("cardLimit", 0.0),
                        lastFourDigits = o.optString("lastFourDigits", "")
                    )
                )
            }

            // Parse Custom Categories
            val catList = mutableListOf<CustomCategory>()
            val catArray = root.optJSONArray("custom_categories") ?: JSONArray()
            for (i in 0 until catArray.length()) {
                val o = catArray.getJSONObject(i)
                catList.add(
                    CustomCategory(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", ""),
                        icon = o.optString("icon", ""),
                        type = o.optString("type", "EXPENSE"),
                        parentCategory = if (o.has("parentCategory") && !o.isNull("parentCategory")) o.optString("parentCategory") else null
                    )
                )
            }

            // Parse Budgets
            val budgetList = mutableListOf<Budget>()
            val budgetArray = root.optJSONArray("budgets") ?: JSONArray()
            for (i in 0 until budgetArray.length()) {
                val o = budgetArray.getJSONObject(i)
                budgetList.add(
                    Budget(
                        id = o.optLong("id", 0L),
                        category = o.optString("category", ""),
                        limitAmount = o.optDouble("limitAmount", 0.0),
                        monthYear = o.optString("monthYear", "")
                    )
                )
            }

            // Parse Assets
            val assetList = mutableListOf<Asset>()
            val assetArray = root.optJSONArray("assets") ?: JSONArray()
            for (i in 0 until assetArray.length()) {
                val o = assetArray.getJSONObject(i)
                assetList.add(
                    Asset(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", ""),
                        type = o.optString("type", "VEHICLE"),
                        purchaseDate = o.optLong("purchaseDate", 0L),
                        purchasePrice = o.optDouble("purchasePrice", 0.0),
                        insuranceDetails = o.optString("insuranceDetails", ""),
                        warrantyDetails = o.optString("warrantyDetails", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }

            // Parse Subscriptions
            val subList = mutableListOf<Subscription>()
            val subArray = root.optJSONArray("subscriptions") ?: JSONArray()
            for (i in 0 until subArray.length()) {
                val o = subArray.getJSONObject(i)
                subList.add(
                    Subscription(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", ""),
                        cost = o.optDouble("cost", 0.0),
                        renewalDate = o.optLong("renewalDate", 0L),
                        billingCycle = o.optString("billingCycle", "MONTHLY"),
                        isActive = o.optBoolean("isActive", true),
                        category = o.optString("category", ""),
                        notes = o.optString("notes", ""),
                        isAutoRenew = o.optBoolean("isAutoRenew", true),
                        reminderDaysInAdvance = o.optInt("reminderDaysInAdvance", 2),
                        paymentAccountId = if (o.has("paymentAccountId") && !o.isNull("paymentAccountId")) o.optLong("paymentAccountId") else null,
                        paymentCardId = if (o.has("paymentCardId") && !o.isNull("paymentCardId")) o.optLong("paymentCardId") else null,
                        paymentMethodName = o.optString("paymentMethodName", ""),
                        lastPaidDate = if (o.has("lastPaidDate") && !o.isNull("lastPaidDate")) o.optLong("lastPaidDate") else null
                    )
                )
            }

            // Parse Savings Goals
            val goalList = mutableListOf<SavingsGoal>()
            val goalArray = root.optJSONArray("savings_goals") ?: JSONArray()
            for (i in 0 until goalArray.length()) {
                val o = goalArray.getJSONObject(i)
                goalList.add(
                    SavingsGoal(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", ""),
                        targetAmount = o.optDouble("targetAmount", 0.0),
                        currentAmount = o.optDouble("currentAmount", 0.0),
                        targetDate = o.optLong("targetDate", 0L),
                        notes = o.optString("notes", "")
                    )
                )
            }

            // Parse Borrow & Lend
            val blList = mutableListOf<BorrowLend>()
            val blArray = root.optJSONArray("borrow_lend") ?: JSONArray()
            for (i in 0 until blArray.length()) {
                val o = blArray.getJSONObject(i)
                blList.add(
                    BorrowLend(
                        id = o.optLong("id", 0L),
                        contactName = o.optString("contactName", ""),
                        amount = o.optDouble("amount", 0.0),
                        type = o.optString("type", "BORROWED"),
                        dueDate = o.optLong("dueDate", 0L),
                        isPaid = o.optBoolean("isPaid", false),
                        notes = o.optString("notes", "")
                    )
                )
            }

            // Parse Wishlist
            val wishList = mutableListOf<Wishlist>()
            val wishArray = root.optJSONArray("wishlist") ?: JSONArray()
            for (i in 0 until wishArray.length()) {
                val o = wishArray.getJSONObject(i)
                wishList.add(
                    Wishlist(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", ""),
                        price = o.optDouble("price", 0.0),
                        priority = o.optString("priority", "MEDIUM"),
                        targetDate = o.optLong("targetDate", 0L),
                        notes = o.optString("notes", ""),
                        isPurchased = o.optBoolean("isPurchased", false),
                        purchasedTransactionId = if (o.has("purchasedTransactionId") && !o.isNull("purchasedTransactionId")) o.optLong("purchasedTransactionId") else null
                    )
                )
            }

            // Parse User Preferences (Optional)
            val userPreferences = if (root.has("user_preferences") || root.has("userPreferences")) {
                val pObj = root.optJSONObject("user_preferences") ?: root.optJSONObject("userPreferences")
                if (pObj != null) {
                    UserPreferencesBackup(
                        cashOnlyMode = if (pObj.has("cash_only_mode")) pObj.optBoolean("cash_only_mode") else pObj.optBoolean("cashOnlyMode", false),
                        numberFormatPreference = if (pObj.has("number_format_preference")) pObj.optString("number_format_preference", "AUTO") else pObj.optString("numberFormatPreference", "AUTO"),
                        smsDetectionEnabled = if (pObj.has("sms_detection_enabled")) pObj.optBoolean("sms_detection_enabled", true) else pObj.optBoolean("smsDetectionEnabled", true),
                        currencySymbol = if (pObj.has("currency_symbol")) pObj.optString("currency_symbol", "₹") else pObj.optString("currencySymbol", "₹"),
                        currencyCode = if (pObj.has("currency_code")) pObj.optString("currency_code", "INR") else pObj.optString("currencyCode", "INR"),
                        isDarkTheme = if (pObj.has("is_dark_theme")) pObj.optBoolean("is_dark_theme") else if (pObj.has("isDarkTheme")) pObj.optBoolean("isDarkTheme") else null
                    )
                } else null
            } else null

            val data = FullBackupData(
                transactions = txList,
                bankAccounts = bankList,
                creditCards = cardList,
                customCategories = catList,
                budgets = budgetList,
                assets = assetList,
                subscriptions = subList,
                savingsGoals = goalList,
                borrowLends = blList,
                wishlists = wishList,
                userPreferences = userPreferences
            )

            val stats = BackupStats(
                appVersion = appName,
                backupTimestamp = timestamp,
                backupDateStr = dateStr,
                totalTransactions = txList.size,
                totalBankAccounts = bankList.size,
                totalCreditCards = cardList.size,
                totalCategories = catList.size,
                totalBudgets = budgetList.size,
                totalAssets = assetList.size,
                totalSubscriptions = subList.size,
                totalSavingsGoals = goalList.size,
                totalBorrowLends = blList.size,
                totalWishlist = wishList.size,
                hasUserPreferences = userPreferences != null
            )

            Pair(data, stats)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
