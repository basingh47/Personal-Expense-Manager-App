# Personal Expense Manager — Product Roadmap & Version Release Plan

This document outlines the step-by-step feature roadmap organized version-by-version. Each release is designed to be backwards-compatible, preserving existing user data, local database records, and settings.

---

## 📌 Release Summary Matrix

| Version | Milestone Name | Key Focus Area | Target Status |
| :--- | :--- | :--- | :--- |
| **v1.0.0** (Current) | **Foundational Core** | Core Ledger, SMS Detection, Budgets, Cards, Offline Room DB | ✅ **Live & Stable** |
| **v1.1.0** | **Security & Export Hub** | Biometric Lock, Custom PIN, PDF/Excel Statement Generator | 🎯 Next Target |
| **v1.2.0** | **Bill Splitter & Shared Wallets** | Group Expenses, Roommate/Trip Split, Settlement Calculator | 📋 Planned |
| **v1.3.0** | **Receipt OCR & Smart Scanner** | Camera/Gallery Bill Scanner, Auto Amount & Date Extraction | 📋 Planned |
| **v1.4.0** | **Wealth & Investments** | Stock/Mutual Fund, Crypto, Fixed Deposit & Gold Portfolio | 📋 Planned |
| **v2.0.0** | **AI Financial Intelligence** | On-device / Gemini AI Insights, Predictive Budget Forecasts | 🚀 Major Milestone |

---

## 🛠️ Detailed Version-by-Version Breakdown

---

### 🟢 Version 1.0.0 — Live & Stable (Current Release)
- [x] **Transaction Ledger**: Complete income and expense tracking with payment methods and category tagging.
- [x] **Bank SMS Auto-Detection**: Background listener, on-device regex parsing, and instant Heads-Up notification alerts.
- [x] **Interactive Categorization Prompt**: 1-tap categorization cards on the Dashboard for detected SMS.
- [x] **Credit Card Tracker**: Cycle date calculation, unbilled vs billed balance, statement due alerts.
- [x] **Budget Planner**: Monthly category caps with green/amber/red threshold bars.
- [x] **Debts & Loans Ledger**: Track informal lending and borrowing with settlement statuses.
- [x] **Savings Goals**: Target tracking with progressive fund deposits and withdrawal controls.
- [x] **Subscriptions Manager**: Normalized monthly cost calculator and renewal due reminders.
- [x] **Wishlist Planner**: Delayed gratification shopping list with 1-tap conversion to expense.
- [x] **Analytics**: Category donut charts, inflow vs outflow trends, and top merchant leaderboards.
- [x] **Local Persistence**: Room SQLite database with multi-table relational schema.

---

### 🔵 Version 1.1.0 — Security, Privacy & Export Hub
*Focus: Data privacy, access protection, and professional financial reporting.*

1. **Biometric Authentication & App Lock**:
   - Fingerprint unlock & Face authentication using Android Jetpack `BiometricPrompt`.
   - Fallback 4-digit or 6-digit numeric Master PIN with scramble keypad.
   - Auto-lock timeout options (Immediately, after 1 minute, 5 minutes, or on backgrounding).
   - "Hide financial balances on app switcher" (Privacy Window flag).

2. **Professional PDF Statement Generator**:
   - Monthly and custom-date PDF financial summary reports.
   - Branded layout with income vs expense graphs, net savings, and categorized lists.
   - Print & Share PDF directly to WhatsApp, Email, or Google Drive.

3. **Excel (.XLSX) & Enhanced CSV Exporter**:
   - Multi-tab Excel export (Tab 1: All Transactions, Tab 2: Category Summary, Tab 3: Credit Card Bills).
   - Full date-range and account filtering before export.

4. **Automated Local Encrypted Backup & Restore**:
   - One-tap backup of entire database into a password-encrypted local JSON/ZIP file.
   - Easy restore flow when switching devices.

---

### 🟣 Version 1.2.0 — Split Expenses & Group Shared Wallets
*Focus: Group travel, roommate rent splitting, and peer settlement mathematics.*

