# 🐛 Bug History & Solutions Log

This document tracks identified bugs, discovery timestamps, root causes, and applied solutions. Each entry follows a concise 5-line format.

---

### Bug 1: SMS Parser Failed to Extract Amount (`Rs.5000` -> `500`) and Account Number (`A/cX1488`)
- **Bug What is**: The SMS parser extracted amount as ₹500 instead of ₹5000 and missed linking the SBI bank account ending in 1488.
- **Time**: August 27, 2026, 05:35 UTC
- **Root Cause**: `amountPatterns` regex matched `Rs.` and truncated digits if no space existed; `cardPatterns` did not support single character/mask prefixes like `A/cX1488`.
- **What is Solution**: Updated `amountPatterns` with `(?:INR|Rs|₹|USD...)\.?\s*` and added `[xX*]*` account prefix matching in `cardPatterns`.
- **Status & Verification**: Fixed and validated with automated JUnit test suite in `ExampleUnitTest.kt` (`testSmsParserSbiAtmWithdrawal`).

---

### Bug 2: Excluded Bank Accounts Still Included in Monthly Budget Spending Calculations
- **Bug What is**: Bank accounts configured with "Exclude from Global Analytics" still had their category expenses counted toward monthly budgets.
- **Time**: August 27, 2026, 05:40 UTC
- **Root Cause**: Budget progress views in `BudgetsScreen` and `HomeScreen` directly queried raw `transactions` without filtering by `isHiddenFromSummary` or current month.
- **What is Solution**: Introduced reactive `budgetCategorySpendings` StateFlow in `FinanceViewModel` that filters out hidden accounts and bounds calculation to current month.
- **Status & Verification**: Fixed across `HomeScreen` (Budget Utilization preview) and `BudgetsScreen` (all category limit progress cards).

---

### Bug 3: New Tracker Button Placement in Add Transaction Dialog
- **Bug What is**: The "+ New Tracker" button was awkwardly positioned inside the header row next to the label, causing visual clipping and cramped layout.
- **Time**: August 27, 2026, 05:15 UTC
- **Root Cause**: The button was hardcoded in the header `Row` instead of inside the horizontal scrollable chips container.
- **What is Solution**: Refactored the button into a standardized Material 3 `FilterChip` positioned at the head of the tracker horizontal scroll list.
- **Status & Verification**: Fixed with smooth responsive wrapping and consistent aesthetic styling matching category addition chips.

---

### Bug 4: Dashboard Cash-Only Mode Toggle Lost on App Restart & Not Cloud Synced
- **Bug What is**: Toggling Cash-Only Mode reset back to OFF whenever the app was closed and re-opened, and never synchronized with user cloud data.
- **Time**: August 27, 2026, 06:00 UTC
- **Root Cause**: The `excludeCreditCards` state was held solely in an in-memory `MutableStateFlow` without local persistence or Firestore replication.
- **What is Solution**: Persisted `cash_only_mode` in local storage (`SharedPreferences`) on startup and integrated two-way sync with Firebase Firestore user settings (`users/{uid}/settings/preferences`).
- **Status & Verification**: Fully persistent across app restarts and synchronized with Firestore when logged in.

---

### Bug 5: Amount Input Fields Displayed Raw Digits Without Real-Time Number Formatting (Commas)
- **Bug What is**: When entering numbers in amount fields across all dialogs, digits appeared without commas (e.g. `500000` instead of `5,00,000`).
- **Time**: August 27, 2026, 06:17 UTC
- **Root Cause**: Text fields bound directly to raw strings without a Jetpack Compose `VisualTransformation`, preventing dynamic comma insertion.
- **What is Solution**: Built `AmountVisualTransformation` with bidirectional `OffsetMapping` that dynamically formats with Indian (Lakhs/Crores) or International (Thousands/Millions) grouping based on locale, while keeping underlying numeric data clean.
- **Status & Verification**: Applied across all 12 monetary input dialogs and verified with comprehensive JUnit tests.

---

### Bug 6: Global & Indian Number Formatting System Setting with Cloud Sync
- **Bug What is**: Users lacked manual control over number formatting systems (Lakhs vs Millions) and preferences did not sync across devices.
- **Time**: August 27, 2026, 06:55 UTC
- **Root Cause**: Number formatting was hardcoded to device locale without user override options or Firestore user profile replication.
- **What is Solution**: Added a "Number Format System" preference in Settings (Auto / Indian / International), stored in SharedPreferences, and synced with Firestore `users/{uid}/settings/preferences`.
- **Status & Verification**: Tested with live preference switching, persistence across app restarts, and automated JUnit test cases.

