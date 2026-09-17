package com.example.sms

import com.example.data.CustomCategory
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsTransaction(
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val merchant: String,
    val paymentMethod: String, // Cash, UPI, Credit Card, Bank
    val lastFourDigits: String,
    val suggestedCategory: String,
    val rawSender: String,
    val rawBody: String,
    val timestamp: Long = System.currentTimeMillis()
)

object SmsParser {

    // Regex for extracting amounts (e.g. INR 450.00, Rs.5000, Rs. 1,200.50, ₹350, USD 20, $50, €75, AED 120, ¥1500)
    private val amountPatterns = listOf(
        Pattern.compile("""(?:INR|Rs|₹|USD|\$|EUR|€|GBP|£|AED|CAD|AUD|JPY|¥|SAR|SGD|CHF|NZD|SEK|kr)\.?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:amount|spent|debited|credited|paid|received|txn of|charge of|withdrawn)\s*(?:of\s*)?(?:INR|Rs|₹|USD|\$|EUR|€|GBP|£|AED|CAD|AUD|JPY|¥|SAR|SGD|CHF|NZD|SEK|kr)?\.?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE)
    )

    private val refundKeywords = listOf(
        "refund", "cashback", "reversed", "reversal", "refunded", "credited back"
    )

    private val expenseKeywords = listOf(
        "debited", "spent", "paid", "sent", "purchase", "txn of", "txn for",
        "charged", "withdrawn", "transferred to", "used at", "swiped at", "deducted"
    )

    private val incomeKeywords = listOf(
        "credited", "received", "deposited", "salary", "added to your"
    )

    private val cardPatterns = listOf(
        Pattern.compile("""(?:a\/c|acct|account|card|ending|ending with|no\.?|num)[\s:\.\-]*[xX*]*\s*([0-9]{4})\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b[xX*]{1,}[\s:\-]*([0-9]{4})\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""([0-9]{4})\s*(?:debited|credited|used|swiped)""", Pattern.CASE_INSENSITIVE)
    )

    private val merchantPatterns = listOf(
        Pattern.compile("""(?:at|to|info\/|towards|for)\s+([A-Za-z0-9\s\.\&\-\_\@]+?)(?=\s+(?:on|using|through|via|avlbl|bal|balance|with|ref|upi|ref\s*no|a\/c|val|\.|$))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""VPA\s+([A-Za-z0-9\.\@\_\-]+)""", Pattern.CASE_INSENSITIVE)
    )

    fun parseSms(
        sender: String,
        body: String,
        customCategories: List<CustomCategory> = emptyList()
    ): ParsedSmsTransaction? {
        val lowerBody = body.lowercase(Locale.getDefault())

        // 1. Filter out pure OTP / promo spam messages without transaction verbs
        val isOtpOnly = (lowerBody.contains("otp") || lowerBody.contains("verification code") || lowerBody.contains("one time password")) &&
                !lowerBody.contains("debited") && !lowerBody.contains("credited") && !lowerBody.contains("spent") && !lowerBody.contains("paid")

        if (isOtpOnly) {
            return null
        }

        // 2. Determine Transaction Type
        var type: String? = null
        for (kw in refundKeywords) {
            if (lowerBody.contains(kw)) {
                type = "REFUND"
                break
            }
        }
        if (type == null) {
            for (kw in expenseKeywords) {
                if (lowerBody.contains(kw)) {
                    type = "EXPENSE"
                    break
                }
            }
        }
        if (type == null) {
            for (kw in incomeKeywords) {
                if (lowerBody.contains(kw)) {
                    type = "INCOME"
                    break
                }
            }
        }

        // If neither matched, it's not a financial transaction SMS
        if (type == null) {
            return null
        }

        // 3. Extract Amount
        var amount: Double? = null
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val rawAmountStr = matcher.group(1)?.replace(",", "")?.trim()
                val parsed = rawAmountStr?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    amount = parsed
                    break
                }
            }
        }

        if (amount == null) {
            return null
        }

        // 4. Extract Card / Account last 4 digits
        var lastFourDigits = ""
        for (pattern in cardPatterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val digits = matcher.group(1)?.trim() ?: ""
                if (digits.length == 4) {
                    lastFourDigits = digits
                    break
                }
            }
        }

        // 5. Extract Payment Method
        val hasAccount = lowerBody.contains("a/c") || lowerBody.contains("acct") || lowerBody.contains("account") || lowerBody.contains("atm ") || lowerBody.contains("withdrawn at") || lowerBody.contains("neft") || lowerBody.contains("imps") || lowerBody.contains("netbanking")
        val hasCreditCard = lowerBody.contains("credit card") || lowerBody.contains("creditcard") || lowerBody.contains("card ending") || lowerBody.contains("cc ending") || (lowerBody.contains("card") && (lowerBody.contains("swiped") || lowerBody.contains("spent on card") || lowerBody.contains("used on card") || lowerBody.contains("card limit")))
        val hasUpi = lowerBody.contains("upi") || lowerBody.contains("vpa") || lowerBody.contains("gpay") || lowerBody.contains("phonepe") || lowerBody.contains("paytm")

        val paymentMethod = when {
            hasAccount -> "Bank"
            hasCreditCard -> "Credit Card"
            hasUpi -> "UPI"
            lowerBody.contains("card") && !lowerBody.contains("block your card") && !lowerBody.contains("block card") -> "Credit Card"
            else -> if (type == "INCOME") "Bank" else "UPI"
        }

        // 6. Extract Merchant / Payee
        var merchant = ""
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val rawMerchant = matcher.group(1)?.trim() ?: ""
                if (rawMerchant.isNotBlank() && rawMerchant.length > 1 && !rawMerchant.equals("your", ignoreCase = true) && !rawMerchant.equals("a/c", ignoreCase = true)) {
                    merchant = cleanMerchantName(rawMerchant)
                    break
                }
            }
        }

        if (merchant.isBlank()) {
            // Fallback to sender or general phrase
            merchant = cleanSenderName(sender)
        }

        // 7. Predict Category with Custom Category Intelligence
        val suggestedCategory = guessCategory(merchant, lowerBody, type, customCategories)

        return ParsedSmsTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            paymentMethod = paymentMethod,
            lastFourDigits = lastFourDigits,
            suggestedCategory = suggestedCategory,
            rawSender = sender,
            rawBody = body
        )
    }

    private fun cleanMerchantName(raw: String): String {
        var clean = raw
            .replace(Regex("""^[0-9\.\-\_\s]+"""), "") // Remove leading numbers
            .replace(Regex("""[\.\,\;]+$"""), "") // Remove trailing punctuation
            .trim()

        if (clean.length > 30) {
            clean = clean.substring(0, 30).trim()
        }
        return if (clean.isBlank()) "Merchant" else clean
    }

    private fun cleanSenderName(sender: String): String {
        // E.g. "VK-HDFCBK" -> "HDFC Bank", "AX-ICICIB" -> "ICICI Bank"
        val clean = sender.replace(Regex("""^[A-Z]{2}-"""), "").trim()
        return when {
            clean.contains("HDFC", ignoreCase = true) -> "HDFC Bank"
            clean.contains("ICICI", ignoreCase = true) -> "ICICI Bank"
            clean.contains("SBI", ignoreCase = true) -> "SBI Bank"
            clean.contains("AXIS", ignoreCase = true) -> "Axis Bank"
            clean.contains("KOTAK", ignoreCase = true) -> "Kotak Bank"
            clean.contains("PAYTM", ignoreCase = true) -> "Paytm"
            clean.contains("AMZN", ignoreCase = true) || clean.contains("AMAZON", ignoreCase = true) -> "Amazon"
            clean.contains("SWIGGY", ignoreCase = true) -> "Swiggy"
            clean.contains("ZOMATO", ignoreCase = true) -> "Zomato"
            clean.isNotBlank() -> clean
            else -> "Bank Alert"
        }
    }

    private fun guessCategory(
        merchant: String,
        body: String,
        type: String,
        customCategories: List<CustomCategory> = emptyList()
    ): String {
        val text = (merchant + " " + body).lowercase(Locale.getDefault())
        val effectiveType = if (type == "REFUND") "INCOME" else type

        // Filter custom categories matching the transaction type (or type is empty)
        val relevantCustom = customCategories.filter {
            it.type.equals(effectiveType, ignoreCase = true) || it.type.equals(type, ignoreCase = true)
        }

        // 1. Direct name match or constituent word match against user custom categories and subcategories
        if (relevantCustom.isNotEmpty()) {
            for (custom in relevantCustom) {
                val catName = custom.name.trim().lowercase(Locale.getDefault())
                if (catName.length >= 3) {
                    if (text.contains(catName)) {
                        return custom.parentCategory ?: custom.name
                    }
                    val words = catName.split(" ", "/", "&", "-", "_").map { it.trim() }.filter { it.length >= 4 }
                    if (words.any { text.contains(it) }) {
                        return custom.parentCategory ?: custom.name
                    }
                }
            }
        }

        // 2. Income Categorization
        if (type == "INCOME" || effectiveType == "INCOME") {
            return when {
                body.contains("salary") || merchant.contains("salary", ignoreCase = true) || text.contains("payroll") || text.contains("stipend") -> {
                    findMatchingCategoryName(listOf("Salary", "Job", "Payroll", "Income"), relevantCustom) ?: "Salary"
                }
                body.contains("refund") || body.contains("cashback") || merchant.contains("refund", ignoreCase = true) || text.contains("reversal") -> {
                    findMatchingCategoryName(listOf("Refund", "Cashback", "Rewards"), relevantCustom) ?: "Refund"
                }
                text.contains("freelance") || text.contains("upwork") || text.contains("fiverr") || text.contains("consulting") -> {
                    findMatchingCategoryName(listOf("Freelancing", "Freelance", "Consulting", "Projects"), relevantCustom) ?: "Freelancing"
                }
                text.contains("rent") || text.contains("rental") || text.contains("tenant") -> {
                    findMatchingCategoryName(listOf("Rentals", "Rent", "Rental Income"), relevantCustom) ?: "Rentals"
                }
                text.contains("dividend") || text.contains("interest") || text.contains("capital gain") || text.contains("zerodha") || text.contains("groww") -> {
                    findMatchingCategoryName(listOf("Investment Payouts", "Investments", "Dividends", "Interest"), relevantCustom) ?: "Investment Payouts"
                }
                else -> {
                    findMatchingCategoryName(listOf("Other Income", "Others", "General Income"), relevantCustom) ?: "Other Income"
                }
            }
        }

        // 3. Expense Categorization with Custom Category Preference Mapping
        return when {
            // Food & Dining
            text.contains("swiggy") || text.contains("zomato") || text.contains("eatclub") ||
            text.contains("starbucks") || text.contains("mcdonald") || text.contains("kfc") ||
            text.contains("dominos") || text.contains("pizza") || text.contains("burger") ||
            text.contains("restaurant") || text.contains("cafe") || text.contains("food") ||
            text.contains("bakery") || text.contains("chai") || text.contains("tea") ||
            text.contains("barbeque") || text.contains("dine") -> {
                findMatchingCategoryName(listOf("Dining Out", "Food & Dining", "Dining", "Restaurants", "Food", "Food & Drinks"), relevantCustom) ?: "Food"
            }

            // Groceries & Marts
            text.contains("blinkit") || text.contains("zepto") || text.contains("instamart") ||
            text.contains("bigbasket") || text.contains("dmart") || text.contains("spencer") ||
            text.contains("grocery") || text.contains("groceries") || text.contains("supermart") ||
            text.contains("supermarket") || text.contains("milk") || text.contains("vegetable") ||
            text.contains("fruit") -> {
                findMatchingCategoryName(listOf("Groceries", "Grocery", "Daily Needs", "Supermarket", "Food"), relevantCustom) ?: "Food"
            }

            // Fuel & Vehicle
            text.contains("fuel") || text.contains("petrol") || text.contains("diesel") ||
            text.contains("indianoil") || text.contains("iocl") || text.contains("hpcl") ||
            text.contains("bpcl") || text.contains("shell") || text.contains("fastag") ||
            text.contains("parking") || text.contains("toll") || text.contains("challan") -> {
                findMatchingCategoryName(listOf("Fuel", "Petrol", "Vehicle", "Transportation", "Car", "Bike"), relevantCustom) ?: "Vehicle"
            }

            // Travel & Commute
            text.contains("uber") || text.contains("ola") || text.contains("rapido") ||
            text.contains("metro") || text.contains("irctc") || text.contains("flight") ||
            text.contains("indigo") || text.contains("airindia") || text.contains("makemytrip") ||
            text.contains("mmt") || text.contains("easemytrip") || text.contains("train") ||
            text.contains("taxi") || text.contains("cab") -> {
                findMatchingCategoryName(listOf("Cab", "Taxi", "Commute", "Travel / Commute", "Travel", "Transport"), relevantCustom) ?: "Travel / Commute"
            }

            // Shopping & Apparel
            text.contains("amazon") || text.contains("amzn") || text.contains("flipkart") ||
            text.contains("myntra") || text.contains("ajio") || text.contains("zara") ||
            text.contains("h&m") || text.contains("uniqlo") || text.contains("nykaa") ||
            text.contains("meesho") || text.contains("clothing") || text.contains("retail") ||
            text.contains("mall") || text.contains("store") -> {
                findMatchingCategoryName(listOf("Online Shopping", "Shopping", "Apparel", "Clothes", "E-Commerce"), relevantCustom) ?: "Shopping"
            }

            // Entertainment & Subscriptions
            text.contains("netflix") || text.contains("spotify") || text.contains("hotstar") ||
            text.contains("prime") || text.contains("youtube") || text.contains("apple") ||
            text.contains("pvr") || text.contains("inox") || text.contains("bookmyshow") ||
            text.contains("cinema") || text.contains("movie") || text.contains("game") ||
            text.contains("steam") || text.contains("playstation") -> {
                findMatchingCategoryName(listOf("OTT", "Movies", "Cinema", "Entertainment", "Gaming", "Subscriptions"), relevantCustom) ?: "Entertainment"
            }

            // Home / Bills & Utilities
            text.contains("airtel") || text.contains("jio") || text.contains("vi") ||
            text.contains("vodafone") || text.contains("recharge") || text.contains("bescom") ||
            text.contains("electricity") || text.contains("gas") || text.contains("lpg") ||
            text.contains("indane") || text.contains("water") || text.contains("broadband") ||
            text.contains("maintenance") || text.contains("rent") -> {
                findMatchingCategoryName(listOf("Utilities", "Bills & Utilities", "Bills", "Home", "Rent"), relevantCustom) ?: "Home"
            }

            // Health & Fitness
            text.contains("apollo") || text.contains("1mg") || text.contains("pharmeasy") ||
            text.contains("medplus") || text.contains("clinic") || text.contains("hospital") ||
            text.contains("doctor") || text.contains("pharmacy") || text.contains("medicines") -> {
                findMatchingCategoryName(listOf("Pharmacy", "Medical", "Healthcare", "Health"), relevantCustom) ?: "Health"
            }
            text.contains("cult") || text.contains("gym") || text.contains("fitness") -> {
                findMatchingCategoryName(listOf("Gym", "Fitness", "Health"), relevantCustom) ?: "Health"
            }

            // Insurance & Loans
            text.contains("emi") || text.contains("insurance") || text.contains("policybazaar") ||
            text.contains("lic") || text.contains("hdfc life") || text.contains("max life") || text.contains("loan") -> {
                findMatchingCategoryName(listOf("Loans", "EMI", "Insurance", "Insurance / Loan"), relevantCustom) ?: "Insurance / Loan"
            }

            // Work & Education
            text.contains("office") || text.contains("coworking") || text.contains("saas") -> {
                findMatchingCategoryName(listOf("Work / Office", "Office", "Work"), relevantCustom) ?: "Work / Office"
            }
            text.contains("college") || text.contains("school") || text.contains("udemy") ||
            text.contains("coursera") || text.contains("tuition") || text.contains("books") -> {
                findMatchingCategoryName(listOf("Education", "Courses", "Studies"), relevantCustom) ?: "Education"
            }

            // Pets
            text.contains("vet") || text.contains("pet") || text.contains("dog") || text.contains("cat") -> {
                findMatchingCategoryName(listOf("Pets", "Pet Care"), relevantCustom) ?: "Pets"
            }

            else -> {
                findMatchingCategoryName(listOf("Food", "Others", "General", "Miscellaneous"), relevantCustom) ?: "Food"
            }
        }
    }

    private fun findMatchingCategoryName(
        preferredNames: List<String>,
        customCategories: List<CustomCategory>
    ): String? {
        if (customCategories.isEmpty()) return null
        // 1. Exact case-insensitive match
        for (pref in preferredNames) {
            val match = customCategories.firstOrNull {
                it.name.equals(pref, ignoreCase = true) && it.parentCategory == null
            }
            if (match != null) return match.name
        }
        // 2. Contains / partial match (e.g. "Freelance" matches "Software Freelance")
        for (pref in preferredNames) {
            val partial = customCategories.firstOrNull {
                (it.name.contains(pref, ignoreCase = true) || pref.contains(it.name, ignoreCase = true)) && it.parentCategory == null
            }
            if (partial != null) return partial.name
        }
        return null
    }
}
