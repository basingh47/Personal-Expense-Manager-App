package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.data.*
import com.example.sms.SmsParser
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateTo: (String) -> Unit
) {
    val todaySpent by viewModel.todaySpending.collectAsStateWithLifecycle()
    val monthSpent by viewModel.thisMonthSpending.collectAsStateWithLifecycle()
    val monthIncome by viewModel.thisMonthIncome.collectAsStateWithLifecycle()
    val dailyAvg by viewModel.averageDailySpending.collectAsStateWithLifecycle()
    val percentChange by viewModel.percentSpendingChange.collectAsStateWithLifecycle()
    val highestExpense by viewModel.highestExpenseOfMonth.collectAsStateWithLifecycle()
    
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val upcomingReminders by viewModel.upcomingReminders.collectAsStateWithLifecycle()
    val pendingSmsList by viewModel.pendingSmsTransactions.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    val currentSavings = monthIncome - monthSpent
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showSimulateSmsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val checkAllPermissions = {
        val smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val notifGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        smsGranted && notifGranted
    }

    var hasSmsPermission by remember {
        mutableStateOf(checkAllPermissions())
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasSmsPermission = checkAllPermissions()
    }

    val categoryDefs = remember(customCategories) {
        val expenseDefs = customCategories.filter { it.parentCategory == null }.map { cat ->
            val subs = customCategories.filter { it.parentCategory == cat.name }.map { it.name }
            CategoryDef(name = cat.name, icon = cat.icon, subcategories = subs, isCustom = true)
        }
        if (expenseDefs.isNotEmpty()) expenseDefs else CategoryData.expenseCategories
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(PrimaryEmerald, Color(0xFF1B2A4A))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "EXPENSE OVERVIEW",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFD6E3FF),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Personal Expense Manager",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFFD6E3FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E7F3)
                        )
                    }
                }
            }
        }

        // Bank SMS Auto-Detection Status & Testing Banner
        item {
            SmsDetectionBannerCard(
                hasPermission = hasSmsPermission,
                pendingCount = pendingSmsList.size,
                onRequestPermission = {
                    val perms = mutableListOf(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(perms.toTypedArray())
                },
                onSimulateClick = {
                    showSimulateSmsDialog = true
                }
            )
        }

        // PENDING SMS TRANSACTIONS SECTION (Waiting for user categorization)
        if (pendingSmsList.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Action Required: Categorize Bank SMS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SmoothWhite
                            )
                        }
                        Text(
                            text = "${pendingSmsList.size} pending",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "We detected transactions from your bank messages. Tap a category below to log them directly:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
            }

            items(pendingSmsList, key = { "pending_sms_${it.id}" }) { pending ->
                PendingSmsCard(
                    pending = pending,
                    categories = categoryDefs,
                    onConfirm = { cat, sub ->
                        viewModel.confirmPendingSms(pending, cat, sub, pending.paymentMethod, pending.creditCardId)
                    },
                    onDismiss = {
                        viewModel.dismissPendingSms(pending)
                    },
                    onEdit = {
                        editingTransaction = Transaction(
                            amount = pending.amount,
                            category = if (pending.suggestedCategory.isNotBlank()) pending.suggestedCategory else "Food",
                            subcategory = "",
                            paymentMethod = pending.paymentMethod,
                            merchant = pending.merchant,
                            notes = "SMS: ${pending.rawBody}",
                            type = pending.type,
                            date = pending.date,
                            creditCardId = pending.creditCardId
                        )
                        viewModel.dismissPendingSms(pending)
                    }
                )
            }
        }

        // Toggle Credit Cards
        item {
            val excludeCreditCards by viewModel.excludeCreditCards.collectAsStateWithLifecycle()
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SecondarySage.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Cash-Only Mode (Exclude Credit Cards)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SmoothWhite
                            )
                            Text(
                                text = if (excludeCreditCards) "Showing bank/cash + credit card bill payments" else "Showing all transactions including active CC spending",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText
                            )
                        }
                    }
                    Switch(
                        checked = excludeCreditCards,
                        onCheckedChange = { viewModel.setExcludeCreditCards(it) },
                        modifier = Modifier.testTag("toggle_credit_cards_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryEmerald,
                            checkedTrackColor = PrimaryEmerald.copy(alpha = 0.3f),
                            uncheckedThumbColor = MutedText,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }
        }

        // Metrics Grid (Today, Month, Income, Savings)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Today's Spending",
                        amount = "₹${String.format(Locale.getDefault(), "%,.2f", todaySpent)}",
                        icon = Icons.Default.Today,
                        accentColor = RedExpense,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "This Month",
                        amount = "₹${String.format(Locale.getDefault(), "%,.2f", monthSpent)}",
                        icon = Icons.Default.Payments,
                        accentColor = RedExpense,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "This Month Income",
                        amount = "₹${String.format(Locale.getDefault(), "%,.2f", monthIncome)}",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = GreenIncome,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Net Savings",
                        amount = "₹${String.format(Locale.getDefault(), "%,.2f", currentSavings)}",
                        icon = Icons.Default.Savings,
                        accentColor = if (currentSavings >= 0) GreenIncome else RedExpense,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Analytics Highlights Row
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SecondarySage.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmoothWhite
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Average", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            Text("₹${String.format(Locale.getDefault(), "%.0f", dailyAvg)}/day", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SmoothWhite)
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text("vs Last Month", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (percentChange <= 0) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = if (percentChange <= 0) GreenIncome else RedExpense,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.1f", Math.abs(percentChange))}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (percentChange <= 0) GreenIncome else RedExpense
                                )
                            }
                        }
                    }

                    highestExpense?.let { hExp ->
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DarkSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Highest Single Expense", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                Text(hExp.merchant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SmoothWhite)
                            }
                            Text("₹${hExp.amount}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = RedExpense)
                        }
                    }
                }
            }
        }

        // Reminders & Upcoming Bills Section
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming Bills & Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmoothWhite
                    )
                    IconButton(onClick = { onNavigateTo("subscriptions") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View All", tint = AccentGold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (upcomingReminders.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateTo("subscriptions") }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "All caught up!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SmoothWhite
                                )
                                Text(
                                    text = "No bills or reminders due in the next 30 days.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        upcomingReminders.take(3).forEach { reminder ->
                            ReminderRowItem(reminder = reminder)
                        }
                    }
                }
            }
        }

        // Budget Status Row (Quick Glimpse)
        if (budgets.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Budget Utilization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SmoothWhite
                        )
                        IconButton(onClick = { onNavigateTo("budgets") }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View Budgets", tint = AccentGold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, SecondarySage.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            budgets.take(2).forEach { budget ->
                                val spent = transactions
                                    .filter { it.type == "EXPENSE" && it.category == budget.category }
                                    .sumOf { it.amount }
                                BudgetProgressRow(
                                    budget = budget,
                                    spent = spent,
                                    getIcon = { viewModel.getIconForCategory(it) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions Section
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmoothWhite
                    )
                    IconButton(onClick = { onNavigateTo("transactions") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View Transactions", tint = AccentGold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions logged yet.", color = MutedText)
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        transactions.take(5).forEach { tx ->
                            TransactionListItem(
                                transaction = tx,
                                onEditClick = { editingTransaction = tx },
                                onDeleteClick = {
                                    viewModel.requestDeleteConfirmation(
                                        title = "Delete Transaction?",
                                        message = "Are you sure you want to permanently delete this transaction for ₹${tx.amount} (${tx.category})?"
                                    ) {
                                        viewModel.deleteTransaction(tx)
                                    }
                                },
                                getIcon = { viewModel.getIconForCategory(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingTransaction != null) {
        AddTransactionDialog(
            viewModel = viewModel,
            transactionToEdit = editingTransaction,
            onDismiss = { editingTransaction = null }
        )
    }

    if (showSimulateSmsDialog) {
        SimulateSmsDialog(
            viewModel = viewModel,
            onDismiss = { showSimulateSmsDialog = false }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    amount: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SecondarySage.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SmoothWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ReminderRowItem(reminder: ReminderItem) {
    val context = LocalContext.current
    val formattedDate = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(reminder.dueDate))
    val isOverdue = reminder.dueDate < System.currentTimeMillis()

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SecondarySage.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (reminder.type) {
                                "SUBSCRIPTION" -> BlueCard.copy(alpha = 0.15f)
                                "LENT" -> GreenIncome.copy(alpha = 0.15f)
                                else -> RedExpense.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (reminder.type) {
                            "SUBSCRIPTION" -> Icons.Default.Autorenew
                            "LENT" -> Icons.AutoMirrored.Filled.CallMade
                            else -> Icons.AutoMirrored.Filled.CallReceived
                        },
                        contentDescription = null,
                        tint = when (reminder.type) {
                            "SUBSCRIPTION" -> BlueCard
                            "LENT" -> GreenIncome
                            else -> RedExpense
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmoothWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Due $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverdue) RedExpense else MutedText,
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Text(
                text = "₹${reminder.amount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SmoothWhite
            )
        }
    }
}

@Composable
fun BudgetProgressRow(
    budget: Budget,
    spent: Double,
    getIcon: (String) -> String = { CategoryData.getIconForCategory(it) }
) {
    val percent = if (budget.limitAmount > 0) spent / budget.limitAmount else 0.0
    val remaining = budget.limitAmount - spent
    val progressColor = if (percent >= 1.0) RedExpense else if (percent > 0.8) AccentGold else PrimaryLightEmerald

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${getIcon(budget.category)} ${budget.category}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = SmoothWhite
            )
            Text(
                text = "₹${spent.toInt()} of ₹${budget.limitAmount.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { Math.min(percent.toFloat(), 1.0f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = progressColor,
            trackColor = DarkSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (percent >= 1.0) "Limit Exceeded!" else "${(percent * 100).toInt()}% spent",
                style = MaterialTheme.typography.labelSmall,
                color = if (percent >= 1.0) RedExpense else MutedText
            )
            Text(
                text = "₹${remaining.toInt()} left",
                style = MaterialTheme.typography.labelSmall,
                color = if (remaining < 0) RedExpense else GreenIncome
            )
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    getIcon: (String) -> String = { CategoryData.getIconForCategory(it) }
) {
    val formattedDate = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Category Icon, Info (Merchant/Category/Date) and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (transaction.category.uppercase()) {
                                "FOOD", "DINING", "FOOD & DINING" -> Color(0xFFFFDAD6)
                                "TRANSPORT", "FUEL", "SCOOTER", "TRAVEL" -> Color(0xFFD6E3FF)
                                "SUBSCRIPTION", "UTILITIES", "BILLS", "SUBSCRIPTIONS" -> Color(0xFFF0E0FF)
                                "ENTERTAINMENT", "LEISURE" -> Color(0xFFE8DDFF)
                                "HEALTH", "MEDICAL" -> Color(0xFFFCE3E3)
                                "SHOPPING", "GROCERIES" -> Color(0xFFFFE0B2)
                                "INCOME", "SALARY", "SAVINGS" -> Color(0xFFD1E8D9)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getIcon(transaction.category),
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Transaction Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (transaction.merchant.isBlank()) transaction.category else transaction.merchant,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (transaction.subcategory.isNotBlank()) {
                            Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            Text(
                                text = transaction.subcategory,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount
                Text(
                    text = if (transaction.type == "INCOME") "+₹${transaction.amount}" else "-₹${transaction.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (transaction.type == "INCOME") GreenIncome else RedExpense
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subdued separator
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Row: Payment Method Badge and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment Method Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = transaction.paymentMethod,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Edit and Delete Actions (Clean, spaced, responsive, perfectly styled in light & dark mode)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onEditClick != null) {
                        FilledTonalIconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onDeleteClick != null) {
                        FilledTonalIconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// TRANSACTIONS SCREEN WITH ACCURATE ADDING & EDITING
@Composable
fun TransactionsScreen(viewModel: FinanceViewModel) {
    val filteredTx by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    
    val selectedCat by viewModel.selectedFilterCategory.collectAsStateWithLifecycle()
    val selectedMethod by viewModel.selectedFilterMethod.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedFilterMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedFilterDate.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                viewModel.setFilterDate(dateStr)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Clean Title & Add button header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SmoothWhite
            )
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_transaction_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Item", fontSize = 13.sp)
            }
        }

        // Advanced Date, Month, Category and Payment Method Filter scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chipColors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            val chipBorder = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                selectedBorderColor = Color.Transparent
            )

            // Filter Info / Clear button
            val isAnyFilterActive = selectedCat != null || selectedMethod != null || selectedMonth != null || selectedDate != null || searchText.isNotBlank()
            if (isAnyFilterActive) {
                FilterChip(
                    selected = true,
                    onClick = { viewModel.clearFilters() },
                    label = { Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = true,
                        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                        selectedBorderColor = Color.Transparent
                    )
                )
            }

            // Date Filter Chip
            val dateLabel = if (selectedDate != null) {
                try {
                    val dateObj = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate!!)
                    "📅 " + SimpleDateFormat("d MMM", Locale.getDefault()).format(dateObj!!)
                } catch (e: Exception) {
                    "📅 $selectedDate"
                }
            } else {
                "📅 Date"
            }
            FilterChip(
                selected = selectedDate != null,
                onClick = {
                    if (selectedDate != null) {
                        viewModel.setFilterDate(null)
                    } else {
                        datePickerDialog.show()
                    }
                },
                label = { Text(dateLabel, fontSize = 11.sp) },
                colors = chipColors,
                border = chipBorder
            )

            // Month Filter Chip
            val monthLabel = if (selectedMonth != null) {
                try {
                    val monthObj = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(selectedMonth!!)
                    "🗓️ " + SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(monthObj!!)
                } catch (e: Exception) {
                    "🗓️ $selectedMonth"
                }
            } else {
                "🗓️ Month"
            }
            Box {
                FilterChip(
                    selected = selectedMonth != null,
                    onClick = { showMonthDropdown = true },
                    label = { Text(monthLabel, fontSize = 11.sp) },
                    colors = chipColors,
                    border = chipBorder,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )

                DropdownMenu(
                    expanded = showMonthDropdown,
                    onDismissRequest = { showMonthDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Months", fontSize = 13.sp) },
                        onClick = {
                            viewModel.setFilterMonth(null)
                            showMonthDropdown = false
                        }
                    )
                    // Generate last 6 months list
                    val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    val valueFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    val cal = Calendar.getInstance()
                    for (i in 0 until 6) {
                        val displayMonth = monthFormat.format(cal.time)
                        val valMonth = valueFormat.format(cal.time)
                        DropdownMenuItem(
                            text = { Text(displayMonth, fontSize = 13.sp) },
                            onClick = {
                                viewModel.setFilterMonth(valMonth)
                                showMonthDropdown = false
                            }
                        )
                        cal.add(Calendar.MONTH, -1)
                    }
                }
            }

            // Category filters
            val categories = remember(customCategories) {
                val standard = listOf("Food", "Vehicle", "Home", "Work", "Entertainment", "Health", "Shopping", "Salary", "Freelancing")
                val customMain = customCategories.filter { it.parentCategory == null }.map { it.name }
                (standard + customMain).distinct()
            }
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCat == cat,
                    onClick = { viewModel.setFilterCategory(if (selectedCat == cat) null else cat) },
                    label = { Text("${viewModel.getIconForCategory(cat)} $cat", fontSize = 11.sp) },
                    colors = chipColors,
                    border = chipBorder
                )
            }

            // Payment method filters
            val methods = listOf("Cash", "UPI", "Credit Card", "Bank")
            methods.forEach { method ->
                FilterChip(
                    selected = selectedMethod == method,
                    onClick = { viewModel.setFilterMethod(if (selectedMethod == method) null else method) },
                    label = { Text(method, fontSize = 11.sp) },
                    colors = chipColors,
                    border = chipBorder
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transactions list
        if (filteredTx.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No transactions match criteria.", color = MutedText)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredTx) { tx ->
                    TransactionListItem(
                        transaction = tx,
                        onEditClick = { editingTransaction = tx },
                        onDeleteClick = {
                            viewModel.requestDeleteConfirmation(
                                title = "Delete Transaction?",
                                message = "Are you sure you want to permanently delete this transaction for ₹${tx.amount} (${tx.category})?"
                            ) {
                                viewModel.deleteTransaction(tx)
                            }
                        },
                        getIcon = { viewModel.getIconForCategory(it) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    if (editingTransaction != null) {
        AddTransactionDialog(
            viewModel = viewModel,
            transactionToEdit = editingTransaction,
            onDismiss = { editingTransaction = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    viewModel: FinanceViewModel,
    transactionToEdit: Transaction? = null,
    onDismiss: () -> Unit
) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()

    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(transactionToEdit?.type ?: "EXPENSE") }
    var selectedCategory by remember { mutableStateOf(transactionToEdit?.category ?: "Food") }
    var selectedSubcategory by remember { mutableStateOf(transactionToEdit?.subcategory ?: "Lunch") }
    var paymentMethod by remember { mutableStateOf(transactionToEdit?.paymentMethod ?: "UPI") }
    var merchant by remember { mutableStateOf(transactionToEdit?.merchant ?: "") }
    var notes by remember { mutableStateOf(transactionToEdit?.notes ?: "") }
    var tagsString by remember { mutableStateOf(transactionToEdit?.tagsString ?: "") }
    var selectedAssetId by remember { mutableStateOf<Long?>(transactionToEdit?.assetId) }
    var selectedCreditCardId by remember { mutableStateOf<Long?>(transactionToEdit?.creditCardId) }

    val context = LocalContext.current
    var selectedDateLong by remember { mutableStateOf(transactionToEdit?.date ?: System.currentTimeMillis()) }
    val calendar = remember(selectedDateLong) {
        Calendar.getInstance().apply { timeInMillis = selectedDateLong }
    }
    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                selectedDateLong = cal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Modal state for custom Category / Subcategory addition
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddSubcategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("🍔") }
    var newSubcategoryName by remember { mutableStateOf("") }

    val categoriesList = remember(selectedType, customCategories) {
        val standard = if (selectedType == "EXPENSE") CategoryData.expenseCategories else CategoryData.incomeCategories
        val customMain = customCategories.filter { it.type == selectedType && it.parentCategory == null }
        
        standard.map { stdCat ->
            val extraSubs = customCategories.filter { it.parentCategory == stdCat.name }.map { it.name }
            if (extraSubs.isNotEmpty()) {
                stdCat.copy(subcategories = stdCat.subcategories + extraSubs)
            } else {
                stdCat
            }
        } + customMain.map { custCat ->
            val subs = customCategories.filter { it.parentCategory == custCat.name }.map { it.name }
            CategoryDef(
                name = custCat.name,
                icon = custCat.icon,
                subcategories = subs
            )
        }
    }

    var isFirstLoad by remember { mutableStateOf(true) }

    // Auto update categories
    LaunchedEffect(selectedType) {
        if (isFirstLoad) {
            // skip on first load to preserve pre-populated properties
        } else {
            val firstCat = categoriesList.firstOrNull()
            if (firstCat != null) {
                selectedCategory = firstCat.name
                selectedSubcategory = firstCat.subcategories.firstOrNull() ?: ""
            }
        }
    }
    LaunchedEffect(selectedCategory, categoriesList) {
        if (isFirstLoad) {
            isFirstLoad = false
        } else {
            val matchingCat = categoriesList.firstOrNull { it.name == selectedCategory }
            selectedSubcategory = matchingCat?.subcategories?.firstOrNull() ?: ""
        }
    }

    if (showAddCategoryDialog) {
        Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Custom Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose Icon / Emoji", style = MaterialTheme.typography.labelSmall, color = MutedText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val emojis = listOf("🍔", "🛵", "🏠", "💻", "📚", "🎮", "✈️", "🏥", "🛍️", "📈", "🛡️", "🐾", "🎁", "🌀", "💵", "🚀", "🏢", "🔄", "💰", "🍽️", "🍿", "👗", "💅", "💆", "💇", "🏋️", "🏀", "🎤", "🚗", "🚲", "🔌", "🧴", "🧸")
                            emojis.forEach { emo ->
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (newCategoryIcon == emo) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { newCategoryIcon = emo }
                                        .padding(8.dp)
                                ) {
                                    Text(emo, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = newCategoryIcon,
                        onValueChange = { if (it.length <= 4) newCategoryIcon = it },
                        label = { Text("Or Type Custom Emoji") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddCategoryDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (newCategoryName.isNotBlank()) {
                                    viewModel.addCustomCategory(
                                        name = newCategoryName.trim(),
                                        icon = newCategoryIcon,
                                        type = selectedType,
                                        parentCategory = null
                                    )
                                    selectedCategory = newCategoryName.trim()
                                    newCategoryName = ""
                                    showAddCategoryDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }

    if (showAddSubcategoryDialog) {
        Dialog(onDismissRequest = { showAddSubcategoryDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Custom Subcategory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Under: $selectedCategory",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                    
                    OutlinedTextField(
                        value = newSubcategoryName,
                        onValueChange = { newSubcategoryName = it },
                        label = { Text("Subcategory Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddSubcategoryDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (newSubcategoryName.isNotBlank()) {
                                    viewModel.addCustomCategory(
                                        name = newSubcategoryName.trim(),
                                        icon = "",
                                        type = selectedType,
                                        parentCategory = selectedCategory
                                    )
                                    selectedSubcategory = newSubcategoryName.trim()
                                    newSubcategoryName = ""
                                    showAddSubcategoryDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (transactionToEdit != null) "Edit Transaction" else "New Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Transaction Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedType = "EXPENSE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "EXPENSE") RedExpense else DarkSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Expense")
                    }
                    Button(
                        onClick = { selectedType = "INCOME" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "INCOME") GreenIncome else DarkSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Income")
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                // Transaction Date Picker
                val formattedDate = remember(selectedDateLong) {
                    SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(selectedDateLong))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .clickable { datePickerDialog.show() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Transaction Date", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(formattedDate, style = MaterialTheme.typography.bodyMedium, color = SmoothWhite, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Choose Date",
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Merchant
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Source", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                // Category selection dropdown
                Column {
                    Text("Category", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categoriesList.forEach { cat ->
                            val isSelected = selectedCategory == cat.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryEmerald else DarkSurfaceVariant)
                                    .clickable { selectedCategory = cat.name }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("${cat.icon} ${cat.name}", color = SmoothWhite, fontSize = 14.sp)
                            }
                        }

                        // Add Custom Category button!
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { showAddCategoryDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Category",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Custom", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Subcategory selection dropdown
                val subcats = remember(selectedCategory, categoriesList) {
                    categoriesList.firstOrNull { it.name == selectedCategory }?.subcategories ?: emptyList()
                }
                Column {
                    Text("Subcategory", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        subcats.forEach { sub ->
                            val isSelected = selectedSubcategory == sub
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AccentGold else DarkSurfaceVariant)
                                    .clickable { selectedSubcategory = sub }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(sub, color = if (isSelected) DarkBackground else SmoothWhite, fontSize = 12.sp)
                            }
                        }

                        // Add Custom Subcategory button!
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { showAddSubcategoryDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Subcategory",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Custom", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Payment Method Selector
                Column {
                    Text("Payment Method", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val pmList = listOf("UPI", "Cash", "Credit Card", "Bank")
                        pmList.forEach { pm ->
                            val isSelected = paymentMethod == pm
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryEmerald else DarkSurfaceVariant)
                                    .clickable { paymentMethod = pm }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(pm, color = SmoothWhite, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Link to Asset / Vehicle (Optional)
                if (assets.isNotEmpty()) {
                    Column {
                        Text("Link to Asset (Optional)", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedAssetId == null) AccentGold else DarkSurfaceVariant)
                                    .clickable { selectedAssetId = null }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("None", color = if (selectedAssetId == null) DarkBackground else SmoothWhite, fontSize = 12.sp)
                            }

                            assets.forEach { ast ->
                                val isSelected = selectedAssetId == ast.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) AccentGold else DarkSurfaceVariant)
                                        .clickable { selectedAssetId = ast.id }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(ast.name, color = if (isSelected) DarkBackground else SmoothWhite, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (paymentMethod == "Credit Card") {
                    val creditCards by viewModel.creditCards.collectAsStateWithLifecycle()
                    if (creditCards.isNotEmpty()) {
                        Column {
                            Text("Select Credit Card", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                creditCards.forEach { card ->
                                    val isSelected = selectedCreditCardId == card.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) AccentGold else DarkSurfaceVariant)
                                            .clickable { selectedCreditCardId = card.id }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        val last4 = if (card.lastFourDigits.isNotBlank()) " (*${card.lastFourDigits})" else ""
                                        Text("${card.cardName}$last4", color = if (isSelected) DarkBackground else SmoothWhite, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Text("No credit cards added. Set up a credit card in the Credit Cards screen to link this transaction.", color = AccentGold, style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Notes & Tags
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = tagsString,
                    onValueChange = { tagsString = it },
                    label = { Text("Tags (comma separated, e.g. Office, Family)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )
                }

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MutedText)
                    }
                    Button(
                        onClick = {
                            val amtVal = amount.toDoubleOrNull() ?: 0.0
                            if (amtVal > 0) {
                                val transaction = if (transactionToEdit != null) {
                                    transactionToEdit.copy(
                                        date = selectedDateLong,
                                        amount = amtVal,
                                        category = selectedCategory,
                                        subcategory = selectedSubcategory,
                                        paymentMethod = paymentMethod,
                                        merchant = merchant,
                                        notes = notes,
                                        tagsString = tagsString,
                                        type = selectedType,
                                        assetId = selectedAssetId,
                                        creditCardId = if (paymentMethod == "Credit Card") selectedCreditCardId else null
                                    )
                                } else {
                                    Transaction(
                                        date = selectedDateLong,
                                        amount = amtVal,
                                        category = selectedCategory,
                                        subcategory = selectedSubcategory,
                                        paymentMethod = paymentMethod,
                                        merchant = merchant,
                                        notes = notes,
                                        tagsString = tagsString,
                                        type = selectedType,
                                        assetId = selectedAssetId,
                                        creditCardId = if (paymentMethod == "Credit Card") selectedCreditCardId else null
                                    )
                                }
                                viewModel.addTransaction(transaction)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ASSETS / VEHICLE HISTORY TIMELINE TRACKING SCREEN
@Composable
fun AssetsScreen(viewModel: FinanceViewModel) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAsset by remember { mutableStateOf<Asset?>(null) }
    var editingAsset by remember { mutableStateOf<Asset?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Valuable Assets & Vehicles",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SmoothWhite,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (assets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No assets registered. Create one to track its timeline cost!", color = MutedText)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(assets) { asset ->
                    val maintenanceTx = transactions.filter { it.assetId == asset.id }
                    val totalMaintenance = maintenanceTx.sumOf { it.amount }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAsset = asset }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (asset.type == "VEHICLE") "🛵" else "💻",
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(asset.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SmoothWhite)
                                        Text("Bought ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(asset.purchaseDate))}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { editingAsset = asset }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryEmerald)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.requestDeleteConfirmation(
                                                title = "Delete Asset?",
                                                message = "Are you sure you want to permanently delete '${asset.name}'?"
                                            ) {
                                                viewModel.deleteAsset(asset)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedExpense)
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DarkSurfaceVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Purchase Price", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    Text("₹${String.format(Locale.getDefault(), "%,.0f", asset.purchasePrice)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = SmoothWhite)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Maintenance & Fuel Cost", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    Text("₹${String.format(Locale.getDefault(), "%,.0f", totalMaintenance)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = AccentGold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Click to view full maintenance timeline & insurance details →",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryLightEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAssetDialog(viewModel = viewModel, onDismiss = { showAddDialog = false })
    }

    if (editingAsset != null) {
        AddAssetDialog(
            viewModel = viewModel,
            assetToEdit = editingAsset,
            onDismiss = { editingAsset = null }
        )
    }

    if (selectedAsset != null) {
        AssetDetailDialog(
            asset = selectedAsset!!,
            transactions = transactions.filter { it.assetId == selectedAsset!!.id },
            onDismiss = { selectedAsset = null }
        )
    }
}

@Composable
fun AddAssetDialog(
    viewModel: FinanceViewModel,
    assetToEdit: Asset? = null,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(assetToEdit?.name ?: "") }
    var selectedType by remember { mutableStateOf(assetToEdit?.type ?: "VEHICLE") }
    var price by remember { mutableStateOf(assetToEdit?.purchasePrice?.toString() ?: "") }
    var insurance by remember { mutableStateOf(assetToEdit?.insuranceDetails ?: "") }
    var warranty by remember { mutableStateOf(assetToEdit?.warrantyDetails ?: "") }
    var notes by remember { mutableStateOf(assetToEdit?.notes ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (assetToEdit != null) "Edit Asset" else "Add New Asset",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedType = "VEHICLE" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "VEHICLE") PrimaryEmerald else DarkSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Vehicle")
                    }
                    Button(
                        onClick = { selectedType = "ELECTRONICS" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "ELECTRONICS") PrimaryEmerald else DarkSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Device / Other")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Asset Name (e.g. Ola Scooter)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Purchase Price (₹)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = insurance,
                    onValueChange = { insurance = it },
                    label = { Text("Insurance details", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = warranty,
                    onValueChange = { warranty = it },
                    label = { Text("Warranty details", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = MutedText)
                    }
                    Button(
                        onClick = {
                            val prVal = price.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) {
                                val asset = if (assetToEdit != null) {
                                    assetToEdit.copy(
                                        name = name,
                                        type = selectedType,
                                        purchasePrice = prVal,
                                        insuranceDetails = insurance,
                                        warrantyDetails = warranty,
                                        notes = notes
                                    )
                                } else {
                                    Asset(
                                        name = name,
                                        type = selectedType,
                                        purchasePrice = prVal,
                                        purchaseDate = System.currentTimeMillis(),
                                        insuranceDetails = insurance,
                                        warrantyDetails = warranty,
                                        notes = notes
                                    )
                                }
                                viewModel.addAsset(asset)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ASSET HISTORY VERTICAL TIMELINE DIALOG
@Composable
fun AssetDetailDialog(
    asset: Asset,
    transactions: List<Transaction>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )
                Text(
                    text = "Total maintenance: ₹${transactions.sumOf { it.amount }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentGold,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Insurance details card
                    if (asset.insuranceDetails.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("🛡️ Insurance Policy Info", style = MaterialTheme.typography.bodySmall, color = AccentGold, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(asset.insuranceDetails, style = MaterialTheme.typography.bodyMedium, color = SmoothWhite)
                            }
                        }
                    }

                    // Timeline Title
                    Text("Maintenance & Service Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SmoothWhite)

                    if (transactions.isEmpty()) {
                        Text("No recorded maintenance history. Add transactions linking to this asset to build your timeline history!", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    } else {
                        // Custom Vertical Timeline
                        Column {
                            transactions.sortedBy { it.date }.forEachIndexed { index, tx ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Bullet line
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(32.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryLightEmerald)
                                        )
                                        if (index < transactions.lastIndex) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(70.dp)
                                                    .background(SecondarySage)
                                            )
                                        }
                                    }

                                    // Timeline contents card
                                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                                        Text(
                                            text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(tx.date)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AccentGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${tx.subcategory} • ₹${tx.amount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SmoothWhite
                                        )
                                        if (tx.notes.isNotBlank()) {
                                            Text(
                                                text = tx.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MutedText
                                            )
                                        }
                                        Text(
                                            text = "Paid via ${tx.paymentMethod} at ${tx.merchant}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}

// BUDGETS SCREEN
@Composable
fun BudgetItemCard(
    budget: Budget,
    spent: Double,
    getIcon: (String) -> String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (budget.category.uppercase()) {
                                "FOOD", "DINING", "FOOD & DINING" -> Color(0xFFFFDAD6)
                                "TRANSPORT", "FUEL", "SCOOTER", "TRAVEL" -> Color(0xFFD6E3FF)
                                "SUBSCRIPTION", "UTILITIES", "BILLS", "SUBSCRIPTIONS" -> Color(0xFFF0E0FF)
                                "ENTERTAINMENT", "LEISURE" -> Color(0xFFE8DDFF)
                                "HEALTH", "MEDICAL" -> Color(0xFFFCE3E3)
                                "SHOPPING", "GROCERIES" -> Color(0xFFFFE0B2)
                                "INCOME", "SALARY", "SAVINGS" -> Color(0xFFD1E8D9)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getIcon(budget.category),
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and values
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₹${spent.toInt()} spent of ₹${budget.limitAmount.toInt()} limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }

                // Edit/Delete actions in a row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryEmerald, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedExpense, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress indicator and remaining calculation
            val percent = if (budget.limitAmount > 0) spent / budget.limitAmount else 0.0
            val remaining = budget.limitAmount - spent
            val progressColor = if (percent >= 1.0) RedExpense else if (percent > 0.8) AccentGold else PrimaryLightEmerald

            LinearProgressIndicator(
                progress = { Math.min(percent.toFloat(), 1.0f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (percent >= 1.0) "Limit Exceeded!" else "${(percent * 100).toInt()}% spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (percent >= 1.0) RedExpense else MutedText,
                    fontWeight = if (percent >= 1.0) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = if (remaining < 0) "₹${Math.abs(remaining.toInt())} over budget" else "₹${remaining.toInt()} left",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remaining < 0) RedExpense else GreenIncome,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BudgetsScreen(viewModel: FinanceViewModel) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<Budget?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monthly Category Budgets",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SmoothWhite,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_budget_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Budget", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No budgets configured yet.", color = MutedText)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(budgets) { budget ->
                    val spent = transactions
                        .filter { it.type == "EXPENSE" && it.category == budget.category }
                        .sumOf { it.amount }

                    BudgetItemCard(
                        budget = budget,
                        spent = spent,
                        getIcon = { viewModel.getIconForCategory(it) },
                        onEditClick = { editingBudget = budget },
                        onDeleteClick = {
                            viewModel.requestDeleteConfirmation(
                                title = "Delete Budget?",
                                message = "Are you sure you want to delete the budget limit for category '${budget.category}'?"
                            ) {
                                viewModel.deleteBudget(budget)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(viewModel = viewModel, onDismiss = { showAddDialog = false })
    }

    if (editingBudget != null) {
        AddBudgetDialog(
            viewModel = viewModel,
            budgetToEdit = editingBudget,
            onDismiss = { editingBudget = null }
        )
    }
}

@Composable
fun AddBudgetDialog(
    viewModel: FinanceViewModel,
    budgetToEdit: Budget? = null,
    onDismiss: () -> Unit
) {
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val categoriesList = remember(customCategories) {
        val standard = CategoryData.expenseCategories
        val customMain = customCategories.filter { it.type == "EXPENSE" && it.parentCategory == null }
        standard + customMain.map { custCat ->
            CategoryDef(
                name = custCat.name,
                icon = custCat.icon,
                subcategories = emptyList()
            )
        }
    }

    var selectedCategory by remember { mutableStateOf(budgetToEdit?.category ?: categoriesList.firstOrNull()?.name ?: "Food") }
    var limit by remember { mutableStateOf(budgetToEdit?.limitAmount?.toString() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (budgetToEdit != null) "Edit Budget Limit" else "Configure Budget",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                Column {
                    Text("Select Category", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoriesList.forEach { cat ->
                            val isSelected = selectedCategory == cat.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryEmerald else DarkSurfaceVariant)
                                    .clickable { selectedCategory = cat.name }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("${cat.icon} ${cat.name}", color = SmoothWhite, fontSize = 14.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("Monthly Limit Amount (₹)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text("Cancel", color = SmoothWhite)
                    }
                    Button(
                        onClick = {
                            val limVal = limit.toDoubleOrNull() ?: 0.0
                            if (limVal > 0) {
                                val budget = if (budgetToEdit != null) {
                                    budgetToEdit.copy(
                                        category = selectedCategory,
                                        limitAmount = limVal
                                    )
                                } else {
                                    Budget(
                                        category = selectedCategory,
                                        limitAmount = limVal,
                                        monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                                    )
                                }
                                viewModel.addBudget(budget)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

private fun getBillingCycleMonths(cycle: String): Int {
    return when (cycle) {
        "MONTHLY" -> 1
        "YEARLY" -> 12
        "2_MONTHS" -> 2
        "3_MONTHS" -> 3
        "4_MONTHS" -> 4
        "5_MONTHS" -> 5
        "6_MONTHS" -> 6
        else -> {
            if (cycle.endsWith("_MONTHS")) {
                cycle.substringBefore("_MONTHS").toIntOrNull() ?: 1
            } else {
                1
            }
        }
    }
}

private fun getBillingCycleCardLabel(cycle: String): String {
    return when (cycle) {
        "MONTHLY" -> "per month"
        "YEARLY" -> "per year"
        "2_MONTHS" -> "every 2 mos"
        "3_MONTHS" -> "every 3 mos"
        "4_MONTHS" -> "every 4 mos"
        "5_MONTHS" -> "every 5 mos"
        "6_MONTHS" -> "every 6 mos"
        else -> {
            if (cycle.endsWith("_MONTHS")) {
                "every ${cycle.substringBefore("_MONTHS")} mos"
            } else {
                "per cycle"
            }
        }
    }
}

// SUBSCRIPTION TRACKER SCREEN
@Composable
fun SubscriptionCard(
    sub: Subscription,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subscription Icon / Brand Logo Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (sub.isActive) PrimaryEmerald.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (sub.billingCycle == "MONTHLY") Icons.Default.CloudSync else Icons.Default.AllInclusive,
                        contentDescription = null,
                        tint = if (sub.isActive) PrimaryLightEmerald else MutedText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Billing details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (sub.isActive) PrimaryEmerald.copy(alpha = 0.12f)
                                    else RedExpense.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (sub.isActive) "Active" else "Paused",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sub.isActive) PrimaryLightEmerald else RedExpense,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Next payment: ${sdf.format(Date(sub.renewalDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }

                // Cost display
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "₹${sub.cost.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = getBillingCycleCardLabel(sub.billingCycle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action row with Edit, Delete, and Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit subscription",
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete subscription",
                            tint = RedExpense,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (sub.isActive) "Auto-renew On" else "Auto-renew Off",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sub.isActive) PrimaryLightEmerald else MutedText
                    )
                    Switch(
                        checked = sub.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SmoothWhite,
                            checkedTrackColor = PrimaryEmerald,
                            uncheckedThumbColor = MutedText,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionsScreen(viewModel: FinanceViewModel) {
    val subs by viewModel.subscriptions.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSub by remember { mutableStateOf<Subscription?>(null) }

    val totalMonthlyCost = subs.filter { it.isActive }.sumOf {
        val months = getBillingCycleMonths(it.billingCycle)
        it.cost / months
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Summary Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Subscription Metric", style = MaterialTheme.typography.labelSmall, color = AccentGold, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${String.format(Locale.getDefault(), "%,.1f", totalMonthlyCost)}/mo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SmoothWhite)
                    Text("Combined cost of active products", style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (subs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No subscriptions tracked yet.", color = MutedText)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(subs) { sub ->
                    SubscriptionCard(
                        sub = sub,
                        onEditClick = { editingSub = sub },
                        onDeleteClick = {
                            viewModel.requestDeleteConfirmation(
                                title = "Delete Subscription?",
                                message = "Are you sure you want to permanently delete tracked subscription for '${sub.name}'?"
                            ) {
                                viewModel.deleteSubscription(sub)
                            }
                        },
                        onToggleActive = { viewModel.toggleSubscriptionActive(sub) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialog(viewModel = viewModel, onDismiss = { showAddDialog = false })
    }

    if (editingSub != null) {
        AddSubscriptionDialog(
            viewModel = viewModel,
            subToEdit = editingSub,
            onDismiss = { editingSub = null }
        )
    }
}

@Composable
fun AddSubscriptionDialog(
    viewModel: FinanceViewModel,
    subToEdit: Subscription? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(subToEdit?.name ?: "") }
    var cost by remember { mutableStateOf(subToEdit?.cost?.toString() ?: "") }
    var selectedCycle by remember { mutableStateOf(subToEdit?.billingCycle ?: "MONTHLY") }

    // Subscription start date selection
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val nextBillingDate = remember(startDate, selectedCycle) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startDate
        }
        val months = getBillingCycleMonths(selectedCycle)
        cal.add(Calendar.MONTH, months)
        cal.timeInMillis
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (subToEdit != null) "Edit Subscription" else "Add Subscription",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name (e.g. Netflix, Spotify)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                )

                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Cost (₹)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                )

                var isCycleDropdownExpanded by remember { mutableStateOf(false) }
                val cycleOptions = remember {
                    listOf(
                        "MONTHLY" to "Monthly (1 Month)",
                        "2_MONTHS" to "Every 2 Months",
                        "3_MONTHS" to "Every 3 Months (Quarterly)",
                        "4_MONTHS" to "Every 4 Months",
                        "5_MONTHS" to "Every 5 Months",
                        "6_MONTHS" to "Every 6 Months (Half-Yearly)",
                        "YEARLY" to "Yearly (12 Months)"
                    )
                }

                Column {
                    Text("Billing Cycle", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .clickable { isCycleDropdownExpanded = true }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = cycleOptions.find { it.first == selectedCycle }?.second ?: "Monthly (1 Month)",
                                color = SmoothWhite,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Cycle",
                                tint = PrimaryLightEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = isCycleDropdownExpanded,
                            onDismissRequest = { isCycleDropdownExpanded = false },
                            modifier = Modifier
                                .background(DarkSurfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        ) {
                            cycleOptions.forEach { (cycleKey, cycleValue) ->
                                DropdownMenuItem(
                                    text = { Text(cycleValue, color = SmoothWhite) },
                                    onClick = {
                                        selectedCycle = cycleKey
                                        isCycleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column {
                    Text("Subscription Start Date", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .clickable {
                                val currentCal = Calendar.getInstance().apply { timeInMillis = startDate }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selectedCal = Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth)
                                        }
                                        startDate = selectedCal.timeInMillis
                                    },
                                    currentCal.get(Calendar.YEAR),
                                    currentCal.get(Calendar.MONTH),
                                    currentCal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateFormatter.format(Date(startDate)),
                            color = SmoothWhite,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = PrimaryLightEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryEmerald.copy(alpha = 0.08f))
                        .border(1.dp, PrimaryEmerald.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Auto-calculated Next Billing Date:",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryLightEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dateFormatter.format(Date(nextBillingDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SmoothWhite,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = SmoothWhite)
                    }
                    Button(
                        onClick = {
                            val costVal = cost.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) {
                                val sub = if (subToEdit != null) {
                                    subToEdit.copy(
                                        name = name,
                                        cost = costVal,
                                        billingCycle = selectedCycle,
                                        renewalDate = nextBillingDate
                                    )
                                } else {
                                    Subscription(
                                        name = name,
                                        cost = costVal,
                                        renewalDate = nextBillingDate,
                                        billingCycle = selectedCycle,
                                        isActive = true
                                    )
                                }
                                viewModel.addSubscription(sub)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", color = SmoothWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// SAVINGS GOALS SCREEN
@Composable
fun SavingsGoalsScreen(viewModel: FinanceViewModel) {
    val goals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var selectedGoalForDeposit by remember { mutableStateOf<SavingsGoal?>(null) }
    var depositAmount by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Savings & Financial Goals",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SmoothWhite,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No savings goals created yet.", color = MutedText)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(goals) { goal ->
                    val progress = if (goal.targetAmount > 0) goal.currentAmount / goal.targetAmount else 0.0
                    val remaining = goal.targetAmount - goal.currentAmount

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SmoothWhite)
                                    Text("Target Date: ${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(goal.targetDate))}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { editingGoal = goal }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryEmerald)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.requestDeleteConfirmation(
                                                title = "Delete Savings Goal?",
                                                message = "Are you sure you want to permanently delete savings goal '${goal.name}'?"
                                            ) {
                                                viewModel.deleteSavingsGoal(goal)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedExpense)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Saved: ₹${goal.currentAmount.toInt()}", style = MaterialTheme.typography.bodyMedium, color = SmoothWhite, fontWeight = FontWeight.Bold)
                                Text("Target: ₹${goal.targetAmount.toInt()}", style = MaterialTheme.typography.bodyMedium, color = MutedText)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { Math.min(progress.toFloat(), 1.0f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = PrimaryLightEmerald,
                                trackColor = DarkSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (remaining <= 0) "Goal Achieved! 🎉" else "₹${remaining.toInt()} remaining",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (remaining <= 0) GreenIncome else AccentGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { selectedGoalForDeposit = goal },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Add Money", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSavingsGoalDialog(viewModel = viewModel, onDismiss = { showAddDialog = false })
    }

    if (editingGoal != null) {
        AddSavingsGoalDialog(
            viewModel = viewModel,
            goalToEdit = editingGoal,
            onDismiss = { editingGoal = null }
        )
    }

    if (selectedGoalForDeposit != null) {
        Dialog(onDismissRequest = { selectedGoalForDeposit = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Add money to ${selectedGoalForDeposit!!.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SmoothWhite)
                    OutlinedTextField(
                        value = depositAmount,
                        onValueChange = { depositAmount = it },
                        label = { Text("Deposit Amount (₹)", color = MutedText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(onClick = { selectedGoalForDeposit = null }) {
                            Text("Cancel", color = MutedText)
                        }
                        Button(
                            onClick = {
                                val dep = depositAmount.toDoubleOrNull() ?: 0.0
                                if (dep > 0) {
                                    viewModel.updateSavingsGoalProgress(selectedGoalForDeposit!!, dep)
                                    depositAmount = ""
                                    selectedGoalForDeposit = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddSavingsGoalDialog(
    viewModel: FinanceViewModel,
    goalToEdit: SavingsGoal? = null,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(goalToEdit?.name ?: "") }
    var target by remember { mutableStateOf(goalToEdit?.targetAmount?.toString() ?: "") }
    var current by remember { mutableStateOf(goalToEdit?.currentAmount?.toString() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (goalToEdit != null) "Edit Savings Goal" else "Create Savings Goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name (e.g. New Laptop, Emergency Fund)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Amount (₹)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text("Currently Saved Amount (Optional)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = MutedText)
                    }
                    Button(
                        onClick = {
                            val tgVal = target.toDoubleOrNull() ?: 0.0
                            val curVal = current.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && tgVal > 0) {
                                val goal = if (goalToEdit != null) {
                                    goalToEdit.copy(
                                        name = name,
                                        targetAmount = tgVal,
                                        currentAmount = curVal
                                    )
                                } else {
                                    SavingsGoal(
                                        name = name,
                                        targetAmount = tgVal,
                                        currentAmount = curVal,
                                        targetDate = System.currentTimeMillis() + 180L * 24 * 3600 * 1000 // 6 months target
                                    )
                                }
                                viewModel.addSavingsGoal(goal)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (goalToEdit != null) "Save" else "Create")
                    }
                }
            }
        }
    }
}

// BORROW & LEND SCREEN
@Composable
fun BorrowLendScreen(viewModel: FinanceViewModel) {
    val items by viewModel.borrowLends.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLoan by remember { mutableStateOf<BorrowLend?>(null) }
    var filterType by remember { mutableStateOf("ALL") } // "ALL", "LENT", "BORROWED"

    val filteredItems = remember(items, filterType) {
        when (filterType) {
            "LENT" -> items.filter { it.type == "LENT" }
            "BORROWED" -> items.filter { it.type != "LENT" }
            else -> items
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Borrow & Lend Track",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SmoothWhite,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Book", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Category Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "All Logs", "LENT" to "Lent (To Collect)", "BORROWED" to "Borrowed (To Pay)").forEach { (type, label) ->
                val isSelected = filterType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimaryEmerald else DarkSurfaceVariant)
                        .clickable { filterType = type }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else SmoothWhite,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bulk Removal Option for Settled Records
        val hasPaid = filteredItems.any { it.isPaid }
        if (hasPaid) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        viewModel.requestDeleteConfirmation(
                            title = "Remove All Settled?",
                            message = "Are you sure you want to delete all paid/settled transactions in your borrow & lend ledger?"
                        ) {
                            filteredItems.filter { it.isPaid }.forEach { loan ->
                                viewModel.deleteBorrowLend(loan)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RedExpense)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove All Settled", color = RedExpense, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (filterType) {
                        "LENT" -> "No recorded loans to collect."
                        "BORROWED" -> "No recorded borrowings to pay."
                        else -> "No recorded loans or borrowings."
                    },
                    color = MutedText
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems) { loan ->
                    val isLent = loan.type == "LENT"
                    val formattedDate = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(loan.dueDate))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SecondarySage.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isLent) GreenIncome.copy(alpha = 0.15f) else RedExpense.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isLent) Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.CallReceived,
                                        contentDescription = null,
                                        tint = if (isLent) GreenIncome else RedExpense,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(loan.contactName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = SmoothWhite)
                                    Text(if (isLent) "Lent to them" else "Borrowed from them", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                    Text("Due: $formattedDate", style = MaterialTheme.typography.labelSmall, color = AccentGold)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("₹${loan.amount}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = SmoothWhite)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = loan.isPaid,
                                            onCheckedChange = { viewModel.toggleBorrowLendPaid(loan) },
                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryEmerald)
                                        )
                                        Text("Paid", style = MaterialTheme.typography.labelSmall, color = SmoothWhite)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { editingLoan = loan }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit record", tint = PrimaryEmerald)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.requestDeleteConfirmation(
                                                title = "Delete Record?",
                                                message = "Are you sure you want to permanently delete the transaction of ₹${loan.amount} for ${loan.contactName}?"
                                            ) {
                                                viewModel.deleteBorrowLend(loan)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete record", tint = RedExpense)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBorrowLendDialog(viewModel = viewModel, onDismiss = { showAddDialog = false })
    }

    if (editingLoan != null) {
        AddBorrowLendDialog(
            viewModel = viewModel,
            loanToEdit = editingLoan,
            onDismiss = { editingLoan = null }
        )
    }
}

@Composable
fun AddBorrowLendDialog(
    viewModel: FinanceViewModel,
    loanToEdit: BorrowLend? = null,
    onDismiss: () -> Unit
) {
    var contact by remember { mutableStateOf(loanToEdit?.contactName ?: "") }
    var amount by remember { mutableStateOf(loanToEdit?.amount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(loanToEdit?.type ?: "LENT") }
    var notes by remember { mutableStateOf(loanToEdit?.notes ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (loanToEdit != null) "Edit Loan / Borrow" else "Log Loan / Borrow",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedType = "LENT" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "LENT") GreenIncome else DarkSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lent")
                    }
                    Button(
                        onClick = { selectedType = "BORROWED" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "BORROWED") RedExpense else DarkSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Borrowed")
                    }
                }

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Name", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note / Reason", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = MutedText)
                    }
                    Button(
                        onClick = {
                            val amtVal = amount.toDoubleOrNull() ?: 0.0
                            if (contact.isNotBlank() && amtVal > 0) {
                                val loan = if (loanToEdit != null) {
                                    loanToEdit.copy(
                                        contactName = contact,
                                        amount = amtVal,
                                        type = selectedType,
                                        notes = notes
                                    )
                                } else {
                                    BorrowLend(
                                        contactName = contact,
                                        amount = amtVal,
                                        type = selectedType,
                                        dueDate = System.currentTimeMillis() + 7L * 24 * 3600 * 1000, // 1 week due date
                                        notes = notes
                                    )
                                }
                                viewModel.addBorrowLend(loan)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// WISHLIST SCREEN
@Composable
fun WishlistCard(
    item: Wishlist,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTogglePurchased: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPurchased) DarkSurfaceVariant.copy(alpha = 0.5f) else DarkSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isPurchased) DarkSurfaceVariant.copy(alpha = 0.15f)
            else DarkSurfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Badge
                val priorityColor = when (item.priority) {
                    "HIGH" -> RedExpense
                    "MEDIUM" -> AccentGold
                    else -> BlueCard
                }
                val priorityEmoji = when (item.priority) {
                    "HIGH" -> "🔥"
                    "MEDIUM" -> "⭐️"
                    else -> "❄️"
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(priorityColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = priorityEmoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Priority column (elements stacked vertically to prevent squishing with long names)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isPurchased) MutedText else SmoothWhite,
                        textDecoration = if (item.isPurchased) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (item.isPurchased) PrimaryEmerald.copy(alpha = 0.15f)
                                else priorityColor.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (item.isPurchased) "Bought" else "${item.priority} PRIORITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.isPurchased) PrimaryLightEmerald else priorityColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Price
                Text(
                    text = "₹${item.price.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isPurchased) MutedText else SmoothWhite
                )
            }

            // Notes below the top row, perfectly aligned/indented under the product name
            if (item.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.width(56.dp)) // Aligns under the name (badge 44dp + spacer 12dp)
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button left side
                Button(
                    onClick = onTogglePurchased,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.isPurchased) DarkSurfaceVariant else PrimaryEmerald
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = if (item.isPurchased) Icons.Default.CheckCircle else Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = if (item.isPurchased) MutedText else SmoothWhite,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.isPurchased) "Bought" else "Mark Bought",
                        fontSize = 12.sp,
                        color = if (item.isPurchased) MutedText else SmoothWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Edit and Delete right side
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Item",
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Item",
                            tint = RedExpense,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WishlistScreen(viewModel: FinanceViewModel) {
    val list by viewModel.wishlist.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Wishlist?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Product Wishlist",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SmoothWhite,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Item", fontSize = 13.sp, color = SmoothWhite, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Your wishlist is empty. Plan upcoming purchases!", color = MutedText)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(list) { item ->
                    WishlistCard(
                        item = item,
                        onEditClick = { editingItem = item },
                        onDeleteClick = {
                            viewModel.requestDeleteConfirmation(
                                title = "Remove from Wishlist?",
                                message = "Are you sure you want to remove '${item.name}' from your wishlist?"
                            ) {
                                viewModel.deleteWishlistItem(item)
                            }
                        },
                        onTogglePurchased = { viewModel.toggleWishlistItemPurchased(item) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddWishlistDialog(viewModel = viewModel, onDismiss = { showAddDialog = false })
    }

    if (editingItem != null) {
        AddWishlistDialog(
            viewModel = viewModel,
            itemToEdit = editingItem,
            onDismiss = { editingItem = null }
        )
    }
}

@Composable
fun AddWishlistDialog(
    viewModel: FinanceViewModel,
    itemToEdit: Wishlist? = null,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var price by remember { mutableStateOf(itemToEdit?.price?.toString() ?: "") }
    var selectedPriority by remember { mutableStateOf(itemToEdit?.priority ?: "HIGH") }
    var notes by remember { mutableStateOf(itemToEdit?.notes ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (itemToEdit != null) "Edit Wishlist Item" else "Add to Wishlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (₹)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                Column {
                    Text("Select Priority", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val pList = listOf("HIGH", "MEDIUM", "LOW")
                        pList.forEach { priority ->
                            val isSelected = selectedPriority == priority
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryEmerald else DarkSurfaceVariant)
                                    .clickable { selectedPriority = priority }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(priority, color = SmoothWhite, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = MutedText)
                    }
                    Button(
                        onClick = {
                            val prVal = price.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && prVal > 0) {
                                val item = if (itemToEdit != null) {
                                    itemToEdit.copy(
                                        name = name,
                                        price = prVal,
                                        priority = selectedPriority,
                                        notes = notes
                                    )
                                } else {
                                    Wishlist(
                                        name = name,
                                        price = prVal,
                                        priority = selectedPriority,
                                        targetDate = System.currentTimeMillis() + 90L * 24 * 3600 * 1000,
                                        notes = notes
                                    )
                                }
                                viewModel.addWishlistItem(item)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// HIGH-FIDELITY ANALYTICS & NATIVE PIE CHART SCREEN
@Composable
fun AnalyticsScreen(viewModel: FinanceViewModel) {
    val categoryData by viewModel.categorySpendingData.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val dailyAvg by viewModel.averageDailySpending.collectAsStateWithLifecycle()
    val highestExpense by viewModel.highestExpenseOfMonth.collectAsStateWithLifecycle()

    val totalSpending = categoryData.values.sum()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Spending Analytics & Insights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SmoothWhite
            )
        }

        // Native Canvas-based Pie Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Monthly Spending Share", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SmoothWhite)
                    Spacer(modifier = Modifier.height(20.dp))

                    if (totalSpending == 0.0) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No expenses yet", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                    } else {
                        // Drawing premium canvas arcs
                        Box(
                            modifier = Modifier.size(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(160.dp)) {
                                var currentStartAngle = -90f
                                val colors = listOf(
                                    Color(0xFF5CE592), Color(0xFFFFD700), Color(0xFF5C9EE5), 
                                    Color(0xFF00E5FF), Color(0xFFE55CE5), Color(0xFFFF9100), Color(0xFF90A49A)
                                )
                                
                                categoryData.values.forEachIndexed { idx, valAmount ->
                                    val sweepAngle = ((valAmount / totalSpending) * 360f).toFloat()
                                    drawArc(
                                        color = colors[idx % colors.size],
                                        startAngle = currentStartAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = 30f, cap = StrokeCap.Round),
                                        size = Size(size.width, size.height)
                                    )
                                    currentStartAngle += sweepAngle
                                }
                            }
                            
                            // Center absolute sum text
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                Text("₹${totalSpending.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SmoothWhite)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Legend and share breakdown
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val colors = listOf(
                            Color(0xFF5CE592), Color(0xFFFFD700), Color(0xFF5C9EE5), 
                            Color(0xFF00E5FF), Color(0xFFE55CE5), Color(0xFFFF9100), Color(0xFF90A49A)
                        )
                        categoryData.entries.forEachIndexed { idx, entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(colors[idx % colors.size])
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${viewModel.getIconForCategory(entry.key)} ${entry.key}", style = MaterialTheme.typography.bodyMedium, color = SmoothWhite)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("₹${entry.value.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SmoothWhite)
                                    Text(
                                        text = "${((entry.value / totalSpending) * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary Insights List
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Performance & Speed Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SmoothWhite)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Average Daily Spend", color = MutedText, style = MaterialTheme.typography.bodyMedium)
                        Text("₹${dailyAvg.toInt()}/day", fontWeight = FontWeight.Bold, color = SmoothWhite, style = MaterialTheme.typography.bodyMedium)
                    }

                    highestExpense?.let { h ->
                        HorizontalDivider(color = DarkSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Peak Spending Spike", color = MutedText, style = MaterialTheme.typography.bodyMedium)
                            Text("₹${h.amount} (${h.merchant})", fontWeight = FontWeight.Bold, color = RedExpense, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recorded Transactions", color = MutedText, style = MaterialTheme.typography.bodyMedium)
                        Text("${transactions.size} entries", fontWeight = FontWeight.Bold, color = SmoothWhite, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(viewModel: FinanceViewModel) {
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf("EXPENSE") }
    
    // Dialog States
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddSubcategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CustomCategory?>(null) }
    var editingSubcategory by remember { mutableStateOf<CustomCategory?>(null) }
    var subcategoryActionsTarget by remember { mutableStateOf<CustomCategory?>(null) }

    val categoriesList = remember(selectedTab, customCategories) {
        val mainCats = customCategories.filter { it.type == selectedTab && it.parentCategory == null }
        if (mainCats.isNotEmpty()) {
            mainCats.map { cat ->
                val subs = customCategories.filter { it.parentCategory == cat.name }.map { it.name }
                CategoryDef(
                    name = cat.name,
                    icon = cat.icon,
                    subcategories = subs,
                    isCustom = true
                )
            }
        } else {
            val standard = if (selectedTab == "EXPENSE") CategoryData.expenseCategories else CategoryData.incomeCategories
            standard.map { stdCat ->
                stdCat.copy(isCustom = true)
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("New Subcategory", color = DarkBackground) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null, tint = DarkBackground) },
                    onClick = {
                        showAddSubcategoryDialog = true
                    },
                    containerColor = AccentGold,
                    modifier = Modifier.testTag("add_subcategory_fab")
                )
                ExtendedFloatingActionButton(
                    text = { Text("New Category", color = SmoothWhite) },
                    icon = { Icon(Icons.Default.Category, contentDescription = null, tint = SmoothWhite) },
                    onClick = { showAddCategoryDialog = true },
                    containerColor = PrimaryEmerald,
                    modifier = Modifier.testTag("add_category_fab")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SettingsSuggest, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Category Customization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SmoothWhite
                        )
                        Text(
                            text = "Add and manage custom categories & subcategories to match your personal cash flow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                }
            }

            // Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .padding(4.dp)
            ) {
                listOf("EXPENSE" to "Expense Categories", "INCOME" to "Income Categories").forEach { (tabType, tabTitle) ->
                    val isSelected = selectedTab == tabType
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryEmerald else Color.Transparent)
                            .clickable { selectedTab = tabType }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            color = if (isSelected) SmoothWhite else MutedText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // List of Categories and Subcategories
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (categoriesList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No categories found.", color = MutedText)
                        }
                    }
                }

                items(categoriesList.size) { index ->
                    val cat = categoriesList[index]
                    val isCustomCat = cat.isCustom

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, if (isCustomCat) PrimaryEmerald.copy(alpha = 0.4f) else DarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Category Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(cat.icon, fontSize = 20.sp)
                                    }
                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SmoothWhite
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val customEnt = customCategories.firstOrNull { it.name == cat.name && it.parentCategory == null && it.type == selectedTab }
                                    val categoryToUse = customEnt ?: CustomCategory(name = cat.name, icon = cat.icon, type = selectedTab, parentCategory = null)

                                    IconButton(
                                        onClick = { editingCategory = categoryToUse }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Category", tint = PrimaryEmerald, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.requestDeleteConfirmation(
                                                title = "Delete Category?",
                                                message = "Are you sure you want to delete category '${cat.name}'? This will also delete any custom subcategories inside it."
                                            ) {
                                                viewModel.deleteCustomCategory(categoryToUse)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = RedExpense, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f))

                            // Subcategories list
                            Text("Subcategories", style = MaterialTheme.typography.labelSmall, color = MutedText)

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                cat.subcategories.forEach { sub ->
                                    val subEnt = customCategories.firstOrNull { it.name == sub && it.parentCategory == cat.name }
                                    val isCustomSub = subEnt != null
                                    val subcategoryToUse = subEnt ?: CustomCategory(name = sub, icon = "", type = selectedTab, parentCategory = cat.name)

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isCustomSub) AccentGold.copy(alpha = 0.12f) else DarkSurfaceVariant)
                                            .border(1.dp, if (isCustomSub) AccentGold.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(12.dp))
                                            .clickable { subcategoryActionsTarget = subcategoryToUse }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = sub,
                                                color = SmoothWhite,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = PrimaryEmerald.copy(alpha = 0.7f),
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (showAddCategoryDialog) {
        CategoryDialog(
            viewModel = viewModel,
            selectedTab = selectedTab,
            onDismiss = { showAddCategoryDialog = false }
        )
    }

    if (editingCategory != null) {
        CategoryDialog(
            viewModel = viewModel,
            categoryToEdit = editingCategory,
            selectedTab = selectedTab,
            onDismiss = { editingCategory = null }
        )
    }

    if (showAddSubcategoryDialog) {
        SubcategoryDialog(
            viewModel = viewModel,
            categoriesList = categoriesList,
            selectedTab = selectedTab,
            onDismiss = { showAddSubcategoryDialog = false }
        )
    }

    if (editingSubcategory != null) {
        SubcategoryDialog(
            viewModel = viewModel,
            subcategoryToEdit = editingSubcategory,
            categoriesList = categoriesList,
            selectedTab = selectedTab,
            onDismiss = { editingSubcategory = null }
        )
    }

    if (subcategoryActionsTarget != null) {
        val target = subcategoryActionsTarget!!
        Dialog(onDismissRequest = { subcategoryActionsTarget = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Manage Subcategory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmoothWhite
                    )
                    Text(
                        text = "What would you like to do with '${target.name}'?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                editingSubcategory = target
                                subcategoryActionsTarget = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = SmoothWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rename Subcategory", color = SmoothWhite, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                subcategoryActionsTarget = null
                                viewModel.requestDeleteConfirmation(
                                    title = "Delete Subcategory?",
                                    message = "Are you sure you want to delete custom subcategory '${target.name}'?"
                                ) {
                                    viewModel.deleteCustomCategory(target)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedExpense),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = SmoothWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Subcategory", color = SmoothWhite, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { subcategoryActionsTarget = null },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel", color = SmoothWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDialog(
    viewModel: FinanceViewModel,
    categoryToEdit: CustomCategory? = null,
    selectedTab: String,
    onDismiss: () -> Unit
) {
    var name by remember(categoryToEdit) { mutableStateOf(categoryToEdit?.name ?: "") }
    var icon by remember(categoryToEdit) { mutableStateOf(categoryToEdit?.icon ?: "🍔") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (categoryToEdit == null) "Add Custom Category" else "Edit Custom Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name", color = MutedText) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose Icon / Emoji", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val emojis = listOf("🍔", "🛵", "🏠", "💻", "📚", "🎮", "✈️", "🏥", "🛍️", "📈", "🛡️", "🐾", "🎁", "🌀", "💵", "🚀", "🏢", "🔄", "💰", "🍽️", "🍿", "👗", "💅", "💆", "💇", "🏋️", "🏀", "🎤", "🚗", "🚲", "🔌", "🧴", "🧸")
                        emojis.forEach { emo ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (icon == emo) PrimaryEmerald.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { icon = emo }
                                    .padding(8.dp)
                            ) {
                                Text(emo, fontSize = 20.sp)
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = icon,
                    onValueChange = { if (it.length <= 4) icon = it },
                    label = { Text("Or Type Custom Emoji", color = MutedText) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = SmoothWhite)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                if (categoryToEdit != null) {
                                    viewModel.updateCustomCategory(
                                        oldCategory = categoryToEdit,
                                        newCategory = categoryToEdit.copy(
                                            name = name.trim(),
                                            icon = icon
                                        )
                                    )
                                } else {
                                    viewModel.addCustomCategory(
                                        name = name.trim(),
                                        icon = icon,
                                        type = selectedTab,
                                        parentCategory = null
                                    )
                                }
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", color = SmoothWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SubcategoryDialog(
    viewModel: FinanceViewModel,
    subcategoryToEdit: CustomCategory? = null,
    categoriesList: List<CategoryDef>,
    selectedTab: String,
    onDismiss: () -> Unit
) {
    var name by remember(subcategoryToEdit) { mutableStateOf(subcategoryToEdit?.name ?: "") }
    var selectedParentCategory by remember(subcategoryToEdit) { mutableStateOf(subcategoryToEdit?.parentCategory ?: (categoriesList.firstOrNull()?.name ?: "")) }
    var showParentDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (subcategoryToEdit == null) "Add Custom Subcategory" else "Edit Custom Subcategory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedParentCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Parent Category", color = MutedText) },
                        trailingIcon = {
                            IconButton(onClick = { showParentDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SmoothWhite)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = showParentDropdown,
                        onDismissRequest = { showParentDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        categoriesList.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.icon} ${cat.name}", color = SmoothWhite) },
                                onClick = {
                                    selectedParentCategory = cat.name
                                    showParentDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subcategory Name", color = MutedText) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = SmoothWhite)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && selectedParentCategory.isNotBlank()) {
                                if (subcategoryToEdit != null) {
                                    viewModel.updateCustomCategory(
                                        oldCategory = subcategoryToEdit,
                                        newCategory = subcategoryToEdit.copy(
                                            name = name.trim(),
                                            parentCategory = selectedParentCategory
                                        )
                                    )
                                } else {
                                    viewModel.addCustomCategory(
                                        name = name.trim(),
                                        icon = "",
                                        type = selectedTab,
                                        parentCategory = selectedParentCategory
                                    )
                                }
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", color = SmoothWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// CREDIT CARDS MANAGEMENT SCREEN
// ==========================================

data class CardCycle(
    val startMillis: Long,
    val endMillis: Long,
    val dueMillis: Long
)

fun getCardCycleDates(billingDay: Int, dueDay: Int): CardCycle {
    val today = Calendar.getInstance()
    val billingDate = Calendar.getInstance()
    val startDate = Calendar.getInstance()
    val dueDate = Calendar.getInstance()

    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    val todayMonth = today.get(Calendar.MONTH)
    val todayYear = today.get(Calendar.YEAR)

    // Determine current billing date (the end of the current billing cycle)
    if (todayDay <= billingDay) {
        // Billing date is this month
        billingDate.set(todayYear, todayMonth, billingDay, 23, 59, 59)
        // Start date is last month's billingDay + 1
        startDate.set(todayYear, todayMonth, billingDay, 0, 0, 0)
        startDate.add(Calendar.MONTH, -1)
        startDate.add(Calendar.DAY_OF_MONTH, 1)
    } else {
        // Billing date is next month
        billingDate.set(todayYear, todayMonth, billingDay, 23, 59, 59)
        billingDate.add(Calendar.MONTH, 1)
        // Start date is this month's billingDay + 1
        startDate.set(todayYear, todayMonth, billingDay, 0, 0, 0)
        startDate.add(Calendar.DAY_OF_MONTH, 1)
    }

    // Determine due date for this billing statement
    dueDate.timeInMillis = billingDate.timeInMillis
    dueDate.set(Calendar.HOUR_OF_DAY, 23)
    dueDate.set(Calendar.MINUTE, 59)
    dueDate.set(Calendar.SECOND, 59)
    
    val billMonth = billingDate.get(Calendar.MONTH)
    val billYear = billingDate.get(Calendar.YEAR)
    if (dueDay <= billingDay) {
        dueDate.set(billYear, billMonth, dueDay)
        dueDate.add(Calendar.MONTH, 1)
    } else {
        dueDate.set(billYear, billMonth, dueDay)
    }

    return CardCycle(
        startMillis = startDate.timeInMillis,
        endMillis = billingDate.timeInMillis,
        dueMillis = dueDate.timeInMillis
    )
}

@Composable
fun CreditCardsScreen(viewModel: FinanceViewModel) {
    val creditCards by viewModel.creditCards.collectAsStateWithLifecycle()
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()

    var showAddCardDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<CreditCard?>(null) }
    var selectedCardId by remember { mutableStateOf<Long?>(null) }

    val selectedCard = remember(creditCards, selectedCardId) {
        creditCards.find { it.id == selectedCardId } ?: creditCards.firstOrNull()
    }

    LaunchedEffect(selectedCard) {
        if (selectedCard != null && selectedCardId != selectedCard.id) {
            selectedCardId = selectedCard.id
        }
    }

    // Modal state for direct transactions adding
    var showDirectAddTxDialog by remember { mutableStateOf(false) }
    var showDirectPaymentDialog by remember { mutableStateOf(false) }

    // Dialog properties for credit card payment logging
    var paymentAmount by remember { mutableStateOf("") }
    var paymentSource by remember { mutableStateOf("Bank") }

    val cardTransactions = remember(allTransactions, selectedCard) {
        if (selectedCard != null) {
            allTransactions.filter { it.creditCardId == selectedCard.id }
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Credit Cards",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SmoothWhite
                        )
                        Text(
                            text = "Track billing cycles, due dates & card limits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                    Button(
                        onClick = {
                            cardToEdit = null
                            showAddCardDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Card", tint = SmoothWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Card", color = SmoothWhite, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (creditCards.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceVariant.copy(alpha = 0.4f))
                        .border(1.dp, DarkSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PrimaryEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = PrimaryEmerald,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "No Credit Cards Added",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SmoothWhite
                        )
                        Text(
                            text = "Add your credit cards here to track statement amounts, payment due dates, and cycle-wise transactions automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = {
                                cardToEdit = null
                                showAddCardDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add Your First Card", color = SmoothWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Horizontal list of cards
                Column {
                    Text(
                        text = "My Cards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmoothWhite,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        creditCards.forEachIndexed { idx, card ->
                            val isSelected = selectedCard?.id == card.id
                            val outstanding = allTransactions.filter { it.creditCardId == card.id && it.type == "EXPENSE" }.sumOf { it.amount } - 
                                            allTransactions.filter { it.creditCardId == card.id && it.type == "INCOME" }.sumOf { it.amount }
                            
                            val grad = remember(idx) {
                                val gradients = listOf(
                                    Brush.linearGradient(colors = listOf(Color(0xFF2E0854), Color(0xFF6B11A6))), // Purple
                                    Brush.linearGradient(colors = listOf(Color(0xFF0F3057), Color(0xFF00587A))), // Teal
                                    Brush.linearGradient(colors = listOf(Color(0xFF4C0E1F), Color(0xFF901C3E))), // Burgundy
                                    Brush.linearGradient(colors = listOf(Color(0xFF0F2C1D), Color(0xFF1E5C3F)))  // Forest Green
                                )
                                gradients[idx % gradients.size]
                            }

                            Box(
                                modifier = Modifier
                                    .width(280.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(grad)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) AccentGold else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedCardId = card.id }
                                    .padding(20.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Card Name & Brand
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column {
                                            Text(
                                                text = card.cardName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (card.lastFourDigits.isNotBlank()) {
                                                Text(
                                                    text = "•••• ${card.lastFourDigits}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Balance info
                                    Column {
                                        Text(
                                            text = "Outstanding Balance",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "₹${String.format(Locale.getDefault(), "%,.2f", outstanding)}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    // Limit progress bar
                                    val limitUsedPercent = if (card.cardLimit > 0) (outstanding / card.cardLimit).coerceIn(0.0, 1.0) else 0.0
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        LinearProgressIndicator(
                                            progress = { limitUsedPercent.toFloat() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = if (limitUsedPercent > 0.9) Color(0xFFE53935) else if (limitUsedPercent > 0.7) Color(0xFFFFB300) else Color(0xFF81C784),
                                            trackColor = Color.White.copy(alpha = 0.2f),
                                            strokeCap = StrokeCap.Round
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Limit Used: ${(limitUsedPercent * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = "₹${String.format(Locale.getDefault(), "%,.0f", card.cardLimit)} Limit",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Card Details & Management Tab
                selectedCard?.let { card ->
                    val cycle = remember(card) { getCardCycleDates(card.billingDay, card.dueDay) }
                    val cycleTransactions = remember(cardTransactions, cycle) {
                        cardTransactions.filter { it.type == "EXPENSE" && it.date in cycle.startMillis..cycle.endMillis }
                    }
                    val cycleBill = remember(cycleTransactions) { cycleTransactions.sumOf { it.amount } }

                    val nextCycleTransactions = remember(cardTransactions, cycle) {
                        cardTransactions.filter { it.type == "EXPENSE" && it.date > cycle.endMillis }
                    }
                    val unbilledSum = remember(nextCycleTransactions) { nextCycleTransactions.sumOf { it.amount } }

                    val paymentTransactions = remember(cardTransactions) {
                        cardTransactions.filter { it.type == "INCOME" }
                    }

                    // Card Info Header Cards
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card Actions (Edit, Delete, Add Tx, Pay Bill)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { showDirectAddTxDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryEmerald,
                                    contentColor = SmoothWhite
                                ),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = SmoothWhite, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Spend", color = SmoothWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    paymentAmount = String.format(Locale.getDefault(), "%.2f", cycleBill)
                                    showDirectPaymentDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkSurfaceVariant,
                                    contentColor = SmoothWhite
                                ),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, SecondarySage.copy(alpha = 0.2f)),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Record Pay", color = SmoothWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = {
                                    cardToEdit = card
                                    showAddCardDialog = true
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, SecondarySage.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Card", tint = AccentGold, modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = {
                                    viewModel.requestDeleteConfirmation(
                                        title = "Delete Credit Card",
                                        message = "Are you sure you want to delete this credit card? This will unlink transactions associated with it but won't delete the transaction entries.",
                                        onConfirm = { viewModel.deleteCreditCard(card) }
                                    )
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(RedExpense.copy(alpha = 0.15f))
                                    .border(1.dp, RedExpense.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Card", tint = RedExpense, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Cycle info block
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Current Statement Cycle",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val cycleStartStr = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(cycle.startMillis))
                                    val cycleEndStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(cycle.endMillis))
                                    Text(
                                        text = "$cycleStartStr - $cycleEndStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Current Cycle Bill", style = MaterialTheme.typography.bodyMedium, color = SmoothWhite, fontWeight = FontWeight.Bold)
                                        Text("Due on ${SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(cycle.dueMillis))}", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    }
                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%,.2f", cycleBill)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cycleBill > 0) RedExpense else SmoothWhite
                                    )
                                }

                                HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Unbilled (Next Cycle)", style = MaterialTheme.typography.bodyMedium, color = SmoothWhite)
                                        Text("These fall after the current billing date", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    }
                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%,.2f", unbilledSum)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MutedText
                                    )
                                }
                            }
                        }

                        // Transaction Lists for Selected Card grouped by Cycle
                        var selectedTabIdx by remember { mutableIntStateOf(0) }
                        val tabs = listOf("Current Bill", "Unbilled/Next", "Payments History")

                        TabRow(
                            selectedTabIndex = selectedTabIdx,
                            containerColor = DarkBackground,
                            contentColor = SmoothWhite,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIdx]),
                                    color = PrimaryEmerald
                                )
                            },
                            divider = { HorizontalDivider(color = DarkSurfaceVariant) }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIdx == index,
                                    onClick = { selectedTabIdx = index },
                                    text = {
                                        Text(
                                            text = title,
                                            fontWeight = if (selectedTabIdx == index) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    }
                                )
                            }
                        }

                        // Display selected tab list
                        val listToShow = when (selectedTabIdx) {
                            0 -> cycleTransactions
                            1 -> nextCycleTransactions
                            else -> paymentTransactions
                        }

                        if (listToShow.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions in this section",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listToShow.sortedByDescending { it.date }.forEach { tx ->
                                    val icon = when (tx.category.uppercase()) {
                                        "FOOD", "DINING" -> "🍔"
                                        "TRANSPORT", "FUEL", "CAB", "METRO" -> "🛵"
                                        "BILLS", "UTILITIES", "RECHARGE" -> "⚡"
                                        "SHOPPING" -> "🛍️"
                                        "ENTERTAINMENT", "MOVIES" -> "🍿"
                                        "HEALTH", "MEDICAL" -> "🏥"
                                        else -> "💸"
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(DarkSurfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(icon, fontSize = 18.sp)
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (tx.merchant.isNotBlank()) tx.merchant else tx.category,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SmoothWhite,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(tx.date)),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MutedText
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${if (tx.type == "INCOME") "+" else "-"} ₹${String.format(Locale.getDefault(), "%,.1f", tx.amount)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tx.type == "INCOME") GreenIncome else RedExpense
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Card Dialog
    if (showAddCardDialog) {
        AddCreditCardDialog(
            viewModel = viewModel,
            cardToEdit = cardToEdit,
            onDismiss = { showAddCardDialog = false }
        )
    }

    // Direct transaction entry preselected with Credit Card
    if (showDirectAddTxDialog && selectedCard != null) {
        val editingTransaction = Transaction(
            date = System.currentTimeMillis(),
            amount = 0.0,
            category = "Shopping",
            subcategory = "General",
            paymentMethod = "Credit Card",
            merchant = "",
            notes = "",
            tagsString = "",
            type = "EXPENSE",
            assetId = null,
            creditCardId = selectedCard.id
        )
        AddTransactionDialog(
            viewModel = viewModel,
            transactionToEdit = editingTransaction,
            onDismiss = { showDirectAddTxDialog = false }
        )
    }

    // Quick Record Payment Dialog
    if (showDirectPaymentDialog && selectedCard != null) {
        Dialog(onDismissRequest = { showDirectPaymentDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Record Card Payment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmoothWhite
                    )
                    Text(
                        text = "This records a payback of the current statement balance on the card.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )

                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text("Payment Amount (₹)", color = MutedText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                    )

                    OutlinedTextField(
                        value = paymentSource,
                        onValueChange = { paymentSource = it },
                        label = { Text("Paid From (e.g. Bank Account, UPI)", color = MutedText) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDirectPaymentDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = MutedText)
                        }
                        Button(
                            onClick = {
                                val amt = paymentAmount.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    val paymentTx = Transaction(
                                        date = System.currentTimeMillis(),
                                        amount = amt,
                                        category = "Credit Card Payment",
                                        subcategory = "Payment",
                                        paymentMethod = "Bank",
                                        merchant = "Paid to ${selectedCard.cardName}",
                                        notes = "Logged credit card statement payback from $paymentSource",
                                        tagsString = "CardPayment",
                                        type = "INCOME", // reduces balance
                                        creditCardId = selectedCard.id
                                    )
                                    viewModel.addTransaction(paymentTx)
                                    showDirectPaymentDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Record", color = SmoothWhite)
                        }
                    }
                }
            }
        }
    }
}

// Dialog for Adding / Editing a Credit Card
@Composable
fun AddCreditCardDialog(
    viewModel: FinanceViewModel,
    cardToEdit: CreditCard? = null,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(cardToEdit?.cardName ?: "") }
    var limitStr by remember { mutableStateOf(cardToEdit?.cardLimit?.toString() ?: "") }
    var billingDayStr by remember { mutableStateOf(cardToEdit?.billingDay?.toString() ?: "15") }
    var dueDayStr by remember { mutableStateOf(cardToEdit?.dueDay?.toString() ?: "5") }
    var last4 by remember { mutableStateOf(cardToEdit?.lastFourDigits ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (cardToEdit != null) "Edit Credit Card" else "Add Credit Card",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Card Name (e.g. HDFC Regalia)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Card Limit (₹)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = billingDayStr,
                        onValueChange = { billingDayStr = it },
                        label = { Text("Billing Day (1-31)", color = MutedText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                    )
                    OutlinedTextField(
                        value = dueDayStr,
                        onValueChange = { dueDayStr = it },
                        label = { Text("Due Day (1-31)", color = MutedText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                    )
                }

                OutlinedTextField(
                    value = last4,
                    onValueChange = { if (it.length <= 4) last4 = it },
                    label = { Text("Last 4 Digits (Optional)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MutedText)
                    }
                    Button(
                        onClick = {
                            val limit = limitStr.toDoubleOrNull() ?: 10000.0
                            val bDay = billingDayStr.toIntOrNull()?.coerceIn(1, 31) ?: 15
                            val dDay = dueDayStr.toIntOrNull()?.coerceIn(1, 31) ?: 5
                            if (name.isNotBlank()) {
                                val card = if (cardToEdit != null) {
                                    cardToEdit.copy(
                                        cardName = name.trim(),
                                        cardLimit = limit,
                                        billingDay = bDay,
                                        dueDay = dDay,
                                        lastFourDigits = last4.trim()
                                    )
                                } else {
                                    CreditCard(
                                        cardName = name.trim(),
                                        cardLimit = limit,
                                        billingDay = bDay,
                                        dueDay = dDay,
                                        lastFourDigits = last4.trim()
                                    )
                                }
                                viewModel.addCreditCard(card)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", color = SmoothWhite)
                    }
                }
            }
        }
    }
}

// ==========================================
// SMS AUTO-DETECTION & CATEGORIZATION UI
// ==========================================

@Composable
fun SmsDetectionBannerCard(
    hasPermission: Boolean,
    pendingCount: Int,
    onRequestPermission: () -> Unit,
    onSimulateClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, if (pendingCount > 0) PrimaryEmerald.copy(alpha = 0.8f) else SecondarySage.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().testTag("sms_detection_banner_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (pendingCount > 0) PrimaryEmerald.copy(alpha = 0.2f) else AccentGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = if (pendingCount > 0) PrimaryEmerald else AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Bank SMS Auto-Detection",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SmoothWhite
                            )
                            if (pendingCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PrimaryEmerald)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$pendingCount NEW",
                                        color = SmoothWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (hasPermission) "Live tracking on-device bank alerts" else "Grant permission to auto-detect bank SMS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!hasPermission) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("enable_sms_permission_btn"),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enable SMS Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onSimulateClick,
                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = if (!hasPermission) Modifier.weight(1f) else Modifier.fillMaxWidth().testTag("simulate_sms_test_btn"),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simulate / Test Bank SMS", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendingSmsCard(
    pending: PendingSmsTransaction,
    categories: List<CategoryDef>,
    onConfirm: (category: String, subcategory: String) -> Unit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    var selectedCategory by remember(pending.id) {
        val initial = if (pending.suggestedCategory.isNotBlank()) pending.suggestedCategory else "Food"
        mutableStateOf(initial)
    }
    var selectedSubcategory by remember(pending.id, selectedCategory) {
        mutableStateOf("")
    }
    var showRawSms by remember { mutableStateOf(false) }

    val availableCategories = remember(categories, pending.type) {
        val typeFiltered = if (pending.type == "INCOME") CategoryData.incomeCategories else CategoryData.expenseCategories
        val merged = (categories + typeFiltered).distinctBy { it.name }
        merged
    }

    val currentCatDef = availableCategories.firstOrNull { it.name == selectedCategory }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.5.dp, PrimaryEmerald),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_sms_card_${pending.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header: Badge + Time + Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryEmerald.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(14.dp))
                            Text("Bank SMS Detected", color = PrimaryEmerald, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    val dateStr = remember(pending.date) {
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(pending.date))
                    }
                    Text(text = dateStr, color = MutedText, fontSize = 12.sp)
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp).testTag("dismiss_pending_sms_${pending.id}")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MutedText, modifier = Modifier.size(18.dp))
                }
            }

            // Amount & Merchant Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (pending.merchant.isNotBlank()) pending.merchant else "Bank Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmoothWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "via ${pending.paymentMethod}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentGold,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (pending.rawSender.isNotBlank()) {
                            Text(text = "•", color = MutedText)
                            Text(
                                text = pending.rawSender,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                        }
                    }
                }

                Text(
                    text = "${if (pending.type == "INCOME") "+" else "-"}₹${"%,.2f".format(pending.amount)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (pending.type == "INCOME") GreenIncome else RedExpense
                )
            }

            // Raw SMS text toggle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBackground)
                    .clickable { showRawSms = !showRawSms }
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showRawSms) "Hide SMS Message" else "View Original SMS Message",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (showRawSms) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (showRawSms) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pending.rawBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = SmoothWhite.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = DarkSurfaceVariant)

            // CATEGORY SELECTION FLOW
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assign Category:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmoothWhite
                    )
                    Text(
                        text = "Tap to select",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }

                // Main Category Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableCategories.forEach { cat ->
                        val isSelected = selectedCategory == cat.name
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = cat.name
                                selectedSubcategory = ""
                            },
                            label = {
                                Text(
                                    text = "${cat.icon} ${cat.name}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryEmerald,
                                selectedLabelColor = SmoothWhite,
                                containerColor = DarkSurfaceVariant,
                                labelColor = SmoothWhite
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) PrimaryEmerald else Color.Transparent
                            )
                        )
                    }
                }

                // Subcategory Chips (if present for chosen category)
                if (currentCatDef != null && currentCatDef.subcategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Subcategory (Optional):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentCatDef.subcategories.forEach { sub ->
                            val isSelected = selectedSubcategory == sub
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedSubcategory = if (isSelected) "" else sub
                                },
                                label = {
                                    Text(
                                        text = sub,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGold,
                                    selectedLabelColor = DarkBackground,
                                    containerColor = DarkBackground,
                                    labelColor = SmoothWhite
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) AccentGold else DarkSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onConfirm(selectedCategory, selectedSubcategory) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.4f).testTag("confirm_sms_transaction_${pending.id}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onEdit,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("edit_sms_transaction_${pending.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = SmoothWhite, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", color = SmoothWhite, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun SimulateSmsDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    var customSender by remember { mutableStateOf("VK-HDFCBK") }
    var customBody by remember { mutableStateOf("") }

    val presets = remember {
        listOf(
            Triple(
                "🍔 Swiggy Food Delivery",
                "VK-HDFCBK",
                "Alert: INR 450.00 debited on HDFC Bank Credit Card ending 4092 at SWIGGY on 24-AUG-26. Avl limit: INR 94,550.00."
            ),
            Triple(
                "🛍️ Amazon Shopping",
                "SBIINB",
                "Dear SBI User, your A/c ending 1234 has been debited by Rs. 1,499.00 on 24-Aug-2026 14:30 via UPI to AMAZON PAY INDIA. Ref 423985729182."
            ),
            Triple(
                "⛽ Bharat Petroleum Fuel",
                "AXISBK",
                "Txn of INR 2,200.00 spent on Axis Bank Credit Card ending 5678 at BHARAT PETROLEUM on 24-AUG-26."
            ),
            Triple(
                "☕ Starbucks Cafe",
                "PAYTM",
                "Paid Rs. 380.00 successfully to STARBUCKS COFFEE from your UPI A/c."
            ),
            Triple(
                "💵 Monthly Salary",
                "ICICIB",
                "Your ICICI Bank A/c xx8821 is credited with INR 75,000.00 on 24-AUG-2026 by SALARY AUGUST 2026. Available bal INR 1,12,400.00."
            ),
            Triple(
                "🎬 BookMyShow Movie",
                "KOTAKB",
                "Your Kotak Bank Card ending 4092 was charged INR 680.00 for purchase at BOOKMYSHOW on 24-Aug."
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentGold)
                        Text(
                            text = "Test Bank SMS Auto-Detect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SmoothWhite
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MutedText)
                    }
                }

                Text(
                    text = "Tap a real bank SMS preset or paste your own message to test instant on-device parsing & category prompt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )

                Text(
                    text = "Preset Bank SMS Samples:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { (title, sender, body) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DarkSurfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.simulateSmsReceived(sender, body)
                                    onDismiss()
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SmoothWhite
                                    )
                                    Icon(Icons.Default.Send, contentDescription = "Send", tint = PrimaryEmerald, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = DarkSurfaceVariant)

                Text(
                    text = "Or Test Custom SMS Text:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                OutlinedTextField(
                    value = customSender,
                    onValueChange = { customSender = it },
                    label = { Text("Sender ID (e.g. HDFCBK, SBIPAY)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                OutlinedTextField(
                    value = customBody,
                    onValueChange = { customBody = it },
                    label = { Text("SMS Message Body", color = MutedText) },
                    placeholder = { Text("e.g. Rs 500 debited for order at Zomato...", color = MutedText.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Button(
                    onClick = {
                        if (customBody.isNotBlank()) {
                            viewModel.simulateSmsReceived(customSender, customBody)
                            onDismiss()
                        }
                    },
                    enabled = customBody.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Parse & Detect SMS", fontWeight = FontWeight.Bold, color = SmoothWhite)
                }
            }
        }
    }
}
