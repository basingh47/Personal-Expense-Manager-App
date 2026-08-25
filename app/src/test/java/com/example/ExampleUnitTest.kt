package com.example

import com.example.data.Subscription
import com.example.subscription.SubscriptionNotificationHelper
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

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
}
