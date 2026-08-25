# Personal Finance Tracker with Cloud Sync

A production-ready, beautiful, and highly comprehensive **Personal Finance Tracker** built using **Kotlin**, **Jetpack Compose (Material Design 3)**, and a **local-first + cloud-synced architecture** (Room SQLite database integrated with Firebase Auth & Firestore).

---

## 🎨 Visual Theme & Style

- **Emerald Slate & Gold Accents**: Tailored for an elegant financial theme, featuring dark matte slate surfaces, crisp emerald elements (`#10B981`) for positive cash flows/income, soft crimson (`#EF4444`) for expense tracking, and refined gold accents (`#F59E0B`) for visual callouts and sync elements.
- **Material 3 Adaptive Design**: Completely fluid layouts leveraging Material Design 3 spacing and container-based dynamic constraints, optimizing perfectly for both compact phone layouts and larger tablet viewports.
- **Rich Iconography**: Extensive, contextual usage of Material Symbols to enable instant scanning of categories, accounts, cards, and debt types.

---

## 📱 Modules & Screens

The application features **12 dedicated visual panels** accessible via the secure left-hand navigation drawer:

### 1. Dashboard Overview (`dashboard`)
- **Net Worth & Monthly Cash Flow**: Summarizes cash inflows, outflows, and remaining funds dynamically.
- **Cash-Only Mode Toggle**: Allows users to filter out outstanding credit card transactions from their current-month financial metrics to avoid skewing cash flow with debt that is not yet paid. When toggled, only bank/cash transactions and actual credit card bill payments are shown in overall summaries.
- **Bank SMS Auto-Detection & Staging**: Real-time SMS transaction parser with 1-tap confirmation card at the top of the dashboard, plus an in-app SMS Simulator.
- **Interactive Reminders**: Displays dynamic billing alerts, unpaid debts, subscription renewals, and custom goals needing attention.
- **Recent Transactions Ledger**: Quick list of the latest recorded financial activities with visual indicators.

### 2. Transactions Ledger (`transactions`)
- **Full Ledger Control**: Add, delete, and view comprehensive list of Incomes, Expenses, Inter-Account Transfers, and Refunds/Reversals.
- **Inter-Account Fund Transfers**: Move funds between registered bank accounts or settle Credit Card bills without falsely inflating monthly income or expense charts.
- **Refunds & Reversals**: Log e-commerce returns, merchant refunds, and cashbacks to directly offset category expenditures rather than distorting income analytics.
- **Intelligent Filtering & Search**: Filter transactions instantly by type (All, Expense, Income, Transfer, Refund), payment methods, bank accounts, parent/subcategories, specific calendar dates, or custom string search.

### 3. Bank Accounts Management (`bank_accounts`)
- **Net Bank Balance Ledger**: Aggregates all bank accounts into a dynamic single-view total balance, with live inflow (credits) and outflow (debits) metrics.
- **Bank Identity & Custom Styling**: Color-coded branding cards for major Indian banks (HDFC, SBI, ICICI, Axis, Kotak, PNB, BOB, etc.) with account nicknames and masked numbers (`•••• Last4`).
- **Granular SMS Detection Toggle**: Turn auto-detection ON or OFF individually per bank account.
- **Selective Analytics Visibility Toggle**: Choose whether to include or hide specific bank accounts from global dashboard spending summaries and analytics charts.
- **Inline Transaction History**: Expandable transaction list per bank card, with one-tap pre-linked transaction creation.

### 4. Category Manager (`categories`)
- **Parent & Subcategory Nesting**: Create custom main categories with custom symbols and nest child subcategories inside them.
- **Dynamic Cascade Updates**: Modifying a category's metadata automatically updates associated budgets, transactions, and subcategory records in the database.

### 5. Monthly Budgets (`budgets`)
- **Visual Progress Trackers**: Displays relative bars indicating budget limits vs actual spent for each custom category.
- **Real-Time Limit Gauges**: Warns users with dynamic color shifts (amber/red) as category expenditures approach or exceed set limits.

### 6. Assets & Vehicles (`assets`)
- **Net Worth Cataloging**: Log financial assets, investments, real estate, tech devices, or vehicles.
- **Asset Metadata Tracking**: Record purchase prices, purchase dates, serial keys, warranty details, and insurance parameters.

### 7. Subscription Tracker (`subscriptions`)
- **Recurring Fee Monitor**: Tracks monthly/annual SaaS subscriptions, digital memberships, and recurring services.
- **Active footprint calculations**: Dynamically displays total periodic overhead cost and renewal calendar alert tickers.