1. **Group & Trip Ledgers**:
   - Create named groups (e.g. *"Goa Trip 2026"*, *"Flat 402 Roommates"*, *"Office Lunch"*).
   - Add group members with contact names and phone numbers.

2. **Smart Split Calculations**:
   - Split equally (`1/N` per person).
   - Split by exact amounts or custom percentages.
   - Split by shares / itemized breakdown.

3. **Debt Simplification Engine**:
   - Calculates the minimum number of transactions needed to settle all group debts (Min-Cash-Flow algorithm).
   - Visual balance cards: *"Alex owes you ₹450"* / *"You owe Sarah ₹1,200"*.

4. **One-Tap UPI Settle Up**:
   - Deep-link directly into GPay, PhonePe, or Paytm with pre-filled amount and recipient UPI ID.
   - Auto-record a settlement transaction once marked paid.

---

### 🟡 Version 1.3.0 — Receipt & Bill Scanner (Camera OCR)
*Focus: Instant physical receipt processing without typing.*

1. **Camera & Gallery Bill Scanner**:
   - In-app camera capture and photo picker with automatic document crop & perspective warp.
   - Real-time ML Kit on-device Text Recognition (OCR).

2. **Smart Extraction Pipeline**:
   - Extracts Merchant / Store Name from receipt header.
   - Extracts Date and Time from receipt body.
   - Extracts Total Final Amount and Tax.
   - Auto-suggests the transaction category based on recognized items.

3. **Receipt Image Attachment**:
   - Store compressed photo proof attached directly to the transaction record in the ledger.
   - Full-screen receipt zoom viewer with pinch-to-zoom.

---

### 🟠 Version 1.4.0 — Net Worth & Investment Portfolio Tracker
*Focus: Comprehensive wealth accumulation across assets and holdings.*

1. **Investment Asset Tracking**:
   - **Mutual Funds & Stocks**: Track units, average purchase NAV/Price, and current value.
   - **Fixed Deposits (FD) & Recurring Deposits (RD)**: Maturity calculator, interest rates, and maturity date alerts.
   - **Physical Gold / Silver & Sovereign Gold Bonds (SGB)**: Weight in grams and purchase valuation.
   - **Crypto / Digital Assets**: Holding quantity and manual/live valuation tracker.

2. **Holistic Net Worth Dashboard**:
   - Total Assets (Cash + Bank + Investments + Lent Money) minus Total Liabilities (Credit Cards + Unpaid Loans).
   - Historical Net Worth trend curve over 6 months / 1 year / 5 years.

3. **Emergency Fund Health Index**:
   - Calculates how many months of living expenses your liquid assets can cover based on your average 3-month spending.

---

### 🔴 Version 2.0.0 — AI Financial Intelligence & Smart Automation
*Focus: Deep financial insights, anomaly detection, and predictive coaching.*

1. **AI Spending Insights & Anomaly Detection**:
   - Detects unusual surges in spending (e.g. *"Your dining expenses are 45% higher this week compared to your average"*).
   - Identifies duplicate charges or creeping subscription fee increases.

2. **Predictive End-of-Month Balance Forecast**:
   - Machine learning projection showing predicted bank balance at month-end based on active burn rate and scheduled bills.

3. **AI Financial Assistant**:
   - Natural language interaction: *"How much did I spend on Swiggy last month?"*, *"What can I cut to save ₹10,000 this month?"*.
   - Smart automated budget recommendations personalized to your income patterns.

4. **Multi-Currency & Travel Mode**:
   - Real-time currency conversions when traveling abroad.
   - Dual-currency display on transactions (e.g. Spent `$25 USD` $\rightarrow$ Recorded as `₹2,100 INR`).

---

## 🔒 Safe Upgrade & Non-Breaking Guidelines

When progressing through this roadmap:
1. **Never drop existing tables**: All updates use SQLite `ALTER TABLE` or additive Room schema definitions with automated fallback migrations.
2. **Component Isolation**: Each new feature is built as an independent Composable screen or isolated domain model.
3. **No forced changes**: Users who only want the basic ledger or SMS detection can continue using the core app without being forced into optional modules.