---

### Bug 7: Hardcoded "₹" Currency Symbol Across App & Lack of Global Currency Selection
- **Bug What is**: Currency symbol was hardcoded as "₹" (Rupee) across dashboard cards, transaction lists, budgets, analytics, notification alerts, dialog labels, and PDF statements, preventing international users from selecting their local currency (e.g. $, €, £, AED, ¥, etc.).
- **Time**: August 27, 2026, 07:12 UTC
- **Root Cause**: Literal string literals like `"₹${...}"` were embedded directly in UI composables, notification generators, and statement builders rather than reading from a centralized currency configuration.
- **What is Solution**: 
  1. Refactored `NumberFormatConfig` to hold dynamic `currencySymbol` and `currencyCode`, and created `NumberFormatConfig.formatAmount()`.
  2. Replaced all 86 hardcoded instances of "₹" across `Screens.kt`, `PdfStatementGenerator.kt`, `SmsNotificationHelper.kt`, `SubscriptionNotificationHelper.kt`, and `FinanceViewModel.kt`.
  3. Added a Currency Picker with 13 world currencies (INR, USD, EUR, GBP, AED, CAD, AUD, JPY, SAR, SGD, CHF, NZD, SEK) in Settings, featuring quick chips, searchable dialog, SharedPreferences persistence, and Firestore sync.
  4. Expanded `SmsParser` regex patterns to automatically parse transactions in all supported global currency formats.
- **Status & Verification**: Verified dynamic switching across all screens, live updates on currency selection, PDF exports, and 100% passing JUnit tests.

---

### Bug 8: Static Category List in SMS Parser vs. Custom User Categories
- **Bug What is**: The SMS detection engine (`SmsParser.kt`) categorized incoming bank and credit card SMS messages using a fixed static keyword map (e.g., Swiggy -> Food). If the user created custom categories (e.g., "Dining Out", "Groceries", "Cab", "Gym", etc.) or custom subcategories, the SMS engine still assigned standard default categories instead of matching user-defined categories.
- **Time**: August 27, 2026, 07:25 UTC
- **Root Cause**: `SmsParser.parseSms` did not accept the user's active custom categories from Room Database / ViewModel, and `guessCategory` only evaluated hardcoded standard categories.
- **What is Solution**:
  1. Updated `FinanceDao` and `FinanceRepository` with `getAllCustomCategoriesList()`.
  2. Modified `SmsParser.parseSms` to accept `customCategories: List<CustomCategory> = emptyList()`.
  3. Implemented intelligent custom category matching in `SmsParser.guessCategory`, checking for direct name matches on custom categories & custom subcategories, plus intent-based semantic mapping (e.g., food delivery -> "Dining Out" / "Food & Dining", grocery delivery -> "Groceries", cabs -> "Cab" / "Commute", fitness -> "Gym", etc.).
  4. Updated `SmsReceiver` and `FinanceViewModel.simulateSmsReceived` to pass the user's active custom categories into `SmsParser`.
  5. Added comprehensive JUnit test suite `testSmsParserCustomCategoriesMatching` verifying custom category & subcategory detection.
- **Status & Verification**: 100% passing automated unit tests and successful build compilation.

---

### Bug 9: Credit Card Outstanding & Available Limit Calculation Did Not Subtract Refunds & Payments
- **Bug What is**: Credit card outstanding balances, billing statement amounts, and limit utilization calculated expenses minus income, but ignored `REFUND` type transactions linked to the card and bank payment `TRANSFER` transactions to the card.
- **Time**: August 27, 2026, 07:48 UTC
- **Root Cause**: Calculations in `CreditCardsScreen` filtered only `EXPENSE` and `INCOME`, ignoring `REFUND` and `TRANSFER` (to credit card), which caused credit card balances to remain inflated and available credit limits to be understated even after receiving merchant refunds or card bill payments.
- **What is Solution**:
  1. Added centralized helper methods in `FinanceViewModel`: `calculateCreditCardOutstanding()`, `calculateCreditCardAvailableLimit()`, and `calculateCreditCardUtilization()`.
  2. Subtracted all `REFUND` transactions, card `INCOME` transactions (direct payments), and `TRANSFER` transactions (`toCreditCardId == card.id`) from card expenses.
  3. Updated `CreditCardsScreen` card carousel cards to show both **Outstanding Balance** and **Available Limit** with dynamic color indicators.
  4. Updated statement cycle calculations (`cycleBill`, `unbilledSum`) to account for credits & refunds within each billing cycle, displaying a breakdown badge (`-₹... credits/refunds`).
  5. Updated tab list to "Payments & Refunds" with proper "+" green formatting and `Refund •` badge prefixes.
  6. Added automated JUnit test `testCreditCardOutstandingAndRefundCalculation` verifying expenses, refunds, payments, and limit utilization.
