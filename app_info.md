# Personal Expense Manager — Complete Architecture, Modules & Functional Specification

## 1. Application Overview
**Personal Expense Manager** is an offline-first, privacy-respecting personal finance management platform natively developed for Android with **Jetpack Compose** and **Room (SQLite)** with optional **Firebase Cloud Sync**. It offers full lifecycle money management across day-to-day spending, bank SMS auto-categorization, credit card billing cycle tracking, budget limits, loan ledgers, recurring subscriptions, and savings milestones.

---

## 2. System Architecture & Modules

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│     MainActivity (Navigation Drawer, TopBar, Window Insets, FAB)       │
│  9 Dedicated Screens (Dashboard, Ledger, Credit Cards, Analytics...)   │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ StateFlow / Actions
┌───────────────────────────────────┴────────────────────────────────────┐
│                           VIEWMODEL LAYER                              │
│            FinanceViewModel (Business Logic & State Aggregation)       │
│   - Cash-flow calculator         - Credit Card Cycle Logic             │
│   - Cash-only toggle             - Budget Limit Alerts                 │
│   - Pending SMS Staging Queue    - Cloud Sync Engine (Firebase)        │
└──────────────────▲─────────────────────────────────▲───────────────────┘
                   │                                 │
┌──────────────────┴───────────────┐ ┌───────────────┴───────────────────┐
│     BACKGROUND SMS SUBSYSTEM     │ │         DATA REPOSITORY           │
│  - SmsReceiver (Broadcast)       │ │     FinanceRepository & Room DB   │
│  - SmsParser (Regex / Keywords)  │ │   8 SQLite Transactional Tables   │
│  - Notification Manager Engine   │ │     (Transactions, Cards, etc.)   │
└──────────────────────────────────┘ └───────────────────────────────────┘
```

---

## 3. Detailed Screen Breakdown & Functional Logic

The application features **9 core functional screens** accessible via the primary navigation drawer:

---

### Screen 1: Dashboard (`DashboardScreen`)
* **Primary Role**: Executive financial cockpit and real-time activity stream.
* **Core Logic & Capabilities**:
  1. **Net Worth Calculator**: Sums active Cash, Bank, and UPI assets minus all outstanding Credit Card balances and unpaid borrowed liabilities.
  2. **Monthly Cash Flow Card**: Calculates current month Inflow (Income) minus Outflow (Expense), showing dynamic savings rate percentage.
  3. **Cash-Only Mode Toggle**: When activated, filters out unpaid Credit Card purchases from the active monthly metrics so you only see actual settled cash leaving your accounts.
  4. **Bank SMS Detection Banner**: Displays real-time SMS listening status, quick permission grant trigger, and an interactive SMS Simulator test suite.
  5. **Pending SMS Action Staging**: When an SMS is caught by the parser, cards appear at the top prompting the user to confirm the predicted category with 1-tap.
  6. **Interactive Alerts Carousel**: Dynamic cards warning about subscriptions renewing in $\le 3$ days, credit cards nearing due dates, and budgets exceeded.
  7. **Recent Activity Feed**: Glanceable list of the latest 5 transactions with quick edit/delete sheet actions.

---

### Screen 2: Transactions Ledger (`TransactionsScreen`)
* **Primary Role**: Complete historical transactional database.
* **Core Logic & Capabilities**:
  1. **CRUD Operations**: Add, update, or remove Expenses and Incomes with amount, category, subcategory, payment method, date/time, merchant, and notes.
  2. **Granular Multi-Filters**: Filter by Month/Year, Transaction Type (Expense / Income / All), Category, or Payment Channel (Cash, UPI, Credit Card, Bank).
  3. **Live Search**: Instant text search across notes, merchant names, tags, and category labels.
  4. **CSV Export / Backup**: Generate local CSV reports of filtered transactions.

---

### Screen 3: Credit Cards Tracker (`CreditCardsScreen`)
* **Primary Role**: Full credit line management with cycle and debt tracking.
* **Core Logic & Capabilities**:
  1. **Card Deck UI**: Visual cards showing bank name, last 4 digits, custom color gradient, total limit, and current utilization percentage bar.
  2. **Cycle & Statement Computation**: Calculates billing start date, billing statement date, and payment due date for the current cycle.
  3. **Unbilled vs Billed Debt**: Segregates spending done in the current open cycle from past statement liabilities.
  4. **"Add Spend" Flow**: Directly books transactions against the card, updating outstanding balances and linking `creditCardId`.
  5. **"Pay Bill" Flow**: Clears card balances by transferring money from a bank/cash account into the card ledger.

---

### Screen 4: Budget Planner (`BudgetScreen`)
* **Primary Role**: Monthly spending caps to prevent overspending.
* **Core Logic & Capabilities**:
  1. **Category Budgets**: Set spending ceilings per category (e.g. ₹5,000 for *Food*, ₹2,000 for *Entertainment*).
  2. **Live Consumption Meter**: Compares active monthly spending in that category against the allocated limit.
  3. **Color-Coded Status Thresholds**:
     * `Green` ($\le 70\%$ spent): Healthy spending.
     * `Amber` ($71\% - 99\%$ spent): Warning threshold.
     * `Red` ($\ge 100\%$ spent): Overbudget alert.

---

### Screen 5: Analytics & Visual Reports (`AnalyticsScreen`)
* **Primary Role**: Visual insights into spending habits and trends.
* **Core Logic & Capabilities**:
  1. **Category Breakdown Pie/Donut Chart**: Percentage distribution of expenses by category.
  2. **Monthly Inflow vs Outflow Trend Bar Chart**: Compare historical earnings against expenditures month-over-month.
  3. **Payment Method Breakdown**: Visual analysis of UPI vs Credit Card vs Cash usage.
  4. **Top Merchants Leaderboard**: Highlights the top vendors where maximum funds were spent.

---

### Screen 6: Debts & Borrowings (`DebtsScreen`)
* **Primary Role**: Informal peer-to-peer loan and liability tracker.
* **Core Logic & Capabilities**:
  1. **Lent Ledger ("I Lent")**: Track money given to friends, colleagues, or family with borrower name, target return date, and notes.
  2. **Borrowed Ledger ("I Borrowed")**: Track liabilities owed to individuals or lenders.
  3. **Settlement Logic**: Mark loans as fully or partially settled, which automatically updates net worth calculations.

---

### Screen 7: Savings Goals (`GoalsScreen`)
* **Primary Role**: Milestone savings and wealth accumulation planner.
* **Core Logic & Capabilities**:
  1. **Goal Targets**: Define milestone goals (e.g. *Emergency Fund*, *New Laptop*, *Vacation*) with target amounts and target dates.
  2. **Interactive Incremental Contributions**: Add or withdraw savings towards specific goals.
  3. **Visual Progress Rings**: Displays percentage completion and remaining amount needed to reach target.

---

### Screen 8: Subscriptions Planner (`SubscriptionsScreen`)
* **Primary Role**: Fixed recurring bill and subscription manager.
* **Core Logic & Capabilities**:
  1. **Recurring Intervals**: Set recurring frequencies (Weekly, Monthly, Quarterly, Yearly).
  2. **Next Renewal Engine**: Calculates exact next payment due dates.
  3. **Monthly Cost Normalization**: Automatically converts annual/weekly subscriptions into a normalized monthly cost metric.
  4. **Renewal Notification Hook**: Feeds upcoming bills into the Dashboard alert system.

---

### Screen 9: Wishlist & Purchase Planner (`WishlistScreen`)
* **Primary Role**: Delayed gratification planner to curb impulse shopping.
* **Core Logic & Capabilities**:
  1. **Item Wishlist**: Log items you want to buy with estimated price and priority rating (High, Medium, Low).
  2. **1-Tap Purchase Conversion**: Once ready to buy, tap "Convert to Expense" to automatically move the item into the real transaction ledger.

---

## 4. Background Subsystems & Special Engines

### 1. Bank SMS Auto-Detection & Parser Subsystem
* **`SmsReceiver` (`BroadcastReceiver`)**: Runs in the background on incoming `SMS_RECEIVED` events with `goAsync()` to avoid blocking the UI thread.
* **`SmsParser`**:
  * Filters out OTPs and promotional spam messages.
  * Detects transaction nature (`EXPENSE` for *debited/spent/paid*, `INCOME` for *credited/salary/refund*).
  * Extracts exact amount (`₹`, `INR`, `Rs.`, `$`).
  * Matches last 4 digits to auto-link with registered credit cards.
  * Predicts the most suitable category (*Food*, *Vehicle/Fuel*, *Shopping*, *Travel*, *Home/Bills*, *Health*, etc.).
* **Staging Queue (`pending_sms_transactions`)**:
  * Avoids unapproved data writes by placing parsed SMS entries in a staging table.
  * Displays an interactive card on the Dashboard where users confirm or change the category in 1 tap.
* **SMS Simulator**: In-app test bed with real preset bank SMS templates (HDFC, SBI UPI, ICICI, Axis, Paytm, etc.) and custom text testing.

---

### 2. Category & Subcategory Taxonomy System
* **Dual-Tier System**: Main categories (e.g., *Food*, *Vehicle*, *Travel*, *Shopping*, *Home*, *Health*, *Insurance/Loan*, *Entertainment*) paired with granular subcategories (e.g., *Groceries*, *Fuel*, *Rent*, *Fast Food*, *Metro*).
* **Custom Categories**: Users can create custom categories with custom icons and custom subcategory trees.

---

### 3. Data Safety, Persistence & Cloud Synchronisation
* **Local Database Engine**: Room Database with SQLite underneath. Completely functional offline.
* **Auto-Schema Migrations**: Incremental schema upgrades preserve all historical user data across application updates.
* **Optional Firebase Sync**: Allows users to back up their data securely in Firebase Cloud and restore on multiple Android devices.

---

## 5. Technical Specifications Table

| Metric | Details |
| :--- | :--- |
| **App Name** | Personal Expense Manager |
| **Package / Application ID** | `com.aistudio.personalfinance.shdwp` |
| **Architecture** | MVVM (Model-View-ViewModel) + Clean Architecture |
| **Language** | Kotlin 100% |
| **UI Framework** | Jetpack Compose + Material Design 3 (M3) |
| **Local Persistence** | Android Jetpack Room (SQLite) |
| **Async Flow** | Kotlin Coroutines & StateFlow |
| **SMS Handling** | Telephony BroadcastReceiver + Background Regex Parser |
| **Supported OS** | Android 7.0 (API 24) to Android 16 (API 36) |
