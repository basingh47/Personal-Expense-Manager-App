package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import android.widget.Toast
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
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.data.*
import com.example.export.*
import com.example.security.AppLockManager
import com.example.security.AutoLockTimeout
import com.example.sms.SmsParser
import com.example.subscription.SubscriptionNotificationHelper
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
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Welcome Header - Elevated Hero Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "EXPENSE OVERVIEW",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Personal Expense Manager",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
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
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "Categorize Bank SMS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "${pendingSmsList.size} pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "Detected from your bank messages. Confirm to log directly or tap Edit to customize:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(pendingSmsList, key = { "pending_sms_${it.id}" }) { pending ->
                PendingSmsCard(
                    pending = pending,
                    categories = categoryDefs,
                    onConfirm = { cat, sub ->
                        viewModel.confirmPendingSms(pending, cat, sub, pending.paymentMethod, pending.creditCardId, pending.bankAccountId)
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
                            creditCardId = pending.creditCardId,
                            bankAccountId = pending.bankAccountId
                        )
                        viewModel.dismissPendingSms(pending)
                    }
                )
            }
        }

        // Toggle Credit Cards - Elevated M3 Card
        item {
            val excludeCreditCards by viewModel.excludeCreditCards.collectAsStateWithLifecycle()
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Cash-Only Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (excludeCreditCards) "Showing bank/cash + CC bill payments" else "Showing all transactions including active CC",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = excludeCreditCards,
                        onCheckedChange = { viewModel.setExcludeCreditCards(it) },
                        modifier = Modifier.testTag("toggle_credit_cards_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            }
        }

        // Metrics Grid (Today, Month, Income, Savings)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
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

        // Analytics Highlights Row - Elevated M3 Card
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoGraph,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Monthly Insights",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Average", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format(Locale.getDefault(), "%.0f", dailyAvg)}/day", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text("vs Last Month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Highest Single Expense", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(hExp.merchant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { onNavigateTo("subscriptions") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View All", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (upcomingReminders.isEmpty()) {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(18.dp),
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "No bills or reminders due in the next 30 days.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GreenIncome,
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { onNavigateTo("budgets") }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View Budgets", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(22.dp),
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { onNavigateTo("transactions") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View Transactions", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (transactions.isEmpty()) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No transactions logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
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
}

@Composable
fun MetricCard(
    title: String,
    amount: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            hoveredElevation = 4.dp
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
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

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (reminder.type) {
                                "SUBSCRIPTION" -> MaterialTheme.colorScheme.primaryContainer
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
                            "SUBSCRIPTION" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "LENT" -> GreenIncome
                            else -> RedExpense
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Due $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverdue) RedExpense else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Text(
                text = "₹${reminder.amount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
    val progressColor = if (percent >= 1.0) RedExpense else if (percent > 0.8) AccentGold else MaterialTheme.colorScheme.primary

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${getIcon(budget.category)} ${budget.category}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "₹${spent.toInt()} of ₹${budget.limitAmount.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { Math.min(percent.toFloat(), 1.0f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (percent >= 1.0) "Limit Exceeded!" else "${(percent * 100).toInt()}% spent",
                style = MaterialTheme.typography.labelSmall,
                color = if (percent >= 1.0) RedExpense else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "₹${remaining.toInt()} left",
                style = MaterialTheme.typography.labelSmall,
                color = if (remaining < 0) RedExpense else GreenIncome
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListItem(
    transaction: Transaction,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    getIcon: (String) -> String = { CategoryData.getIconForCategory(it) }
) {
    val formattedDate = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.date))
    val tags = remember(transaction.tagsString) { FinanceViewModel.parseTags(transaction.tagsString) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect?.invoke()
                    }
                },
                onLongClick = {
                    onToggleSelect?.invoke()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Checkbox (if in select mode), Category Icon, Info (Merchant/Category/Date) and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection Checkbox
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect?.invoke() },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                // Category Icon Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (transaction.type) {
                                "TRANSFER" -> Color(0xFFD0E4FF)
                                "REFUND" -> Color(0xFFCCE8E4)
                                else -> when (transaction.category.uppercase()) {
                                    "FOOD", "DINING", "FOOD & DINING" -> Color(0xFFFFDAD6)
                                    "TRANSPORT", "FUEL", "SCOOTER", "TRAVEL" -> Color(0xFFD6E3FF)
                                    "SUBSCRIPTION", "UTILITIES", "BILLS", "SUBSCRIPTIONS" -> Color(0xFFF0E0FF)
                                    "ENTERTAINMENT", "LEISURE" -> Color(0xFFE8DDFF)
                                    "HEALTH", "MEDICAL" -> Color(0xFFFCE3E3)
                                    "SHOPPING", "GROCERIES" -> Color(0xFFFFE0B2)
                                    "INCOME", "SALARY", "SAVINGS" -> Color(0xFFD1E8D9)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (transaction.type) {
                            "TRANSFER" -> "⇄"
                            "REFUND" -> "↩"
                            else -> getIcon(transaction.category)
                        },
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
                    text = when (transaction.type) {
                        "INCOME" -> "+₹${transaction.amount}"
                        "TRANSFER" -> "⇄ ₹${transaction.amount}"
                        "REFUND" -> "↩ +₹${transaction.amount}"
                        else -> "-₹${transaction.amount}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = when (transaction.type) {
                        "INCOME" -> GreenIncome
                        "TRANSFER" -> Color(0xFF64B5F6)
                        "REFUND" -> Color(0xFF26A69A)
                        else -> RedExpense
                    }
                )
            }

            // Tags row if available
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subdued separator
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Row: Payment Method Badge and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment Method & Type Badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    if (transaction.type == "TRANSFER") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1976D2).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Transfer",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF64B5F6),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (transaction.type == "REFUND") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00796B).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Refund / Reversal",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF26A69A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Edit and Delete Actions (Hidden during Selection Mode to avoid mis-taps)
                if (!isSelectionMode) {
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
}

