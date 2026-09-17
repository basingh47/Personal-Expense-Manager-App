package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.Subscription
import com.example.export.BackupEngine
import com.example.export.FullBackupData
import com.example.export.UserPreferencesBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Personal Expense Manager", appName)
  }

  @Test
  fun `test subscription backup serialization and restoration`() {
    val originalSub = Subscription(
      id = 42,
      name = "Claude Pro",
      cost = 1999.0,
      renewalDate = 1774500000000L,
      billingCycle = "MONTHLY",
      isActive = true,
      category = "Work & AI",
      notes = "Auto-pay via HDFC",
      isAutoRenew = true,
      reminderDaysInAdvance = 5,
      paymentAccountId = 101,
      paymentMethodName = "HDFC Bank",
      lastPaidDate = 1772000000000L
    )

    val fullBackupData = FullBackupData(
      transactions = emptyList(),
      bankAccounts = emptyList(),
      creditCards = emptyList(),
      customCategories = emptyList(),
      budgets = emptyList(),
      assets = emptyList(),
      subscriptions = listOf(originalSub),
      savingsGoals = emptyList(),
      borrowLends = emptyList(),
      wishlists = emptyList()
    )
    val json = BackupEngine.buildBackupJsonString(fullBackupData)
    val result = BackupEngine.parseBackupJson(json)

    assertNotNull(result)
    val restoredData = result!!.first
    assertEquals(1, restoredData.subscriptions.size)
    val restoredSub = restoredData.subscriptions[0]
    assertEquals(originalSub.name, restoredSub.name)
    assertEquals(originalSub.cost, restoredSub.cost, 0.001)
    assertEquals(originalSub.isAutoRenew, restoredSub.isAutoRenew)
    assertEquals(originalSub.reminderDaysInAdvance, restoredSub.reminderDaysInAdvance)
    assertEquals(originalSub.paymentAccountId, restoredSub.paymentAccountId)
    assertEquals(originalSub.paymentMethodName, restoredSub.paymentMethodName)
    assertEquals(originalSub.lastPaidDate, restoredSub.lastPaidDate)
  }

  @Test
  fun `test backup and restore with user preferences`() {
    val userPrefs = UserPreferencesBackup(
      cashOnlyMode = true,
      numberFormatPreference = "INDIAN",
      smsDetectionEnabled = false,
      currencySymbol = "$",
      currencyCode = "USD",
      isDarkTheme = true
    )

    val fullBackupData = FullBackupData(
      transactions = emptyList(),
      bankAccounts = emptyList(),
      creditCards = emptyList(),
      customCategories = emptyList(),
      budgets = emptyList(),
      assets = emptyList(),
      subscriptions = emptyList(),
      savingsGoals = emptyList(),
      borrowLends = emptyList(),
      wishlists = emptyList(),
      userPreferences = userPrefs
    )

    val json = BackupEngine.buildBackupJsonString(fullBackupData)
    val result = BackupEngine.parseBackupJson(json)

    assertNotNull(result)
    val (restoredData, stats) = result!!
    assertTrue(stats.hasUserPreferences)
    assertNotNull(restoredData.userPreferences)

    val restoredPrefs = restoredData.userPreferences!!
    assertEquals(true, restoredPrefs.cashOnlyMode)
    assertEquals("INDIAN", restoredPrefs.numberFormatPreference)
    assertEquals(false, restoredPrefs.smsDetectionEnabled)
    assertEquals("$", restoredPrefs.currencySymbol)
    assertEquals("USD", restoredPrefs.currencyCode)
    assertEquals(true, restoredPrefs.isDarkTheme)
  }

  @Test
  fun `test backup and restore backward compatibility without user preferences`() {
    val legacyJson = """
    {
      "app_name": "Expense Manager",
      "schema_version": 1,
      "export_timestamp": 1772000000000,
      "transactions": [],
      "bank_accounts": [],
      "credit_cards": [],
      "custom_categories": [],
      "budgets": [],
      "assets": [],
      "subscriptions": [],
      "savings_goals": [],
      "borrow_lend": [],
      "wishlist": []
    }
    """.trimIndent()

    val result = BackupEngine.parseBackupJson(legacyJson)
    assertNotNull(result)
    val (restoredData, stats) = result!!
    assertFalse(stats.hasUserPreferences)
    assertNull(restoredData.userPreferences)
  }
}
