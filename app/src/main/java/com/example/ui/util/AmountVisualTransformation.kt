package com.example.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.Locale

/**
 * Currency option descriptor for global multi-currency support.
 */
data class CurrencyOption(
    val symbol: String,
    val code: String,
    val name: String,
    val defaultNumberFormat: String // "INDIAN" or "INTERNATIONAL"
)

/**
 * Global configuration object for the active Number Formatting system and Currency.
 * - "AUTO": Automatically detects based on device country (India -> Indian Lakhs/Crores, others -> International)
 * - "INDIAN": Enforces Indian number grouping (e.g. 1,50,000.00)
 * - "INTERNATIONAL": Enforces Standard International 3-digit number grouping (e.g. 150,000.00)
 */
object NumberFormatConfig {
    var activePreference: String = "AUTO" // "AUTO", "INDIAN", "INTERNATIONAL"
    var currencySymbol: String = "₹"
    var currencyCode: String = "INR"

    val SUPPORTED_CURRENCIES = listOf(
        CurrencyOption("₹", "INR", "Indian Rupee (₹)", "INDIAN"),
        CurrencyOption("$", "USD", "US Dollar ($)", "INTERNATIONAL"),
        CurrencyOption("€", "EUR", "Euro (€)", "INTERNATIONAL"),
        CurrencyOption("£", "GBP", "British Pound (£)", "INTERNATIONAL"),
        CurrencyOption("AED", "AED", "UAE Dirham (AED)", "INTERNATIONAL"),
        CurrencyOption("CA$", "CAD", "Canadian Dollar (CA$)", "INTERNATIONAL"),
        CurrencyOption("A$", "AUD", "Australian Dollar (A$)", "INTERNATIONAL"),
        CurrencyOption("¥", "JPY", "Japanese Yen (¥)", "INTERNATIONAL"),
        CurrencyOption("SAR", "SAR", "Saudi Riyal (SAR)", "INTERNATIONAL"),
        CurrencyOption("SGD", "SGD", "Singapore Dollar (SGD)", "INTERNATIONAL"),
        CurrencyOption("CHF", "CHF", "Swiss Franc (CHF)", "INTERNATIONAL"),
        CurrencyOption("NZ$", "NZD", "New Zealand Dollar (NZ$)", "INTERNATIONAL"),
        CurrencyOption("kr", "SEK", "Swedish Krona (kr)", "INTERNATIONAL")
    )

    fun isIndian(): Boolean {
        return when (activePreference) {
            "INDIAN" -> true
            "INTERNATIONAL" -> false
            else -> {
                if (currencyCode == "INR" || currencySymbol == "₹") {
                    AmountVisualTransformation.isDefaultIndianLocale()
                } else {
                    false
                }
            }
        }
    }

    /**
     * Format a double amount using the active currency symbol and active number grouping.
     */
    fun formatAmount(amount: Double, withSymbol: Boolean = true, decimals: Int = 2): String {
        val sym = if (withSymbol) currencySymbol else ""
        val absAmount = kotlin.math.abs(amount)
        val intPart = absAmount.toLong().toString()
        val formattedInt = if (isIndian()) AmountVisualTransformation.formatIndian(intPart) else AmountVisualTransformation.formatInternational(intPart)
        val formattedStr = if (decimals > 0) {
            val decVal = String.format(Locale.US, "%.${decimals}f", absAmount)
            val decPart = if (decVal.contains(".")) decVal.substringAfter(".") else ""
            "$formattedInt.$decPart"
        } else {
            formattedInt
        }
        val prefix = if (amount < 0) "-" else ""
        return "$prefix$sym$formattedStr"
    }

    /**
     * Integer formatted amount with currency symbol (e.g. ₹5,000 or $5,000).
     */
    fun formatAmountInt(amount: Double, withSymbol: Boolean = true): String {
        return formatAmount(amount, withSymbol = withSymbol, decimals = 0)
    }
}

/**
 * VisualTransformation that dynamically formats number inputs with commas in real-time.
 * Automatically adapts between Indian numbering system (Lakhs/Crores, e.g. 1,50,000.00)
 * and International numbering system (Millions/Billions, e.g. 150,000.00) based on configuration or locale.
 */
class AmountVisualTransformation(
    private val isIndianLocale: Boolean = NumberFormatConfig.isIndian()
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        if (rawText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val parts = rawText.split(".", limit = 2)
        val intPart = parts[0]
        val hasDec = parts.size > 1
        val decPart = if (hasDec) parts[1] else ""

        val formattedInt = if (intPart.isEmpty()) {
            ""
        } else if (isIndianLocale) {
            formatIndian(intPart)
        } else {
            formatInternational(intPart)
        }

        val formattedText = if (hasDec) {
            "$formattedInt.$decPart"
        } else {
            formattedInt
        }

        val origToTrans = IntArray(rawText.length + 1)
        val transToOrig = IntArray(formattedText.length + 1)

        var origIdx = 0
        for (transIdx in formattedText.indices) {
            transToOrig[transIdx] = origIdx.coerceIn(0, rawText.length)
            val ch = formattedText[transIdx]
            if (ch != ',') {
                if (origIdx <= rawText.length) {
                    origToTrans[origIdx] = transIdx
                }
                origIdx++
            }
        }
        origToTrans[rawText.length] = formattedText.length
        transToOrig[formattedText.length] = rawText.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, rawText.length)
                return origToTrans[clamped].coerceIn(0, formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formattedText.length)
                return transToOrig[clamped].coerceIn(0, rawText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }

    companion object {
        fun isDefaultIndianLocale(): Boolean {
            val loc = Locale.getDefault()
            val country = loc.country.uppercase(Locale.ROOT)
            val lang = loc.language.lowercase(Locale.ROOT)
            return country == "IN" || country.isEmpty() || lang == "hi"
        }

        fun formatIndian(intPart: String): String {
            if (intPart.length <= 3) return intPart
            val last3 = intPart.takeLast(3)
            var rem = intPart.dropLast(3)
            val chunks = mutableListOf<String>()
            while (rem.length > 2) {
                chunks.add(0, rem.takeLast(2))
                rem = rem.dropLast(2)
            }
            if (rem.isNotEmpty()) {
                chunks.add(0, rem)
            }
            return chunks.joinToString(",") + "," + last3
        }

        fun formatInternational(intPart: String): String {
            if (intPart.length <= 3) return intPart
            var rem = intPart
            val chunks = mutableListOf<String>()
            while (rem.length > 3) {
                chunks.add(0, rem.takeLast(3))
                rem = rem.dropLast(3)
            }
            if (rem.isNotEmpty()) {
                chunks.add(0, rem)
            }
            return chunks.joinToString(",")
        }

        fun sanitizeAmountInput(input: String, maxDecimals: Int = 2): String {
            val filtered = input.filter { it.isDigit() || it == '.' }
            val dotIndex = filtered.indexOf('.')
            return if (dotIndex != -1) {
                val beforeDot = filtered.substring(0, dotIndex)
                val afterDot = filtered.substring(dotIndex + 1).replace(".", "")
                if (maxDecimals > 0) {
                    "$beforeDot.${afterDot.take(maxDecimals)}"
                } else {
                    beforeDot
                }
            } else {
                filtered
            }
        }
    }
}