// TRANSACTIONS SCREEN WITH SMART SEARCH, BULK EDITING & CUSTOM TAGS ENGINE
@Composable
fun TransactionsScreen(viewModel: FinanceViewModel) {
    val filteredTx by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    
    val selectedCat by viewModel.selectedFilterCategory.collectAsStateWithLifecycle()
    val selectedMethod by viewModel.selectedFilterMethod.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedFilterMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedFilterDate.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedFilterType.collectAsStateWithLifecycle()
    val selectedTagFilter by viewModel.selectedFilterTag.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    // Multi-Select States
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedTransactionIds.collectAsStateWithLifecycle()
    val allUniqueTags by viewModel.allUniqueTags.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showMonthDropdown by remember { mutableStateOf(false) }
    var showTagProjectsSheet by remember { mutableStateOf(false) }
    var showBulkEditDialog by remember { mutableStateOf(false) }
    var bulkEditInitialTab by remember { mutableStateOf(0) } // 0: Tag, 1: Category, 2: Account
    var showStatementExportDialog by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header: Multi-Select Mode Bar OR Standard Title & Controls
            if (isSelectionMode) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Selection", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${selectedIds.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    if (selectedIds.size == filteredTx.size) {
                                        viewModel.clearSelection()
                                    } else {
                                        viewModel.selectAllTransactions(filteredTx.map { it.id })
                                    }
                                }
                            ) {
                                Text(
                                    text = if (selectedIds.size == filteredTx.size && filteredTx.isNotEmpty()) "Deselect All" else "Select All",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Responsive Header: Title & Counter + Prominent Add Transaction Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SmoothWhite
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceVariant
                        ) {
                            Text(
                                text = "${filteredTx.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Add Transaction Button
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("add_transaction_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Quick Action Bar (Projects, Statement Export, Multi-Select) - Responsively scrollable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tag Projects Button
                    FilledTonalButton(
                        onClick = { showTagProjectsSheet = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("🏷️ Projects", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Statement Export Button
                    FilledTonalButton(
                        onClick = { showStatementExportDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Statement", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Select Mode Toggle Button
                    FilledTonalButton(
                        onClick = { viewModel.setSelectionMode(true) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Select", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Live Smart Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { viewModel.updateSearchText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                placeholder = { Text("Search merchants, #tags, notes, ₹amount...", fontSize = 13.sp, color = MutedText) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchText.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateSearchText("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryEmerald,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Multi-Category, Date, Month, Tag, Type and Payment Method Filter scroll
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
                val isAnyFilterActive = selectedCat != null || selectedMethod != null || selectedMonth != null || selectedDate != null || selectedTypeFilter != null || selectedTagFilter != null || searchText.isNotBlank()
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

                // Tag Filter Chip (Active / Picker)
                if (selectedTagFilter != null) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.setFilterTag(null) },
                        label = { Text("🏷️ $selectedTagFilter", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                } else if (allUniqueTags.isNotEmpty()) {
                    allUniqueTags.take(4).forEach { tag ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.setFilterTag(tag) },
                            label = { Text(tag, fontSize = 11.sp) },
                            colors = chipColors,
                            border = chipBorder
                        )
                    }
                }

                // Transaction Type Filters
                val types = listOf(
                    "EXPENSE" to "Expenses",
                    "INCOME" to "Income",
                    "TRANSFER" to "Transfers",
                    "REFUND" to "Refunds"
                )
                types.forEach { (typeKey, typeLabel) ->
                    FilterChip(
                        selected = selectedTypeFilter == typeKey,
                        onClick = { viewModel.setFilterType(if (selectedTypeFilter == typeKey) null else typeKey) },
                        label = { Text(typeLabel, fontSize = 11.sp) },
                        colors = chipColors,
                        border = chipBorder
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

            Spacer(modifier = Modifier.height(10.dp))

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
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = if (isSelectionMode && selectedIds.isNotEmpty()) 80.dp else 16.dp
                    )
                ) {
                    items(filteredTx, key = { it.id }) { tx ->
                        TransactionListItem(
                            transaction = tx,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedIds.contains(tx.id),
                            onToggleSelect = { viewModel.toggleTransactionSelection(tx.id) },
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

        // Floating Bulk Action Bar when items are selected
        if (isSelectionMode && selectedIds.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tag button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            bulkEditInitialTab = 0
                            showBulkEditDialog = true
                        }
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                bulkEditInitialTab = 0
                                showBulkEditDialog = true
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Label, contentDescription = "Manage Tags", modifier = Modifier.size(20.dp))
                        }
                        Text("Tags", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Category button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            bulkEditInitialTab = 1
                            showBulkEditDialog = true
                        }
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                bulkEditInitialTab = 1
                                showBulkEditDialog = true
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Category, contentDescription = "Change Category", modifier = Modifier.size(20.dp))
                        }
                        Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Bank Account / Payment Method button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            bulkEditInitialTab = 2
                            showBulkEditDialog = true
                        }
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                bulkEditInitialTab = 2
                                showBulkEditDialog = true
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = "Change Account", modifier = Modifier.size(20.dp))
                        }
                        Text("Account", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Delete button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.bulkDeleteSelected() }
                    ) {
                        FilledTonalIconButton(
                            onClick = { viewModel.bulkDeleteSelected() },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", modifier = Modifier.size(20.dp))
                        }
                        Text("Delete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
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

    if (showTagProjectsSheet) {
        TagProjectsDialog(
            viewModel = viewModel,
            onDismiss = { showTagProjectsSheet = false },
            onSelectTag = { tag ->
                viewModel.setFilterTag(tag)
                showTagProjectsSheet = false
            }
        )
    }

    if (showBulkEditDialog) {
        BulkEditDialog(
            viewModel = viewModel,
            selectedIds = selectedIds,
            initialTab = bulkEditInitialTab,
            onDismiss = { showBulkEditDialog = false }
        )
    }

    if (showStatementExportDialog) {
        val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
        val creditCards by viewModel.creditCards.collectAsStateWithLifecycle()
        val allTx by viewModel.transactions.collectAsStateWithLifecycle()
        PdfStatementExportDialog(
            viewModel = viewModel,
            allTransactions = allTx,
            bankAccounts = bankAccounts,
            creditCards = creditCards,
            onDismiss = { showStatementExportDialog = false }
        )
    }
}

// TAG PROJECTS & CROSS-CATEGORY COST SUMMARY DIALOG
@Composable
fun TagProjectsDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onSelectTag: (String) -> Unit
) {
    val tagSummaries by viewModel.tagProjectSummaries.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🏷️ Project & Tag Costs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Track multi-category project budgets & expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (tagSummaries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Tagged Projects Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tag any transaction with tags like #Vacation2026, #Wedding, #TaxDeductible, or #HomeRenovation to automatically track total costs across food, travel, shopping, and bills in one view!",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tagSummaries) { summary ->
                            val dateRangeStr = remember(summary.firstDate, summary.lastDate) {
                                val fmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                                if (summary.firstDate == summary.lastDate) {
                                    fmt.format(Date(summary.firstDate))
                                } else {
                                    "${fmt.format(Date(summary.firstDate))} - ${fmt.format(Date(summary.lastDate))}"
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Top Row: Tag Name & Total Spent
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = summary.tag,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "₹${summary.totalExpense.toInt()}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (summary.totalIncome > 0) {
                                                Text(
                                                    text = "Net: ₹${summary.netSpent.toInt()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (summary.netSpent < 0) GreenIncome else RedExpense,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Meta Info: Transaction count & dates
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${summary.transactionCount} transactions",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = dateRangeStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Category breakdown chips
                                    if (summary.categoryBreakdown.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Category Breakdown",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            summary.categoryBreakdown.forEach { (cat, amount) ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "${viewModel.getIconForCategory(cat)} $cat: ₹${amount.toInt()}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Actions: Filter Ledger / Copy Summary
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                val breakdownText = summary.categoryBreakdown.entries.joinToString("\n") { "  • ${it.key}: ₹${it.value.toInt()}" }
                                                val shareText = "📊 Project Cost Summary: ${summary.tag}\nTotal Spent: ₹${summary.totalExpense.toInt()}\nTransactions: ${summary.transactionCount}\nTimeline: $dateRangeStr\n\nBreakdown:\n$breakdownText"
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("Project Cost Summary", shareText)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy")
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = { onSelectTag(summary.tag) },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("View in Ledger", fontSize = 12.sp)
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
}

// BULK EDIT DIALOG (Change Category, Reassign Bank/Method, Bulk Tagging)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkEditDialog(
    viewModel: FinanceViewModel,
    selectedIds: Set<Long>,
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabs = listOf("🏷️ Tags", "📁 Category", "🏦 Account & Method")

    // Category state
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val standardCategories = listOf("Food", "Vehicle", "Home", "Work", "Entertainment", "Health", "Shopping", "Salary", "Freelancing")
    val allCategories = remember(customCategories) {
        (standardCategories + customCategories.filter { it.parentCategory == null }.map { it.name }).distinct()
    }
    var targetCategory by remember { mutableStateOf(allCategories.firstOrNull() ?: "Food") }
    var targetSubcategory by remember { mutableStateOf("") }

    // Account state
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val creditCards by viewModel.creditCards.collectAsStateWithLifecycle()
    var targetPaymentMethod by remember { mutableStateOf("UPI") }
    var targetBankAccountId by remember { mutableStateOf<Long?>(null) }
    var targetCreditCardId by remember { mutableStateOf<Long?>(null) }

    // Tag state
    val allUniqueTags by viewModel.allUniqueTags.collectAsStateWithLifecycle()
    var customTagInput by remember { mutableStateOf("") }
    var tagActionType by remember { mutableStateOf("ADD") } // "ADD" or "REPLACE"
    val popularSuggestions = listOf("#Vacation2026", "#TaxDeductible", "#Wedding", "#HomeRenovation", "#WorkExpense", "#Medical", "#Personal")

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bulk Edit (${selectedIds.size} Items)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // TAGS TAB
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Assign or update project tags across ${selectedIds.size} transactions:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Tag Action Radio (Add vs Replace)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = tagActionType == "ADD",
                                    onClick = { tagActionType = "ADD" },
                                    label = { Text("Add Tag (Keep Existing)", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = tagActionType == "REPLACE",
                                    onClick = { tagActionType = "REPLACE" },
                                    label = { Text("Replace All Tags", fontSize = 11.sp) }
                                )
                            }

                            // Input
                            OutlinedTextField(
                                value = customTagInput,
                                onValueChange = { customTagInput = it },
                                label = { Text("Tag (e.g. #Vacation2026)", color = MutedText) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                            )

                            // Quick Suggestions
                            Text(
                                text = "Quick Tag Suggestions:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            val combinedSuggestions = (popularSuggestions + allUniqueTags).distinct()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                combinedSuggestions.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (customTagInput.contains(tag)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                            )
                                            .clickable {
                                                customTagInput = if (tagActionType == "ADD") {
                                                    tag
                                                } else {
                                                    tag
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 11.sp,
                                            color = if (customTagInput.contains(tag)) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // CATEGORY TAB
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Select new Category for all ${selectedIds.size} transactions:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allCategories.forEach { cat ->
                                    val isSelected = targetCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) PrimaryEmerald else MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable { targetCategory = cat }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${viewModel.getIconForCategory(cat)} $cat",
                                            fontSize = 12.sp,
                                            color = if (isSelected) SmoothWhite else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            // Subcategory Input
                            OutlinedTextField(
                                value = targetSubcategory,
                                onValueChange = { targetSubcategory = it },
                                label = { Text("Subcategory (Optional, e.g. Groceries, Fuel)", color = MutedText) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                            )
                        }
                    }
                    2 -> {
                        // ACCOUNT & PAYMENT METHOD TAB
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Re-assign Payment Channel & Bank Account:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Payment Method
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val methods = listOf("UPI", "Cash", "Credit Card", "Bank")
                                methods.forEach { method ->
                                    val isSelected = targetPaymentMethod == method
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AccentGold else MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable { targetPaymentMethod = method }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = method,
                                            fontSize = 12.sp,
                                            color = if (isSelected) DarkBackground else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Bank Account selection if Bank or UPI
                            if (targetPaymentMethod == "Bank" || targetPaymentMethod == "UPI") {
                                Text(
                                    text = "Assign to Bank Account:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isNone = targetBankAccountId == null
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isNone) AccentGold else MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable { targetBankAccountId = null }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("None / Unlinked", fontSize = 11.sp, color = if (isNone) DarkBackground else MaterialTheme.colorScheme.onSurface)
                                    }

                                    bankAccounts.forEach { bank ->
                                        val isSel = targetBankAccountId == bank.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) AccentGold else MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .clickable { targetBankAccountId = bank.id }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("🏦 ${bank.bankName} (${bank.accountNickname})", fontSize = 11.sp, color = if (isSel) DarkBackground else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            } else if (targetPaymentMethod == "Credit Card") {
                                Text(
                                    text = "Assign to Credit Card:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    creditCards.forEach { card ->
                                        val isSel = targetCreditCardId == card.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) AccentGold else MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .clickable { targetCreditCardId = card.id }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("💳 ${card.cardName}", fontSize = 11.sp, color = if (isSel) DarkBackground else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MutedText)
                    }

                    Button(
                        onClick = {
                            when (selectedTab) {
                                0 -> {
                                    if (customTagInput.isNotBlank()) {
                                        if (tagActionType == "ADD") {
                                            viewModel.bulkAddTag(selectedIds, customTagInput)
                                        } else {
                                            viewModel.bulkSetTags(selectedIds, customTagInput)
                                        }
                                    }
                                }
                                1 -> {
                                    viewModel.bulkUpdateCategory(selectedIds, targetCategory, targetSubcategory)
                                }
                                2 -> {
                                    viewModel.bulkUpdateAccountAndMethod(
                                        targetIds = selectedIds,
                                        bankAccountId = if (targetPaymentMethod == "Bank" || targetPaymentMethod == "UPI") targetBankAccountId else null,
                                        creditCardId = if (targetPaymentMethod == "Credit Card") targetCreditCardId else null,
                                        paymentMethod = targetPaymentMethod
                                    )
                                }
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply to ${selectedIds.size}", color = SmoothWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    viewModel: FinanceViewModel,
    transactionToEdit: Transaction? = null,
    initialAssetId: Long? = null,
    onDismiss: () -> Unit
) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(transactionToEdit?.type ?: "EXPENSE") }
    var selectedCategory by remember { mutableStateOf(transactionToEdit?.category ?: "Food") }
    var selectedSubcategory by remember { mutableStateOf(transactionToEdit?.subcategory ?: "Lunch") }
    var paymentMethod by remember { mutableStateOf(transactionToEdit?.paymentMethod ?: "UPI") }
    var merchant by remember { mutableStateOf(transactionToEdit?.merchant ?: "") }
    var notes by remember { mutableStateOf(transactionToEdit?.notes ?: "") }
    var tagsString by remember { mutableStateOf(transactionToEdit?.tagsString ?: "") }
    var selectedAssetId by remember { mutableStateOf<Long?>(transactionToEdit?.assetId ?: initialAssetId) }
    var selectedCreditCardId by remember { mutableStateOf<Long?>(transactionToEdit?.creditCardId) }
    var selectedBankAccountId by remember { mutableStateOf<Long?>(transactionToEdit?.bankAccountId) }
    var selectedToBankAccountId by remember { mutableStateOf<Long?>(transactionToEdit?.toBankAccountId) }
    var selectedToCreditCardId by remember { mutableStateOf<Long?>(transactionToEdit?.toCreditCardId) }
    var transferDestinationType by remember { mutableStateOf(if (transactionToEdit?.toCreditCardId != null) "CARD" else "BANK") }
    var showCreateTrackerInDialog by remember { mutableStateOf(false) }

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
        val effectiveType = if (selectedType == "INCOME") "INCOME" else "EXPENSE"
        val standard = if (effectiveType == "EXPENSE") CategoryData.expenseCategories else CategoryData.incomeCategories
        val customMain = customCategories.filter { it.type == effectiveType && it.parentCategory == null }
        
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
                                    val catType = if (selectedType == "INCOME") "INCOME" else "EXPENSE"
                                    viewModel.addCustomCategory(
                                        name = newCategoryName.trim(),
                                        icon = newCategoryIcon,
                                        type = catType,
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
                                    val catType = if (selectedType == "INCOME") "INCOME" else "EXPENSE"
                                    viewModel.addCustomCategory(
                                        name = newSubcategoryName.trim(),
                                        icon = "",
                                        type = catType,
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

    if (showCreateTrackerInDialog) {
        AddAssetDialog(
            viewModel = viewModel,
            onDismiss = { showCreateTrackerInDialog = false }
        )
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
                    // Transaction Type Selector (4 Types: Expense, Income, Transfer, Refund)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { selectedType = "EXPENSE" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedType == "EXPENSE") RedExpense else DarkSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Expense", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { selectedType = "INCOME" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedType == "INCOME") GreenIncome else DarkSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Income", fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { selectedType = "TRANSFER" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedType == "TRANSFER") Color(0xFF1976D2) else DarkSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("⇄ Transfer", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { selectedType = "REFUND" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedType == "REFUND") Color(0xFF00796B) else DarkSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("↩ Refund", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Contextual Banner for Special Types
                    if (selectedType == "TRANSFER") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1976D2).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⇄", fontSize = 18.sp)
                                Text(
                                    text = "Inter-account fund transfers and card bill payments shift balances without inflating your monthly expense or income charts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF90CAF9)
                                )
                            }
                        }
                    } else if (selectedType == "REFUND") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF00796B).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF00796B).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("↩", fontSize = 18.sp)
                                Text(
                                    text = "Refunds & cashbacks directly reduce this category's monthly expenses instead of artificially counting as salary/income.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF80CBC4)
                                )
                            }
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

                    // TRANSFER SPECIFIC FIELDS
                    if (selectedType == "TRANSFER") {
                        // 1. Source Account (From)
                        Column {
                            Text("Source Account (Transfer From)", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isCashSelected = selectedBankAccountId == null
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCashSelected) Color(0xFF1976D2) else DarkSurfaceVariant)
                                        .clickable { selectedBankAccountId = null }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("💵 Cash / In-Hand", color = if (isCashSelected) Color.White else SmoothWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                bankAccounts.forEach { bank ->
                                    val isSelected = selectedBankAccountId == bank.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0xFF1976D2) else DarkSurfaceVariant)
                                            .clickable { selectedBankAccountId = bank.id }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        val last4 = if (bank.accountNumberLast4.isNotBlank()) " (*${bank.accountNumberLast4})" else ""
                                        Text("🏦 ${bank.bankName} - ${bank.accountNickname}$last4", color = if (isSelected) Color.White else SmoothWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // 2. Destination Type (Bank Account vs Credit Card Bill Settlement)
                        Column {
                            Text("Destination Type", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (transferDestinationType == "BANK") PrimaryEmerald else DarkSurfaceVariant)
                                        .clickable { transferDestinationType = "BANK" }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("To Bank Account", color = SmoothWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (transferDestinationType == "CARD") AccentGold else DarkSurfaceVariant)
                                        .clickable { transferDestinationType = "CARD" }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Card Bill Settlement", color = if (transferDestinationType == "CARD") DarkBackground else SmoothWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 3. Destination Account / Card Selector
                        if (transferDestinationType == "BANK") {
                            val availableDestBanks = bankAccounts.filter { it.id != selectedBankAccountId }
                            Column {
                                Text("Destination Bank Account (Transfer To)", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (availableDestBanks.isEmpty()) {
                                    Text("Add another bank account to transfer funds between accounts.", color = AccentGold, style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        availableDestBanks.forEach { bank ->
                                            val isSelected = selectedToBankAccountId == bank.id
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) PrimaryEmerald else DarkSurfaceVariant)
                                                    .clickable { selectedToBankAccountId = bank.id }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                val last4 = if (bank.accountNumberLast4.isNotBlank()) " (*${bank.accountNumberLast4})" else ""
                                                Text("🏦 ${bank.bankName} - ${bank.accountNickname}$last4", color = SmoothWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val creditCards by viewModel.creditCards.collectAsStateWithLifecycle()
                            Column {
                                Text("Destination Credit Card (Pay Down Balance)", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (creditCards.isEmpty()) {
                                    Text("No credit cards added. Create a card in Credit Cards to record bill settlements.", color = AccentGold, style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        creditCards.forEach { card ->
                                            val isSelected = selectedToCreditCardId == card.id
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) AccentGold else DarkSurfaceVariant)
                                                    .clickable { selectedToCreditCardId = card.id }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                val last4 = if (card.lastFourDigits.isNotBlank()) " (*${card.lastFourDigits})" else ""
                                                Text("💳 ${card.cardName}$last4", color = if (isSelected) DarkBackground else SmoothWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Merchant / Description
                        OutlinedTextField(
                            value = merchant,
                            onValueChange = { merchant = it },
                            label = { Text(if (transferDestinationType == "CARD") "Payment Reference (e.g. Card Bill)" else "Transfer Reference (e.g. Savings)", color = MutedText) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                        )
                    } else {
                        // NON-TRANSFER (EXPENSE, INCOME, REFUND) FIELDS

                        // Merchant / Source
                        OutlinedTextField(
                            value = merchant,
                            onValueChange = { merchant = it },
                            label = { Text(when (selectedType) { "INCOME" -> "Source / Payer (e.g. Salary, Client)"; "REFUND" -> "Refund Source (e.g. Amazon, Flight Cancel)"; else -> "Merchant / Recipient" }, color = MutedText) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                        )

                        // Category selection dropdown
                        Column {
                            Text(if (selectedType == "REFUND") "Expense Category to Offset" else "Category", style = MaterialTheme.typography.bodySmall, color = MutedText)
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
                            Text(if (selectedType == "REFUND") "Refunded To Account" else "Payment Method", style = MaterialTheme.typography.bodySmall, color = MutedText)
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

                        // Link to Project / Item Tracker (Optional)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Link to Item / Project Tracker (Optional)", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                TextButton(
                                    onClick = { showCreateTrackerInDialog = true },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryLightEmerald)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("New Tracker", fontSize = 11.sp, color = PrimaryLightEmerald, fontWeight = FontWeight.Bold)
                                }
                            }
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
                                    Text("None", color = if (selectedAssetId == null) DarkBackground else SmoothWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                assets.forEach { ast ->
                                    val isSelected = selectedAssetId == ast.id
                                    val emoji = getAssetCategoryEmoji(ast.type, ast.name)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) AccentGold else DarkSurfaceVariant)
                                            .clickable { selectedAssetId = ast.id }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("$emoji ${ast.name}", color = if (isSelected) DarkBackground else SmoothWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        } else if (bankAccounts.isNotEmpty()) {
                            Column {
                                Text(
                                    text = if (paymentMethod == "Bank" || paymentMethod == "UPI") "Select Bank Account" else "Link Bank Account (Optional)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isNoneSelected = selectedBankAccountId == null
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isNoneSelected) AccentGold else DarkSurfaceVariant)
                                            .clickable { selectedBankAccountId = null }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("None", color = if (isNoneSelected) DarkBackground else SmoothWhite, fontSize = 12.sp)
                                    }

                                    bankAccounts.forEach { bank ->
                                        val isSelected = selectedBankAccountId == bank.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) AccentGold else DarkSurfaceVariant)
                                                .clickable { selectedBankAccountId = bank.id }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            val last4 = if (bank.accountNumberLast4.isNotBlank()) " (*${bank.accountNumberLast4})" else ""
                                            Text("🏦 ${bank.bankName} - ${bank.accountNickname}$last4", color = if (isSelected) DarkBackground else SmoothWhite, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
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

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = tagsString,
                            onValueChange = { tagsString = it },
                            label = { Text("Tags (e.g. #Vacation2026, #TaxDeductible, #Wedding)", color = MutedText) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                        )

                        val allTags by viewModel.allUniqueTags.collectAsStateWithLifecycle()
                        val defaultTagSuggestions = listOf("#Vacation2026", "#TaxDeductible", "#Wedding", "#HomeRenovation", "#WorkExpense", "#Medical")
                        val tagSuggestions = remember(allTags) {
                            (defaultTagSuggestions + allTags).distinct()
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tagSuggestions.forEach { suggestion ->
                                val active = tagsString.contains(suggestion, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable {
                                            if (active) {
                                                tagsString = tagsString
                                                    .split(",")
                                                    .map { it.trim() }
                                                    .filter { !it.equals(suggestion, ignoreCase = true) }
                                                    .joinToString(", ")
                                            } else {
                                                tagsString = if (tagsString.isBlank()) suggestion else "$tagsString, $suggestion"
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = suggestion,
                                        fontSize = 11.sp,
                                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
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
                                val effectiveCategory = when (selectedType) {
                                    "TRANSFER" -> if (transferDestinationType == "CARD") "Credit Card Payment" else "Transfer"
                                    else -> selectedCategory
                                }
                                val effectiveSubcategory = when (selectedType) {
                                    "TRANSFER" -> if (transferDestinationType == "CARD") "Card Settlement" else "Bank Transfer"
                                    else -> selectedSubcategory
                                }
                                val effectiveMerchant = when (selectedType) {
                                    "TRANSFER" -> if (merchant.isNotBlank()) merchant else if (transferDestinationType == "CARD") "Credit Card Bill Pay" else "Fund Transfer"
                                    else -> merchant
                                }
                                val effectivePm = when (selectedType) {
                                    "TRANSFER" -> if (selectedBankAccountId == null) "Cash" else "Bank"
                                    else -> paymentMethod
                                }

                                val transaction = if (transactionToEdit != null) {
                                    transactionToEdit.copy(
                                        date = selectedDateLong,
                                        amount = amtVal,
                                        category = effectiveCategory,
                                        subcategory = effectiveSubcategory,
                                        paymentMethod = effectivePm,
                                        merchant = effectiveMerchant,
                                        notes = notes,
                                        tagsString = tagsString,
                                        type = selectedType,
                                        assetId = if (selectedType == "TRANSFER") null else selectedAssetId,
                                        creditCardId = if (selectedType == "TRANSFER" || paymentMethod != "Credit Card") null else selectedCreditCardId,
                                        bankAccountId = if (selectedType == "TRANSFER") selectedBankAccountId else if (paymentMethod == "Credit Card") null else selectedBankAccountId,
                                        toBankAccountId = if (selectedType == "TRANSFER" && transferDestinationType == "BANK") selectedToBankAccountId else null,
                                        toCreditCardId = if (selectedType == "TRANSFER" && transferDestinationType == "CARD") selectedToCreditCardId else null
                                    )
                                } else {
                                    Transaction(
                                        date = selectedDateLong,
                                        amount = amtVal,
                                        category = effectiveCategory,
                                        subcategory = effectiveSubcategory,
                                        paymentMethod = effectivePm,
                                        merchant = effectiveMerchant,
                                        notes = notes,
                                        tagsString = tagsString,
                                        type = selectedType,
                                        assetId = if (selectedType == "TRANSFER") null else selectedAssetId,
                                        creditCardId = if (selectedType == "TRANSFER" || paymentMethod != "Credit Card") null else selectedCreditCardId,
                                        bankAccountId = if (selectedType == "TRANSFER") selectedBankAccountId else if (paymentMethod == "Credit Card") null else selectedBankAccountId,
                                        toBankAccountId = if (selectedType == "TRANSFER" && transferDestinationType == "BANK") selectedToBankAccountId else null,
                                        toCreditCardId = if (selectedType == "TRANSFER" && transferDestinationType == "CARD") selectedToCreditCardId else null
                                    )
                                }
                                viewModel.addTransaction(transaction)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (transactionToEdit != null) "Update" else "Save", color = SmoothWhite)
                    }
                }
            }
        }
    }
}

// HELPER FUNCTIONS FOR UNIVERSAL ITEM & PROJECT TRACKERS
fun getAssetCategoryEmoji(type: String, name: String = "", customCategories: List<CustomCategory> = emptyList()): String {
    // 1. Check if the type matches an existing custom category's icon
    val matchedCustom = customCategories.firstOrNull { it.name.equals(type, ignoreCase = true) }
    if (matchedCustom != null && matchedCustom.icon.isNotBlank()) {
        return matchedCustom.icon
    }
    // 2. Check standard categories
    val standardMatch = (CategoryData.expenseCategories + CategoryData.incomeCategories).firstOrNull { it.name.equals(type, ignoreCase = true) }
    if (standardMatch != null && standardMatch.icon.isNotBlank()) {
        return standardMatch.icon
    }

    val lower = name.lowercase()
    return when {
        lower.contains("scooter") || lower.contains("activa") || lower.contains("jupiter") || lower.contains("ola") || lower.contains("ather") || lower.contains("access") -> "🛵"
        lower.contains("bike") || lower.contains("motorcycle") || lower.contains("bullet") || lower.contains("royal enfield") || lower.contains("pulsar") || lower.contains("ktm") -> "🏍️"
        lower.contains("car") || lower.contains("swift") || lower.contains("creta") || lower.contains("nexon") || lower.contains("thar") || lower.contains("baleno") || lower.contains("ev") -> "🚗"
        lower.contains("cycle") || lower.contains("bicycle") -> "🚲"
        lower.contains("hair") || lower.contains("treatment") || lower.contains("clinic") || lower.contains("derma") || lower.contains("skin") || lower.contains("dental") || lower.contains("teeth") || lower.contains("therapy") || lower.contains("doctor") -> "💇"
        lower.contains("laptop") || lower.contains("macbook") || lower.contains("computer") || lower.contains("pc") || lower.contains("dell") || lower.contains("thinkpad") -> "💻"
        lower.contains("phone") || lower.contains("iphone") || lower.contains("pixel") || lower.contains("samsung") || lower.contains("oneplus") || lower.contains("mobile") -> "📱"
        lower.contains("watch") || lower.contains("apple watch") || lower.contains("smartwatch") -> "⌚"
        lower.contains("trip") || lower.contains("tour") || lower.contains("vacation") || lower.contains("travel") || lower.contains("goa") || lower.contains("manali") || lower.contains("flight") -> "✈️"
        lower.contains("wedding") || lower.contains("marriage") || lower.contains("reception") -> "💍"
        lower.contains("renovation") || lower.contains("interior") || lower.contains("home") || lower.contains("flat") || lower.contains("house") || lower.contains("furniture") -> "🏠"
        lower.contains("gold") || lower.contains("jewelry") || lower.contains("jewellery") || lower.contains("diamond") || lower.contains("silver") -> "💎"
        type == "VEHICLE" || type.contains("Vehicle", ignoreCase = true) || type.contains("Transport", ignoreCase = true) -> "🛵"
        type == "HEALTH_TREATMENT" || type.contains("Health", ignoreCase = true) || type.contains("Medical", ignoreCase = true) -> "💇"
        type == "ELECTRONICS" || type.contains("Electronic", ignoreCase = true) -> "💻"
        type == "PROJECT" || type == "TRIP" || type.contains("Travel", ignoreCase = true) -> "🎯"
        type == "VALUABLE" || type.contains("Investment", ignoreCase = true) -> "💎"
        else -> "📦"
    }
}

fun getAssetTypeBadgeLabel(type: String): String {
    return when (type) {
        "VEHICLE" -> "Vehicle & Transport"
        "HEALTH_TREATMENT", "HEALTH" -> "Health & Treatment"
        "ELECTRONICS", "DEVICE" -> "Electronics & Tech"
        "PROJECT", "TRIP" -> "Project & Trip"
        "VALUABLE" -> "Asset & Valuable"
        else -> type
    }
}

// UNIVERSAL ITEM & PROJECT TRACKERS (COST HUB) SCREEN
@Composable
fun AssetsScreen(viewModel: FinanceViewModel) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAsset by remember { mutableStateOf<Asset?>(null) }
    var editingAsset by remember { mutableStateOf<Asset?>(null) }
    var addingExpenseForAssetId by remember { mutableStateOf<Long?>(null) }
    var selectedFilterCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    // Summary calculations
    val trackedTransactions = remember(transactions) { transactions.filter { it.assetId != null } }
    val totalLifetimeSpent = remember(trackedTransactions) { trackedTransactions.sumOf { it.amount } }
    val totalBudgetOrInitialValue = remember(assets) { assets.sumOf { it.purchasePrice } }

    // Available categories from existing assets
    val presentCategories = remember(assets) {
        assets.map { it.type }.distinct()
    }

    val filteredAssets = remember(assets, selectedFilterCategory, searchQuery) {
        assets.filter { asset ->
            val matchesCategory = if (selectedFilterCategory == "ALL") true else {
                asset.type.equals(selectedFilterCategory, ignoreCase = true) ||
                (selectedFilterCategory == "VEHICLE" && (asset.type == "VEHICLE" || asset.type.contains("Vehicle", ignoreCase = true) || asset.type.contains("Transport", ignoreCase = true))) ||
                (selectedFilterCategory == "HEALTH_TREATMENT" && (asset.type == "HEALTH_TREATMENT" || asset.type.contains("Health", ignoreCase = true)))
            }
            val matchesSearch = searchQuery.isBlank() || asset.name.contains(searchQuery, ignoreCase = true) || asset.notes.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Title & Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎯 Item & Project Trackers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )
                Text(
                    text = "Track cumulative spend on treatments, vehicles, & projects",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Tracker", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High-level Stats Hub Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkSurfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Tracked Spend", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "₹${String.format(Locale.getDefault(), "%,.0f", totalLifetimeSpent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentGold
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(32.dp).background(DarkSurfaceVariant))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Active Trackers", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${assets.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryLightEmerald
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(32.dp).background(DarkSurfaceVariant))

                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Target / Initial", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "₹${String.format(Locale.getDefault(), "%,.0f", totalBudgetOrInitialValue)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmoothWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips Bar (Dynamically populated from existing trackers)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isAllSelected = selectedFilterCategory == "ALL"
            FilterChip(
                selected = isAllSelected,
                onClick = { selectedFilterCategory = "ALL" },
                label = { Text("All (${assets.size})", fontSize = 12.sp, fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryEmerald,
                    selectedLabelColor = SmoothWhite,
                    containerColor = DarkSurface,
                    labelColor = MutedText
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isAllSelected,
                    borderColor = DarkSurfaceVariant,
                    selectedBorderColor = PrimaryEmerald
                )
            )

            presentCategories.forEach { catName ->
                val isSelected = selectedFilterCategory == catName
                val count = assets.count { it.type == catName }
                val emoji = getAssetCategoryEmoji(catName, "", customCategories)
                val badge = getAssetTypeBadgeLabel(catName)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilterCategory = catName },
                    label = { Text("$emoji $badge ($count)", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryEmerald,
                        selectedLabelColor = SmoothWhite,
                        containerColor = DarkSurface,
                        labelColor = MutedText
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = DarkSurfaceVariant,
                        selectedBorderColor = PrimaryEmerald
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (assets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text("🎯", fontSize = 48.sp)
                    Text(
                        "No Item or Project Trackers Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SmoothWhite
                    )
                    Text(
                        "Create a tracker for anything you spend on over time (e.g. Scooter, Hair Treatment, Goa Vacation, Laptop, Home Renovation) to see lifetime costs & timeline history!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Your First Tracker")
                    }
                }
            }
        } else if (filteredAssets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No trackers match the selected filter.", color = MutedText)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredAssets, key = { it.id }) { asset ->
                    val linkedTx = transactions.filter { it.assetId == asset.id }
                    val totalSpent = linkedTx.sumOf { it.amount }
                    val emoji = getAssetCategoryEmoji(asset.type, asset.name, customCategories)
                    val badgeLabel = getAssetTypeBadgeLabel(asset.type)
                    val targetBudgetOrPrice = asset.purchasePrice
                    val hasTarget = targetBudgetOrPrice > 0

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, DarkSurfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAsset = asset }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Emoji + Title + Badge + Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(PrimaryEmerald.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 24.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = asset.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SmoothWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = DarkSurfaceVariant
                                            ) {
                                                Text(
                                                    text = badgeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PrimaryLightEmerald,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = "Started ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(asset.purchaseDate))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MutedText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { editingAsset = asset },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryLightEmerald, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            viewModel.requestDeleteConfirmation(
                                                title = "Delete Tracker?",
                                                message = "Are you sure you want to permanently delete tracker '${asset.name}'? Existing linked transactions will be retained but unlinked."
                                            ) {
                                                viewModel.deleteAsset(asset)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedExpense, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DarkSurfaceVariant)

                            // Cost Metrics Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Cumulative Total Spent", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "₹${String.format(Locale.getDefault(), "%,.0f", totalSpent)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AccentGold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Expenses Logged", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${linkedTx.size} entries",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SmoothWhite
                                    )
                                }
                            }

                            // Progress Bar if target/budget exists
                            if (hasTarget) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val progress = (totalSpent / targetBudgetOrPrice).coerceIn(0.0, 1.0).toFloat()
                                val isOver = totalSpent > targetBudgetOrPrice
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isOver) "Budget Exceeded!" else "Target Budget / Value: ₹${String.format(Locale.getDefault(), "%,.0f", targetBudgetOrPrice)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOver) RedExpense else MutedText
                                        )
                                        Text(
                                            text = "${((totalSpent / targetBudgetOrPrice) * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOver) RedExpense else PrimaryLightEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (isOver) RedExpense else PrimaryEmerald,
                                        trackColor = DarkSurfaceVariant
                                    )
                                }
                            }

                            // Notes / Details snippet if any
                            if (asset.notes.isNotBlank() || asset.insuranceDetails.isNotBlank() || asset.warrantyDetails.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val detailSnippet = listOfNotNull(
                                    asset.insuranceDetails.takeIf { it.isNotBlank() },
                                    asset.warrantyDetails.takeIf { it.isNotBlank() },
                                    asset.notes.takeIf { it.isNotBlank() }
                                ).joinToString(" • ")
                                Text(
                                    text = detailSnippet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Bottom Card Actions: Add Expense & View Details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { addingExpenseForAssetId = asset.id },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, PrimaryEmerald),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryLightEmerald),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { selectedAsset = asset },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Text("Timeline History →", fontSize = 12.sp, color = SmoothWhite, fontWeight = FontWeight.SemiBold)
                                }
                            }
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
            customCategories = customCategories,
            onDismiss = { selectedAsset = null },
            onAddExpense = {
                val assetId = selectedAsset!!.id
                selectedAsset = null
                addingExpenseForAssetId = assetId
            },
            onEditAsset = {
                val current = selectedAsset!!
                selectedAsset = null
                editingAsset = current
            }
        )
    }

    if (addingExpenseForAssetId != null) {
        AddTransactionDialog(
            viewModel = viewModel,
            initialAssetId = addingExpenseForAssetId,
            onDismiss = { addingExpenseForAssetId = null }
        )
    }
}

// ADD / EDIT UNIVERSAL ITEM OR PROJECT TRACKER DIALOG
@Composable
fun AddAssetDialog(
    viewModel: FinanceViewModel,
    assetToEdit: Asset? = null,
    onDismiss: () -> Unit
) {
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    // Unified categories from Category Manager (Expense categories + Custom categories)
    val categoriesList = remember(customCategories) {
        val standard = CategoryData.expenseCategories
        val customMain = customCategories.filter { it.type == "EXPENSE" && it.parentCategory == null }
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

    var name by remember { mutableStateOf(assetToEdit?.name ?: "") }
    var selectedCategoryName by remember {
        mutableStateOf(
            assetToEdit?.type ?: (categoriesList.firstOrNull()?.name ?: "Transport")
        )
    }
    var price by remember { mutableStateOf(if (assetToEdit?.purchasePrice != null && assetToEdit.purchasePrice > 0) assetToEdit.purchasePrice.toString() else "") }
    var insurance by remember { mutableStateOf(assetToEdit?.insuranceDetails ?: "") }
    var warranty by remember { mutableStateOf(assetToEdit?.warrantyDetails ?: "") }
    var notes by remember { mutableStateOf(assetToEdit?.notes ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (assetToEdit != null) "Edit Tracker" else "New Item / Project Tracker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                Text(
                    text = "Track cumulative expenses on your vehicle, health treatment, trip, or gadget using your existing categories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )

                // Category Selection from unified Category Manager
                Column {
                    Text("Select Category", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categoriesList.forEach { cat ->
                            val isSelected = selectedCategoryName.equals(cat.name, ignoreCase = true) ||
                                (selectedCategoryName == "VEHICLE" && cat.name.contains("Transport", ignoreCase = true)) ||
                                (selectedCategoryName == "HEALTH_TREATMENT" && cat.name.contains("Health", ignoreCase = true))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryEmerald else DarkSurfaceVariant)
                                    .clickable { selectedCategoryName = cat.name }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("${cat.icon} ${cat.name}", color = SmoothWhite, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tracker / Item Name (e.g. Activa 6G, Hair Treatment, Goa Trip)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                // Budget / Initial Cost Input (Optional)
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Target Budget or Initial Cost (₹) - Optional", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                // Doctor / Clinic / Insurance / Policy info
                OutlinedTextField(
                    value = insurance,
                    onValueChange = { insurance = it },
                    label = { Text("Policy / Clinic / Contact Info (Optional)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                // Warranty / Schedule info
                OutlinedTextField(
                    value = warranty,
                    onValueChange = { warranty = it },
                    label = { Text("Warranty / Schedule / Protocol (Optional)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                // Additional Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("General Notes & Reminders", color = MutedText) },
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
                                        name = name.trim(),
                                        type = selectedCategoryName,
                                        purchasePrice = prVal,
                                        insuranceDetails = insurance.trim(),
                                        warrantyDetails = warranty.trim(),
                                        notes = notes.trim()
                                    )
                                } else {
                                    Asset(
                                        name = name.trim(),
                                        type = selectedCategoryName,
                                        purchasePrice = prVal,
                                        purchaseDate = System.currentTimeMillis(),
                                        insuranceDetails = insurance.trim(),
                                        warrantyDetails = warranty.trim(),
                                        notes = notes.trim()
                                    )
                                }
                                viewModel.addAsset(asset)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (assetToEdit != null) "Update" else "Save Tracker", color = SmoothWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// UNIVERSAL ITEM / PROJECT TIMELINE DRILLDOWN DIALOG
@Composable
fun AssetDetailDialog(
    asset: Asset,
    transactions: List<Transaction>,
    customCategories: List<CustomCategory> = emptyList(),
    onDismiss: () -> Unit,
    onAddExpense: () -> Unit = {},
    onEditAsset: () -> Unit = {}
) {
    val totalSpent = remember(transactions) { transactions.sumOf { it.amount } }
    val emoji = getAssetCategoryEmoji(asset.type, asset.name, customCategories)
    val badgeLabel = getAssetTypeBadgeLabel(asset.type)
    val targetBudgetOrPrice = asset.purchasePrice
    val hasTarget = targetBudgetOrPrice > 0

    // Grouping by Subcategory/Category to show spend breakdown
    val categoryBreakdown = remember(transactions) {
        transactions.groupBy { if (it.subcategory.isNotBlank()) it.subcategory else it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = asset.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SmoothWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$badgeLabel • Started ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(asset.purchaseDate))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText
                            )
                        }
                    }
                    IconButton(onClick = onEditAsset) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryLightEmerald)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MutedText)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 3-Stat Summary Grid
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkBackground,
                        border = BorderStroke(1.dp, DarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("₹${String.format(Locale.getDefault(), "%,.0f", totalSpent)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AccentGold)
                            }
                            Box(modifier = Modifier.width(1.dp).height(30.dp).background(DarkSurfaceVariant))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Transactions", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${transactions.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SmoothWhite)
                            }
                            if (hasTarget) {
                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(DarkSurfaceVariant))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Target / Limit", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("₹${String.format(Locale.getDefault(), "%,.0f", targetBudgetOrPrice)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryLightEmerald)
                                }
                            }
                        }
                    }

                    // Spend Breakdown by Category / Purpose
                    if (categoryBreakdown.isNotEmpty()) {
                        Column {
                            Text("📊 Spend Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SmoothWhite)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categoryBreakdown.forEach { (catName, amount) ->
                                    val pct = if (totalSpent > 0) ((amount / totalSpent) * 100).toInt() else 0
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = DarkBackground,
                                        border = BorderStroke(1.dp, DarkSurfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(catName, style = MaterialTheme.typography.bodySmall, color = SmoothWhite, fontWeight = FontWeight.Medium)
                                            Text("₹${String.format(Locale.getDefault(), "%,.0f", amount)} ($pct%)", style = MaterialTheme.typography.labelSmall, color = AccentGold, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Contact / Doctor / Insurance Info
                    if (asset.insuranceDetails.isNotBlank() || asset.warrantyDetails.isNotBlank() || asset.notes.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkBackground,
                            border = BorderStroke(1.dp, DarkSurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (asset.insuranceDetails.isNotBlank()) {
                                    Text("📋 Details & Policy:", style = MaterialTheme.typography.labelSmall, color = AccentGold, fontWeight = FontWeight.Bold)
                                    Text(asset.insuranceDetails, style = MaterialTheme.typography.bodySmall, color = SmoothWhite)
                                }
                                if (asset.warrantyDetails.isNotBlank()) {
                                    Text("⏱️ Schedule & Timeline:", style = MaterialTheme.typography.labelSmall, color = PrimaryLightEmerald, fontWeight = FontWeight.Bold)
                                    Text(asset.warrantyDetails, style = MaterialTheme.typography.bodySmall, color = SmoothWhite)
                                }
                                if (asset.notes.isNotBlank()) {
                                    Text("📝 Notes:", style = MaterialTheme.typography.labelSmall, color = MutedText, fontWeight = FontWeight.Bold)
                                    Text(asset.notes, style = MaterialTheme.typography.bodySmall, color = SmoothWhite)
                                }
                            }
                        }
                    }

                    // Chronological Timeline Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📅 Expense Timeline History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SmoothWhite)
                        Text("${transactions.size} records", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    }

                    if (transactions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkBackground,
                            border = BorderStroke(1.dp, DarkSurfaceVariant),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("No expenses recorded for this tracker yet.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                                Text("Tap '+ Record Expense' below to add your first expense!", color = PrimaryLightEmerald, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else {
                        // Chronological vertical timeline list
                        Column {
                            transactions.sortedByDescending { it.date }.forEachIndexed { index, tx ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // Bullet line
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(28.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryLightEmerald)
                                        )
                                        if (index < transactions.lastIndex) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(65.dp)
                                                    .background(DarkSurfaceVariant)
                                            )
                                        }
                                    }

                                    // Timeline card
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = DarkBackground,
                                        border = BorderStroke(1.dp, DarkSurfaceVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(tx.date)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AccentGold,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "₹${String.format(Locale.getDefault(), "%,.0f", tx.amount)}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = RedExpense
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = if (tx.subcategory.isNotBlank()) "${tx.category} • ${tx.subcategory}" else tx.category,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SmoothWhite
                                            )

                                            if (tx.merchant.isNotBlank() || tx.paymentMethod.isNotBlank()) {
                                                val details = listOfNotNull(
                                                    tx.merchant.takeIf { it.isNotBlank() }?.let { "At $it" },
                                                    tx.paymentMethod.takeIf { it.isNotBlank() }?.let { "via $it" }
                                                ).joinToString(" • ")
                                                Text(
                                                    text = details,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MutedText
                                                )
                                            }

                                            if (tx.notes.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = tx.notes,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MutedText
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Dialog Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddExpense,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record Expense", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkSurfaceVariant),
                        modifier = Modifier.weight(0.5f)
                    ) {
                        Text("Done", color = SmoothWhite)
                    }
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
        // Responsive Two-Tier Header: Title + Badge and Prominent Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = "Budgets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant
                ) {
                    Text(
                        text = "${budgets.size} limits",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("add_budget_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Budget", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
    onToggleActive: (Boolean) -> Unit,
    onToggleAutoRenew: (Boolean) -> Unit,
    onPayAndAdvanceClick: () -> Unit,
    onTestAlertClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val daysLeft = remember(sub.renewalDate) {
        SubscriptionNotificationHelper.calculateDaysUntilRenewal(sub.renewalDate)
    }
    val isUrgent = sub.isActive && (daysLeft in 0..sub.reminderDaysInAdvance || daysLeft < 0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("subscription_card_${sub.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent && sub.isAutoRenew) DarkSurfaceVariant.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (isUrgent) 1.5.dp else 1.dp,
            if (isUrgent) AccentGold.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: Icon, Name, Category, and Cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subscription Icon / Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (sub.isActive) PrimaryEmerald.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (sub.isAutoRenew) Icons.Default.Autorenew else Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = if (sub.isActive) PrimaryLightEmerald else MutedText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Category details
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (sub.isActive) PrimaryEmerald.copy(alpha = 0.15f) else RedExpense.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (sub.isActive) "Active" else "Paused",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sub.isActive) PrimaryLightEmerald else RedExpense,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (sub.category.isNotBlank()) sub.category else "Subscription",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        maxLines = 1
                    )
                }

                // Cost display
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.0f", sub.cost)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
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

            // Badges row: Auto-Renew status, Reminder timing, and Due countdown
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto-Renew badge
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (sub.isAutoRenew) PrimaryEmerald.copy(alpha = 0.15f) else DarkSurfaceVariant,
                        border = BorderStroke(0.5.dp, if (sub.isAutoRenew) PrimaryEmerald.copy(alpha = 0.4f) else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (sub.isAutoRenew) Icons.Default.Bolt else Icons.Default.EditCalendar,
                                contentDescription = null,
                                tint = if (sub.isAutoRenew) PrimaryLightEmerald else MutedText,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (sub.isAutoRenew) "Auto-Renew ON" else "Manual Pay",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (sub.isAutoRenew) PrimaryLightEmerald else MutedText
                            )
                        }
                    }
                }

                // Advance Alert badge
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (sub.reminderDaysInAdvance == 0) "Same day alert" else "${sub.reminderDaysInAdvance}d prior alert",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = AccentGold
                            )
                        }
                    }
                }

                // Due countdown badge
                item {
                    val (dueBg, dueTextColor, dueLabel) = when {
                        daysLeft < 0 -> Triple(RedExpense.copy(alpha = 0.18f), RedExpense, "Overdue ${-daysLeft}d")
                        daysLeft == 0 -> Triple(RedExpense.copy(alpha = 0.18f), RedExpense, "Due Today!")
                        daysLeft == 1 -> Triple(AccentGold.copy(alpha = 0.18f), AccentGold, "Due Tomorrow")
                        daysLeft in 2..sub.reminderDaysInAdvance -> Triple(AccentGold.copy(alpha = 0.18f), AccentGold, "In $daysLeft days")
                        else -> Triple(DarkSurfaceVariant, MutedText, "In $daysLeft days")
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = dueBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = dueTextColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = dueLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = dueTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Renewal date and Linked payment method
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Renewal: ${sdf.format(Date(sub.renewalDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmoothWhite,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (sub.paymentMethodName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceVariant
                    ) {
                        Text(
                            text = "🏦 ${sub.paymentMethodName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (sub.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📝 ${sub.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            // Row 1: Primary Action ("Pay & Advance") and Status Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pay & Advance Cycle button
                Button(
                    onClick = onPayAndAdvanceClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald.copy(alpha = 0.22f)),
                    border = BorderStroke(1.dp, PrimaryEmerald.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    modifier = Modifier.testTag("pay_advance_sub_${sub.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Pay and Advance",
                        tint = PrimaryLightEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pay & Advance",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryLightEmerald
                    )
                }

                // Active status toggle with clear spacing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (sub.isActive) "Active" else "Paused",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
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
                        ),
                        modifier = Modifier.testTag("toggle_sub_active_${sub.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Secondary Tool Actions (Test Alert, Edit, Delete) with clear buttons and labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Test Notification Alert Button
                OutlinedButton(
                    onClick = onTestAlertClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_notif_sub_${sub.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationImportant,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Test Alert",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGold
                    )
                }

                // Edit Button
                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SmoothWhite),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_sub_${sub.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = PrimaryLightEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Edit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SmoothWhite
                    )
                }

                // Delete Button
                OutlinedButton(
                    onClick = onDeleteClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, RedExpense.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedExpense),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("delete_sub_${sub.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = RedExpense,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RedExpense
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionsScreen(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val subs by viewModel.subscriptions.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val creditCards by viewModel.creditCards.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSub by remember { mutableStateOf<Subscription?>(null) }
    var payingSub by remember { mutableStateOf<Subscription?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    // Android 13+ Notification Permission Launcher
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted! Advance renewal alerts active.", Toast.LENGTH_SHORT).show()
            viewModel.triggerUpcomingSubscriptionCheck(context)
        } else {
            Toast.makeText(context, "Enable notifications in settings to receive renewal reminders.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val totalMonthlyCost = subs.filter { it.isActive }.sumOf {
        val months = getBillingCycleMonths(it.billingCycle)
        it.cost / months
    }

    val activeSubs = subs.filter { it.isActive }
    val autoRenewSubs = subs.filter { it.isActive && it.isAutoRenew }
    val urgentSubs = subs.filter { sub ->
        if (!sub.isActive) return@filter false
        val days = SubscriptionNotificationHelper.calculateDaysUntilRenewal(sub.renewalDate)
        days in 0..sub.reminderDaysInAdvance || days < 0
    }

    val upcoming7DaysCost = subs.filter { sub ->
        if (!sub.isActive) return@filter false
        val days = SubscriptionNotificationHelper.calculateDaysUntilRenewal(sub.renewalDate)
        days in 0..7
    }.sumOf { it.cost }

    val filteredSubs = subs.filter { sub ->
        val matchesQuery = searchQuery.isBlank() || sub.name.contains(searchQuery, ignoreCase = true) || sub.category.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "UPCOMING" -> {
                val days = SubscriptionNotificationHelper.calculateDaysUntilRenewal(sub.renewalDate)
                sub.isActive && (days in 0..sub.reminderDaysInAdvance || days < 0)
            }
            "AUTORENEW" -> sub.isActive && sub.isAutoRenew
            "ACTIVE" -> sub.isActive
            "PAUSED" -> !sub.isActive
            else -> true
        }
        matchesQuery && matchesFilter
    }.sortedBy { it.renewalDate }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // Summary Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DarkSurface, DarkSurfaceVariant.copy(alpha = 0.7f))
                        )
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SUBSCRIPTION & AUTO-RENEW TRACKER",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGold,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%,.0f", totalMonthlyCost)} / mo",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = SmoothWhite
                            )
                            Text(
                                text = "${activeSubs.size} active • ${autoRenewSubs.size} auto-debiting",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Scan & Test Notifications button
                            IconButton(
                                onClick = {
                                    viewModel.triggerUpcomingSubscriptionCheck(context)
                                    Toast.makeText(
                                        context,
                                        if (urgentSubs.isNotEmpty()) "Sent ${urgentSubs.size} upcoming renewal alert(s) to notification bar!" else "All active subscriptions scanned. No renewals due today.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceVariant)
                                    .testTag("check_all_due_notifications_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Check Notifications",
                                    tint = AccentGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Add Subscription Button
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("add_subscription_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (upcoming7DaysCost > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AccentGold.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Due Next 7 Days: ₹${String.format(Locale.getDefault(), "%,.0f", upcoming7DaysCost)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SmoothWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Auto-Debit Safe",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Urgent Renewal Alerts Banner (If any subscriptions are due or in reminder window)
        if (urgentSubs.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = RedExpense.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, RedExpense.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("urgent_renewals_banner")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RedExpense, modifier = Modifier.size(20.dp))
                            Text(
                                text = "⚡ ${urgentSubs.size} Upcoming / Due Renewal Alert(s)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SmoothWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "The following active subscriptions will renew or auto-debit soon. Check your payment balance or advance the cycle once paid:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        urgentSubs.take(3).forEach { sub ->
                            val days = SubscriptionNotificationHelper.calculateDaysUntilRenewal(sub.renewalDate)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "• ${sub.name}",
                                            fontWeight = FontWeight.Bold,
                                            color = SmoothWhite,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Text(
                                            text = "(₹${sub.cost.toInt()})",
                                            color = PrimaryLightEmerald,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = SubscriptionNotificationHelper.getFormattedDaysLeft(days),
                                        color = if (days <= 1) RedExpense else AccentGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = { payingSub = sub },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SmoothWhite)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // Search and Filter Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search subscriptions...", color = MutedText) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MutedText) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MutedText)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryEmerald,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subscription_search_input")
            )
        }

        // Filter Pills
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf(
                    "ALL" to "All (${subs.size})",
                    "UPCOMING" to "Upcoming Alerts (${urgentSubs.size})",
                    "AUTORENEW" to "Auto-Renew ON (${autoRenewSubs.size})",
                    "ACTIVE" to "Active (${activeSubs.size})",
                    "PAUSED" to "Paused (${subs.size - activeSubs.size})"
                )
                items(filters) { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SmoothWhite else MutedText
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryEmerald,
                            containerColor = DarkSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) PrimaryEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
        }

        if (filteredSubs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No subscriptions match '$searchQuery'" else "No subscriptions in this view.",
                            color = MutedText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add New Subscription")
                        }
                    }
                }
            }
        } else {
            items(filteredSubs, key = { it.id }) { sub ->
                SubscriptionCard(
                    sub = sub,
                    onEditClick = { editingSub = sub },
                    onDeleteClick = {
                        viewModel.requestDeleteConfirmation(
                            title = "Delete Subscription?",
                            message = "Are you sure you want to permanently delete '${sub.name}' (₹${sub.cost.toInt()})?"
                        ) {
                            viewModel.deleteSubscription(sub)
                        }
                    },
                    onToggleActive = { viewModel.toggleSubscriptionActive(sub) },
                    onToggleAutoRenew = { viewModel.toggleSubscriptionAutoRenew(sub) },
                    onPayAndAdvanceClick = { payingSub = sub },
                    onTestAlertClick = {
                        viewModel.testSubscriptionAlert(context, sub)
                        Toast.makeText(
                            context,
                            "🔔 Test reminder sent to notification bar for ${sub.name}!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    if (editingSub != null) {
        AddSubscriptionDialog(
            viewModel = viewModel,
            subToEdit = editingSub,
            onDismiss = { editingSub = null }
        )
    }

    if (payingSub != null) {
        PayAndAdvanceDialog(
            sub = payingSub!!,
            bankAccounts = bankAccounts,
            creditCards = creditCards,
            onDismiss = { payingSub = null },
            onConfirm = { recordTx, accountId, cardId, method ->
                viewModel.advanceSubscriptionRenewal(
                    sub = payingSub!!,
                    recordPaymentTransaction = recordTx,
                    paymentAccountId = accountId,
                    paymentCardId = cardId,
                    paymentMethod = method
                )
                Toast.makeText(
                    context,
                    "✅ ${payingSub!!.name} marked as paid! Advanced to next cycle.",
                    Toast.LENGTH_SHORT
                ).show()
                payingSub = null
            }
        )
    }
}

@Composable
fun PayAndAdvanceDialog(
    sub: Subscription,
    bankAccounts: List<BankAccount>,
    creditCards: List<CreditCard>,
    onDismiss: () -> Unit,
    onConfirm: (recordTx: Boolean, accountId: Long?, cardId: Long?, method: String) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val nextRenewal = remember(sub.renewalDate, sub.billingCycle) {
        SubscriptionNotificationHelper.getNextCycleTimestamp(sub.renewalDate, sub.billingCycle)
    }

    var recordTransaction by remember { mutableStateOf(true) }
    var selectedPaymentSource by remember {
        mutableStateOf(
            if (sub.paymentAccountId != null) "BANK_${sub.paymentAccountId}"
            else if (sub.paymentCardId != null) "CARD_${sub.paymentCardId}"
            else if (bankAccounts.isNotEmpty()) "BANK_${bankAccounts.first().id}"
            else "UPI"
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, PrimaryEmerald.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().testTag("pay_and_advance_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryLightEmerald, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            text = "Pay & Advance Renewal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SmoothWhite
                        )
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Details Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment Amount:", color = MutedText, fontSize = 13.sp)
                            Text("₹${String.format(Locale.getDefault(), "%,.2f", sub.cost)}", color = SmoothWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Current Due Date:", color = MutedText, fontSize = 13.sp)
                            Text(sdf.format(Date(sub.renewalDate)), color = AccentGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Next Cycle Date:", color = MutedText, fontSize = 13.sp)
                            Text(sdf.format(Date(nextRenewal)), color = PrimaryLightEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Checkbox to record transaction
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { recordTransaction = !recordTransaction }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = recordTransaction,
                        onCheckedChange = { recordTransaction = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryEmerald,
                            checkmarkColor = SmoothWhite
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Record in Expense Ledger", style = MaterialTheme.typography.bodyMedium, color = SmoothWhite, fontWeight = FontWeight.SemiBold)
                        Text("Deduct amount from your chosen account/card", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    }
                }

                if (recordTransaction && (bankAccounts.isNotEmpty() || creditCards.isNotEmpty())) {
                    Column {
                        Text("Deduct From Account / Card:", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(bankAccounts) { acc ->
                                val key = "BANK_${acc.id}"
                                val isSel = selectedPaymentSource == key
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedPaymentSource = key },
                                    label = { Text("🏦 ${acc.bankName} (...${acc.accountNumberLast4})", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryEmerald,
                                        containerColor = DarkSurfaceVariant
                                    )
                                )
                            }
                            items(creditCards) { card ->
                                val key = "CARD_${card.id}"
                                val isSel = selectedPaymentSource == key
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedPaymentSource = key },
                                    label = { Text("💳 ${card.cardName} (...${card.lastFourDigits})", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryEmerald,
                                        containerColor = DarkSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text("Cancel", color = SmoothWhite)
                    }
                    Button(
                        onClick = {
                            val (accId, cardId, method) = when {
                                selectedPaymentSource.startsWith("BANK_") -> {
                                    val id = selectedPaymentSource.removePrefix("BANK_").toLongOrNull()
                                    Triple(id, null, "Bank")
                                }
                                selectedPaymentSource.startsWith("CARD_") -> {
                                    val id = selectedPaymentSource.removePrefix("CARD_").toLongOrNull()
                                    Triple(null, id, "Credit Card")
                                }
                                else -> Triple(null, null, "UPI")
                            }
                            onConfirm(recordTransaction, accId, cardId, method)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm", color = SmoothWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddSubscriptionDialog(
    viewModel: FinanceViewModel,
    subToEdit: Subscription? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val creditCards by viewModel.creditCards.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf(subToEdit?.name ?: "") }
    var cost by remember { mutableStateOf(subToEdit?.cost?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var category by remember { mutableStateOf(subToEdit?.category ?: "Entertainment") }
    var selectedCycle by remember { mutableStateOf(subToEdit?.billingCycle ?: "MONTHLY") }
    var isAutoRenew by remember { mutableStateOf(subToEdit?.isAutoRenew ?: true) }
    var reminderDays by remember { mutableStateOf(subToEdit?.reminderDaysInAdvance ?: 2) }
    var paymentMethodName by remember { mutableStateOf(subToEdit?.paymentMethodName ?: "") }
    var selectedAccountId by remember { mutableStateOf(subToEdit?.paymentAccountId) }
    var selectedCreditCardId by remember { mutableStateOf(subToEdit?.paymentCardId) }
    var notes by remember { mutableStateOf(subToEdit?.notes ?: "") }

    // Start date selection
    var startDate by remember {
        mutableStateOf(subToEdit?.renewalDate ?: System.currentTimeMillis())
    }

    val nextBillingDate = remember(startDate, selectedCycle, subToEdit) {
        if (subToEdit != null) {
            startDate
        } else {
            val cal = Calendar.getInstance().apply { timeInMillis = startDate }
            val months = getBillingCycleMonths(selectedCycle)
            cal.add(Calendar.MONTH, months)
            cal.timeInMillis
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val presetSuggestions = remember {
        listOf(
            Triple("Netflix", "649", "Entertainment"),
            Triple("Spotify", "149", "Entertainment"),
            Triple("Amazon Prime", "1499", "Shopping & Prime"),
            Triple("YouTube Premium", "149", "Entertainment"),
            Triple("ChatGPT Plus", "1999", "Work & AI"),
            Triple("Disney+ Hotstar", "899", "Entertainment"),
            Triple("Google One 2TB", "650", "Cloud Storage"),
            Triple("iCloud+", "219", "Cloud Storage"),
            Triple("Gym Membership", "2000", "Fitness & Health"),
            Triple("Broadband / WiFi", "999", "Utilities")
        )
    }

    val reminderOptions = remember {
        listOf(
            0 to "Same Day (0d)",
            1 to "1 Day Prior",
            2 to "2 Days (Recommended)",
            3 to "3 Days Prior",
            5 to "5 Days Prior",
            7 to "1 Week Prior (7d)",
            14 to "2 Weeks Prior (14d)"
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().testTag("add_subscription_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (subToEdit != null) "Edit Subscription" else "Add Subscription Tracker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                // Quick preset pills (only if adding new)
                if (subToEdit == null) {
                    Column {
                        Text("Quick Presets:", style = MaterialTheme.typography.labelSmall, color = AccentGold, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(presetSuggestions) { (presetName, presetCost, presetCat) ->
                                SuggestionChip(
                                    onClick = {
                                        name = presetName
                                        cost = presetCost
                                        category = presetCat
                                    },
                                    label = { Text(presetName, fontSize = 11.sp, color = SmoothWhite) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkSurfaceVariant),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        enabled = true
                                    )
                                )
                            }
                        }
                    }
                }

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product / Service Name *", color = MutedText) },
                    placeholder = { Text("e.g. Netflix, Spotify, Gym", color = MutedText) },
                    modifier = Modifier.fillMaxWidth().testTag("sub_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                )

                // Cost and Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it },
                        label = { Text("Cost (₹) *", color = MutedText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("sub_cost_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category", color = MutedText) },
                        modifier = Modifier.weight(1f).testTag("sub_category_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    )
                }

                // Billing Cycle Dropdown
                var isCycleDropdownExpanded by remember { mutableStateOf(false) }
                val cycleOptions = remember {
                    listOf(
                        "MONTHLY" to "Monthly (1 Month)",
                        "2_MONTHS" to "Every 2 Months",
                        "3_MONTHS" to "Every 3 Months (Quarterly)",
                        "4_MONTHS" to "Every 4 Months",
                        "6_MONTHS" to "Every 6 Months (Half-Yearly)",
                        "YEARLY" to "Yearly (12 Months)"
                    )
                }

                Column {
                    Text("Billing Cycle", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Spacer(modifier = Modifier.height(4.dp))
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
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Cycle", tint = PrimaryLightEmerald, modifier = Modifier.size(24.dp))
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

                // Next Renewal Date Picker
                Column {
                    Text(if (subToEdit != null) "Next Renewal Date" else "Subscription Start Date", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Spacer(modifier = Modifier.height(4.dp))
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
                            text = dateFormatter.format(Date(if (subToEdit != null) startDate else nextBillingDate)),
                            color = SmoothWhite,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = PrimaryLightEmerald, modifier = Modifier.size(20.dp))
                    }
                }

                // ADVANCE NOTIFICATION WINDOW SETTINGS (User Request)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AccentGold.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Advance Renewal Notification Alert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SmoothWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "How many days in advance should the app notify you before the renewal date / auto-debit charge?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(reminderOptions) { (days, label) ->
                                val isSelected = reminderDays == days
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { reminderDays = days },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) SmoothWhite else MutedText
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentGold.copy(alpha = 0.85f),
                                        containerColor = DarkSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Auto-Renew Toggle Switch
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = if (isAutoRenew) PrimaryLightEmerald else MutedText, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Auto-Renew Enabled",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SmoothWhite
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "App warns you before auto-debits so you can check account balance or cancel.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText
                            )
                        }
                        Switch(
                            checked = isAutoRenew,
                            onCheckedChange = { isAutoRenew = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SmoothWhite,
                                checkedTrackColor = PrimaryEmerald,
                                uncheckedThumbColor = MutedText,
                                uncheckedTrackColor = DarkSurface
                            ),
                            modifier = Modifier.testTag("sub_autorenew_toggle")
                        )
                    }
                }

                // Linked Payment Source / Method
                Column {
                    Text("Linked Payment Source (Optional)", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (bankAccounts.isNotEmpty() || creditCards.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(bankAccounts) { acc ->
                                val isSel = selectedAccountId == acc.id
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        selectedAccountId = if (isSel) null else acc.id
                                        if (!isSel) {
                                            selectedCreditCardId = null
                                            paymentMethodName = "${acc.bankName} (...${acc.accountNumberLast4})"
                                        } else {
                                            paymentMethodName = ""
                                        }
                                    },
                                    label = { Text("🏦 ${acc.bankName}", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryEmerald,
                                        containerColor = DarkSurfaceVariant
                                    )
                                )
                            }
                            items(creditCards) { card ->
                                val isSel = selectedCreditCardId == card.id
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        selectedCreditCardId = if (isSel) null else card.id
                                        if (!isSel) {
                                            selectedAccountId = null
                                            paymentMethodName = "${card.cardName} (...${card.lastFourDigits})"
                                        } else {
                                            paymentMethodName = ""
                                        }
                                    },
                                    label = { Text("💳 ${card.cardName}", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryEmerald,
                                        containerColor = DarkSurfaceVariant
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    OutlinedTextField(
                        value = paymentMethodName,
                        onValueChange = { paymentMethodName = it },
                        label = { Text("Payment Method / Card / Account Label", color = MutedText) },
                        placeholder = { Text("e.g. HDFC Regalia, UPI AutoPay", color = MutedText) },
                        modifier = Modifier.fillMaxWidth().testTag("sub_payment_method_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    )
                }

                // Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Cancellation Policy", color = MutedText) },
                    placeholder = { Text("e.g. Plan includes 4 screens, cancel via App Store", color = MutedText) },
                    modifier = Modifier.fillMaxWidth().testTag("sub_notes_input"),
                    maxLines = 2,
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
                                        name = name.trim(),
                                        cost = costVal,
                                        category = category.trim(),
                                        billingCycle = selectedCycle,
                                        renewalDate = startDate,
                                        isAutoRenew = isAutoRenew,
                                        reminderDaysInAdvance = reminderDays,
                                        paymentAccountId = selectedAccountId,
                                        paymentCardId = selectedCreditCardId,
                                        paymentMethodName = paymentMethodName.trim(),
                                        notes = notes.trim()
                                    )
                                } else {
                                    Subscription(
                                        name = name.trim(),
                                        cost = costVal,
                                        category = category.trim(),
                                        renewalDate = nextBillingDate,
                                        billingCycle = selectedCycle,
                                        isActive = true,
                                        isAutoRenew = isAutoRenew,
                                        reminderDaysInAdvance = reminderDays,
                                        paymentAccountId = selectedAccountId,
                                        paymentCardId = selectedCreditCardId,
                                        paymentMethodName = paymentMethodName.trim(),
                                        notes = notes.trim()
                                    )
                                }
                                viewModel.addSubscription(sub)
                                Toast.makeText(context, "Subscription '${sub.name}' saved with $reminderDays-day renewal alerts!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Please enter subscription name", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f).testTag("save_subscription_btn"),
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
        // Responsive Header: Title + Active Badge & Add Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = "Borrow & Lend",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant
                ) {
                    Text(
                        text = "${filteredItems.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("add_borrow_lend_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Book", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Quick Category Filter Row (Scrollable to guarantee no text truncation on 360dp widths)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "All Logs", "LENT" to "Lent (To Collect)", "BORROWED" to "Borrowed (To Pay)").forEach { (type, label) ->
                val isSelected = filterType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimaryEmerald else DarkSurfaceVariant)
                        .clickable { filterType = type }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
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
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(loan.contactName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = SmoothWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (isLent) "Lent to them" else "Borrowed from them", style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Due: $formattedDate", style = MaterialTheme.typography.labelSmall, color = AccentGold, maxLines = 1)
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
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (item.isPurchased) PrimaryEmerald.copy(alpha = 0.15f)
                        else priorityColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (item.isPurchased) "Bought" else "${item.priority} PRIORITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.isPurchased) PrimaryLightEmerald else priorityColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
        // Responsive Header: Title + Count Badge & Add Item Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = "Wishlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant
                ) {
                    Text(
                        text = "${list.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("add_wishlist_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
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
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Native Canvas-based Pie Chart
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Monthly Spending Share",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    if (totalSpending == 0.0) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No expenses yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        // Drawing canvas arcs
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
                                Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${"%,.0f".format(totalSpending)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Legend and share breakdown
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(colors[idx % colors.size])
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${viewModel.getIconForCategory(entry.key)} ${entry.key}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("₹${"%,.0f".format(entry.value)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "${((entry.value / totalSpending) * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary Insights List
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Performance & Speed Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Average Daily Spend", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text("Estimated expense pace per day", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        Text("₹${"%,.2f".format(dailyAvg)}/day", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    }

                    highestExpense?.let { h ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = RedExpense,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Peak Spending Spike",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                val merchantLabel = when {
                                    h.merchant.isNotBlank() -> h.merchant
                                    h.category.isNotBlank() -> h.category
                                    else -> "Single Expense"
                                }
                                Text(
                                    text = "$merchantLabel • ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(h.date))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RedExpense.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "₹${"%,.2f".format(h.amount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RedExpense,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Recorded Transactions", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text("Total logged entries this month", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text("${transactions.size} entries", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), maxLines = 1)
                        }
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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Credit Cards",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SmoothWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Track billing cycles, due dates & card limits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
// BANK ACCOUNTS MANAGEMENT SCREEN
// ==========================================

@Composable
fun BankAccountsScreen(viewModel: FinanceViewModel) {
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()

    var showAddBankDialog by remember { mutableStateOf(false) }
    var bankToEdit by remember { mutableStateOf<BankAccount?>(null) }
    var selectedBankForTx by remember { mutableStateOf<BankAccount?>(null) }
    var showDirectAddTxDialog by remember { mutableStateOf(false) }

    // Aggregate statistics across bank accounts
    val totalCalculatedBalance = remember(bankAccounts, allTransactions) {
        bankAccounts.sumOf { bank ->
            val bankTxs = allTransactions.filter { it.bankAccountId == bank.id }
            val incomes = bankTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expenses = bankTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            bank.initialBalance + incomes - expenses
        }
    }

    val visibleAccountsCount = remember(bankAccounts) {
        bankAccounts.count { !it.isHiddenFromSummary }
    }

    val totalInflow = remember(bankAccounts, allTransactions) {
        val bankIds = bankAccounts.map { it.id }.toSet()
        allTransactions.filter { it.bankAccountId in bankIds && it.type == "INCOME" }.sumOf { it.amount }
    }

    val totalOutflow = remember(bankAccounts, allTransactions) {
        val bankIds = bankAccounts.map { it.id }.toSet()
        allTransactions.filter { it.bankAccountId in bankIds && it.type == "EXPENSE" }.sumOf { it.amount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Responsive Header: Title + Active Count Pill & Add Bank Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = "Bank Accounts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant
                ) {
                    Text(
                        text = "${bankAccounts.size} accounts",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Button(
                onClick = {
                    bankToEdit = null
                    showAddBankDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("add_bank_account_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Bank", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            // Net Bank Balance Hero Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header with Icon, Title & Status Chip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Total Bank Balance",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${bankAccounts.size} Accounts ($visibleAccountsCount active)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = if (totalCalculatedBalance >= 0) "Net Positive" else "Negative",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalCalculatedBalance >= 0) GreenIncome else RedExpense,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Prominent Net Balance Display (Full-width, never wraps or truncates awkwardly)
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.2f", totalCalculatedBalance)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalCalculatedBalance >= 0) GreenIncome else RedExpense,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Quick Inflow / Outflow Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Total Inflow (Credits)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "+₹${String.format(Locale.getDefault(), "%,.2f", totalInflow)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenIncome,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Outflow (Debits)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "-₹${String.format(Locale.getDefault(), "%,.2f", totalOutflow)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RedExpense,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Bank Accounts List
            if (bankAccounts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "No Bank Accounts Configured",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add your bank accounts (e.g. HDFC, SBI, ICICI) to link transactions, auto-detect incoming bank SMS, or selectively hide bank data from analytics.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    bankToEdit = null
                                    showAddBankDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Your First Bank Account", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(bankAccounts, key = { "bank_${it.id}" }) { bank ->
                    val bankTxs = remember(allTransactions, bank.id) {
                        allTransactions.filter { it.bankAccountId == bank.id }
                    }
                    val bankIncomes = remember(bankTxs) {
                        bankTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
                    }
                    val bankExpenses = remember(bankTxs) {
                        bankTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    }
                    val currentBal = remember(bank.initialBalance, bankIncomes, bankExpenses) {
                        bank.initialBalance + bankIncomes - bankExpenses
                    }

                    var isExpanded by remember { mutableStateOf(false) }

                    // Bank Card Color Theme
                    val bankColor = remember(bank.bankName) {
                        val nameUpper = bank.bankName.uppercase()
                        when {
                            "HDFC" in nameUpper -> Color(0xFF0A3871)
                            "SBI" in nameUpper || "STATE BANK" in nameUpper -> Color(0xFF1A237E)
                            "ICICI" in nameUpper -> Color(0xFF8B0000)
                            "AXIS" in nameUpper -> Color(0xFF800020)
                            "KOTAK" in nameUpper -> Color(0xFFDA251D)
                            "PNB" in nameUpper || "PUNJAB" in nameUpper -> Color(0xFFA01F24)
                            "BARODA" in nameUpper || "BOB" in nameUpper -> Color(0xFFF26522)
                            else -> Color(0xFF006C4C)
                        }
                    }

                    ElevatedCard(
                        shape = RoundedCornerShape(22.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth().testTag("bank_account_card_${bank.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Top Row: Bank Icon, Name, Nickname & Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(bankColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bank.bankName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = bank.accountNickname,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (bank.accountNumberLast4.isNotBlank()) {
                                                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                                Text(
                                                    text = "•••• ${bank.accountNumberLast4}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentGold
                                                )
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = bank.accountType,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Balance & Flow Dashboard inside Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Calculated Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "₹${String.format(Locale.getDefault(), "%,.2f", currentBal)}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (currentBal >= 0) GreenIncome else RedExpense
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Opening Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("₹${String.format(Locale.getDefault(), "%,.2f", bank.initialBalance)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Credits: +₹${String.format(Locale.getDefault(), "%,.0f", bankIncomes)}", style = MaterialTheme.typography.labelSmall, color = GreenIncome, fontWeight = FontWeight.Bold)
                                        Text("Debits: -₹${String.format(Locale.getDefault(), "%,.0f", bankExpenses)}", style = MaterialTheme.typography.labelSmall, color = RedExpense, fontWeight = FontWeight.Bold)
                                        Text("${bankTxs.size} txns", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // CONTROLS SECTION: 2 Toggles (SMS Tracking & Visibility)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Toggle 1: SMS Auto-Tracking
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sms,
                                            contentDescription = null,
                                            tint = if (bank.isSmsDetectionEnabled) PrimaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "SMS Auto-Detection",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (bank.isSmsDetectionEnabled) "Listening for debits/credits ending •••• ${bank.accountNumberLast4.ifEmpty { "xxxx" }}" else "SMS from this bank are currently ignored",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = bank.isSmsDetectionEnabled,
                                        onCheckedChange = { viewModel.toggleBankSmsDetection(bank) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryEmerald, checkedTrackColor = PrimaryEmerald.copy(alpha = 0.5f)),
                                        modifier = Modifier.testTag("toggle_bank_sms_${bank.id}")
                                    )
                                }

                                // Toggle 2: Hide from Global Summary & Analytics
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (bank.isHiddenFromSummary) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = if (bank.isHiddenFromSummary) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Hide from Dashboard Analytics",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (bank.isHiddenFromSummary) "Excluded from all global spending summaries & charts" else "Included in global spending & metrics",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (bank.isHiddenFromSummary) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = bank.isHiddenFromSummary,
                                        onCheckedChange = { viewModel.toggleBankHideFromSummary(bank) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = AccentGold, checkedTrackColor = AccentGold.copy(alpha = 0.5f)),
                                        modifier = Modifier.testTag("toggle_bank_hide_${bank.id}")
                                    )
                                }
                            }

                            // Actions Row: Add Tx, Edit, Delete, View Txns
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedBankForTx = bank
                                        showDirectAddTxDialog = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1.2f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Tx", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        bankToEdit = bank
                                        showAddBankDialog = true
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Bank", modifier = Modifier.size(16.dp))
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        viewModel.requestDeleteConfirmation(
                                            title = "Delete Bank Account?",
                                            message = "Are you sure you want to delete ${bank.bankName} (${bank.accountNickname})? Associated transactions will remain but become unlinked."
                                        ) {
                                            viewModel.deleteBankAccount(bank)
                                        }
                                    },
                                    modifier = Modifier.size(38.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Bank", modifier = Modifier.size(16.dp))
                                }

                                FilledTonalIconButton(
                                    onClick = { isExpanded = !isExpanded },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand Transactions",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Expanded Transactions Section
                            if (isExpanded) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Recent Transactions (${bankTxs.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    if (bankTxs.isEmpty()) {
                                        Text("No transactions linked to this bank account yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        bankTxs.take(5).forEach { tx ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (tx.merchant.isNotBlank()) tx.merchant else tx.category,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(tx.date)),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "${if (tx.type == "INCOME") "+" else "-"}₹${String.format(Locale.getDefault(), "%,.1f", tx.amount)}",
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
    }

    // Add / Edit Bank Dialog
    if (showAddBankDialog) {
        AddBankAccountDialog(
            viewModel = viewModel,
            bankToEdit = bankToEdit,
            onDismiss = { showAddBankDialog = false }
        )
    }

    // Direct Transaction entry pre-linked to selected Bank
    if (showDirectAddTxDialog && selectedBankForTx != null) {
        val editingTransaction = Transaction(
            date = System.currentTimeMillis(),
            amount = 0.0,
            category = "General",
            subcategory = "",
            paymentMethod = "Bank",
            merchant = "",
            notes = "",
            tagsString = "",
            type = "EXPENSE",
            assetId = null,
            creditCardId = null,
            bankAccountId = selectedBankForTx?.id
        )
        AddTransactionDialog(
            viewModel = viewModel,
            transactionToEdit = editingTransaction,
            onDismiss = { showDirectAddTxDialog = false }
        )
    }
}

// Dialog for Adding / Editing a Bank Account
@Composable
fun AddBankAccountDialog(
    viewModel: FinanceViewModel,
    bankToEdit: BankAccount? = null,
    onDismiss: () -> Unit
) {
    val quickBankNames = listOf("HDFC Bank", "State Bank of India", "ICICI Bank", "Axis Bank", "Kotak Mahindra", "Punjab National Bank", "Bank of Baroda", "Union Bank")

    var bankName by remember { mutableStateOf(bankToEdit?.bankName ?: "HDFC Bank") }
    var accountNickname by remember { mutableStateOf(bankToEdit?.accountNickname ?: "") }
    var accountNumberLast4 by remember { mutableStateOf(bankToEdit?.accountNumberLast4 ?: "") }
    var accountType by remember { mutableStateOf(bankToEdit?.accountType ?: "SAVINGS") }
    var initialBalanceStr by remember { mutableStateOf(bankToEdit?.initialBalance?.toString() ?: "0.0") }
    var isSmsDetectionEnabled by remember { mutableStateOf(bankToEdit?.isSmsDetectionEnabled ?: true) }
    var isHiddenFromSummary by remember { mutableStateOf(bankToEdit?.isHiddenFromSummary ?: false) }

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
                    text = if (bankToEdit != null) "Edit Bank Account" else "Add Bank Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmoothWhite
                )

                // Quick Bank Selector Chips
                Column {
                    Text("Select Bank or Type Name", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickBankNames.forEach { qBank ->
                            val isSelected = bankName.equals(qBank, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryEmerald else DarkSurfaceVariant)
                                    .clickable { bankName = qBank }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(qBank, color = SmoothWhite, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name (e.g. HDFC Bank, SBI)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = accountNickname,
                    onValueChange = { accountNickname = it },
                    label = { Text("Account Nickname (e.g. Salary, Savings, Emergency)", color = MutedText) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                OutlinedTextField(
                    value = accountNumberLast4,
                    onValueChange = { if (it.length <= 4) accountNumberLast4 = it },
                    label = { Text("Last 4 Digits of Account (e.g. 4589)", color = MutedText) },
                    supportingText = { Text("Used to match incoming SMS debits & credits automatically", color = MutedText, fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                // Account Type Selector
                Column {
                    Text("Account Type", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SAVINGS", "CURRENT", "SALARY").forEach { type ->
                            val isSelected = accountType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryEmerald else DarkSurfaceVariant)
                                    .clickable { accountType = type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(type, color = SmoothWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = initialBalanceStr,
                    onValueChange = { initialBalanceStr = it },
                    label = { Text("Opening / Starting Balance (₹)", color = MutedText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryEmerald)
                )

                // Switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("SMS Auto-Tracking", color = SmoothWhite, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Auto-detect transactions from bank SMS", color = MutedText, style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = isSmsDetectionEnabled,
                        onCheckedChange = { isSmsDetectionEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryEmerald, checkedTrackColor = PrimaryEmerald.copy(alpha = 0.5f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Hide from Dashboard Summary", color = SmoothWhite, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Exclude this account's records from global totals", color = MutedText, style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = isHiddenFromSummary,
                        onCheckedChange = { isHiddenFromSummary = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentGold, checkedTrackColor = AccentGold.copy(alpha = 0.5f))
                    )
                }

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
                            val initBal = initialBalanceStr.toDoubleOrNull() ?: 0.0
                            val nick = if (accountNickname.isNotBlank()) accountNickname.trim() else "${bankName.trim()} Account"
                            if (bankName.isNotBlank()) {
                                val bank = if (bankToEdit != null) {
                                    bankToEdit.copy(
                                        bankName = bankName.trim(),
                                        accountNickname = nick,
                                        accountNumberLast4 = accountNumberLast4.trim(),
                                        accountType = accountType,
                                        initialBalance = initBal,
                                        isSmsDetectionEnabled = isSmsDetectionEnabled,
                                        isHiddenFromSummary = isHiddenFromSummary
                                    )
                                } else {
                                    BankAccount(
                                        bankName = bankName.trim(),
                                        accountNickname = nick,
                                        accountNumberLast4 = accountNumberLast4.trim(),
                                        accountType = accountType,
                                        initialBalance = initBal,
                                        isSmsDetectionEnabled = isSmsDetectionEnabled,
                                        isHiddenFromSummary = isHiddenFromSummary
                                    )
                                }
                                viewModel.addBankAccount(bank)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
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
// SMS AUTO-DETECTION & CATEGORIZATION UI
// ==========================================

@Composable
fun SmsDetectionBannerCard(
    hasPermission: Boolean,
    pendingCount: Int,
    onRequestPermission: () -> Unit,
    onSimulateClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (pendingCount > 0) 3.dp else 1.dp
        ),
        modifier = Modifier.fillMaxWidth().testTag("sms_detection_banner_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (pendingCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = if (pendingCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Bank SMS Auto-Detection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (pendingCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "$pendingCount NEW",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (hasPermission) "Live tracking on-device bank alerts" else "Grant permission to auto-detect bank SMS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!hasPermission) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("enable_sms_permission_btn"),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enable SMS Access", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                FilledTonalButton(
                    onClick = onSimulateClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = if (!hasPermission) Modifier.weight(1f).height(46.dp) else Modifier.fillMaxWidth().height(46.dp).testTag("simulate_sms_test_btn"),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simulate / Test Bank SMS", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_sms_card_${pending.id}")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header: Badge + Time + Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
                            Text("Bank SMS Detected", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        }
                    }

                    val dateStr = remember(pending.date) {
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(pending.date))
                    }
                    Text(text = dateStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("dismiss_pending_sms_${pending.id}")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "via ${pending.paymentMethod}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (pending.rawSender.isNotBlank()) {
                            Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = pending.rawSender,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { showRawSms = !showRawSms }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showRawSms) "Hide Original SMS" else "View Original SMS Message",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (showRawSms) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (showRawSms) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pending.rawBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Detected Category indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text(
                        text = "Detected Category:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${currentCatDef?.icon ?: "🏷️"} $selectedCategory",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onConfirm(selectedCategory, selectedSubcategory) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.3f).height(48.dp).testTag("confirm_sms_transaction_${pending.id}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onEdit,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("edit_sms_transaction_${pending.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = "Test Bank SMS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    text = "Tap a bank SMS preset or paste custom message text to test instant on-device parsing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Preset Bank SMS Samples:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { (title, sender, body) ->
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.simulateSmsReceived(sender, body)
                                    onDismiss()
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Text(
                    text = "Or Test Custom SMS Text:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = customSender,
                    onValueChange = { customSender = it },
                    label = { Text("Sender ID (e.g. HDFCBK, SBIPAY)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customBody,
                    onValueChange = { customBody = it },
                    label = { Text("SMS Message Body") },
                    placeholder = { Text("e.g. Rs 500 debited for order at Zomato...") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Button(
                    onClick = {
                        if (customBody.isNotBlank()) {
                            viewModel.simulateSmsReceived(customSender, customBody)
                            onDismiss()
                        }
                    },
                    enabled = customBody.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Parse & Detect SMS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ==========================================
// SETTINGS SCREEN (SMS Auto-Detect, Test & Permissions)
// ==========================================

@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    appLockManager: AppLockManager = AppLockManager.getInstance(LocalContext.current)
) {
    val context = LocalContext.current
    val isSmsDetectionEnabled by viewModel.isSmsDetectionEnabled.collectAsStateWithLifecycle()
    val transactions = viewModel.transactions.collectAsStateWithLifecycle()
    var showSimulateDialog by remember { mutableStateOf(false) }
    var showDeleteMonthDialog by remember { mutableStateOf(false) }
    var showClearSmsQueueDialog by remember { mutableStateOf(false) }
    var showClearTransactionsDialog by remember { mutableStateOf(false) }
    var showFactoryResetDialog by remember { mutableStateOf(false) }
    var showPdfExportDialog by remember { mutableStateOf(false) }
    var showCsvExportDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var pendingRestoreStats by remember { mutableStateOf<BackupStats?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    // PIN App Lock States
    val isPinLockEnabled by appLockManager.isPinLockEnabled.collectAsStateWithLifecycle()
    val autoLockTimeout by appLockManager.timeoutOption.collectAsStateWithLifecycle()
    val isPrivacyModeEnabled by appLockManager.isPrivacyModeEnabled.collectAsStateWithLifecycle()
    var showSetupPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }

    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val creditCards by viewModel.creditCards.collectAsStateWithLifecycle()

    val restoreFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val jsonContent = FileShareUtils.readTextFromUri(context, uri)
            if (jsonContent.isNullOrBlank()) {
                Toast.makeText(context, "Could not read backup file", Toast.LENGTH_SHORT).show()
            } else {
                val parsed = BackupEngine.parseBackupJson(jsonContent)
                if (parsed != null) {
                    pendingRestoreJson = jsonContent
                    pendingRestoreStats = parsed.second
                    showRestoreConfirmDialog = true
                } else {
                    Toast.makeText(context, "Selected file is not a valid Expense Manager backup", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Permission states
    val isReceiveSmsGranted = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    val isReadSmsGranted = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    val isNotificationGranted = remember {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        mutableStateOf(granted)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isReceiveSmsGranted.value = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        isReadSmsGranted.value = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isNotificationGranted.value = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
    }

    val allPermissionsGranted = isReceiveSmsGranted.value && isReadSmsGranted.value && isNotificationGranted.value
    val grantedCount = listOf(isReceiveSmsGranted.value, isReadSmsGranted.value, isNotificationGranted.value).count { it }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // TOP HERO HEADER
        item {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App Settings",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Security, Privacy & Data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "ON-DEVICE",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // 0. SECURITY & PIN APP LOCK (AUTO-LOCK ON MINIMIZE / 1-MIN INACTIVITY)
        item {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header & Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isPinLockEnabled) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHighest
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPinLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (isPinLockEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Security & PIN Lock",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isPinLockEnabled) GreenIncome else MutedText)
                                    )
                                    Text(
                                        text = if (isPinLockEnabled) "PROTECTED (4-DIGIT PIN)" else "UNPROTECTED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPinLockEnabled) GreenIncome else MutedText
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = isPinLockEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showSetupPinDialog = true
                                } else {
                                    showDisablePinDialog = true
                                }
                            },
                            modifier = Modifier.testTag("pin_lock_toggle_switch")
                        )
                    }

                    Text(
                        text = "Locks automatically when minimized or left inactive for 1 minute so no one browsing your phone can see bank balances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    if (isPinLockEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Sub-setting 1: Auto-lock Timeout option
                            Surface(
                                onClick = { showTimeoutDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth().testTag("auto_lock_timeout_tile")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Auto-Lock Timeout",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = autoLockTimeout.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Sub-setting 2: Change PIN
                            Surface(
                                onClick = { showChangePinDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth().testTag("change_pin_tile")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Password,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Change 4-Digit PIN",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Update your stored security PIN code",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Sub-setting 3: Privacy Mode (FLAG_SECURE)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Hide in Recent Apps",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Blanks screen snapshot in Android task switcher (FLAG_SECURE)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isPrivacyModeEnabled,
                                        onCheckedChange = { checked ->
                                            appLockManager.setPrivacyModeEnabled(checked, context as? Activity)
                                        },
                                        modifier = Modifier.testTag("privacy_mode_switch")
                                    )
                                }
                            }

                            // Sub-setting 4: Test Lock Now Button
                            OutlinedButton(
                                onClick = { appLockManager.lockNow() },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("lock_app_now_btn")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lock App Immediately", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // 1. BANK SMS AUTO-DETECTION ENGINE
        item {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header & Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSmsDetectionEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = if (isSmsDetectionEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Bank SMS Detection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isSmsDetectionEnabled) GreenIncome else RedExpense)
                                    )
                                    Text(
                                        text = if (isSmsDetectionEnabled) "Active & Listening" else "Disabled",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSmsDetectionEnabled) GreenIncome else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = isSmsDetectionEnabled,
                            onCheckedChange = { viewModel.setSmsDetectionEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                            modifier = Modifier.testTag("sms_detection_toggle")
                        )
                    }

                    // Feature highlights
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                            Text(
                                text = "Instantly parses bank debits, credits, UPI & card transactions into your spending ledger.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = GreenIncome, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                            Text(
                                text = "100% Private on-device processing. No messages or credentials leave your phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Test / Simulator trigger button (Comfortable 52dp height, M3 filled tonal styling)
                    FilledTonalButton(
                        onClick = { showSimulateDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("settings_test_sms_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Launch SMS Message Simulator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 2. SYSTEM PERMISSIONS AUDIT
        item {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "System Permissions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Background SMS detection",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (allPermissionsGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = if (allPermissionsGranted) "3 OF 3 ACTIVE" else "$grantedCount/3 GRANTED",
                                color = if (allPermissionsGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Permission 1: Receive SMS
                    ModernPermissionTile(
                        icon = Icons.Default.Inbox,
                        title = "Receive SMS",
                        subtitle = "android.permission.RECEIVE_SMS",
                        description = "Intercepts real-time bank debits and credits as they arrive",
                        isGranted = isReceiveSmsGranted.value,
                        onRequest = {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS))
                        }
                    )

                    // Permission 2: Read SMS
                    ModernPermissionTile(
                        icon = Icons.Default.Description,
                        title = "Read SMS",
                        subtitle = "android.permission.READ_SMS",
                        description = "Extracts transaction amount, merchant name & account info locally",
                        isGranted = isReadSmsGranted.value,
                        onRequest = {
                            permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS))
                        }
                    )

                    // Permission 3: Post Notifications
                    ModernPermissionTile(
                        icon = Icons.Default.NotificationsActive,
                        title = "Push Notifications",
                        subtitle = "android.permission.POST_NOTIFICATIONS",
                        description = "Alerts you immediately when a transaction is logged",
                        isGranted = isNotificationGranted.value,
                        onRequest = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                            }
                        }
                    )

                    // Grant All Button if any is missing (Comfortable 52dp height)
                    if (!allPermissionsGranted) {
                        Button(
                            onClick = {
                                val list = mutableListOf(
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    list.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(list.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("grant_all_permissions_btn")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant All Required Permissions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 3. DATA BACKUP, EXPORT & FINANCIAL REPORTS HUB
        item {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Backup & Audit Reports",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "JSON snapshot, PDF & Excel exports",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "OFFLINE & SAFE",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Feature 1: Export Full Backup (JSON)
                    DataActionTile(
                        icon = Icons.Default.Backup,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Export Full Backup (JSON)",
                        description = "Save all transactions, banks, cards, assets, custom categories, budgets & goals into an encrypted portable JSON file for Google Drive or local storage.",
                        buttonText = if (isExporting) "Generating..." else "Export JSON Backup",
                        buttonColor = MaterialTheme.colorScheme.primaryContainer,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            isExporting = true
                            viewModel.exportFullBackup(context) { success ->
                                isExporting = false
                                if (success) {
                                    Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        testTag = "export_backup_json_btn"
                    )

                    // Feature 2: Restore from Backup File (JSON)
                    DataActionTile(
                        icon = Icons.Default.SettingsBackupRestore,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "Restore from Backup File",
                        description = "Restore your complete financial state from a previously exported JSON backup file with merge or clean overwrite options.",
                        buttonText = "Select Backup File to Restore",
                        buttonColor = MaterialTheme.colorScheme.secondaryContainer,
                        textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            restoreFilePickerLauncher.launch("*/*")
                        },
                        testTag = "restore_backup_json_btn"
                    )

                    // Feature 3: Professional PDF Financial Statement
                    DataActionTile(
                        icon = Icons.Default.PictureAsPdf,
                        iconTint = RedExpense,
                        title = "Accountant & Tax PDF Statement",
                        description = "Generate an audit-ready multi-page PDF financial report with executive KPI summaries, category distribution, tax breakdown, and itemized transaction ledger.",
                        buttonText = "Generate PDF Statement",
                        buttonColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        onClick = { showPdfExportDialog = true },
                        testTag = "generate_pdf_statement_btn"
                    )

                    // Feature 4: Detailed Excel / CSV Export
                    DataActionTile(
                        icon = Icons.Default.TableChart,
                        iconTint = GreenIncome,
                        title = "Detailed Excel / CSV Export",
                        description = "Export all or filtered financial records to tabular spreadsheet format ready for Microsoft Excel, Google Sheets, or Apple Numbers.",
                        buttonText = "Export to Excel / CSV",
                        buttonColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        onClick = { showCsvExportDialog = true },
                        testTag = "export_csv_report_btn"
                    )
                }
            }
        }

        // 4. DATA MANAGEMENT & SELECTIVE PURGE HUB
        item {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Data Management",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Purge by month or factory reset",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "ROOM SQLITE",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Action 1: Delete by Month Tile
                    DataActionTile(
                        icon = Icons.Default.CalendarMonth,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Delete Transactions by Month",
                        description = "Selectively remove expenses & incomes for a specific billing cycle.",
                        buttonText = "Select Month",
                        buttonColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        onClick = { showDeleteMonthDialog = true },
                        testTag = "delete_month_data_btn"
                    )

                    // Action 2: Clear Pending SMS Queue
                    DataActionTile(
                        icon = Icons.Default.MarkChatRead,
                        iconTint = GreenIncome,
                        title = "Clear Pending SMS Queue",
                        description = "Wipe unconfirmed bank alerts awaiting your categorization.",
                        buttonText = "Clear Queue",
                        buttonColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        onClick = { showClearSmsQueueDialog = true },
                        testTag = "clear_sms_queue_btn"
                    )

                    // Action 3: Clear All Transactions & Reset Balances
                    DataActionTile(
                        icon = Icons.Default.RestartAlt,
                        iconTint = RedExpense,
                        title = "Clear All Transactions",
                        description = "Purge all ledger records & reset card balances while keeping your accounts & categories.",
                        buttonText = "Clear History",
                        buttonColor = MaterialTheme.colorScheme.errorContainer,
                        textColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = { showClearTransactionsDialog = true },
                        testTag = "clear_all_tx_btn"
                    )

                    // Action 4: Full Factory Reset (Prominent 52dp height, M3 error button)
                    Button(
                        onClick = { showFactoryResetDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("factory_reset_btn")
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Erase Everything (Factory Reset)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // 4. PRIVACY GUARANTEE NOTE
        item {
            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Zero Cloud Exposure Guarantee",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Your SMS messages and financial entries are processed and stored strictly on-device in Room SQLite.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // DIALOG 1: Delete by Month
    if (showDeleteMonthDialog) {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val allTx = transactions.value
        val availableMonths = remember(allTx) {
            allTx.map { sdf.format(Date(it.date)) }.distinct().sortedDescending()
        }
        var selectedMonth by remember { mutableStateOf(if (availableMonths.isNotEmpty()) availableMonths.first() else sdf.format(Date())) }

        AlertDialog(
            onDismissRequest = { showDeleteMonthDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                    }
                    Text("Delete Month Data", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Select a specific billing month to permanently delete all associated expenses and incomes from both local storage and cloud database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (availableMonths.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "No recorded transactions found in ledger.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Text("Available Months in Ledger:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableMonths) { m ->
                                FilterChip(
                                    selected = selectedMonth == m,
                                    onClick = { selectedMonth = m },
                                    label = { Text(m, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.error,
                                        selectedLabelColor = MaterialTheme.colorScheme.onError,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransactionsByMonth(selectedMonth) { deletedCount ->
                            Toast.makeText(context, "Deleted $deletedCount transactions for $selectedMonth (Local & Cloud)", Toast.LENGTH_LONG).show()
                            showDeleteMonthDialog = false
                        }
                    },
                    enabled = availableMonths.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Delete $selectedMonth", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMonthDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // DIALOG 2: Clear SMS Queue
    if (showClearSmsQueueDialog) {
        AlertDialog(
            onDismissRequest = { showClearSmsQueueDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MarkChatRead, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                    }
                    Text("Clear Pending SMS Queue", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to dismiss and clear all unconfirmed bank SMS alerts from the review queue?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllPendingSmsQueue {
                            Toast.makeText(context, "Pending SMS queue cleared", Toast.LENGTH_SHORT).show()
                            showClearSmsQueueDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Clear Queue", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSmsQueueDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // DIALOG 3: Clear All Transactions & Reset Balances
    if (showClearTransactionsDialog) {
        var typedConfirmation by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showClearTransactionsDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                    }
                    Text("Clear All Transactions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "This will permanently wipe ALL recorded expenses and incomes across all time from both local device and cloud sync, and reset credit card balances to ₹0.00.\n\nYour accounts, cards, and categories will remain saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Type 'CLEAR' below to confirm:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = typedConfirmation,
                        onValueChange = { typedConfirmation = it },
                        placeholder = { Text("CLEAR") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllTransactionsOnly {
                            Toast.makeText(context, "All transactions cleared from local & cloud", Toast.LENGTH_LONG).show()
                            showClearTransactionsDialog = false
                        }
                    },
                    enabled = typedConfirmation.trim().equals("CLEAR", ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Clear History", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTransactionsDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // DIALOG 4: Full Factory Reset
    if (showFactoryResetDialog) {
        var typedReset by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFactoryResetDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                    }
                    Text("Factory Reset (Erase All)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "⚠️ WARNING: This will permanently erase ALL data across local storage and cloud database:\n• All Transactions & Ledgers\n• All Bank Accounts & Credit Cards\n• All Budgets, Goals, Debts & Subscriptions\n• All Custom Categories & Wishlists\n• App Preferences & Staging Queues",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "Type 'RESET' below to confirm full wipe:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = typedReset,
                        onValueChange = { typedReset = it },
                        placeholder = { Text("RESET") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.executeFullFactoryReset {
                            Toast.makeText(context, "App completely reset (Local & Cloud)", Toast.LENGTH_LONG).show()
                            showFactoryResetDialog = false
                        }
                    },
                    enabled = typedReset.trim().equals("RESET", ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Factory Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFactoryResetDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showSimulateDialog) {
        SimulateSmsDialog(
            viewModel = viewModel,
            onDismiss = { showSimulateDialog = false }
        )
    }

    if (showPdfExportDialog) {
        PdfStatementExportDialog(
            viewModel = viewModel,
            allTransactions = transactions.value,
            bankAccounts = bankAccounts,
            creditCards = creditCards,
            onDismiss = { showPdfExportDialog = false }
        )
    }

    if (showCsvExportDialog) {
        CsvReportExportDialog(
            viewModel = viewModel,
            allTransactions = transactions.value,
            bankAccounts = bankAccounts,
            creditCards = creditCards,
            onDismiss = { showCsvExportDialog = false }
        )
    }

    if (showRestoreConfirmDialog && pendingRestoreJson != null && pendingRestoreStats != null) {
        RestoreBackupConfirmDialog(
            stats = pendingRestoreStats!!,
            onDismiss = {
                showRestoreConfirmDialog = false
                pendingRestoreJson = null
                pendingRestoreStats = null
            },
            onConfirmRestore = { isMergeMode ->
                val jsonToRestore = pendingRestoreJson ?: return@RestoreBackupConfirmDialog
                viewModel.restoreFullBackup(jsonToRestore, isMergeMode) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    showRestoreConfirmDialog = false
                    pendingRestoreJson = null
                    pendingRestoreStats = null
                }
            }
        )
    }

    // Security & PIN Lock Dialogs
    if (showSetupPinDialog) {
        SetupPinDialog(
            appLockManager = appLockManager,
            onDismiss = { showSetupPinDialog = false },
            onPinSet = { showSetupPinDialog = false }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            appLockManager = appLockManager,
            onDismiss = { showChangePinDialog = false }
        )
    }

    if (showDisablePinDialog) {
        DisablePinDialog(
            appLockManager = appLockManager,
            onDismiss = { showDisablePinDialog = false },
            onPinDisabled = { showDisablePinDialog = false }
        )
    }

    if (showTimeoutDialog) {
        AutoLockTimeoutDialog(
            appLockManager = appLockManager,
            currentTimeout = autoLockTimeout,
            onDismiss = { showTimeoutDialog = false }
        )
    }
}

@Composable
fun PdfStatementExportDialog(
    viewModel: FinanceViewModel,
    allTransactions: List<Transaction>,
    bankAccounts: List<BankAccount>,
    creditCards: List<CreditCard>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf("Current Month") }
    var selectedAccount by remember { mutableStateOf("All Accounts") }
    var selectedTag by remember { mutableStateOf("All Tags") }
    var isGenerating by remember { mutableStateOf(false) }

    val periodOptions = listOf("Current Month", "Previous Month", "FY 2025-26", "FY 2026-27", "All Time")

    // Account options
    val accountOptions = remember(bankAccounts, creditCards) {
        val list = mutableListOf("All Accounts")
        bankAccounts.forEach { list.add("Bank: ${it.bankName} (${it.accountNickname})") }
        creditCards.forEach { list.add("Card: ${it.cardName}") }
        list
    }

    // Available tags in transactions
    val availableTags = remember(allTransactions) {
        val tags = allTransactions.flatMap { it.tags }.filter { it.isNotBlank() }.distinct()
        val list = mutableListOf("All Tags")
        if (tags.any { it.contains("tax", ignoreCase = true) }) {
            list.add("#TaxDeductible")
        }
        list.addAll(tags.filterNot { it.equals("#TaxDeductible", ignoreCase = true) })
        list
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(RedExpense.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = RedExpense, modifier = Modifier.size(20.dp))
                }
                Text("Generate PDF Statement", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Generate a formal accountant-ready statement with executive KPI summaries, category share, and an itemized ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Period Selector
                Text("Statement Period:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(periodOptions) { opt ->
                        FilterChip(
                            selected = selectedPeriod == opt,
                            onClick = { selectedPeriod = opt },
                            label = { Text(opt, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Account Filter
                Text("Account Scope:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accountOptions) { opt ->
                        FilterChip(
                            selected = selectedAccount == opt,
                            onClick = { selectedAccount = opt },
                            label = { Text(opt, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Tag Filter (e.g., #TaxDeductible)
                if (availableTags.size > 1) {
                    Text("Tag & Tax Filter:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableTags) { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { selectedTag = tag },
                                label = { Text(tag, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Highlight summary preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Report Highlights:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• Executive Cash Flow & Savings Rate Cards\n• Top Spending Categories breakdown\n• Tax-deductible items flagged for filing\n• Formatted multi-page ledger with A4 pagination",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isGenerating = true
                    val cal = Calendar.getInstance()
                    val (startDate, endDate, periodLabel) = when (selectedPeriod) {
                        "Current Month" -> {
                            cal.set(Calendar.DAY_OF_MONTH, 1)
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            val start = cal.timeInMillis
                            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
                            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            Triple(start, cal.timeInMillis, monthName)
                        }
                        "Previous Month" -> {
                            cal.add(Calendar.MONTH, -1)
                            cal.set(Calendar.DAY_OF_MONTH, 1)
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            val start = cal.timeInMillis
                            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
                            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            Triple(start, cal.timeInMillis, monthName)
                        }
                        "FY 2025-26" -> {
                            cal.set(2025, Calendar.APRIL, 1, 0, 0, 0)
                            val start = cal.timeInMillis
                            cal.set(2026, Calendar.MARCH, 31, 23, 59, 59)
                            Triple(start, cal.timeInMillis, "FY 2025-26")
                        }
                        "FY 2026-27" -> {
                            cal.set(2026, Calendar.APRIL, 1, 0, 0, 0)
                            val start = cal.timeInMillis
                            cal.set(2027, Calendar.MARCH, 31, 23, 59, 59)
                            Triple(start, cal.timeInMillis, "FY 2026-27")
                        }
                        else -> Triple(null, null, "All Time")
                    }

                    val bankId = if (selectedAccount.startsWith("Bank: ")) {
                        bankAccounts.find { selectedAccount.contains(it.accountNickname) }?.id
                    } else null

                    val cardId = if (selectedAccount.startsWith("Card: ")) {
                        creditCards.find { selectedAccount.contains(it.cardName) }?.id
                    } else null

                    val tagFilter = if (selectedTag != "All Tags") selectedTag else null

                    val filter = StatementFilter(
                        title = if (selectedAccount != "All Accounts") selectedAccount else "Consolidated Financial Statement",
                        periodLabel = periodLabel,
                        startDate = startDate,
                        endDate = endDate,
                        bankAccountId = bankId,
                        creditCardId = cardId,
                        tagFilter = tagFilter
                    )

                    viewModel.exportPdfStatement(context, filter) { success ->
                        isGenerating = false
                        if (success) {
                            Toast.makeText(context, "PDF Statement generated successfully", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Generate PDF", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun CsvReportExportDialog(
    viewModel: FinanceViewModel,
    allTransactions: List<Transaction>,
    bankAccounts: List<BankAccount>,
    creditCards: List<CreditCard>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf("Current Month") }
    var selectedAccount by remember { mutableStateOf("All Accounts") }
    var selectedTag by remember { mutableStateOf("All Tags") }
    var isGenerating by remember { mutableStateOf(false) }

    val periodOptions = listOf("Current Month", "Previous Month", "FY 2025-26", "FY 2026-27", "All Time")

    val accountOptions = remember(bankAccounts, creditCards) {
        val list = mutableListOf("All Accounts")
        bankAccounts.forEach { list.add("Bank: ${it.bankName} (${it.accountNickname})") }
        creditCards.forEach { list.add("Card: ${it.cardName}") }
        list
    }

    val availableTags = remember(allTransactions) {
        val tags = allTransactions.flatMap { it.tags }.filter { it.isNotBlank() }.distinct()
        val list = mutableListOf("All Tags")
        list.addAll(tags)
        list
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GreenIncome.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = GreenIncome, modifier = Modifier.size(20.dp))
                }
                Text("Export Excel / CSV Report", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Export tabular data with dates, amounts, categories, tags, accounts, and merchant details for spreadsheet analysis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Period Selector
                Text("Export Period:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(periodOptions) { opt ->
                        FilterChip(
                            selected = selectedPeriod == opt,
                            onClick = { selectedPeriod = opt },
                            label = { Text(opt, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Account Filter
                Text("Account Scope:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accountOptions) { opt ->
                        FilterChip(
                            selected = selectedAccount == opt,
                            onClick = { selectedAccount = opt },
                            label = { Text(opt, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Tag Filter
                if (availableTags.size > 1) {
                    Text("Tag & Tax Filter:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableTags) { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { selectedTag = tag },
                                label = { Text(tag, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isGenerating = true
                    val cal = Calendar.getInstance()
                    val (startDate, endDate, periodLabel) = when (selectedPeriod) {
                        "Current Month" -> {
                            cal.set(Calendar.DAY_OF_MONTH, 1)
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            val start = cal.timeInMillis
                            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
                            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            Triple(start, cal.timeInMillis, monthName)
                        }
                        "Previous Month" -> {
                            cal.add(Calendar.MONTH, -1)
                            cal.set(Calendar.DAY_OF_MONTH, 1)
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            val start = cal.timeInMillis
                            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
                            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            Triple(start, cal.timeInMillis, monthName)
                        }
                        "FY 2025-26" -> {
                            cal.set(2025, Calendar.APRIL, 1, 0, 0, 0)
                            val start = cal.timeInMillis
                            cal.set(2026, Calendar.MARCH, 31, 23, 59, 59)
                            Triple(start, cal.timeInMillis, "FY 2025-26")
                        }
                        "FY 2026-27" -> {
                            cal.set(2026, Calendar.APRIL, 1, 0, 0, 0)
                            val start = cal.timeInMillis
                            cal.set(2027, Calendar.MARCH, 31, 23, 59, 59)
                            Triple(start, cal.timeInMillis, "FY 2026-27")
                        }
                        else -> Triple(null, null, "All Time")
                    }

                    val bankId = if (selectedAccount.startsWith("Bank: ")) {
                        bankAccounts.find { selectedAccount.contains(it.accountNickname) }?.id
                    } else null

                    val cardId = if (selectedAccount.startsWith("Card: ")) {
                        creditCards.find { selectedAccount.contains(it.cardName) }?.id
                    } else null

                    val tagFilter = if (selectedTag != "All Tags") selectedTag else null

                    val filter = StatementFilter(
                        title = "$periodLabel - $selectedAccount",
                        periodLabel = periodLabel,
                        startDate = startDate,
                        endDate = endDate,
                        bankAccountId = bankId,
                        creditCardId = cardId,
                        tagFilter = tagFilter
                    )

                    viewModel.exportCsvReport(context, filterDescription = "$periodLabel - $selectedAccount", filter = filter) { success ->
                        isGenerating = false
                        if (success) {
                            Toast.makeText(context, "CSV exported successfully", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Export CSV", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun RestoreBackupConfirmDialog(
    stats: BackupStats,
    onDismiss: () -> Unit,
    onConfirmRestore: (Boolean) -> Unit
) {
    var isMergeMode by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Text("Confirm Backup Restore", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "A valid Expense Manager backup file was detected. Review the payload contents below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Stats overview card
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Backup Date: ${stats.backupDateStr}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Transactions:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${stats.totalTransactions}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bank Accounts:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${stats.totalBankAccounts}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Credit Cards:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${stats.totalCreditCards}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Categories & Budgets:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${stats.totalCategories} cats, ${stats.totalBudgets} budgets", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Assets & Savings Goals:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${stats.totalAssets} assets, ${stats.totalSavingsGoals} goals", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Restore Mode Selection
                Text("Select Restore Mode:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                // Option 1: Merge Mode
                Surface(
                    onClick = { isMergeMode = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isMergeMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, if (isMergeMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(selected = isMergeMode, onClick = { isMergeMode = true })
                        Column {
                            Text("Merge with Existing Data", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Keeps your existing transactions and appends backup records seamlessly.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Option 2: Clean Replace Mode
                Surface(
                    onClick = { isMergeMode = false },
                    shape = RoundedCornerShape(14.dp),
                    color = if (!isMergeMode) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, if (!isMergeMode) MaterialTheme.colorScheme.error else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = !isMergeMode,
                            onClick = { isMergeMode = false },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error)
                        )
                        Column {
                            Text("Clean Overwrite (Full Mirror)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                            Text("Erases current database and mirrors this exact backup snapshot.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRestore(isMergeMode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMergeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(if (isMergeMode) "Merge Restore" else "Clean Replace", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun ModernPermissionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                    Text("Granted", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DataActionTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    buttonText: String,
    buttonColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        FilledTonalButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = buttonColor,
                contentColor = textColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag(testTag)
        ) {
            Text(text = buttonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