### 8. Credit Cards Tracker (`credit_cards`)
- **Statement & Cycle Trackers**: Input credit limits, last 4 digits, billing/due days.
- **Dynamic Limit Monitoring**: Tracks outstanding balances against total credit limits to display real-time utilization stats.

### 9. Savings & Goals (`savings`)
- **Interactive Contributions**: Track progress towards specific dreams (e.g., Vacation Fund, Down Payment) with instant increments (e.g. "+$50" button).
- **Milestone Indicators**: Visually charts the journey toward the goal with progress bars and dynamic percentage markers.

### 10. Borrow & Lend Book (`borrow_lend`)
- **P2P Debt Ledgers**: Track money you have lent to or borrowed from colleagues, friends, or family.
- **Repayment Calendar & State**: Logs contact names, due dates, repayment status (paid/unpaid), and updates total net debt indicators.

### 11. Wishlist (`wishlist`)
- **Pre-purchase Cataloging**: Prioritize prospective purchases (High/Medium/Low priority) with estimated pricing and targeting dates.
- **One-Click Expense Conversion**: Marking a wishlist item as **Purchased** automatically transitions it into an active expense in your Transactions ledger, updating your monthly budgets and cash flow instantly.

### 12. Analytics & Insights (`analytics`)
- **Visual Breakdown Charts**: Renders visual distribution analytics, including expense category breakdowns, income-to-expense charts, payment method distribution, and multi-month budget performance trends.

---

## ☁️ Cloud Sync Architecture & Data Safety

The app utilizes a state-of-the-art **Offline-First, Cloud-Synced** approach, ensuring zero friction and high availability:

### 💾 Local SQLite Persistence (Room DB)
- All records are saved instantly to a local, high-performance Room SQLite database.
- The app remains **100% functional and fast offline**, with no loading indicators or network dependency for day-to-day entries.

### 🔐 Firebase Authentication
- Secure email & password registration and login options.
- User status updates are handled via an active auth listener inside the central `FinanceViewModel`.

### 🔄 Bidirectional Firestore Sync
- When authenticated, all local transactions, budgets, custom categories, credit cards, assets, subscriptions, goals, wishlist items, and peer debts are synced automatically to cloud Firestore.
- **Multi-Device Cohesion**: Logging in on a new device pulls down all cloud-stored histories and instantly populates the local SQLite tables, making phone switching seamless.
- **Data Isolation & Sandboxing**: To maintain strict security and complete multi-user compliance, data is securely stored in a sandboxed path unique to each individual user:
  ```
  /users/{FirebaseUID}/{CollectionName}/{DocumentID}
  ```
- **Security & Privacy**: No user can query, modify, or read data belonging to another user. If a user logs out, the app reverts securely to offline workspace mode.

---

## 📁 Technical Code Structure

- `/app/src/main/java/com/example/MainActivity.kt`: Controls the central layout container, dynamic dark/light configuration, navigation drawers, and the user Firebase Auth dialogue wrapper.
- `/app/src/main/java/com/example/ui/Screens.kt`: Houses the Jetpack Compose user interfaces, alerts, dialogs, charts, and control flows for all 11 modules.
- `/app/src/main/java/com/example/ui/FinanceViewModel.kt`: Central state manager. Exposes reactive streams (`StateFlow`), handles user registration/sign-in, and coordinates transactional Firestore-SQLite syncing.
- `/app/src/main/java/com/example/data/Entities.kt`: Models schemas for all database instances: `Transaction`, `Budget`, `Asset`, `Subscription`, `SavingsGoal`, `BorrowLend`, `Wishlist`, `CustomCategory`, `CreditCard`.
- `/app/src/main/java/com/example/data/FinanceDao.kt` & `FinanceRepository.kt`: Defines the robust room database transactions, queries, inserts, and local delete statements.
- `/app/src/main/java/com/example/ui/theme/Theme.kt` & `Color.kt`: Material 3 theme configurations, custom palettes, and visual dark-theme properties.

---

## 🚀 Confirming Production Readiness

This app is **production-ready and deployable to real-world users**.
- **Individually Isolated Syncing**: Each user's data is partitioned via their unique Firebase user ID. Multiple users can use the app simultaneously on different phones without overlapping data or security conflicts.
- **Seamless Local Caching**: Offline-first ensures that even with intermittent internet or zero cellular coverage, the user never suffers from lag or data loss.
- **Robust Schema Mapping**: Built with custom types and serializable elements, preventing sync bugs or type mismatch errors on high-frequency transactions.
