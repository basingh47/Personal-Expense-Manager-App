package com.example

import com.example.data.Subscription
import com.example.data.Transaction
import com.example.subscription.SubscriptionNotificationHelper
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class ExampleUnitTest {

    @Test
    fun testDaysUntilRenewalCalculation() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 3)
        }
        val days = SubscriptionNotificationHelper.calculateDaysUntilRenewal(cal.timeInMillis)
        assertEquals(3, days)
    }

    @Test
    fun testNotificationTriggerCondition() {
        val now = System.currentTimeMillis()

        // 1. Subscription due in 2 days with 3 days advance alert window -> should notify
        val calDueIn2Days = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 2)
        }
        val sub1 = Subscription(
            name = "Netflix",
            cost = 649.0,
            renewalDate = calDueIn2Days.timeInMillis,
            billingCycle = "MONTHLY",
            isActive = true,
            isAutoRenew = true,
            reminderDaysInAdvance = 3
        )
        val days1 = SubscriptionNotificationHelper.calculateDaysUntilRenewal(sub1.renewalDate)
        val shouldNotify1 = sub1.isActive && (days1 in 0..sub1.reminderDaysInAdvance || days1 < 0)
        assertTrue(shouldNotify1)

        // 2. Subscription due in 10 days with 2 days advance alert window -> should not notify yet
        val calDueIn10Days = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 10)
        }
        val sub2 = Subscription(
            name = "Spotify",
            cost = 149.0,
            renewalDate = calDueIn10Days.timeInMillis,
            billingCycle = "MONTHLY",
            isActive = true,
            isAutoRenew = true,
            reminderDaysInAdvance = 2
        )
        val days2 = SubscriptionNotificationHelper.calculateDaysUntilRenewal(sub2.renewalDate)
        val shouldNotify2 = sub2.isActive && (days2 in 0..sub2.reminderDaysInAdvance || days2 < 0)
        assertFalse(shouldNotify2)
    }

    @Test
    fun testNextCycleCalculation() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 12, 0, 0)
        }
        val baseDate = cal.timeInMillis

        // Monthly advance
        val nextMonthly = SubscriptionNotificationHelper.getNextCycleTimestamp(baseDate, "MONTHLY")
        val nextMonthlyCal = Calendar.getInstance().apply { timeInMillis = nextMonthly }
        assertEquals(Calendar.APRIL, nextMonthlyCal.get(Calendar.MONTH))
        assertEquals(15, nextMonthlyCal.get(Calendar.DAY_OF_MONTH))

        // Yearly advance
        val nextYearly = SubscriptionNotificationHelper.getNextCycleTimestamp(baseDate, "YEARLY")
        val nextYearlyCal = Calendar.getInstance().apply { timeInMillis = nextYearly }
        assertEquals(2027, nextYearlyCal.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, nextYearlyCal.get(Calendar.MONTH))
    }

    @Test
    fun testFormattedDaysLabel() {
        assertEquals("Renews Today", SubscriptionNotificationHelper.getFormattedDaysLeft(0))
        assertEquals("Renews Tomorrow", SubscriptionNotificationHelper.getFormattedDaysLeft(1))
        assertEquals("Renews in 5 days", SubscriptionNotificationHelper.getFormattedDaysLeft(5))
        assertEquals("Overdue by 2 days", SubscriptionNotificationHelper.getFormattedDaysLeft(-2))
    }

    @Test
    fun testSmsParserSbiAtmWithdrawal() {
        val rawSms = "Dear SBI Customer, Rs.5000 withdrawn at KMB ATM AHBN1031 from A/cX1488 on 27Aug26 Transaction Number 623908843394. Available Balance Rs.91095.47. If not withdrawn by you, forward this SMS to 7400165218 / call 1800111109 or 09449112211 to block your card. Call 18001234 if cash not received."
        val parsed = com.example.sms.SmsParser.parseSms("SBIINB", rawSms)
        assertNotNull(parsed)
        assertEquals(5000.0, parsed!!.amount, 0.001)
        assertEquals("1488", parsed.lastFourDigits)
        assertEquals("EXPENSE", parsed.type)
        assertEquals("Bank", parsed.paymentMethod)
    }

    @Test
    fun testSmsParserFormats() {
        val s1 = "HDFC Bank: Rs 1500.50 debited from a/c **4589 at Swiggy"
        val p1 = com.example.sms.SmsParser.parseSms("HDFCBK", s1)
        assertNotNull(p1)
        assertEquals(1500.50, p1!!.amount, 0.001)
        assertEquals("4589", p1.lastFourDigits)

        val s2 = "ICICI Bank: Acct XX8901 debited with INR 2,400.00 on 26-Aug-26"
        val p2 = com.example.sms.SmsParser.parseSms("ICICIB", s2)
        assertNotNull(p2)
        assertEquals(2400.0, p2!!.amount, 0.001)
        assertEquals("8901", p2.lastFourDigits)
    }

    @Test
    fun testAmountVisualTransformationIndianAndInternational() {
        val indianVt = com.example.ui.util.AmountVisualTransformation(isIndianLocale = true)
        val intlVt = com.example.ui.util.AmountVisualTransformation(isIndianLocale = false)

        val t1 = indianVt.filter(androidx.compose.ui.text.AnnotatedString("500000"))
        assertEquals("5,00,000", t1.text.text)
        assertEquals(0, t1.offsetMapping.originalToTransformed(0))
        assertEquals(2, t1.offsetMapping.originalToTransformed(1))
        assertEquals(8, t1.offsetMapping.originalToTransformed(6))
        assertEquals(6, t1.offsetMapping.transformedToOriginal(8))

        val t2 = intlVt.filter(androidx.compose.ui.text.AnnotatedString("5000000"))
        assertEquals("5,000,000", t2.text.text)

        val t3 = indianVt.filter(androidx.compose.ui.text.AnnotatedString("12345.50"))
        assertEquals("12,345.50", t3.text.text)

        val sanitized = com.example.ui.util.AmountVisualTransformation.sanitizeAmountInput("1,234.567")
        assertEquals("1234.56", sanitized)

        // Test NumberFormatConfig dynamic switching
        com.example.ui.util.NumberFormatConfig.activePreference = "INDIAN"
        assertTrue(com.example.ui.util.NumberFormatConfig.isIndian())
        val dynIndian = com.example.ui.util.AmountVisualTransformation().filter(androidx.compose.ui.text.AnnotatedString("2500000"))
        assertEquals("25,00,000", dynIndian.text.text)

        com.example.ui.util.NumberFormatConfig.activePreference = "INTERNATIONAL"
        assertFalse(com.example.ui.util.NumberFormatConfig.isIndian())
        val dynIntl = com.example.ui.util.AmountVisualTransformation().filter(androidx.compose.ui.text.AnnotatedString("2500000"))
        assertEquals("2,500,000", dynIntl.text.text)

        // Test Currency Formatting
        com.example.ui.util.NumberFormatConfig.currencySymbol = "$"
        com.example.ui.util.NumberFormatConfig.currencyCode = "USD"
        assertEquals("$2,500,000.00", com.example.ui.util.NumberFormatConfig.formatAmount(2500000.0))

        com.example.ui.util.NumberFormatConfig.currencySymbol = "€"
        com.example.ui.util.NumberFormatConfig.currencyCode = "EUR"
        assertEquals("€1,250.50", com.example.ui.util.NumberFormatConfig.formatAmount(1250.50))

        com.example.ui.util.NumberFormatConfig.activePreference = "INDIAN"
        com.example.ui.util.NumberFormatConfig.currencySymbol = "₹"
        com.example.ui.util.NumberFormatConfig.currencyCode = "INR"
        assertEquals("₹25,00,000.00", com.example.ui.util.NumberFormatConfig.formatAmount(2500000.0))
    }

    @Test
    fun testSmsParserCustomCategoriesMatching() {
        val customCats = listOf(
            com.example.data.CustomCategory(id = 1, name = "Dining Out", icon = "🍽️", type = "EXPENSE"),
            com.example.data.CustomCategory(id = 2, name = "Groceries", icon = "🛒", type = "EXPENSE"),
            com.example.data.CustomCategory(id = 3, name = "Cab", icon = "🚕", type = "EXPENSE"),
            com.example.data.CustomCategory(id = 4, name = "Gym", icon = "💪", type = "EXPENSE"),
            com.example.data.CustomCategory(id = 5, name = "Software Freelance", icon = "💻", type = "INCOME"),
            com.example.data.CustomCategory(id = 6, name = "Zomato Gold", icon = "✨", type = "EXPENSE", parentCategory = "Dining Out")
        )

        // 1. Test Swiggy maps to user custom category "Dining Out" instead of generic "Food"
        val swiggySms = "Alert: INR 450.00 debited on HDFC Bank Credit Card ending 4092 at SWIGGY on 24-AUG-26."
        val pSwiggy = com.example.sms.SmsParser.parseSms("VK-HDFCBK", swiggySms, customCats)
        assertNotNull(pSwiggy)
        assertEquals("Dining Out", pSwiggy!!.suggestedCategory)

        // 2. Test Blinkit maps to "Groceries"
        val blinkitSms = "Rs 850.00 paid to BLINKIT from A/c XX1234 on 25-Aug."
        val pBlinkit = com.example.sms.SmsParser.parseSms("HDFCBK", blinkitSms, customCats)
        assertNotNull(pBlinkit)
        assertEquals("Groceries", pBlinkit!!.suggestedCategory)

        // 3. Test Uber maps to "Cab"
        val uberSms = "Paid Rs. 350.00 to UBER RIDES via UPI."
        val pUber = com.example.sms.SmsParser.parseSms("PAYTM", uberSms, customCats)
        assertNotNull(pUber)
        assertEquals("Cab", pUber!!.suggestedCategory)

        // 4. Test Cult.fit maps to "Gym"
        val gymSms = "Your card was debited by INR 3000 for cult.fit membership renewal."
        val pGym = com.example.sms.SmsParser.parseSms("ICICIB", gymSms, customCats)
        assertNotNull(pGym)
        assertEquals("Gym", pGym!!.suggestedCategory)

        // 5. Test subcategory matching
        val zomatoSubSms = "Paid Rs 600 at ZOMATO GOLD subscription."
        val pZomato = com.example.sms.SmsParser.parseSms("KOTAK", zomatoSubSms, customCats)
        assertNotNull(pZomato)
        assertEquals("Dining Out", pZomato!!.suggestedCategory)

        // 6. Test income freelance custom category
        val incomeSms = "Your account is credited with INR 50000 for freelance consulting project."
        val pIncome = com.example.sms.SmsParser.parseSms("SBIINB", incomeSms, customCats)
        assertNotNull(pIncome)
        assertEquals("Software Freelance", pIncome!!.suggestedCategory)
    }

    @Test
    fun testCreditCardOutstandingAndRefundCalculation() {
        val card = com.example.data.CreditCard(
            id = 101L,
            cardName = "HDFC Regalia",
            billingDay = 15,
            dueDay = 5,
            cardLimit = 150000.0,
            lastFourDigits = "4092"
        )

        val txs = listOf(
            // Expense 1: Rs 20,000 on Flight Tickets
            com.example.data.Transaction(
                id = 1,
                date = System.currentTimeMillis() - 86400000 * 5,
                amount = 20000.0,
                category = "Travel",
                subcategory = "Flight",
                paymentMethod = "Credit Card",
                merchant = "MakeMyTrip",
                notes = "Flight booking",
                tagsString = "",
                type = "EXPENSE",
                creditCardId = 101L
            ),
            // Expense 2: Rs 5,000 on Shopping
            com.example.data.Transaction(
                id = 2,
                date = System.currentTimeMillis() - 86400000 * 3,
                amount = 5000.0,
                category = "Shopping",
                subcategory = "Clothing",
                paymentMethod = "Credit Card",
                merchant = "Zara",
                notes = "Clothes",
                tagsString = "",
                type = "EXPENSE",
                creditCardId = 101L
            ),
            // REFUND: Rs 8,000 refund on flight cancellation
            com.example.data.Transaction(
                id = 3,
                date = System.currentTimeMillis() - 86400000 * 2,
                amount = 8000.0,
                category = "Travel",
                subcategory = "Refund",
                paymentMethod = "Credit Card",
                merchant = "MakeMyTrip Refund",
                notes = "Partial cancellation refund",
                tagsString = "Refund",
                type = "REFUND",
                creditCardId = 101L
            ),
            // PAYMENT: Rs 10,000 payment towards bill
            com.example.data.Transaction(
                id = 4,
                date = System.currentTimeMillis() - 86400000 * 1,
                amount = 10000.0,
                category = "Credit Card Payment",
                subcategory = "Payment",
                paymentMethod = "Bank",
                merchant = "HDFC Card Pay",
                notes = "Card bill payment",
                tagsString = "Payment",
                type = "INCOME",
                creditCardId = 101L
            ),
            // Another card transaction (should NOT affect card 101)
            com.example.data.Transaction(
                id = 5,
                date = System.currentTimeMillis() - 86400000 * 1,
                amount = 3000.0,
                category = "Food",
                subcategory = "Dinner",
                paymentMethod = "Credit Card",
                merchant = "Restaurant",
                notes = "",
                tagsString = "",
                type = "EXPENSE",
                creditCardId = 999L
            )
        )

        // Total Spent on Card 101 = 20,000 + 5,000 = 25,000
        // Total Credits on Card 101 = 8,000 (Refund) + 10,000 (Payment) = 18,000
        // Net Outstanding = 25,000 - 18,000 = 7,000
        // Available Limit = 150,000 - 7,000 = 143,000
        // Utilization = 7,000 / 150,000 = 0.04666... (4.67%)

        val expenses = txs.filter { it.creditCardId == card.id && it.type == "EXPENSE" }.sumOf { it.amount }
        val refundsAndCredits = txs.filter {
            (it.creditCardId == card.id && (it.type == "REFUND" || it.type == "INCOME")) ||
            (it.toCreditCardId == card.id && it.type == "TRANSFER")
        }.sumOf { it.amount }
        val outstanding = maxOf(0.0, expenses - refundsAndCredits)
        val availableLimit = maxOf(0.0, card.cardLimit - outstanding)
        val utilization = if (card.cardLimit > 0) (outstanding / card.cardLimit).coerceIn(0.0, 1.0) else 0.0

        assertEquals(25000.0, expenses, 0.001)
        assertEquals(18000.0, refundsAndCredits, 0.001)
        assertEquals(7000.0, outstanding, 0.001)
        assertEquals(143000.0, availableLimit, 0.001)
        assertEquals(7000.0 / 150000.0, utilization, 0.0001)
    }

    @Test
    fun testTransactionSearchWithCommasAndAmounts() {
        val tx1 = Transaction(id = 1, amount = 5000.0, merchant = "Starbucks", category = "Food", subcategory = "Coffee", paymentMethod = "UPI", date = 1000L, type = "EXPENSE")
        val tx2 = Transaction(id = 2, amount = 150000.0, merchant = "Apple Store", category = "Electronics", subcategory = "Laptop", paymentMethod = "Credit Card", date = 2000L, type = "EXPENSE")
        val tx3 = Transaction(id = 3, amount = 250.75, merchant = "Uber", category = "Travel", subcategory = "Cab", paymentMethod = "UPI", date = 3000L, type = "EXPENSE")
        val list = listOf(tx1, tx2, tx3)

        fun searchFilter(query: String): List<Transaction> {
            val trimmedSearch = query.trim()
            val searchWithoutCommas = trimmedSearch.replace(",", "")
            val searchCleanedAmount = searchWithoutCommas.replace("₹", "").replace("$", "").replace("€", "").replace("£", "").trim()

            return list.filter { tx ->
                val amountStr = tx.amount.toString()
                val amountIntStr = if (tx.amount % 1.0 == 0.0) tx.amount.toLong().toString() else ""
                val amountFormatted = String.format(Locale.US, "%.2f", tx.amount)

                val matchAmount = if (searchCleanedAmount.isNotEmpty()) {
                    amountStr.contains(searchCleanedAmount) ||
                    (amountIntStr.isNotEmpty() && amountIntStr.contains(searchCleanedAmount)) ||
                    amountFormatted.contains(searchCleanedAmount)
                } else false

                trimmedSearch.isBlank() ||
                tx.merchant.contains(trimmedSearch, ignoreCase = true) ||
                tx.category.contains(trimmedSearch, ignoreCase = true) ||
                tx.subcategory.contains(trimmedSearch, ignoreCase = true) ||
                tx.notes.contains(trimmedSearch, ignoreCase = true) ||
                tx.tagsString.contains(trimmedSearch, ignoreCase = true) ||
                matchAmount
            }
        }

        // Test comma searches (matching amounts containing 5000 and 150000)
        assertEquals(listOf(tx1, tx2), searchFilter("5,000"))
        assertEquals(listOf(tx2), searchFilter("1,50,000"))
        assertEquals(listOf(tx2), searchFilter("150,000"))
        assertEquals(listOf(tx3), searchFilter("250.75"))

        // Test currency prefix searches
        assertEquals(listOf(tx1, tx2), searchFilter("₹5,000"))
        assertEquals(listOf(tx1, tx2), searchFilter("$5,000"))

        // Test text search still works seamlessly
        assertEquals(listOf(tx1), searchFilter("Starbucks"))
        assertEquals(listOf(tx3), searchFilter("Uber"))
        assertEquals(3, searchFilter("").size)
    }
}