- **Status & Verification**: 100% passing unit tests and verified via `compile_applet`.

---

### Bug 10: JSON Backup & Restore Engine Omitted Custom User Preferences
- **Bug What is**: The JSON Backup & Restore engine (`BackupEngine.kt`) exported accounts, credit cards, transactions, budgets, assets, and goals, but omitted user preference flags (`cash_only_mode`, `number_format_preference`, `sms_detection_enabled`, `currency_symbol`, `currency_code`, `is_dark_theme`). Restoring a backup on a fresh device reset these UI preferences to defaults.
- **Time**: August 27, 2026, 08:00 UTC
- **Root Cause**: `FullBackupData` and `BackupEngine.buildBackupJsonString()` had no representation for user preferences in the JSON schema, and `restoreFullBackup()` only inserted Room entity models without restoring SharedPreferences / ViewModel state flows.
- **What is Solution**:
  1. Created `UserPreferencesBackup` data class with `cashOnlyMode`, `numberFormatPreference`, `smsDetectionEnabled`, `currencySymbol`, `currencyCode`, and `isDarkTheme`.
  2. Updated `FullBackupData` with optional `userPreferences: UserPreferencesBackup? = null`.
  3. Extended `BackupEngine.buildBackupJsonString` to serialize `user_preferences` object into the JSON payload.
  4. Extended `BackupEngine.parseBackupJson` to parse `user_preferences` (supporting both snake_case and camelCase), setting `hasUserPreferences` flag in `BackupStats`.
  5. Updated `FinanceViewModel.exportFullBackup` to capture current active preferences.
  6. Updated `FinanceViewModel.restoreFullBackup` to restore `cash_only_mode`, `number_format_preference`, `sms_detection_enabled`, `currency_symbol`, `currency_code`, and `is_dark_theme` into state flows, `NumberFormatConfig`, and `SharedPreferences` (plus cloud sync when logged in).
  7. Updated `RestoreBackupConfirmDialog` in `Screens.kt` to display a custom preferences badge in the backup preview summary.
  8. Added unit tests for serialization, deserialization, and backward compatibility with legacy backups.
- **Status & Verification**: 100% passing automated unit tests and verified via `compile_applet`.

---

### Bug 11: Transaction Search by Numeric Amount Failed When User Entered Commas or Currency Symbols
- **Bug What is**: Searching for transaction amounts in the Transactions screen (e.g. typing `"5,000"` or `"₹5,000"`) returned no matches because `filteredTransactions` compared raw search strings against `tx.amount.toString()`.
- **Time**: August 27, 2026, 08:10 UTC
- **Root Cause**: `tx.amount.toString()` produces unformatted numeric strings like `"5000.0"`. Queries containing comma separators (like `"5,000"` or `"1,50,000"`) or currency symbols did not match raw string representations.
- **What is Solution**:
  1. Cleaned and normalized search queries in `FinanceViewModel.filteredTransactions` by stripping commas (`,`) and currency symbols (`₹`, `$`, `€`, `£`).
  2. Matched against both decimal (`"5000.0"`, `"5000.00"`) and integer (`"5000"`) formats of the transaction amount.
  3. Preserved full text matching for merchants, categories, subcategories, notes, and hashtags.
  4. Added automated JUnit test `testTransactionSearchWithCommasAndAmounts` verifying comma-separated amounts (`"5,000"`, `"1,50,000"`), decimals, currency-prefixed searches, and text queries.
- **Status & Verification**: 100% passing unit tests and verified via `compile_applet`.




