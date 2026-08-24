package com.example.sms

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

    // Regex for extracting amounts (e.g. INR 450.00, Rs. 1,200.50, ₹350, USD 20)
    private val amountPatterns = listOf(
        Pattern.compile("""(?:INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:amount|spent|debited|credited|paid|received|txn of|charge of)\s*(?:of\s*)?(?:INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£)?\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE)
    )

    private val expenseKeywords = listOf(
        "debited", "spent", "paid", "sent", "purchase", "txn of", "txn for",
        "charged", "withdrawn", "transferred to", "used at", "swiped at", "deducted"
    )

    private val incomeKeywords = listOf(
        "credited", "received", "deposited", "refund", "cashback", "salary", "added to your", "reversed"
    )

    private val cardPatterns = listOf(
        Pattern.compile("""(?:card|ending|xx|x{2,}|[*]{2,}|ending with|a/c|acct)[\s:]*([0-9]{4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""([0-9]{4})\s*(?:debited|credited|used)""", Pattern.CASE_INSENSITIVE)
    )

    private val merchantPatterns = listOf(
        Pattern.compile("""(?:at|to|info\/|towards|for)\s+([A-Za-z0-9\s\.\&\-\_\@]+?)(?=\s+(?:on|using|through|via|avlbl|bal|balance|with|ref|upi|ref\s*no|a\/c|val|\.|$))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""VPA\s+([A-Za-z0-9\.\@\_\-]+)""", Pattern.CASE_INSENSITIVE)
    )

    fun parseSms(sender: String, body: String): ParsedSmsTransaction? {
        val lowerBody = body.lowercase(Locale.getDefault())

        // 1. Filter out pure OTP / promo spam messages without transaction verbs
        val isOtpOnly = (lowerBody.contains("otp") || lowerBody.contains("verification code") || lowerBody.contains("one time password")) &&
                !lowerBody.contains("debited") && !lowerBody.contains("credited") && !lowerBody.contains("spent") && !lowerBody.contains("paid")

        if (isOtpOnly) {
            return null
        }

        // 2. Determine Transaction Type
        var type: String? = null
        for (kw in expenseKeywords) {
            if (lowerBody.contains(kw)) {
                type = "EXPENSE"
                break
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

        // If neither expense nor income keyword matched, it's not a financial transaction SMS
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
        val paymentMethod = when {
            lowerBody.contains("credit card") || lowerBody.contains("card ending") || lowerBody.contains("cc ") || (lastFourDigits.isNotEmpty() && lowerBody.contains("card")) -> "Credit Card"
            lowerBody.contains("upi") || lowerBody.contains("vpa") || lowerBody.contains("gpay") || lowerBody.contains("phonepe") || lowerBody.contains("paytm") -> "UPI"
            lowerBody.contains("a/c") || lowerBody.contains("acct") || lowerBody.contains("account") || lowerBody.contains("neft") || lowerBody.contains("imps") || lowerBody.contains("netbanking") -> "Bank"
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

        // 7. Predict Category
        val suggestedCategory = guessCategory(merchant, lowerBody, type)

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

    private fun guessCategory(merchant: String, body: String, type: String): String {
        if (type == "INCOME") {
            return if (body.contains("salary") || merchant.contains("salary", ignoreCase = true)) "Salary"
            else if (body.contains("refund") || body.contains("cashback") || merchant.contains("refund", ignoreCase = true)) "Refund"
            else "Other Income"
        }

        val text = (merchant + " " + body).lowercase(Locale.getDefault())

        return when {
            // Food & Dining / Groceries
            text.contains("swiggy") || text.contains("zomato") || text.contains("eatclub") ||
            text.contains("starbucks") || text.contains("mcdonald") || text.contains("kfc") ||
            text.contains("dominos") || text.contains("pizza") || text.contains("burger") ||
            text.contains("restaurant") || text.contains("cafe") || text.contains("food") ||
            text.contains("bakery") || text.contains("chai") || text.contains("tea") ||
            text.contains("barbeque") || text.contains("dine") ||
            text.contains("blinkit") || text.contains("zepto") || text.contains("instamart") ||
            text.contains("bigbasket") || text.contains("dmart") || text.contains("spencer") ||
            text.contains("grocery") || text.contains("supermart") || text.contains("supermarket") ||
            text.contains("milk") || text.contains("vegetable") || text.contains("fruit") -> "Food"

            // Fuel & Vehicle
            text.contains("fuel") || text.contains("petrol") || text.contains("diesel") ||
            text.contains("indianoil") || text.contains("iocl") || text.contains("hpcl") ||
            text.contains("bpcl") || text.contains("shell") || text.contains("fastag") ||
            text.contains("parking") || text.contains("toll") || text.contains("challan") -> "Vehicle"

            // Travel & Commute
            text.contains("uber") || text.contains("ola") || text.contains("rapido") ||
            text.contains("metro") || text.contains("irctc") || text.contains("flight") ||
            text.contains("indigo") || text.contains("airindia") || text.contains("makemytrip") ||
            text.contains("mmt") || text.contains("easemytrip") || text.contains("train") -> "Travel / Commute"

            // Shopping
            text.contains("amazon") || text.contains("amzn") || text.contains("flipkart") ||
            text.contains("myntra") || text.contains("ajio") || text.contains("zara") ||
            text.contains("h&m") || text.contains("uniqlo") || text.contains("nykaa") ||
            text.contains("meesho") || text.contains("clothing") || text.contains("retail") ||
            text.contains("mall") || text.contains("store") -> "Shopping"

            // Entertainment
            text.contains("netflix") || text.contains("spotify") || text.contains("hotstar") ||
            text.contains("prime") || text.contains("youtube") || text.contains("apple") ||
            text.contains("pvr") || text.contains("inox") || text.contains("bookmyshow") ||
            text.contains("cinema") || text.contains("movie") || text.contains("game") ||
            text.contains("steam") || text.contains("playstation") -> "Entertainment"

            // Home / Bills & Utilities
            text.contains("airtel") || text.contains("jio") || text.contains("vi") ||
            text.contains("vodafone") || text.contains("recharge") || text.contains("bescom") ||
            text.contains("electricity") || text.contains("gas") || text.contains("lpg") ||
            text.contains("indane") || text.contains("water") || text.contains("broadband") ||
            text.contains("maintenance") || text.contains("rent") -> "Home"

            // Health
            text.contains("apollo") || text.contains("1mg") || text.contains("pharmeasy") ||
            text.contains("medplus") || text.contains("clinic") || text.contains("hospital") ||
            text.contains("doctor") || text.contains("pharmacy") || text.contains("cult") ||
            text.contains("gym") || text.contains("fitness") -> "Health"

            // Insurance & Loans
            text.contains("emi") || text.contains("insurance") || text.contains("policybazaar") ||
            text.contains("lic") || text.contains("hdfc life") || text.contains("max life") -> "Insurance / Loan"

            else -> "Food"
        }
    }
}
