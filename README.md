# Personal Expense & Finance Manager

A comprehensive, production-ready **Personal Finance Management System** built with **Kotlin**, **Jetpack Compose (Material Design 3)**, and a **Local-First + Cloud-Synced Architecture** (Room SQLite Database with Firebase Auth & Cloud Firestore).

---

## 🎨 Visual Theme & Design Language

- **Emerald Slate & Gold Aesthetics**: Refined financial palette with dark matte slate backgrounds (`#0F172A`), bright emerald accents (`#10B981`) for positive cash flows & income, soft crimson (`#EF4444`) for expense tracking, and gold highlights (`#F59E0B`) for goals and alerts.
- **Adaptive Material 3 Architecture**: Responsive container layouts and edge-to-edge system insets ensuring seamless usability across compact phones, foldables, and tablets.
- **Rich Contextual Iconography**: Comprehensive Material Symbols integration across categories, bank accounts, credit cards, debt books, and tracker projects.

---

## 📱 Complete System Modules & Capabilities

The application features **13 dedicated modules** accessible via the secure navigation drawer:

### 1. Dashboard Overview (`dashboard`)
- **Real-Time Net Worth & Monthly Cash Flow**: Dynamic calculation of total bank balances, active credit card liabilities, monthly income, expenses, and net savings.
- **Cash-Only Mode Toggle**: Filter out outstanding credit card transactions to view pure bank/cash cashflow without skewed unpaid credit card debt.
- **Bank SMS Auto-Detection & Review Queue**: Real-time SMS transaction parser with 1-tap confirmation card at the top of the dashboard.
- **Interactive Action Center**: Timely alerts for upcoming subscription renewals, due debt collections, category budget thresholds, and savings goals.
- **Recent Activity Ledger**: Chronological feed of recorded entries with one-tap drill-down.

### 2. Transactions Ledger (`transactions`)
- **Full Ledger Operations**: Record and manage Expenses, Incomes, Inter-Account Transfers, and Refunds/Reversals.
- **Direct Linking**: Link transactions to specific Bank Accounts, Credit Cards, Custom Categories/Subcategories, and Item/Project Trackers.
- **Inline Entity Creation**: Create new Custom Categories, Subcategories, or Item Trackers on the fly directly inside the Add/Edit Transaction sheet.
- **Smart Filtering & Search**: Multi-parameter filtering by type, payment method, bank account, date range, tracker, or custom query.

### 3. Bank Accounts Management (`bank_accounts`)
- **Aggregated Bank Balance Hub**: Live total balance across all registered savings and current accounts.
- **Bank Branding & Styling**: Visual cards for major banks (HDFC, SBI, ICICI, Axis, Kotak, PNB, BOB, etc.) with masked numbers (`•••• Last4`).
- **Granular SMS Detection**: Toggle auto-detection on or off per bank account.
- **Selective Analytics Visibility**: Include or exclude specific bank accounts from global dashboard totals and analytics charts.

### 4. Category Manager (`categories`)
- **Hierarchical Category Trees**: Create and customize main expense/income categories with custom icons and nested subcategories.
- **Cascade Updates**: Automatically updates associated budgets, transactions, and linked trackers when categories change.

### 5. Monthly Budgets (`budgets`)
- **Category Limit Gauges**: Set spending limits per category with dynamic progress bars and percentage markers.
- **Visual Threshold Alerts**: Automatic color shifts (Emerald ➔ Amber ➔ Red) as expenses approach or exceed allocated limits.

### 6. Item & Project Trackers — Cost Hub (`assets`)
- **Cumulative Lifetime Cost Tracking**: Track total maintenance and operational expenses for vehicles, electronics, health treatments, travel trips, or home renovations.
- **Unified Category Integration**: Organizes trackers using your standard and custom categories.
- **Metadata & Documentation**: Store target budgets, purchase prices, warranty info, insurance policies, clinic contacts, and service notes.
- **Detailed Timeline Ledger**: Filter and review every linked transaction logged against the specific asset or project.

### 7. Subscription Tracker (`subscriptions`)
- **Recurring Fee Monitoring**: Track monthly and annual SaaS subscriptions, memberships, and utility auto-debits.
- **Proactive Renewal Alerts**: Automatic background checks with multi-tier notifications (3 days prior, 1 day prior, day of renewal).
- **One-Tap Cycle Advance & Pay**: Fast payment recording and automatic due date rollover.

### 8. Credit Cards Tracker (`credit_cards`)
- **Statement & Cycle Management**: Track credit limits, billing cycle dates, payment due dates, and available credit.
- **Real-Time Utilization Monitoring**: Visual indicators for credit utilization ratio with recommended 30% safety thresholds.
- **Card Bill Settlement**: Record credit card bill payments as fund transfers from bank accounts without double-counting expenses.

### 9. Savings & Goals (`savings`)
- **Goal Progress Tracking**: Set target amounts and target dates for emergency funds, vacations, investments, and major purchases.
- **Interactive Quick-Add Contributions**: One-tap increments (`+₹500`, `+₹1,000`, `+₹5,000`) with visual progress bars.

### 10. Borrow & Lend Book (`borrow_lend`)
- **Peer-to-Peer Debt Ledger**: Record money lent to or borrowed from friends, family, or business partners.
- **Settlement & History**: Track due dates, partial repayments, paid status, and net balance owed/receivable.

### 11. Wishlist (`wishlist`)
- **Purchase Planning**: Prioritize prospective purchases (High/Medium/Low priority) with target dates and notes.
- **One-Click Expense Conversion**: Convert wishlist items into active transactions with pre-filled category and price.

### 12. Analytics & Insights (`analytics`)
- **Distribution Charts**: Interactive category spending breakdowns, income vs. expense ratios, and payment method share.
- **Multi-Month Spending Trends**: Month-over-month cash flow trajectory and category budget adherence.

### 13. Security & App Settings (`settings`)
- **PIN App Lock**: 4-digit PIN authentication with automatic lock on app minimize / background.
- **Configurable Auto-Lock Timeouts**: Choose between Immediate, 30 Seconds, 1 Minute, 2 Minutes, or 5 Minutes inactivity timeouts.
- **Screenshot & Preview Protection**: `FLAG_SECURE` integration to prevent screen recording and Android Recent Apps task switcher previews.
- **Full JSON Backup & Restore**: Export encrypted portable JSON snapshots and restore with Clean Overwrite or Merge modes.
- **Professional PDF Financial Statements**: Multi-page audit-ready statement with KPI metrics, category breakdowns, and transaction ledgers.
- **Tabular CSV / Excel Export**: Raw transaction exports compatible with Microsoft Excel and Google Sheets.
- **Granular Data Purging**: Delete transactions by specific billing month, clear SMS review queues, or execute full factory reset.

---

## 🔒 Security, Privacy & Local-First Philosophy

- **Zero-Cloud Exposure for SMS**: SMS parsing and message interception execute 100% locally on-device. No raw text messages or SMS contents are ever transmitted over the network.
- **Offline-First Room SQLite Database**: Instant speed, zero network latency, and complete offline functionality.
- **Multi-User Isolated Cloud Sync**: When logged in via Firebase Auth, data synchronizes to isolated user paths (`/users/{UID}/...`), guaranteeing strict partition and privacy.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin (100%)
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Local Persistence**: Room SQLite with Kotlin Coroutines & Flow
- **Cloud Backend**: Firebase Authentication & Cloud Firestore
- **Security**: Android Keystore, SharedPreferences encryption, Biometric/PIN AppLockManager
- **Architecture**: Clean Architecture / MVVM with reactive `StateFlow` streams

